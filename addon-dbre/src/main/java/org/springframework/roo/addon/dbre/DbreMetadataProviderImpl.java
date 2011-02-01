package org.springframework.roo.addon.dbre;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.dbre.model.Database;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.entity.IdentifierMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * Provides {@link DbreMetadata}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component(immediate = true)
@Service
public class DbreMetadataProviderImpl extends AbstractItdMetadataProvider implements DbreMetadataProvider {
	@Reference private DbreModelService dbreModelService;
	@Reference private TypeLocationService typeLocationService;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(new JavaType(RooDbManaged.class.getName()));
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(new JavaType(RooDbManaged.class.getName()));
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return DbreMetadata.createIdentifier(javaType, path);
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = DbreMetadata.getJavaType(metadataIdentificationString);
		Path path = DbreMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {		
		// We know governor type details are non-null and can be safely cast
		JavaType javaType = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails().getName();

		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA));
		if (entityMetadata == null) {
			return null;
		}
		List<? extends FieldMetadata> entityFields = entityMetadata.getMemberHoldingTypeDetails().getDeclaredFields();
		List<? extends MethodMetadata> entityMethods = entityMetadata.getMemberHoldingTypeDetails().getDeclaredMethods();
		
		List<FieldMetadata> identifierFields = new LinkedList<FieldMetadata>();
		String identifierMetadataMid = IdentifierMetadata.createIdentifier(entityMetadata.getIdentifierField().getFieldType(), Path.SRC_MAIN_JAVA);
		IdentifierMetadata identifierMetadata = (IdentifierMetadata) metadataService.get(identifierMetadataMid);
		if (identifierMetadata != null) {
			identifierFields.addAll(identifierMetadata.getFields());
		}

		// Abort if the database couldn't be deserialized. This can occur if the dbre xml file has been deleted or is empty.
		Database database = dbreModelService.getDatabase(null);
		if (database == null) {
			return null;
		}

		// Search for database-managed entities
		Set<ClassOrInterfaceTypeDetails> managedEntities = typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(new JavaType(RooDbManaged.class.getName()));

		return new DbreMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, entityFields, entityMethods, entityMetadata.getVersionField(), identifierFields, managedEntities, database);
	}

	public String getItdUniquenessFilenameSuffix() {
		return "DbManaged";
	}

	public String getProvidesType() {
		return DbreMetadata.getMetadataIdentiferType();
	}
}
