package org.springframework.roo.addon.layers.repository.mongo;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.StringUtils;
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
		
		FieldMetadata idField = getIdField(idType);
		builder.addAnnotation(new AnnotationMetadataBuilder("javax.persistence.Entity"));
		builder.addField(getIdField(idType));
		builder.addMethod(getIdAccessor(idField));
		builder.addMethod(getIdMutator(idField));
		
		// Build the ITD
		itdTypeDetails = builder.build();
	}
	
	private FieldMetadata getIdField(JavaType idType) {
		// Try to locate an existing field with SPRING_DATA_ID
		final List<FieldMetadata> idFields = MemberFindingUtils.getFieldsWithAnnotation(governorTypeDetails, SpringJavaType.DATA_ID);
		if (!idFields.isEmpty()) {
			return idFields.get(0);
		}
		JavaSymbolName idFieldName = new JavaSymbolName("id");
		if (MemberFindingUtils.getField(governorTypeDetails, idFieldName) != null) {
			idFieldName = new JavaSymbolName("id_");
		}
		FieldMetadataBuilder fieldMetadataBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, idFieldName, idType, null);
		fieldMetadataBuilder.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.DATA_ID));
		return fieldMetadataBuilder.build();
	}
	
	private MethodMetadata getIdAccessor(FieldMetadata idField) {
		JavaSymbolName idAccessorName = new JavaSymbolName("get" + StringUtils.capitalize(idField.getFieldName().getSymbolName()));
		if (MemberFindingUtils.getMethod(governorTypeDetails, idAccessorName, new ArrayList<JavaType>()) != null) {
			return null;
		}
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return " + idField.getFieldName().getSymbolName() + ";");
		return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, idAccessorName, idField.getFieldType(), bodyBuilder).build();
	}
	
	private MethodMetadata getIdMutator(FieldMetadata idField) {
		JavaSymbolName idMutatorName = new JavaSymbolName("set" + StringUtils.capitalize(idField.getFieldName().getSymbolName()));
		List<JavaType> paramTypes = Arrays.asList(idField.getFieldType());
		if (MemberFindingUtils.getMethod(governorTypeDetails, idMutatorName, paramTypes) != null) {
			return null;
		}
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("this." + idField.getFieldName().getSymbolName() + " = " + idField.getFieldName().getSymbolName() + ";");
		return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, idMutatorName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(paramTypes), Arrays.asList(idField.getFieldName()), bodyBuilder).build();
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
