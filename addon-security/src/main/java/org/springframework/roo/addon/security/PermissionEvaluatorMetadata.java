package org.springframework.roo.addon.security;

import static java.lang.reflect.Modifier.PUBLIC;
import static org.springframework.roo.model.SpringJavaType.AUTHENTICATION;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

public class PermissionEvaluatorMetadata extends
        AbstractItdTypeDetailsProvidingMetadataItem {
    private static final String PROVIDES_TYPE_STRING = PermissionEvaluatorMetadata.class
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

    protected PermissionEvaluatorMetadata(String identifier,
            JavaType aspectName,
            PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final MemberDetails governorDetails) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);

        List<JavaType> hasPermissionParameterTypes = new ArrayList<JavaType>();
        hasPermissionParameterTypes.add(AUTHENTICATION);
        hasPermissionParameterTypes.add(JavaType.OBJECT);
        hasPermissionParameterTypes.add(JavaType.OBJECT);

        JavaSymbolName methodName = new JavaSymbolName("hasPermission");
        if (!governorDetails.isMethodDeclaredByAnother(methodName,
                hasPermissionParameterTypes, getId())) {
            final InvocableMemberBodyBuilder hasPermissionBodyBuilder = new InvocableMemberBodyBuilder();
            hasPermissionBodyBuilder.append("\n\treturn true;\n");

            List<JavaSymbolName> hasPermissionParameterNames = new ArrayList<JavaSymbolName>();
            hasPermissionParameterNames
                    .add(new JavaSymbolName("authentication"));
            hasPermissionParameterNames.add(new JavaSymbolName("targetObject"));
            hasPermissionParameterNames.add(new JavaSymbolName("permission"));

            MethodMetadataBuilder hasPermissionMethodMetadataBuilder = new MethodMetadataBuilder(
                    getId(), PUBLIC, methodName, JavaType.BOOLEAN_PRIMITIVE,
                    AnnotatedJavaType
                            .convertFromJavaTypes(hasPermissionParameterTypes),
                    hasPermissionParameterNames, hasPermissionBodyBuilder);

            builder.addMethod(hasPermissionMethodMetadataBuilder.build());
        }

        List<JavaType> hasPermissionParameterTypes2 = new ArrayList<JavaType>();
        hasPermissionParameterTypes2.add(AUTHENTICATION);
        hasPermissionParameterTypes2.add(JavaType.SERIALIZABLE);
        hasPermissionParameterTypes2.add(JavaType.STRING);
        hasPermissionParameterTypes2.add(JavaType.OBJECT);

        if (!governorDetails.isMethodDeclaredByAnother(methodName,
                hasPermissionParameterTypes2, getId())) {
            final InvocableMemberBodyBuilder hasPermissionBodyBuilder2 = new InvocableMemberBodyBuilder();
            hasPermissionBodyBuilder2.append("\n\treturn true;\n");

            List<JavaSymbolName> hasPermissionParameterNames2 = new ArrayList<JavaSymbolName>();
            hasPermissionParameterNames2.add(new JavaSymbolName(
                    "authentication"));
            hasPermissionParameterNames2.add(new JavaSymbolName("targetId"));
            hasPermissionParameterNames2.add(new JavaSymbolName("targetType"));
            hasPermissionParameterNames2.add(new JavaSymbolName("permission"));

            MethodMetadataBuilder hasPermissionMethodMetadataBuilder2 = new MethodMetadataBuilder(
                    getId(),
                    PUBLIC,
                    new JavaSymbolName("hasPermission"),
                    JavaType.BOOLEAN_PRIMITIVE,
                    AnnotatedJavaType
                            .convertFromJavaTypes(hasPermissionParameterTypes2),
                    hasPermissionParameterNames2, hasPermissionBodyBuilder2);

            builder.addMethod(hasPermissionMethodMetadataBuilder2.build());

            itdTypeDetails = builder.build();
        }
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
