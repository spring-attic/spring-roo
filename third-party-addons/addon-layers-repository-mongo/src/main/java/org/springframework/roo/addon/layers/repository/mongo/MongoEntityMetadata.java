package org.springframework.roo.addon.layers.repository.mongo;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
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
import org.springframework.roo.project.LogicalPath;

/**
 * Creates metadata for domain entity ITDs (annotated with
 * {@link RooMongoEntity}.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public class MongoEntityMetadata extends
        AbstractItdTypeDetailsProvidingMetadataItem {

    private static final String PROVIDES_TYPE_STRING = MongoEntityMetadata.class
            .getName();
    private static final String PROVIDES_TYPE = MetadataIdentificationUtils
            .create(PROVIDES_TYPE_STRING);

    public static String createIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return PhysicalTypeIdentifierNamingUtils.createIdentifier(
                PROVIDES_TYPE_STRING, javaType, path);
    }

    public static JavaType getJavaType(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getJavaType(
                PROVIDES_TYPE_STRING, metadataIdentificationString);
    }

    public static String getMetadataIdentiferType() {
        return PROVIDES_TYPE;
    }

    public static LogicalPath getPath(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING,
                metadataIdentificationString);
    }

    public static boolean isValid(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING,
                metadataIdentificationString);
    }

    private MemberDetails memberDetails;
    private MongoEntityMetadata parent;
    private JavaType idType;
    private FieldMetadata idField;

    /**
     * Constructor
     * 
     * @param identifier the identifier for this item of metadata (required)
     * @param aspectName the Java type of the ITD (required)
     * @param governorPhysicalTypeMetadata the governor, which is expected to
     *            contain a {@link ClassOrInterfaceTypeDetails} (required)
     * @param parent
     * @param idType the type of the entity's identifier field (required)
     * @param governorMemberDetails the member details of the entity
     */
    public MongoEntityMetadata(final String identifier,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            MongoEntityMetadata parent, final JavaType idType,
            final MemberDetails memberDetails) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
        Validate.notNull(idType, "Id type required");
        Validate.notNull(memberDetails, "Entity MemberDetails required");

        if (!isValid()) {
            return;
        }

        this.memberDetails = memberDetails;
        this.parent = parent;
        this.idType = idType;

        builder.addAnnotation(getTypeAnnotation(SpringJavaType.PERSISTENT));

        idField = getIdentifierField();
        if (idField != null) {
            builder.addField(idField);
            builder.addMethod(getIdentifierAccessor());
            builder.addMethod(getIdentifierMutator());
        }

        // Build the ITD
        itdTypeDetails = builder.build();
    }

    private MethodMetadataBuilder getIdentifierAccessor() {
        if (parent != null) {
            final MethodMetadataBuilder parentIdAccessor = parent
                    .getIdentifierAccessor();
            if (parentIdAccessor != null
                    && parentIdAccessor.getReturnType().equals(idType)) {
                return parentIdAccessor;
            }
        }

        JavaSymbolName requiredAccessorName = BeanInfoUtils
                .getAccessorMethodName(idField);

        // See if the user provided the field
        if (!getId().equals(idField.getDeclaredByMetadataId())) {
            // Locate an existing accessor
            final MethodMetadata method = memberDetails.getMethod(
                    requiredAccessorName, new ArrayList<JavaType>());
            if (method != null) {
                if (Modifier.isPublic(method.getModifier())) {
                    // Method exists and is public so return it
                    return new MethodMetadataBuilder(method);
                }

                // Method is not public so make the required accessor name
                // unique
                requiredAccessorName = new JavaSymbolName(
                        requiredAccessorName.getSymbolName() + "_");
            }
        }

        // We declared the field in this ITD, so produce a public accessor for
        // it
        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("return this."
                + idField.getFieldName().getSymbolName() + ";");

        return new MethodMetadataBuilder(getId(), Modifier.PUBLIC,
                requiredAccessorName, idField.getFieldType(), bodyBuilder);
    }

    private FieldMetadata getIdentifierField() {
        if (parent != null) {
            final FieldMetadata parentIdField = parent.getIdentifierField();
            if (parentIdField.getFieldType().equals(idType)) {
                return parentIdField;
            }
        }

        // Try to locate an existing field with DATA_ID
        final List<FieldMetadata> idFields = governorTypeDetails
                .getFieldsWithAnnotation(SpringJavaType.DATA_ID);
        if (!idFields.isEmpty()) {
            return idFields.get(0);
        }
        final JavaSymbolName idFieldName = governorTypeDetails
                .getUniqueFieldName("id");
        final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(
                getId(), Modifier.PRIVATE, idFieldName, idType, null);
        fieldBuilder.addAnnotation(new AnnotationMetadataBuilder(
                SpringJavaType.DATA_ID));
        return fieldBuilder.build();
    }

    private MethodMetadataBuilder getIdentifierMutator() {
        if (parent != null) {
            final MethodMetadataBuilder parentIdMutator = parent
                    .getIdentifierMutator();
            if (parentIdMutator != null
                    && parentIdMutator.getParameterTypes().get(0).getJavaType()
                            .equals(idType)) {
                return parentIdMutator;
            }
        }

        JavaSymbolName requiredMutatorName = BeanInfoUtils
                .getMutatorMethodName(idField);

        final List<JavaType> parameterTypes = Arrays.asList(idField
                .getFieldType());
        final List<JavaSymbolName> parameterNames = Arrays
                .asList(new JavaSymbolName("id"));

        // See if the user provided the field
        if (!getId().equals(idField.getDeclaredByMetadataId())) {
            // Locate an existing mutator
            final MethodMetadata method = memberDetails.getMethod(
                    requiredMutatorName, parameterTypes);
            if (method != null) {
                if (Modifier.isPublic(method.getModifier())) {
                    // Method exists and is public so return it
                    return new MethodMetadataBuilder(method);
                }

                // Method is not public so make the required mutator name unique
                requiredMutatorName = new JavaSymbolName(
                        requiredMutatorName.getSymbolName() + "_");
            }
        }

        // We declared the field in this ITD, so produce a public mutator for it
        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("this."
                + idField.getFieldName().getSymbolName() + " = id;");

        return new MethodMetadataBuilder(getId(), Modifier.PUBLIC,
                requiredMutatorName, JavaType.VOID_PRIMITIVE,
                AnnotatedJavaType.convertFromJavaTypes(parameterTypes),
                parameterNames, bodyBuilder);
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("identifier", getId());
        builder.append("valid", valid);
        builder.append("aspectName", aspectName);
        builder.append("destinationType", destination);
        builder.append("governor", governorPhysicalTypeMetadata.getId());
        builder.append("itdTypeDetails", itdTypeDetails);
        return builder.toString();
    }
}
