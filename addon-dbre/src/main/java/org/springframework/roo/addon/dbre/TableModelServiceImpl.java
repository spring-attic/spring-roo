package org.springframework.roo.addon.dbre;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.dbre.db.DbMetadataConverter;
import org.springframework.roo.addon.dbre.db.IdentifiableTable;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataNotificationListener;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;

/**
 * Tracks all Roo entities currently on disk and their corresponding table names.
 * 
 * <p>
 * This class is not thread safe (fine for Roo, as Process Manager guarantees single threading).
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Component
@Service
public class TableModelServiceImpl implements MetadataNotificationListener, TableModelService {
	@Reference protected MetadataService metadataService;
	@Reference protected MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference protected FileManager fileManager;
	@Reference protected DbMetadataConverter dbMetadataConverter;
	private Map<IdentifiableTable, JavaType> tableNamesToTypes = new HashMap<IdentifiableTable, JavaType>();

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
	}

	public IdentifiableTable findTableIdentity(JavaType type) {
		Assert.notNull(type, "Type to locate required");
		for (IdentifiableTable identity : tableNamesToTypes.keySet()) {
			if (tableNamesToTypes.get(identity).equals(type)) {
				return identity;
			}
		}
		return null;
	}

	public JavaType findTypeForTableIdentity(IdentifiableTable identifiableTable) {
		Assert.notNull(identifiableTable, "Table identity to locate required");
		return tableNamesToTypes.get(identifiableTable);
	}

	public final void notify(String upstreamDependency, String downstreamDependency) {
		if (!MetadataIdentificationUtils.getMetadataClass(upstreamDependency).equals(MetadataIdentificationUtils.getMetadataClass(PhysicalTypeIdentifier.getMetadataIdentiferType()))) {
			return;
		}
		Assert.isTrue(MetadataIdentificationUtils.isIdentifyingInstance(upstreamDependency), "This class should only receive instance-identifying upstream dependency notifications (not " + upstreamDependency + ")");
		Assert.isNull(downstreamDependency, "This class should only receive class-wide downstream dependency notifications (not " + downstreamDependency + ")");

		// A physical Java type has changed
		JavaType javaType = PhysicalTypeIdentifier.getJavaType(upstreamDependency);
		Path path = PhysicalTypeIdentifier.getPath(upstreamDependency);

		// Acquire information about the .java file
		Assert.notNull(metadataService, "Metadata Service missing");
		MetadataItem md = metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType, path));
		PhysicalTypeMetadata governorPhysicalTypeMetadata = (PhysicalTypeMetadata) md;
		if (governorPhysicalTypeMetadata == null || !governorPhysicalTypeMetadata.isValid() || !(governorPhysicalTypeMetadata.getPhysicalTypeDetails() instanceof ClassOrInterfaceTypeDetails)) {
			// The file might have been deleted, or is of invalid syntax, or is not a class (eg might be an enum, annotation etc)
			// All we do in this case is remove it from our Map should it have ever been in there
			removeFromMapIfFound(javaType);
			return;
		}
		ClassOrInterfaceTypeDetails typeDetails = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getPhysicalTypeDetails();

		// If this remains null by the end of the method, this Java notification wasn't for an entity
		IdentifiableTable computedTableIdentity = null;

		// See if a table name has been declared
		AnnotationMetadata tableAnnotation = MemberFindingUtils.getAnnotationOfType(typeDetails.getTypeAnnotations(), new JavaType("javax.persistence.Table"));
		if (tableAnnotation != null) {
			String table = getStringAttributeValueIfProvided(tableAnnotation, "name");
			String catalog = getStringAttributeValueIfProvided(tableAnnotation, "catalog");
			String schema = getStringAttributeValueIfProvided(tableAnnotation, "schema");
			if (table != null && table.length() > 0) {
				computedTableIdentity = new IdentifiableTable(catalog, schema, table);
			}
		}

		if (computedTableIdentity == null) {
			// It could still be an entity...
			AnnotationMetadata rooEntity = MemberFindingUtils.getAnnotationOfType(typeDetails.getTypeAnnotations(), new JavaType("org.springframework.roo.addon.entity.RooEntity"));
			AnnotationMetadata jpaEntity = MemberFindingUtils.getAnnotationOfType(typeDetails.getTypeAnnotations(), new JavaType("javax.persistence.Entity"));
			if (rooEntity != null || jpaEntity != null) {
				// Calculate the table name using a presumed strategy
				computedTableIdentity = suggestTableNameForNewType(javaType);
			}
		}

		if (computedTableIdentity == null) {
			// Ensure this java type is not present in our little map (it might have once been an entity, but no longer is)
			removeFromMapIfFound(javaType);
			return;
		}

		// Put it in the map
		tableNamesToTypes.put(computedTableIdentity, javaType);
	}

	public Map<IdentifiableTable, JavaType> getAllDetectedEntities() {
		return Collections.unmodifiableMap(tableNamesToTypes);
	}
	
	private void removeFromMapIfFound(JavaType javaType) {
		IdentifiableTable identifiableTable = findTableIdentity(javaType);
		if (identifiableTable != null) {
			tableNamesToTypes.remove(identifiableTable);
		}
	}

	private String getStringAttributeValueIfProvided(AnnotationMetadata annotationMetadata, String attributeName) {
		AnnotationAttributeValue<?> val = annotationMetadata.getAttribute(new JavaSymbolName(attributeName));
		if (val != null && val instanceof StringAttributeValue) {
			StringAttributeValue sav = (StringAttributeValue) val;
			return sav.getValue();
		}
		return null;
	}

	public IdentifiableTable suggestTableNameForNewType(JavaType type) {
		return dbMetadataConverter.convertTypeToTableType(type);
	}
	
	public JavaType suggestTypeNameForNewTable(IdentifiableTable identifiableTable, JavaPackage javaPackage) {
		return dbMetadataConverter.convertTableIdentityToType(identifiableTable, javaPackage);
	}

	public JavaSymbolName suggestFieldNameForColumn(String columnName) {
		return new JavaSymbolName(dbMetadataConverter.getFieldName(columnName));
	}

	public String dump() {
		return tableNamesToTypes.toString();
	}
}
