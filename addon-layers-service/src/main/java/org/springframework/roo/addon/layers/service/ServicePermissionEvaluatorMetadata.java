package org.springframework.roo.addon.layers.service;

import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PUBLIC;
import static org.springframework.roo.model.SpringJavaType.AUTHENTICATION;
import static org.springframework.roo.model.SpringJavaType.AUTOWIRED;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

public class ServicePermissionEvaluatorMetadata extends
        AbstractItdTypeDetailsProvidingMetadataItem {
    private static final String PROVIDES_TYPE_STRING = ServicePermissionEvaluatorMetadata.class
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

    protected ServicePermissionEvaluatorMetadata(String identifier,
            JavaType aspectName,
            PhysicalTypeMetadata governorPhysicalTypeMetadata,
            Map<String, ClassOrInterfaceTypeDetails> servicePermissions) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.append("\n");
        bodyBuilder.append("\tif(!(permission instanceof String))\n");
        bodyBuilder.append("\t{\n");
        bodyBuilder.append("\t\treturn false;\n");
        bodyBuilder.append("\t}\n\n");
        bodyBuilder
                .append("\tString[] arr = ((String)permission).split(\":\");\n\n");
        bodyBuilder.append("\tif(arr.length < 2)\n");
        bodyBuilder.append("\t{\n");
        bodyBuilder.append("\t\treturn false;\n");
        bodyBuilder.append("\t}\n\n");
        bodyBuilder.append("\tString serviceName = arr[0];\n");
        bodyBuilder.append("\tpermission = arr[1];\n\n");

        for (final Entry<String, ClassOrInterfaceTypeDetails> entry : servicePermissions
                .entrySet()) {
            final String serviceName = entry.getKey();
            final ClassOrInterfaceTypeDetails servicePermission = entry
                    .getValue();
            String servicePermissionName = servicePermission.getName()
                    .getSimpleTypeName();
            String lowerCaseCharacter = servicePermissionName.substring(0, 1)
                    .toLowerCase();
            String fieldName = lowerCaseCharacter
                    + servicePermissionName.substring(1);

            final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
            annotations.add(new AnnotationMetadataBuilder(AUTOWIRED));

            FieldMetadataBuilder fieldMetadataBuilder = new FieldMetadataBuilder(
                    getId(), PRIVATE, annotations,
                    new JavaSymbolName(fieldName), servicePermission.getName());
            builder.addField(fieldMetadataBuilder.build());

            bodyBuilder.append("\tif(serviceName.equals(\"" + serviceName
                    + "\"))\n");
            bodyBuilder
                    .append("\t\treturn "
                            + fieldName
                            + ".isAllowed(authentication, targetObject, permission);\n\n");
        }

        bodyBuilder.append("\treturn false;\n");

        List<JavaType> parameterTypes = new ArrayList<JavaType>();
        parameterTypes.add(AUTHENTICATION);
        parameterTypes.add(JavaType.OBJECT);
        parameterTypes.add(JavaType.OBJECT);

        List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
        parameterNames.add(new JavaSymbolName("authentication"));
        parameterNames.add(new JavaSymbolName("targetObject"));
        parameterNames.add(new JavaSymbolName("permission"));

        MethodMetadataBuilder methodMetadataBuilder = new MethodMetadataBuilder(
                getId(), PUBLIC, new JavaSymbolName("hasPermission"),
                JavaType.BOOLEAN_PRIMITIVE,
                AnnotatedJavaType.convertFromJavaTypes(parameterTypes),
                parameterNames, bodyBuilder);

        builder.addMethod(methodMetadataBuilder.build());

        final InvocableMemberBodyBuilder bodyBuilder2 = new InvocableMemberBodyBuilder();
        bodyBuilder2.append("\n\treturn false;\n");

        List<JavaType> parameterTypes2 = new ArrayList<JavaType>();
        parameterTypes2.add(AUTHENTICATION);
        parameterTypes2.add(JavaType.SERIALIZABLE);
        parameterTypes2.add(JavaType.STRING);
        parameterTypes2.add(JavaType.OBJECT);

        List<JavaSymbolName> parameterNames2 = new ArrayList<JavaSymbolName>();
        parameterNames2.add(new JavaSymbolName("authentication"));
        parameterNames2.add(new JavaSymbolName("targetId"));
        parameterNames2.add(new JavaSymbolName("targetType"));
        parameterNames2.add(new JavaSymbolName("permission"));

        MethodMetadataBuilder methodMetadataBuilder2 = new MethodMetadataBuilder(
                getId(), PUBLIC, new JavaSymbolName("hasPermission"),
                JavaType.BOOLEAN_PRIMITIVE,
                AnnotatedJavaType.convertFromJavaTypes(parameterTypes2),
                parameterNames2, bodyBuilder2);

        builder.addMethod(methodMetadataBuilder2.build());

        itdTypeDetails = builder.build();
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
