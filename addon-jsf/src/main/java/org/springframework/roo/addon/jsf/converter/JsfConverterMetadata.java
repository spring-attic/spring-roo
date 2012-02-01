package org.springframework.roo.addon.jsf.converter;

import static java.lang.reflect.Modifier.PUBLIC;
import static org.springframework.roo.addon.jsf.JsfJavaType.CONVERTER;
import static org.springframework.roo.addon.jsf.JsfJavaType.FACES_CONTEXT;
import static org.springframework.roo.addon.jsf.JsfJavaType.FACES_CONVERTER;
import static org.springframework.roo.addon.jsf.JsfJavaType.UI_COMPONENT;
import static org.springframework.roo.model.JavaType.OBJECT;
import static org.springframework.roo.model.JavaType.STRING;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooJsfConverter}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public class JsfConverterMetadata extends
        AbstractItdTypeDetailsProvidingMetadataItem {

    static final String ID_FIELD_NAME = "id";
    private static final String PROVIDES_TYPE_STRING = JsfConverterMetadata.class
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

    public JsfConverterMetadata(final String identifier,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final JsfConverterAnnotationValues annotationValues,
            final MemberTypeAdditions findMethod,
            final MethodMetadata identifierAccessor) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
        Validate.isTrue(isValid(identifier), "Metadata identification string '"
                + identifier + "' is invalid");
        Validate.notNull(annotationValues, "Annotation values required");

        if (!isValid()) {
            return;
        }

        if (findMethod == null || identifierAccessor == null) {
            valid = false;
            return;
        }

        if (!isConverterInterfaceIntroduced()) {
            builder.getImportRegistrationResolver().addImport(CONVERTER);
            builder.addImplementsType(CONVERTER);
        }

        builder.addAnnotation(getFacesConverterAnnotation());
        builder.addMethod(getGetAsObjectMethod(findMethod, identifierAccessor));
        builder.addMethod(getGetAsStringMethod(annotationValues.getEntity(),
                identifierAccessor));

        // Create a representation of the desired output ITD
        itdTypeDetails = builder.build();
    }

    private AnnotationMetadata getFacesConverterAnnotation() {
        final AnnotationMetadata annotation = getTypeAnnotation(FACES_CONVERTER);
        if (annotation == null) {
            return null;
        }

        final AnnotationMetadataBuilder annotationBuulder = new AnnotationMetadataBuilder(
                annotation);
        // annotationBuulder.addClassAttribute("forClass", entity); // TODO The
        // forClass attribute causes issues
        annotationBuulder.addStringAttribute("value",
                destination.getFullyQualifiedTypeName());
        return annotationBuulder.build();
    }

    private MethodMetadataBuilder getGetAsObjectMethod(
            final MemberTypeAdditions findMethod,
            final MethodMetadata identifierAccessor) {
        final JavaSymbolName methodName = new JavaSymbolName("getAsObject");
        final JavaType[] parameterTypes = { FACES_CONTEXT, UI_COMPONENT, STRING };
        if (governorHasMethod(methodName, parameterTypes)) {
            return null;
        }

        findMethod.copyAdditionsTo(builder, governorTypeDetails);
        final JavaType returnType = identifierAccessor.getReturnType();

        builder.getImportRegistrationResolver().addImports(returnType,
                FACES_CONTEXT, UI_COMPONENT);

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder
                .appendFormalLine("if (value == null || value.length() == 0) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine("return null;");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder.appendFormalLine(returnType.getSimpleTypeName() + " "
                + ID_FIELD_NAME + " = "
                + getJavaTypeConversionString(returnType) + ";");
        bodyBuilder.appendFormalLine("return " + findMethod.getMethodCall()
                + ";");

        // Create getAsObject method
        final List<JavaSymbolName> parameterNames = Arrays.asList(
                new JavaSymbolName("context"), new JavaSymbolName("component"),
                new JavaSymbolName("value"));
        return new MethodMetadataBuilder(getId(), PUBLIC, methodName, OBJECT,
                AnnotatedJavaType.convertFromJavaTypes(parameterTypes),
                parameterNames, bodyBuilder);
    }

    private MethodMetadataBuilder getGetAsStringMethod(final JavaType entity,
            final MethodMetadata identifierAccessor) {
        final JavaSymbolName methodName = new JavaSymbolName("getAsString");
        final JavaType[] parameterTypes = { FACES_CONTEXT, UI_COMPONENT, OBJECT };
        if (governorHasMethod(methodName, parameterTypes)) {
            return null;
        }

        builder.getImportRegistrationResolver().addImports(entity,
                FACES_CONTEXT, UI_COMPONENT);

        final String simpleTypeName = entity.getSimpleTypeName();

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("return value instanceof "
                + simpleTypeName + " ? ((" + simpleTypeName + ") value)."
                + identifierAccessor.getMethodName().getSymbolName()
                + "().toString() : \"\";");

        // Create getAsString method
        final List<JavaSymbolName> parameterNames = Arrays.asList(
                new JavaSymbolName("context"), new JavaSymbolName("component"),
                new JavaSymbolName("value"));
        return new MethodMetadataBuilder(getId(), PUBLIC, methodName,
                JavaType.STRING,
                AnnotatedJavaType.convertFromJavaTypes(parameterTypes),
                parameterNames, bodyBuilder);
    }

    private String getJavaTypeConversionString(final JavaType javaType) {
        if (javaType.equals(JavaType.LONG_OBJECT)
                || javaType.equals(JavaType.LONG_PRIMITIVE)) {
            return "Long.parseLong(value)";
        }
        else if (javaType.equals(JavaType.INT_OBJECT)
                || javaType.equals(JavaType.INT_PRIMITIVE)) {
            return "Integer.parseInt(value)";
        }
        else if (javaType.equals(JavaType.DOUBLE_OBJECT)
                || javaType.equals(JavaType.DOUBLE_PRIMITIVE)) {
            return "Double.parseDouble(value)";
        }
        else if (javaType.equals(JavaType.FLOAT_OBJECT)
                || javaType.equals(JavaType.FLOAT_PRIMITIVE)) {
            return "Float.parseFloat(value)";
        }
        else if (javaType.equals(JavaType.SHORT_OBJECT)
                || javaType.equals(JavaType.SHORT_PRIMITIVE)) {
            return "Short.parseShort(value)";
        }
        else if (javaType.equals(JavaType.BYTE_OBJECT)
                || javaType.equals(JavaType.BYTE_PRIMITIVE)) {
            return "Byte.parseByte(value)";
        }
        else if (javaType.equals(JdkJavaType.BIG_DECIMAL)) {
            return "new BigDecimal(value)";
        }
        else if (javaType.equals(JdkJavaType.BIG_INTEGER)) {
            return "new BigInteger(value)";
        }
        else if (javaType.equals(STRING)) {
            return "value";
        }
        else {
            return "value.toString()";
        }
    }

    private boolean isConverterInterfaceIntroduced() {
        return isImplementing(governorTypeDetails, CONVERTER);
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
