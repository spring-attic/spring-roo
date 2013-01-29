package org.springframework.roo.addon.property.editor;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooEditor}.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
public class EditorMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

    private static final String PROVIDES_TYPE_STRING = EditorMetadata.class
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

    public EditorMetadata(final String identifier, final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final JavaType javaType, final JavaType idType,
            final MethodMetadata identifierAccessorMethod,
            final MethodMetadata findMethod) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
        Validate.isTrue(
                isValid(identifier),
                "Metadata identification string '%s' does not appear to be a valid",
                identifier);
        Validate.notNull(javaType, "Java type required");
        Validate.notNull(idType, "Identifier field metadata required");
        Validate.notNull(identifierAccessorMethod,
                "Identifier accessor metadata required");

        if (!isValid() || findMethod == null) {
            valid = false;
            return;
        }

        // Only make the ITD cause PropertyEditorSupport to be subclasses if the
        // governor doesn't already subclass it
        final JavaType requiredSuperclass = JdkJavaType.PROPERTY_EDITOR_SUPPORT;
        if (!governorTypeDetails.extendsType(requiredSuperclass)) {
            builder.addImplementsType(requiredSuperclass);
        }

        builder.addField(getField());
        builder.addMethod(getGetAsTextMethod(javaType, identifierAccessorMethod));
        builder.addMethod(getSetAsTextMethod(javaType, idType, findMethod));

        // Create a representation of the desired output ITD
        itdTypeDetails = builder.build();
    }

    private FieldMetadataBuilder getField() {
        final JavaSymbolName fieldName = new JavaSymbolName("typeConverter");

        // Locate user-defined field
        final FieldMetadata userField = governorTypeDetails.getField(fieldName);
        final JavaType fieldType = SpringJavaType.SIMPLE_TYPE_CONVERTER;
        if (userField != null) {
            Validate.isTrue(userField.getFieldType().equals(fieldType),
                    "Field '%s' on '%s' must be of type '%s'", fieldName,
                    destination, fieldType.getNameIncludingTypeParameters());
            return new FieldMetadataBuilder(userField);
        }

        return new FieldMetadataBuilder(getId(), Modifier.PRIVATE, fieldName,
                fieldType, "new " + fieldType + "()");
    }

    private MethodMetadataBuilder getGetAsTextMethod(final JavaType javaType,
            final MethodMetadata identifierAccessorMethod) {
        final JavaType returnType = JavaType.STRING;
        final JavaSymbolName methodName = new JavaSymbolName("getAsText");
        final JavaType[] parameterTypes = {};

        // Locate user-defined method
        final MethodMetadata userMethod = getGovernorMethod(methodName,
                parameterTypes);
        if (userMethod != null) {
            Validate.isTrue(userMethod.getReturnType().equals(returnType),
                    "Method '%s' on '%s' must return '%s'", methodName,
                    destination, returnType.getNameIncludingTypeParameters());
            return new MethodMetadataBuilder(userMethod);
        }

        final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("Object obj = getValue();");
        bodyBuilder.appendFormalLine("if (obj == null) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine("return null;");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder
                .appendFormalLine("return (String) typeConverter.convertIfNecessary((("
                        + javaType.getNameIncludingTypeParameters(false,
                                builder.getImportRegistrationResolver())
                        + ") obj)."
                        + identifierAccessorMethod.getMethodName()
                        + "(), String.class);");

        return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
                returnType,
                AnnotatedJavaType.convertFromJavaTypes(parameterTypes),
                parameterNames, bodyBuilder);
    }

    private MethodMetadataBuilder getSetAsTextMethod(final JavaType javaType,
            final JavaType idType, final MethodMetadata findMethod) {
        final JavaType parameterType = JavaType.STRING;
        final List<JavaSymbolName> parameterNames = Arrays
                .asList(new JavaSymbolName("text"));

        final JavaSymbolName methodName = new JavaSymbolName("setAsText");
        final JavaType returnType = JavaType.VOID_PRIMITIVE;

        // Locate user-defined method
        final MethodMetadata userMethod = getGovernorMethod(methodName,
                parameterType);
        if (userMethod != null) {
            Validate.isTrue(userMethod.getReturnType().equals(returnType),
                    "Method '%s' on '%s' must return '%s'", methodName,
                    destination, returnType.getNameIncludingTypeParameters());
            return new MethodMetadataBuilder(userMethod);
        }

        final String identifierTypeName = idType
                .getNameIncludingTypeParameters(false,
                        builder.getImportRegistrationResolver());

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder
                .appendFormalLine("if (text == null || 0 == text.length()) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine("setValue(null);");
        bodyBuilder.appendFormalLine("return;");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder.newLine();
        bodyBuilder.appendFormalLine(identifierTypeName + " identifier = ("
                + identifierTypeName
                + ") typeConverter.convertIfNecessary(text, "
                + identifierTypeName + ".class);");
        bodyBuilder.appendFormalLine("if (identifier == null) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine("setValue(null);");
        bodyBuilder.appendFormalLine("return;");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder.newLine();
        bodyBuilder.appendFormalLine("setValue("
                + javaType.getNameIncludingTypeParameters(false,
                        builder.getImportRegistrationResolver()) + "."
                + findMethod.getMethodName() + "(identifier));");

        return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
                returnType,
                AnnotatedJavaType.convertFromJavaTypes(parameterType),
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