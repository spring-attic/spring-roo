package org.springframework.roo.addon.layers.repository.mongo;

import static org.springframework.roo.model.JpaJavaType.ENTITY;

import java.lang.reflect.Modifier;
import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.uaa.client.util.Assert;

/**
 * Creates metadata for domain entity ITDs (annotated with {@link RooMongoEntity}.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public class MongoEntityMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	
	// Constants
	private static final String PROVIDES_TYPE_STRING = MongoEntityMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	
	/**
	 * Constructor
	 *
	 * @param identifier the identifier for this item of metadata (required)
	 * @param aspectName the Java type of the ITD (required)
	 * @param governorPhysicalTypeMetadata the governor, which is expected to contain a {@link ClassOrInterfaceTypeDetails} (required)
	 * @param idType the type of the entity's identifier field (required)
	 */
	public MongoEntityMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, JavaType idType) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.notNull(idType, "Id type required");
		
		builder.addAnnotation(new AnnotationMetadataBuilder(ENTITY));
		FieldMetadata idField = getIdField(idType);
		if (idField != null) {
			builder.addField(idField);
			builder.addMethod(getAccessorMethod(idField.getFieldName(), idField.getFieldType()));
			builder.addMethod(getMutatorMethod(idField.getFieldName(), idField.getFieldType()));
		}
		
		// Build the ITD
		itdTypeDetails = builder.build();
	}
	
	private FieldMetadata getIdField(JavaType idType) {
		// Try to locate an existing field with SPRING_DATA_ID
		final List<FieldMetadata> idFields = MemberFindingUtils.getFieldsWithAnnotation(governorTypeDetails, SpringJavaType.DATA_ID);
		if (!idFields.isEmpty()) {
			return idFields.get(0);
		}
		JavaSymbolName idFieldName = governorTypeDetails.getUniqueFieldName("id", false);
		FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, idFieldName, idType, null);
		fieldBuilder.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.DATA_ID));
		return fieldBuilder.build();
	}

	public static String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}

	public static String createIdentifier(JavaType javaType, Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static JavaType getJavaType(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static Path getPath(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
	
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", valid);
		tsc.append("aspectName", aspectName);
		tsc.append("destinationType", destination);
		tsc.append("governor", governorPhysicalTypeMetadata.getId());
		tsc.append("itdTypeDetails", itdTypeDetails);
		return tsc.toString();
	}
}
