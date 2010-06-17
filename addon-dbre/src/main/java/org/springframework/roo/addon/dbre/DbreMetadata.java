package org.springframework.roo.addon.dbre;

import java.lang.reflect.Modifier;
import java.util.List;

import org.springframework.roo.addon.dbre.db.IdentifiableTable;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.DefaultFieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Metadata for {@link RooDbManaged}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class DbreMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	private static final String PROVIDES_TYPE_STRING = DbreMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private static final JavaType ID = new JavaType("javax.persistence.Id");
	private static final JavaType EMBEDDED_ID = new JavaType("javax.persistence.EmbeddedId");

	private ProjectMetadata projectMetadata;
	private EntityMetadata entityMetadata;
	private TableModelService tableModelService;
	private Document dbre;

	public DbreMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, ProjectMetadata projectMetadata, EntityMetadata entityMetadata, TableModelService tableModelService, Document dbre) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");

		this.projectMetadata = projectMetadata;
		this.entityMetadata = entityMetadata;
		this.tableModelService = tableModelService;
		this.dbre = dbre;

		// Process values from the annotation, if present
		AnnotationMetadata annotation = MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, new JavaType(RooDbManaged.class.getName()));
		if (annotation != null) {
			AutoPopulationUtils.populate(this, annotation);
		}

		// Add fields with their respective accessors and mutators
		Element dbMetadataElement = dbre.getDocumentElement();
		JavaType javaType = governorPhysicalTypeMetadata.getPhysicalTypeDetails().getName();
		IdentifiableTable identifiableTable = tableModelService.findTableIdentity(javaType);
		if (identifiableTable == null) {
			identifiableTable = tableModelService.suggestTableNameForNewType(javaType);
		}
		List<Element> columns = XmlUtils.findElements(getSpecifiedTableXPath(identifiableTable.getTable()) + "/column", dbMetadataElement);
		for (Element columnElement : columns) {
			// Check for existing declared fields in the governor or in the entity metadata
			if (!hasField(columnElement, javaType)) {
				builder.addField(getField(columnElement, javaType));
			}
		}

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	private boolean hasField(Element columnElement, JavaType javaType) {
		JavaSymbolName fieldName = tableModelService.suggestFieldNameForColumn(columnElement.getAttribute("name"));
	//	System.out.println("column name = " + columnElement.getAttribute("name") + ", field name = " + fieldName.getSymbolName());
		// Check governor for field
		if (MemberFindingUtils.getField(governorTypeDetails, fieldName) != null) {
		//	System.out.println("found on governor " + fieldName + " - not adding to ITD");
			return true;
		}
		
		// Check entity ITD for field
		List<? extends FieldMetadata> itdFields = entityMetadata.getItdTypeDetails().getDeclaredFields();
		for (FieldMetadata field : itdFields) {
			if (field.getFieldName().equals(fieldName)) {
		//		System.out.println("found on entity " + fieldName + " - not adding to ITD");
				return true;
			}
		}
		
		// Try to locate an existing field with @javax.persistence.Id
//		List<FieldMetadata> foundId = MemberFindingUtils.getFieldsWithAnnotation(governorTypeDetails, ID);
//		if (foundId.size() > 0) {
//			Assert.isTrue(foundId.size() == 1, "More than one field was annotated with @javax.persistence.Id in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
//			return true;
//		}
//		foundId = MemberFindingUtils.getFieldsWithAnnotation(entityMetadata., ID);
//		if (foundId.size() > 0) {
//			Assert.isTrue(foundId.size() == 1, "More than one field was annotated with @javax.persistence.Id in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
//			return true;
//		}
		
		// Try to locate an existing field with @javax.persistence.EmbeddedId
//		List<FieldMetadata> foundEmbeddedId = MemberFindingUtils.getFieldsWithAnnotation(governorTypeDetails, EMBEDDED_ID);
//		if (foundEmbeddedId.size() > 0) {
//			Assert.isTrue(foundEmbeddedId.size() == 1, "More than one field was annotated with @javax.persistence.EmbeddedId in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
//			return true;
//		}
		return false;
	}

	private FieldMetadata getField(Element columnElement, JavaType javaType) {
		JavaSymbolName fieldName = new JavaSymbolName(columnElement.getAttribute("name"));
		JavaType fieldType = new JavaType(columnElement.getAttribute("type"));
		return new DefaultFieldMetadata(getId(), Modifier.PRIVATE, fieldName, fieldType, null, null);
	}

	public static final String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}

	public static final String createIdentifier(JavaType javaType, Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static final JavaType getJavaType(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static final Path getPath(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
	
	private String getSpecifiedTableXPath(String tableName) {
		return DbrePath.DBRE_TABLE_XPATH.getPath() + "[@name = '" + tableName + "']";
	}
}
