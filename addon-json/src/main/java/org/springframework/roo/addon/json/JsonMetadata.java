package org.springframework.roo.addon.json;

import static org.springframework.roo.model.JavaType.STRING;
import static org.springframework.roo.model.JdkJavaType.ARRAY_LIST;
import static org.springframework.roo.model.JdkJavaType.COLLECTION;
import static org.springframework.roo.model.JdkJavaType.LIST;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata to be triggered by {@link RooJson} annotation
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public class JsonMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

    private static final JavaType JSON_DESERIALIZER = new JavaType(
            "flexjson.JSONDeserializer");
    private static final JavaType JSON_SERIALIZER = new JavaType(
            "flexjson.JSONSerializer");
    private static final String PROVIDES_TYPE_STRING = JsonMetadata.class
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

    private JsonAnnotationValues annotationValues;

    private String typeNamePlural;

    public JsonMetadata(final String identifier, final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final String typeNamePlural,
            final JsonAnnotationValues annotationValues) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
        Validate.notNull(annotationValues, "Annotation values required");
        Validate.notBlank(typeNamePlural, "Plural of the target type required");
        Validate.isTrue(
                isValid(identifier),
                "Metadata identification string '%s' does not appear to be a valid",
                identifier);

        if (!isValid()) {
            return;
        }

        this.annotationValues = annotationValues;
        this.typeNamePlural = typeNamePlural;

        builder.addMethod(getToJsonMethod(false));
        builder.addMethod(getToJsonMethod(true));
        builder.addMethod(getFromJsonMethod());
        builder.addMethod(getToJsonArrayMethod(false));
        builder.addMethod(getToJsonArrayMethod(true));
        builder.addMethod(getFromJsonArrayMethod());

        // Create a representation of the desired output ITD
        itdTypeDetails = builder.build();
    }

    private MethodMetadataBuilder getFromJsonArrayMethod() {
        // Compute the relevant method name
        final JavaSymbolName methodName = getFromJsonArrayMethodName();
        if (methodName == null) {
            return null;
        }

        final JavaType parameterType = JavaType.STRING;
        if (governorHasMethod(methodName, parameterType)) {
            return null;
        }

        final String list = LIST.getNameIncludingTypeParameters(false,
                builder.getImportRegistrationResolver());
        final String arrayList = ARRAY_LIST.getNameIncludingTypeParameters(
                false, builder.getImportRegistrationResolver());
        final String bean = destination.getSimpleTypeName();

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        final String deserializer = JSON_DESERIALIZER
                .getNameIncludingTypeParameters(false,
                        builder.getImportRegistrationResolver());
        bodyBuilder.appendFormalLine("return new " + deserializer + "<" + list
                + "<" + bean + ">>().use(null, " + arrayList
                + ".class).use(\"values\", " + bean
                + ".class).deserialize(json);");

        final List<JavaSymbolName> parameterNames = Arrays
                .asList(new JavaSymbolName("json"));

        final JavaType collection = new JavaType(
                COLLECTION.getFullyQualifiedTypeName(), 0, DataType.TYPE, null,
                Arrays.asList(destination));

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), Modifier.PUBLIC | Modifier.STATIC, methodName,
                collection,
                AnnotatedJavaType.convertFromJavaTypes(parameterType),
                parameterNames, bodyBuilder);
        methodBuilder.putCustomData(CustomDataJsonTags.FROM_JSON_ARRAY_METHOD,
                null);
        return methodBuilder;
    }

    public JavaSymbolName getFromJsonArrayMethodName() {
        final String methodLabel = annotationValues.getFromJsonArrayMethod();
        if (StringUtils.isBlank(methodLabel)) {
            return null;
        }

        return new JavaSymbolName(methodLabel.replace("<TypeNamePlural>",
                typeNamePlural));
    }

    private MethodMetadataBuilder getFromJsonMethod() {
        final JavaSymbolName methodName = getFromJsonMethodName();
        if (methodName == null) {
            return null;
        }

        final JavaType parameterType = JavaType.STRING;
        if (governorHasMethod(methodName, parameterType)) {
            return null;
        }

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        final String deserializer = JSON_DESERIALIZER
                .getNameIncludingTypeParameters(false,
                        builder.getImportRegistrationResolver());
        bodyBuilder.appendFormalLine("return new " + deserializer + "<"
                + destination.getSimpleTypeName() + ">().use(null, "
                + destination.getSimpleTypeName()
                + ".class).deserialize(json);");

        final List<JavaSymbolName> parameterNames = Arrays
                .asList(new JavaSymbolName("json"));

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), Modifier.PUBLIC | Modifier.STATIC, methodName,
                destination,
                AnnotatedJavaType.convertFromJavaTypes(parameterType),
                parameterNames, bodyBuilder);
        methodBuilder.putCustomData(CustomDataJsonTags.FROM_JSON_METHOD, null);
        return methodBuilder;
    }

    public JavaSymbolName getFromJsonMethodName() {
        final String methodLabel = annotationValues.getFromJsonMethod();
        if (StringUtils.isBlank(methodLabel)) {
            return null;
        }

        // Compute the relevant method name
        return new JavaSymbolName(methodLabel.replace("<TypeName>",
                destination.getSimpleTypeName()));
    }

    private MethodMetadataBuilder getToJsonArrayMethod(boolean includeParams) {
        // Compute the relevant method name
        final JavaSymbolName methodName = getToJsonArrayMethodName();
        if (methodName == null) {
            return null;
        }

        final JavaType parameterType = new JavaType(Collection.class.getName(),
                0, DataType.TYPE, null, Arrays.asList(destination));

        // See if the type itself declared the method
        if (governorHasMethod(methodName, parameterType)) {
            return null;
        }

        final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
        parameterNames.add(new JavaSymbolName("collection"));

        final List<AnnotatedJavaType> parameterTypes = AnnotatedJavaType
                .convertFromJavaTypes(parameterType);

        if (includeParams) {
            parameterTypes.add(new AnnotatedJavaType(JavaType.STRING_ARRAY));
            parameterNames.add(new JavaSymbolName("fields"));
        }

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        final String serializer = JSON_SERIALIZER
                .getNameIncludingTypeParameters(false,
                        builder.getImportRegistrationResolver());
        final String root = annotationValues.getRootName() != null
                && annotationValues.getRootName().length() > 0 ? ".rootName(\""
                + annotationValues.getRootName() + "\")" : "";
        bodyBuilder
                .appendFormalLine("return new "
                        + serializer
                        + "()"
                        + root
                        + (!includeParams ? "" : ".include(fields)")
                        + ".exclude(\"*.class\")"
                        + (annotationValues.isDeepSerialize() ? ".deepSerialize(collection)"
                                : ".serialize(collection)") + ";");

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), Modifier.PUBLIC | Modifier.STATIC, methodName, STRING,
                parameterTypes, parameterNames, bodyBuilder);
        methodBuilder.putCustomData(CustomDataJsonTags.TO_JSON_ARRAY_METHOD,
                null);
        return methodBuilder;
    }

    public JavaSymbolName getToJsonArrayMethodName() {
        final String methodLabel = annotationValues.getToJsonArrayMethod();
        if (StringUtils.isBlank(methodLabel)) {
            return null;
        }
        return new JavaSymbolName(methodLabel);
    }

    private MethodMetadataBuilder getToJsonMethod(boolean includeParams) {
        // Compute the relevant method name
        final JavaSymbolName methodName = getToJsonMethodName();
        if (methodName == null) {
            return null;
        }

        // See if the type itself declared the method
        if (governorHasMethod(methodName)) {
            return null;
        }

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        final String serializer = JSON_SERIALIZER
                .getNameIncludingTypeParameters(false,
                        builder.getImportRegistrationResolver());
        final String root = annotationValues.getRootName() != null
                && annotationValues.getRootName().length() > 0 ? ".rootName(\""
                + annotationValues.getRootName() + "\")" : "";
        bodyBuilder.appendFormalLine("return new "
                + serializer
                + "()"
                + root
                + (!includeParams ? "" : ".include(fields)")
                + ".exclude(\"*.class\")"
                + (annotationValues.isDeepSerialize() ? ".deepSerialize(this)"
                        : ".serialize(this)") + ";");

        List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
        List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

        if (includeParams) {
            parameterTypes.add(new AnnotatedJavaType(JavaType.STRING_ARRAY));
            parameterNames.add(new JavaSymbolName("fields"));
        }

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), Modifier.PUBLIC, methodName, STRING, parameterTypes,
                parameterNames, bodyBuilder);
        methodBuilder.putCustomData(CustomDataJsonTags.TO_JSON_METHOD, null);
        return methodBuilder;
    }

    public JavaSymbolName getToJsonMethodName() {
        final String methodLabel = annotationValues.getToJsonMethod();
        if (StringUtils.isBlank(methodLabel)) {
            return null;
        }
        return new JavaSymbolName(methodLabel);
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
