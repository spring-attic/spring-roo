package org.springframework.roo.addon.layers.repository.mongo;

import static org.springframework.roo.model.JpaJavaType.ENTITY;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.scanner.MemberDetails;
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

	// Fields
	private final MemberDetails entityMemberDetails;

	/**
	 * Constructor
	 *
	 * @param identifier the identifier for this item of metadata (required)
	 * @param aspectName the Java type of the ITD (required)
	 * @param governorPhysicalTypeMetadata the governor, which is expected to contain a {@link ClassOrInterfaceTypeDetails} (required)
	 * @param idType the type of the entity's identifier field (required)
	 * @param governorMemberDetails the member details of the entity
	 */
	public MongoEntityMetadata(final String identifier, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final JavaType idType, final MemberDetails entityMemberDetails) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.notNull(idType, "Id type required");
		Assert.notNull(entityMemberDetails, "Entity MemberDetails required");

		this.entityMemberDetails = entityMemberDetails;

		builder.addAnnotation(getTypeAnnotation(ENTITY));

		FieldMetadata idField = getIdentifierField(idType);
		if (idField != null) {
			builder.addField(idField);
			builder.addMethod(getIdentifierAccessor(idField));
			builder.addMethod(getIdentifierMutator(idField));
		}

		// Build the ITD
		itdTypeDetails = builder.build();
	}

	private FieldMetadata getIdentifierField(final JavaType idType) {
		// Try to locate an existing field with SPRING_DATA_ID
		final List<FieldMetadata> idFields = governorTypeDetails.getFieldsWithAnnotation(SpringJavaType.DATA_ID);
		if (!idFields.isEmpty()) {
			return idFields.get(0);
		}
		JavaSymbolName idFieldName = governorTypeDetails.getUniqueFieldName("id", false);
		FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, idFieldName, idType, null);
		fieldBuilder.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.DATA_ID));
		return fieldBuilder.build();
	}

	private MethodMetadata getIdentifierAccessor(final FieldMetadata idField) {
		JavaSymbolName requiredAccessorName = BeanInfoUtils.getAccessorMethodName(idField);

		// See if the user provided the field
		if (!getId().equals(idField.getDeclaredByMetadataId())) {
			// Locate an existing accessor
			final MethodMetadata method = entityMemberDetails.getMethod(requiredAccessorName, new ArrayList<JavaType>());
			if (method != null) {
				if (Modifier.isPublic(method.getModifier())) {
					// Method exists and is public so return it
					return method;
				}

				// Method is not public so make the required accessor name unique
				requiredAccessorName = new JavaSymbolName(requiredAccessorName.getSymbolName() + "_");
			}
		}

		// We declared the field in this ITD, so produce a public accessor for it
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return this." + idField.getFieldName().getSymbolName() + ";");

		return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, requiredAccessorName, idField.getFieldType(), bodyBuilder).build();
	}

	private MethodMetadata getIdentifierMutator(final FieldMetadata idField) {
		JavaSymbolName requiredMutatorName = BeanInfoUtils.getMutatorMethodName(idField);

		final List<JavaType> parameterTypes = Arrays.asList(idField.getFieldType());
		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("id"));

		// See if the user provided the field
		if (!getId().equals(idField.getDeclaredByMetadataId())) {
			// Locate an existing mutator
			final MethodMetadata method = entityMemberDetails.getMethod(requiredMutatorName, parameterTypes);
			if (method != null) {
				if (Modifier.isPublic(method.getModifier())) {
					// Method exists and is public so return it
					return method;
				}

				// Method is not public so make the required mutator name unique
				requiredMutatorName = new JavaSymbolName(requiredMutatorName.getSymbolName() + "_");
			}
		}

		// We declared the field in this ITD, so produce a public mutator for it
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("this." + idField.getFieldName().getSymbolName() + " = id;");

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, requiredMutatorName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	public static String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}

	public static String createIdentifier(final JavaType javaType, final Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static JavaType getJavaType(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static Path getPath(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	@Override
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
