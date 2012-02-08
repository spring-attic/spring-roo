package org.springframework.roo.addon.web.mvc.controller.json;

import static java.lang.reflect.Modifier.PUBLIC;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.MERGE_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.PERSIST_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.REMOVE_METHOD;
import static org.springframework.roo.model.JdkJavaType.CALENDAR;
import static org.springframework.roo.model.JdkJavaType.DATE;
import static org.springframework.roo.model.SpringJavaType.DATE_TIME_FORMAT;
import static org.springframework.roo.model.SpringJavaType.HTTP_HEADERS;
import static org.springframework.roo.model.SpringJavaType.HTTP_STATUS;
import static org.springframework.roo.model.SpringJavaType.PATH_VARIABLE;
import static org.springframework.roo.model.SpringJavaType.REQUEST_BODY;
import static org.springframework.roo.model.SpringJavaType.REQUEST_MAPPING;
import static org.springframework.roo.model.SpringJavaType.REQUEST_METHOD;
import static org.springframework.roo.model.SpringJavaType.REQUEST_PARAM;
import static org.springframework.roo.model.SpringJavaType.RESPONSE_BODY;
import static org.springframework.roo.model.SpringJavaType.RESPONSE_ENTITY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.json.JsonMetadata;
import org.springframework.roo.addon.web.mvc.controller.details.FinderMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.scaffold.RooWebScaffold;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.BooleanAttributeValue;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for Json functionality provided through {@link RooWebScaffold}.
 * 
 * @author Stefan Schmidt
 * @since 1.1.3
 */
public class WebJsonMetadata extends
        AbstractItdTypeDetailsProvidingMetadataItem {

    private static final String CONTENT_TYPE = "application/json";
    private static final String PROVIDES_TYPE_STRING = WebJsonMetadata.class
            .getName();
    private static final String PROVIDES_TYPE = MetadataIdentificationUtils
            .create(PROVIDES_TYPE_STRING);
    private static final JavaType RESPONSE_ENTITY_STRING = new JavaType(
            RESPONSE_ENTITY.getFullyQualifiedTypeName(), 0, DataType.TYPE,
            null, Arrays.asList(JavaType.STRING));

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

    private WebJsonAnnotationValues annotationValues;
    private boolean introduceLayerComponents;
    private JavaType jsonEnabledType;
    private String jsonEnabledTypeShortName;
    private JsonMetadata jsonMetadata;
    private String jsonBeanName;

    /**
     * Constructor
     * 
     * @param identifier
     * @param aspectName
     * @param governorPhysicalTypeMetadata
     * @param annotationValues
     * @param persistenceAdditions
     * @param identifierField
     * @param plural
     * @param finderDetails (required)
     * @param jsonMetadata
     * @param introduceLayerComponents whether to introduce any required layer
     *            components (services, repositories, etc.)
     */
    public WebJsonMetadata(
            final String identifier,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final WebJsonAnnotationValues annotationValues,
            final Map<MethodMetadataCustomDataKey, MemberTypeAdditions> persistenceAdditions,
            final FieldMetadata identifierField, final String plural,
            final Set<FinderMetadataDetails> finderDetails,
            final JsonMetadata jsonMetadata,
            final boolean introduceLayerComponents) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
        Validate.isTrue(isValid(identifier), "Metadata identification string '"
                + identifier + "' is invalid");
        Validate.notNull(annotationValues, "Annotation values required");
        Validate.notNull(persistenceAdditions, "Persistence additions required");
        Validate.notNull(finderDetails,
                "Set of dynamic finder methods cannot be null");
        Validate.notNull(jsonMetadata, "Json metadata required");

        if (!isValid()) {
            return;
        }

        this.annotationValues = annotationValues;
        jsonEnabledType = annotationValues.getJsonObject();
        jsonEnabledTypeShortName = getShortName(jsonEnabledType);
        jsonBeanName = JavaSymbolName.getReservedWordSafeName(jsonEnabledType)
                .getSymbolName();

        this.introduceLayerComponents = introduceLayerComponents;
        this.jsonMetadata = jsonMetadata;

        final MemberTypeAdditions findMethod = persistenceAdditions
                .get(FIND_METHOD);
        builder.addMethod(getShowJsonMethod(identifierField, findMethod));

        final MemberTypeAdditions findAllMethod = persistenceAdditions
                .get(FIND_ALL_METHOD);
        builder.addMethod(getListJsonMethod(findAllMethod));

        final MemberTypeAdditions persistMethod = persistenceAdditions
                .get(PERSIST_METHOD);
        builder.addMethod(getCreateFromJsonMethod(persistMethod));
        builder.addMethod(getCreateFromJsonArrayMethod(persistMethod));

        final MemberTypeAdditions mergeMethod = persistenceAdditions
                .get(MERGE_METHOD);
        builder.addMethod(getUpdateFromJsonMethod(mergeMethod));
        builder.addMethod(getUpdateFromJsonArrayMethod(mergeMethod));

        final MemberTypeAdditions removeMethod = persistenceAdditions
                .get(REMOVE_METHOD);
        builder.addMethod(getDeleteFromJsonMethod(removeMethod,
                identifierField, findMethod));

        if (annotationValues.isExposeFinders()) {
            for (final FinderMetadataDetails finder : finderDetails) {
                builder.addMethod(getJsonFindMethod(finder, plural));
            }
        }

        itdTypeDetails = builder.build();
    }

    private MethodMetadataBuilder getCreateFromJsonArrayMethod(
            final MemberTypeAdditions persistMethod) {
        if (StringUtils
                .isBlank(annotationValues.getCreateFromJsonArrayMethod())
                || persistMethod == null) {
            return null;
        }
        final JavaSymbolName methodName = new JavaSymbolName(
                annotationValues.getCreateFromJsonArrayMethod());
        if (governorHasMethodWithSameName(methodName)) {
            return null;
        }

        final JavaSymbolName fromJsonArrayMethodName = jsonMetadata
                .getFromJsonArrayMethodName();

        final AnnotationMetadataBuilder requestBodyAnnotation = new AnnotationMetadataBuilder(
                REQUEST_BODY);
        final List<AnnotatedJavaType> parameterTypes = Arrays
                .asList(new AnnotatedJavaType(JavaType.STRING,
                        requestBodyAnnotation.build()));
        final List<JavaSymbolName> parameterNames = Arrays
                .asList(new JavaSymbolName("json"));

        final List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
        requestMappingAttributes.add(new StringAttributeValue(
                new JavaSymbolName("value"), "/jsonArray"));
        requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName(
                "method"), new EnumDetails(REQUEST_METHOD, new JavaSymbolName(
                "POST"))));
        requestMappingAttributes.add(new StringAttributeValue(
                new JavaSymbolName("headers"), "Accept=application/json"));
        final AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(
                REQUEST_MAPPING, requestMappingAttributes);
        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        annotations.add(requestMapping);

        final List<JavaType> params = new ArrayList<JavaType>();
        params.add(jsonEnabledType);

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("for (" + jsonEnabledTypeShortName + " "
                + jsonBeanName + ": " + jsonEnabledTypeShortName + "."
                + fromJsonArrayMethodName.getSymbolName() + "(json)) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine(persistMethod.getMethodCall() + ";");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        final String httpHeadersShortName = getShortName(HTTP_HEADERS);
        bodyBuilder.appendFormalLine(httpHeadersShortName + " headers = new "
                + httpHeadersShortName + "();");
        bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \""
                + CONTENT_TYPE + "\");");
        bodyBuilder
                .appendFormalLine("return new ResponseEntity<String>(headers, "
                        + getShortName(HTTP_STATUS) + ".CREATED);");

        if (introduceLayerComponents) {
            persistMethod.copyAdditionsTo(builder, governorTypeDetails);
        }

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), PUBLIC, methodName, RESPONSE_ENTITY_STRING,
                parameterTypes, parameterNames, bodyBuilder);
        methodBuilder.setAnnotations(annotations);
        return methodBuilder;
    }

    private MethodMetadataBuilder getCreateFromJsonMethod(
            final MemberTypeAdditions persistMethod) {
        if (StringUtils.isBlank(annotationValues.getCreateFromJsonMethod())
                || persistMethod == null) {
            return null;
        }
        final JavaSymbolName methodName = new JavaSymbolName(
                annotationValues.getCreateFromJsonMethod());
        if (governorHasMethodWithSameName(methodName)) {
            return null;
        }

        final JavaSymbolName fromJsonMethodName = jsonMetadata
                .getFromJsonMethodName();

        final AnnotationMetadataBuilder requestBodyAnnotation = new AnnotationMetadataBuilder(
                REQUEST_BODY);
        final List<AnnotatedJavaType> parameterTypes = Arrays
                .asList(new AnnotatedJavaType(JavaType.STRING,
                        requestBodyAnnotation.build()));
        final List<JavaSymbolName> parameterNames = Arrays
                .asList(new JavaSymbolName("json"));

        final List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
        requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName(
                "method"), new EnumDetails(REQUEST_METHOD, new JavaSymbolName(
                "POST"))));
        requestMappingAttributes.add(new StringAttributeValue(
                new JavaSymbolName("headers"), "Accept=application/json"));
        final AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(
                REQUEST_MAPPING, requestMappingAttributes);
        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        annotations.add(requestMapping);

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine(jsonEnabledTypeShortName + " "
                + jsonBeanName + " = " + jsonEnabledTypeShortName + "."
                + fromJsonMethodName.getSymbolName() + "(json);");
        bodyBuilder.appendFormalLine(persistMethod.getMethodCall() + ";");
        final String httpHeadersShortName = getShortName(HTTP_HEADERS);
        bodyBuilder.appendFormalLine(httpHeadersShortName + " headers = new "
                + httpHeadersShortName + "();");
        bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \""
                + CONTENT_TYPE + "\");");
        bodyBuilder
                .appendFormalLine("return new ResponseEntity<String>(headers, "
                        + getShortName(HTTP_STATUS) + ".CREATED);");

        if (introduceLayerComponents) {
            persistMethod.copyAdditionsTo(builder, governorTypeDetails);
        }

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), PUBLIC, methodName, RESPONSE_ENTITY_STRING,
                parameterTypes, parameterNames, bodyBuilder);
        methodBuilder.setAnnotations(annotations);
        return methodBuilder;
    }

    private MethodMetadataBuilder getDeleteFromJsonMethod(
            final MemberTypeAdditions removeMethod,
            final FieldMetadata identifierField,
            final MemberTypeAdditions findMethod) {
        if (StringUtils.isBlank(annotationValues.getDeleteFromJsonMethod())
                || removeMethod == null || identifierField == null
                || findMethod == null) {
            return null;
        }
        final JavaSymbolName methodName = new JavaSymbolName(
                annotationValues.getDeleteFromJsonMethod());
        if (governorHasMethodWithSameName(methodName)) {
            return null;
        }

        final List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
        attributes.add(new StringAttributeValue(new JavaSymbolName("value"),
                identifierField.getFieldName().getSymbolName()));
        final AnnotationMetadataBuilder pathVariableAnnotation = new AnnotationMetadataBuilder(
                PATH_VARIABLE, attributes);

        final List<AnnotatedJavaType> parameterTypes = Arrays
                .asList(new AnnotatedJavaType(identifierField.getFieldType(),
                        pathVariableAnnotation.build()));
        final List<JavaSymbolName> parameterNames = Arrays
                .asList(identifierField.getFieldName());

        final List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
        requestMappingAttributes
                .add(new StringAttributeValue(new JavaSymbolName("value"), "/{"
                        + identifierField.getFieldName().getSymbolName() + "}"));
        requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName(
                "method"), new EnumDetails(REQUEST_METHOD, new JavaSymbolName(
                "DELETE"))));
        requestMappingAttributes.add(new StringAttributeValue(
                new JavaSymbolName("headers"), "Accept=application/json"));
        final AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(
                REQUEST_MAPPING, requestMappingAttributes);

        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        annotations.add(requestMapping);

        final String httpHeadersShortName = getShortName(HTTP_HEADERS);

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine(jsonEnabledTypeShortName + " "
                + jsonBeanName + " = " + findMethod.getMethodCall() + ";");
        bodyBuilder.appendFormalLine(httpHeadersShortName + " headers = new "
                + httpHeadersShortName + "();");
        bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \""
                + CONTENT_TYPE + "\");");
        bodyBuilder.appendFormalLine("if (" + jsonBeanName + " == null) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine("return new "
                + getShortName(RESPONSE_ENTITY) + "<String>(headers, "
                + getShortName(HTTP_STATUS) + ".NOT_FOUND);");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder.appendFormalLine(removeMethod.getMethodCall() + ";");
        bodyBuilder
                .appendFormalLine("return new ResponseEntity<String>(headers, "
                        + getShortName(HTTP_STATUS) + ".OK);");

        if (introduceLayerComponents) {
            removeMethod.copyAdditionsTo(builder, governorTypeDetails);
            findMethod.copyAdditionsTo(builder, governorTypeDetails);
        }
        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), PUBLIC, methodName, RESPONSE_ENTITY_STRING,
                parameterTypes, parameterNames, bodyBuilder);
        methodBuilder.setAnnotations(annotations);
        return methodBuilder;
    }

    private MethodMetadataBuilder getJsonFindMethod(
            final FinderMetadataDetails finderDetails, final String plural) {
        if (finderDetails == null
                || jsonMetadata.getToJsonArrayMethodName() == null) {
            return null;
        }
        final JavaSymbolName finderMethodName = new JavaSymbolName("json"
                + StringUtils.capitalize(finderDetails
                        .getFinderMethodMetadata().getMethodName()
                        .getSymbolName()));
        if (governorHasMethodWithSameName(finderMethodName)) {
            return null;
        }

        final List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
        final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
        final StringBuilder methodParams = new StringBuilder();

        for (final FieldMetadata field : finderDetails
                .getFinderMethodParamFields()) {
            final JavaSymbolName fieldName = field.getFieldName();
            final List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
            final List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
            attributes.add(new StringAttributeValue(
                    new JavaSymbolName("value"), StringUtils
                            .uncapitalize(fieldName.getSymbolName())));
            if (field.getFieldType().equals(JavaType.BOOLEAN_PRIMITIVE)
                    || field.getFieldType().equals(JavaType.BOOLEAN_OBJECT)) {
                attributes.add(new BooleanAttributeValue(new JavaSymbolName(
                        "required"), false));
            }
            final AnnotationMetadataBuilder requestParamAnnotation = new AnnotationMetadataBuilder(
                    REQUEST_PARAM, attributes);
            annotations.add(requestParamAnnotation.build());
            if (field.getFieldType().equals(DATE)
                    || field.getFieldType().equals(CALENDAR)) {
                final AnnotationMetadata annotation = MemberFindingUtils
                        .getAnnotationOfType(field.getAnnotations(),
                                DATE_TIME_FORMAT);
                if (annotation != null) {
                    annotations.add(annotation);
                }
            }
            parameterNames.add(fieldName);
            parameterTypes.add(new AnnotatedJavaType(field.getFieldType(),
                    annotations));

            if (field.getFieldType().equals(JavaType.BOOLEAN_OBJECT)) {
                methodParams.append(field.getFieldName()
                        + " == null ? Boolean.FALSE : " + field.getFieldName()
                        + ", ");
            }
            else {
                methodParams.append(field.getFieldName() + ", ");
            }
        }

        if (methodParams.length() > 0) {
            methodParams.delete(methodParams.length() - 2,
                    methodParams.length());
        }

        final List<JavaSymbolName> newParamNames = new ArrayList<JavaSymbolName>();
        newParamNames.addAll(parameterNames);

        final List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
        requestMappingAttributes.add(new StringAttributeValue(
                new JavaSymbolName("params"), "find="
                        + finderDetails.getFinderMethodMetadata()
                                .getMethodName().getSymbolName()
                                .replaceFirst("find" + plural, "")));
        requestMappingAttributes.add(new StringAttributeValue(
                new JavaSymbolName("headers"), "Accept=application/json"));
        final AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(
                REQUEST_MAPPING, requestMappingAttributes);
        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        annotations.add(requestMapping);
        annotations.add(new AnnotationMetadataBuilder(RESPONSE_BODY));

        final String httpHeadersShortName = getShortName(HTTP_HEADERS);
        final String responseEntityShortName = getShortName(RESPONSE_ENTITY);
        final String httpStatusShortName = getShortName(HTTP_STATUS);

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine(httpHeadersShortName + " headers = new "
                + httpHeadersShortName + "();");
        bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \""
                + CONTENT_TYPE + "; charset=utf-8\");");
        bodyBuilder.appendFormalLine("return new "
                + responseEntityShortName
                + "<String>("
                + jsonEnabledTypeShortName
                + "."
                + jsonMetadata.getToJsonArrayMethodName().getSymbolName()
                        .toString()
                + "("
                + jsonEnabledTypeShortName
                + "."
                + finderDetails.getFinderMethodMetadata().getMethodName()
                        .getSymbolName() + "(" + methodParams.toString()
                + ").getResultList()), headers, " + httpStatusShortName
                + ".OK);");

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), PUBLIC, finderMethodName, RESPONSE_ENTITY_STRING,
                parameterTypes, newParamNames, bodyBuilder);
        methodBuilder.setAnnotations(annotations);
        return methodBuilder;
    }

    private MethodMetadataBuilder getListJsonMethod(
            final MemberTypeAdditions findAllMethod) {
        if (StringUtils.isBlank(annotationValues.getListJsonMethod())
                || findAllMethod == null) {
            return null;
        }
        final JavaSymbolName methodName = new JavaSymbolName(
                annotationValues.getListJsonMethod());
        if (governorHasMethodWithSameName(methodName)) {
            return null;
        }

        final JavaSymbolName toJsonArrayMethodName = jsonMetadata
                .getToJsonArrayMethodName();

        final List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
        requestMappingAttributes.add(new StringAttributeValue(
                new JavaSymbolName("headers"), "Accept=application/json"));
        final AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(
                REQUEST_MAPPING, requestMappingAttributes);
        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        annotations.add(requestMapping);

        annotations.add(new AnnotationMetadataBuilder(RESPONSE_BODY));

        final String httpHeadersShortName = getShortName(HTTP_HEADERS);
        final String responseEntityShortName = getShortName(RESPONSE_ENTITY);
        final String httpStatusShortName = getShortName(HTTP_STATUS);

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine(httpHeadersShortName + " headers = new "
                + httpHeadersShortName + "();");
        bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \""
                + CONTENT_TYPE + "; charset=utf-8\");");
        final JavaType list = new JavaType(List.class.getName(), 0,
                DataType.TYPE, null, Arrays.asList(jsonEnabledType));
        bodyBuilder.appendFormalLine(getShortName(list) + " result = "
                + findAllMethod.getMethodCall() + ";");
        bodyBuilder.appendFormalLine("return new " + responseEntityShortName
                + "<String>(" + jsonEnabledTypeShortName + "."
                + toJsonArrayMethodName.getSymbolName() + "(result), headers, "
                + httpStatusShortName + ".OK);");

        if (introduceLayerComponents) {
            findAllMethod.copyAdditionsTo(builder, governorTypeDetails);
        }

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), PUBLIC, methodName, RESPONSE_ENTITY_STRING,
                bodyBuilder);
        methodBuilder.setAnnotations(annotations);
        return methodBuilder;
    }

    private String getShortName(final JavaType type) {
        return type.getNameIncludingTypeParameters(false,
                builder.getImportRegistrationResolver());
    }

    private MethodMetadataBuilder getShowJsonMethod(
            final FieldMetadata identifierField,
            final MemberTypeAdditions findMethod) {
        if (StringUtils.isBlank(annotationValues.getShowJsonMethod())
                || identifierField == null || findMethod == null) {
            return null;
        }
        final JavaSymbolName methodName = new JavaSymbolName(
                annotationValues.getShowJsonMethod());
        if (governorHasMethodWithSameName(methodName)) {
            return null;
        }

        final JavaSymbolName toJsonMethodName = jsonMetadata
                .getToJsonMethodName();

        final List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
        attributes.add(new StringAttributeValue(new JavaSymbolName("value"),
                identifierField.getFieldName().getSymbolName()));
        final AnnotationMetadataBuilder pathVariableAnnotation = new AnnotationMetadataBuilder(
                PATH_VARIABLE, attributes);

        final List<AnnotatedJavaType> parameterTypes = Arrays
                .asList(new AnnotatedJavaType(identifierField.getFieldType(),
                        pathVariableAnnotation.build()));
        final List<JavaSymbolName> parameterNames = Arrays
                .asList(new JavaSymbolName(identifierField.getFieldName()
                        .getSymbolName()));

        final List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
        requestMappingAttributes
                .add(new StringAttributeValue(new JavaSymbolName("value"), "/{"
                        + identifierField.getFieldName().getSymbolName() + "}"));
        requestMappingAttributes.add(new StringAttributeValue(
                new JavaSymbolName("headers"), "Accept=application/json"));
        final AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(
                REQUEST_MAPPING, requestMappingAttributes);

        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        annotations.add(requestMapping);
        annotations.add(new AnnotationMetadataBuilder(RESPONSE_BODY));

        final String httpHeadersShortName = getShortName(HTTP_HEADERS);
        final String responseEntityShortName = getShortName(RESPONSE_ENTITY);
        final String httpStatusShortName = getShortName(HTTP_STATUS);

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine(jsonEnabledTypeShortName + " "
                + jsonBeanName + " = " + findMethod.getMethodCall() + ";");
        bodyBuilder.appendFormalLine(httpHeadersShortName + " headers = new "
                + httpHeadersShortName + "();");
        bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \""
                + CONTENT_TYPE + "; charset=utf-8\");");
        bodyBuilder.appendFormalLine("if (" + jsonBeanName + " == null) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine("return new " + responseEntityShortName
                + "<String>(headers, " + httpStatusShortName + ".NOT_FOUND);");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder.appendFormalLine("return new " + responseEntityShortName
                + "<String>(" + jsonBeanName + "."
                + toJsonMethodName.getSymbolName() + "(), headers, "
                + httpStatusShortName + ".OK);");

        if (introduceLayerComponents) {
            findMethod.copyAdditionsTo(builder, governorTypeDetails);
        }

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), PUBLIC, methodName, RESPONSE_ENTITY_STRING,
                parameterTypes, parameterNames, bodyBuilder);
        methodBuilder.setAnnotations(annotations);
        return methodBuilder;
    }

    private MethodMetadataBuilder getUpdateFromJsonArrayMethod(
            final MemberTypeAdditions mergeMethod) {
        if (StringUtils
                .isBlank(annotationValues.getUpdateFromJsonArrayMethod())
                || mergeMethod == null) {
            return null;
        }
        final JavaSymbolName methodName = new JavaSymbolName(
                annotationValues.getUpdateFromJsonArrayMethod());
        if (governorHasMethodWithSameName(methodName)) {
            return null;
        }

        final JavaSymbolName fromJsonArrayMethodName = jsonMetadata
                .getFromJsonArrayMethodName();

        final AnnotationMetadataBuilder requestBodyAnnotation = new AnnotationMetadataBuilder(
                REQUEST_BODY);

        final List<AnnotatedJavaType> parameterTypes = Arrays
                .asList(new AnnotatedJavaType(JavaType.STRING,
                        requestBodyAnnotation.build()));
        final List<JavaSymbolName> parameterNames = Arrays
                .asList(new JavaSymbolName("json"));

        final List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
        requestMappingAttributes.add(new StringAttributeValue(
                new JavaSymbolName("value"), "/jsonArray"));
        requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName(
                "method"), new EnumDetails(REQUEST_METHOD, new JavaSymbolName(
                "PUT"))));
        requestMappingAttributes.add(new StringAttributeValue(
                new JavaSymbolName("headers"), "Accept=application/json"));
        final AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(
                REQUEST_MAPPING, requestMappingAttributes);
        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        annotations.add(requestMapping);

        final List<JavaType> params = new ArrayList<JavaType>();
        params.add(jsonEnabledType);

        final String httpHeadersShortName = getShortName(HTTP_HEADERS);

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine(httpHeadersShortName + " headers = new "
                + httpHeadersShortName + "();");
        bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \""
                + CONTENT_TYPE + "\");");
        bodyBuilder.appendFormalLine("for (" + jsonEnabledTypeShortName + " "
                + jsonBeanName + ": " + jsonEnabledTypeShortName + "."
                + fromJsonArrayMethodName.getSymbolName() + "(json)) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine("if (" + mergeMethod.getMethodCall()
                + " == null) {");
        bodyBuilder.indent();
        bodyBuilder
                .appendFormalLine("return new ResponseEntity<String>(headers, "
                        + getShortName(HTTP_STATUS) + ".NOT_FOUND);");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder
                .appendFormalLine("return new ResponseEntity<String>(headers, "
                        + getShortName(HTTP_STATUS) + ".OK);");

        if (introduceLayerComponents) {
            mergeMethod.copyAdditionsTo(builder, governorTypeDetails);
        }

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), PUBLIC, methodName, RESPONSE_ENTITY_STRING,
                parameterTypes, parameterNames, bodyBuilder);
        methodBuilder.setAnnotations(annotations);
        return methodBuilder;
    }

    private MethodMetadataBuilder getUpdateFromJsonMethod(
            final MemberTypeAdditions mergeMethod) {
        if (StringUtils.isBlank(annotationValues.getUpdateFromJsonMethod())
                || mergeMethod == null) {
            return null;
        }
        final JavaSymbolName methodName = new JavaSymbolName(
                annotationValues.getUpdateFromJsonMethod());
        if (governorHasMethodWithSameName(methodName)) {
            return null;
        }

        final JavaSymbolName fromJsonMethodName = jsonMetadata
                .getFromJsonMethodName();

        final AnnotationMetadataBuilder requestBodyAnnotation = new AnnotationMetadataBuilder(
                REQUEST_BODY);

        final List<AnnotatedJavaType> parameterTypes = Arrays
                .asList(new AnnotatedJavaType(JavaType.STRING,
                        requestBodyAnnotation.build()));
        final List<JavaSymbolName> parameterNames = Arrays
                .asList(new JavaSymbolName("json"));

        final List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
        requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName(
                "method"), new EnumDetails(REQUEST_METHOD, new JavaSymbolName(
                "PUT"))));
        requestMappingAttributes.add(new StringAttributeValue(
                new JavaSymbolName("headers"), "Accept=application/json"));
        final AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(
                REQUEST_MAPPING, requestMappingAttributes);
        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        annotations.add(requestMapping);

        final String httpHeadersShortName = getShortName(HTTP_HEADERS);

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine(httpHeadersShortName + " headers = new "
                + httpHeadersShortName + "();");
        bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \""
                + CONTENT_TYPE + "\");");
        bodyBuilder.appendFormalLine(jsonEnabledTypeShortName + " "
                + jsonBeanName + " = " + jsonEnabledTypeShortName + "."
                + fromJsonMethodName.getSymbolName() + "(json);");
        bodyBuilder.appendFormalLine("if (" + mergeMethod.getMethodCall()
                + " == null) {");
        bodyBuilder.indent();
        bodyBuilder
                .appendFormalLine("return new ResponseEntity<String>(headers, "
                        + getShortName(HTTP_STATUS) + ".NOT_FOUND);");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder
                .appendFormalLine("return new ResponseEntity<String>(headers, "
                        + getShortName(HTTP_STATUS) + ".OK);");

        if (introduceLayerComponents) {
            mergeMethod.copyAdditionsTo(builder, governorTypeDetails);
        }

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), PUBLIC, methodName, RESPONSE_ENTITY_STRING,
                parameterTypes, parameterNames, bodyBuilder);
        methodBuilder.setAnnotations(annotations);
        return methodBuilder;
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
