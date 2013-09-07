package org.springframework.roo.addon.web.mvc.controller.finder;

import static org.springframework.roo.model.JdkJavaType.CALENDAR;
import static org.springframework.roo.model.JdkJavaType.DATE;
import static org.springframework.roo.model.SpringJavaType.DATE_TIME_FORMAT;
import static org.springframework.roo.model.SpringJavaType.MODEL;
import static org.springframework.roo.model.SpringJavaType.REQUEST_MAPPING;
import static org.springframework.roo.model.SpringJavaType.REQUEST_METHOD;
import static org.springframework.roo.model.SpringJavaType.REQUEST_PARAM;

import java.beans.Introspector;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.web.mvc.controller.details.DateTimeFormatDetails;
import org.springframework.roo.addon.web.mvc.controller.details.FinderMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.details.JavaTypeMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.details.JavaTypePersistenceMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.scaffold.RooWebScaffold;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldAnnotationValues;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.BooleanAttributeValue;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for finder functionality provided via {@link RooWebScaffold}.
 * 
 * @author Stefan Schmidt
 * @since 1.1.3
 */
public class WebFinderMetadata extends
        AbstractItdTypeDetailsProvidingMetadataItem {

    private static final String PROVIDES_TYPE_STRING = WebFinderMetadata.class
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

    private WebScaffoldAnnotationValues annotationValues;
    private String controllerPath;
    private JavaType formBackingType;
    private JavaTypeMetadataDetails javaTypeMetadataHolder;
    private Map<JavaType, JavaTypeMetadataDetails> specialDomainTypes;
    
    private Map<JavaSymbolName, DateTimeFormatDetails> dateTypes;

    /**
     * Constructor
     * 
     * @param identifier
     * @param aspectName
     * @param governorPhysicalTypeMetadata
     * @param annotationValues
     * @param specialDomainTypes
     * @param dynamicFinderMethods
     */
    public WebFinderMetadata(
            final String identifier,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final WebScaffoldAnnotationValues annotationValues,
            final SortedMap<JavaType, JavaTypeMetadataDetails> specialDomainTypes,
            final Set<FinderMetadataDetails> dynamicFinderMethods,
            final Map<JavaSymbolName, DateTimeFormatDetails> dateTypes) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
        Validate.isTrue(
                isValid(identifier),
                "Metadata identification string '%s' does not appear to be a valid",
                identifier);
        Validate.notNull(annotationValues, "Annotation values required");
        Validate.notNull(specialDomainTypes, "Special domain type map required");
        Validate.notNull(dynamicFinderMethods,
                "Dynamoic finder methods required");
        
        this.dateTypes = dateTypes;
        
        if (!isValid()) {
            return;
        }

        this.annotationValues = annotationValues;
        controllerPath = annotationValues.getPath();
        formBackingType = annotationValues.getFormBackingObject();
        this.specialDomainTypes = specialDomainTypes;

        if (dynamicFinderMethods.isEmpty()) {
            valid = false;
            return;
        }

        javaTypeMetadataHolder = specialDomainTypes.get(formBackingType);
        Validate.notNull(javaTypeMetadataHolder,
                "Metadata holder required for form backing type %s",
                formBackingType);

        for (final FinderMetadataDetails finder : dynamicFinderMethods) {
            builder.addMethod(getFinderFormMethod(finder));
            builder.addMethod(getFinderMethod(finder));
        }

        itdTypeDetails = builder.build();
    }

    public WebScaffoldAnnotationValues getAnnotationValues() {
        return annotationValues;
    }

    private MethodMetadataBuilder getFinderFormMethod(
            final FinderMetadataDetails finder) {
        Validate.notNull(finder, "Method metadata required for finder");
        final JavaSymbolName finderFormMethodName = new JavaSymbolName(finder
                .getFinderMethodMetadata().getMethodName().getSymbolName()
                + "Form");

        final List<JavaType> methodParameterTypes = new ArrayList<JavaType>();
        final List<JavaSymbolName> methodParameterNames = new ArrayList<JavaSymbolName>();
        final List<JavaType> finderParameterTypes = AnnotatedJavaType
                .convertFromAnnotatedJavaTypes(finder.getFinderMethodMetadata()
                        .getParameterTypes());

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

        boolean needModel = false;
        for (final JavaType finderParameterType : finderParameterTypes) {
            JavaTypeMetadataDetails typeMd = specialDomainTypes
                    .get(finderParameterType);
            JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataHolder = null;
            if (finderParameterType.isCommonCollectionType()) {
                typeMd = specialDomainTypes.get(finderParameterType
                        .getParameters().get(0));
                if (typeMd != null && typeMd.isApplicationType()) {
                    javaTypePersistenceMetadataHolder = typeMd
                            .getPersistenceDetails();
                }
            }
            else if (typeMd != null && typeMd.isEnumType()) {
                bodyBuilder.appendFormalLine("uiModel.addAttribute(\""
                        + typeMd.getPlural().toLowerCase()
                        + "\", java.util.Arrays.asList("
                        + getShortName(finderParameterType)
                        + ".class.getEnumConstants()));");
            }
            else if (typeMd != null && typeMd.isApplicationType()) {
                javaTypePersistenceMetadataHolder = typeMd
                        .getPersistenceDetails();
            }
            if (typeMd != null
                    && javaTypePersistenceMetadataHolder != null
                    && javaTypePersistenceMetadataHolder.getFindAllMethod() != null) {
                bodyBuilder.appendFormalLine("uiModel.addAttribute(\""
                        + typeMd.getPlural().toLowerCase()
                        + "\", "
                        + javaTypePersistenceMetadataHolder.getFindAllMethod()
                                .getMethodCall() + ");");
            }
            needModel = true;
        }
        if (finderParameterTypes.contains(DATE)
                || finderParameterTypes.contains(CALENDAR)) {
            bodyBuilder.appendFormalLine("addDateTimeFormatPatterns(uiModel);");
        }
        bodyBuilder.appendFormalLine("return \""
                + controllerPath
                + "/"
                + finder.getFinderMethodMetadata().getMethodName()
                        .getSymbolName() + "\";");

        if (needModel) {
            methodParameterTypes.add(MODEL);
            methodParameterNames.add(new JavaSymbolName("uiModel"));
        }

        if (governorHasMethod(finderFormMethodName, methodParameterTypes)) {
            return null;
        }

        final List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
        final List<StringAttributeValue> arrayValues = new ArrayList<StringAttributeValue>();
        arrayValues.add(new StringAttributeValue(new JavaSymbolName("value"),
                "find="
                        + finder.getFinderMethodMetadata()
                                .getMethodName()
                                .getSymbolName()
                                .replaceFirst(
                                        "find"
                                                + javaTypeMetadataHolder
                                                        .getPlural(), "")));
        arrayValues.add(new StringAttributeValue(new JavaSymbolName("value"),
                "form"));
        requestMappingAttributes
                .add(new ArrayAttributeValue<StringAttributeValue>(
                        new JavaSymbolName("params"), arrayValues));
        requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName(
                "method"), new EnumDetails(REQUEST_METHOD, new JavaSymbolName(
                "GET"))));
        final AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(
                REQUEST_MAPPING, requestMappingAttributes);
        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        annotations.add(requestMapping);

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), Modifier.PUBLIC, finderFormMethodName,
                JavaType.STRING,
                AnnotatedJavaType.convertFromJavaTypes(methodParameterTypes),
                methodParameterNames, bodyBuilder);
        methodBuilder.setAnnotations(annotations);
        return methodBuilder;
    }

    private MethodMetadataBuilder getFinderMethod(
            final FinderMetadataDetails finderMetadataDetails) {
        Validate.notNull(finderMetadataDetails,
                "Method metadata required for finder");
        final JavaSymbolName finderMethodName = new JavaSymbolName(
                finderMetadataDetails.getFinderMethodMetadata().getMethodName()
                        .getSymbolName());

        final List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
        final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        final StringBuilder methodParams = new StringBuilder();

        boolean dateFieldPresent = !dateTypes.isEmpty();
        for (final FieldMetadata field : finderMetadataDetails
                .getFinderMethodParamFields()) {
            final JavaSymbolName fieldName = field.getFieldName();
            final List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
            final List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
            attributes.add(new StringAttributeValue(
                    new JavaSymbolName("value"), uncapitalize(fieldName
                            .getSymbolName())));
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
                dateFieldPresent = true;
                final AnnotationMetadata annotation = MemberFindingUtils
                        .getAnnotationOfType(field.getAnnotations(),
                                DATE_TIME_FORMAT);
                if (annotation != null) {
                    getShortName(DATE_TIME_FORMAT);
                    annotations.add(annotation);
                }
            }
            parameterNames.add(fieldName);
            parameterTypes.add(new AnnotatedJavaType(field.getFieldType(),
                    annotations));

            if (field.getFieldType().equals(JavaType.BOOLEAN_OBJECT)) {
                methodParams.append(fieldName + " == null ? Boolean.FALSE : "
                        + fieldName + ", ");
            }
            else {
                methodParams.append(fieldName + ", ");
            }
        }

        if (methodParams.length() > 0) {
            methodParams.delete(methodParams.length() - 2,
                    methodParams.length());
        }
        
        final List<AnnotationAttributeValue<?>> firstResultAttributes = new ArrayList<AnnotationAttributeValue<?>>();
        firstResultAttributes.add(new StringAttributeValue(new JavaSymbolName(
                "value"), "page"));
        firstResultAttributes.add(new BooleanAttributeValue(new JavaSymbolName(
                "required"), false));
        final AnnotationMetadataBuilder firstResultAnnotation = new AnnotationMetadataBuilder(
                REQUEST_PARAM, firstResultAttributes);

        final List<AnnotationAttributeValue<?>> maxResultsAttributes = new ArrayList<AnnotationAttributeValue<?>>();
        maxResultsAttributes.add(new StringAttributeValue(new JavaSymbolName(
                "value"), "size"));
        maxResultsAttributes.add(new BooleanAttributeValue(new JavaSymbolName(
                "required"), false));
        final AnnotationMetadataBuilder maxResultAnnotation = new AnnotationMetadataBuilder(
                REQUEST_PARAM, maxResultsAttributes);
        
        final List<AnnotationAttributeValue<?>> sortFieldNameAttributes = new ArrayList<AnnotationAttributeValue<?>>();
        sortFieldNameAttributes.add(new StringAttributeValue(new JavaSymbolName(
                "value"), "sortFieldName"));
        sortFieldNameAttributes.add(new BooleanAttributeValue(new JavaSymbolName(
                "required"), false));
        final AnnotationMetadataBuilder sortFieldNameAnnotation = new AnnotationMetadataBuilder(
                REQUEST_PARAM, sortFieldNameAttributes);
        
        final List<AnnotationAttributeValue<?>> sortOrderAttributes = new ArrayList<AnnotationAttributeValue<?>>();
        sortOrderAttributes.add(new StringAttributeValue(new JavaSymbolName(
                "value"), "sortOrder"));
        sortOrderAttributes.add(new BooleanAttributeValue(new JavaSymbolName(
                "required"), false));
        final AnnotationMetadataBuilder sortOrderAnnotation = new AnnotationMetadataBuilder(
                REQUEST_PARAM, sortOrderAttributes);
        
        
        parameterTypes.add(new AnnotatedJavaType(
                new JavaType(Integer.class.getName()),
                firstResultAnnotation.build()));
        parameterTypes.add(new AnnotatedJavaType(
                new JavaType(Integer.class.getName()),
                maxResultAnnotation.build()));
        parameterTypes.add(new AnnotatedJavaType(
                new JavaType(String.class.getName()),
                sortFieldNameAnnotation.build()));
        parameterTypes.add(new AnnotatedJavaType(
                new JavaType(String.class.getName()),
                sortOrderAnnotation.build()));
        
        parameterTypes.add(new AnnotatedJavaType(MODEL));
        if (getGovernorMethod(finderMethodName,
                AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes)) != null) {
            return null;
        }

        final List<JavaSymbolName> newParamNames = new ArrayList<JavaSymbolName>();
        newParamNames.addAll(parameterNames);
        newParamNames.add(new JavaSymbolName("page"));
        newParamNames.add(new JavaSymbolName("size"));
        newParamNames.add(new JavaSymbolName("sortFieldName"));
        newParamNames.add(new JavaSymbolName("sortOrder"));
        newParamNames.add(new JavaSymbolName("uiModel"));     
        
        final List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
        requestMappingAttributes.add(new StringAttributeValue(
                new JavaSymbolName("params"), "find="
                        + finderMetadataDetails
                                .getFinderMethodMetadata()
                                .getMethodName()
                                .getSymbolName()
                                .replaceFirst(
                                        "find"
                                                + javaTypeMetadataHolder
                                                        .getPlural(), "")));
        requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName(
                "method"), new EnumDetails(REQUEST_METHOD, new JavaSymbolName(
                "GET"))));
        final AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(
                REQUEST_MAPPING, requestMappingAttributes);
        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        annotations.add(requestMapping);

        bodyBuilder.appendFormalLine("if (page != null || size != null) {");
        bodyBuilder.indent();
        bodyBuilder
                .appendFormalLine("int sizeNo = size == null ? 10 : size.intValue();");
        bodyBuilder
                .appendFormalLine("final int firstResult = page == null ? 0 : (page.intValue() - 1) * sizeNo;");
        bodyBuilder.appendFormalLine("uiModel.addAttribute(\""
                + javaTypeMetadataHolder.getPlural().toLowerCase()
                + "\", "
                + getShortName(formBackingType)
                + "."
                + finderMetadataDetails.getFinderMethodMetadata()
                        .getMethodName().getSymbolName() + "("
                + methodParams.toString() + ", sortFieldName, sortOrder).setFirstResult(firstResult).setMaxResults(sizeNo).getResultList());");
        
        char[] methodNameArray = finderMetadataDetails.getFinderMethodMetadata()
                .getMethodName().getSymbolName().toCharArray();
        methodNameArray[0] = Character.toUpperCase(methodNameArray[0]);
        String countMethodName = "count" + new String(methodNameArray);
        
        bodyBuilder.appendFormalLine("float nrOfPages = (float) "
        		+ getShortName(formBackingType)
                + "."
                + countMethodName + "("
                + methodParams.toString() + ") / sizeNo;");
        bodyBuilder
                .appendFormalLine("uiModel.addAttribute(\"maxPages\", (int) ((nrOfPages > (int) nrOfPages || nrOfPages == 0.0) ? nrOfPages + 1 : nrOfPages));");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("} else {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine("uiModel.addAttribute(\""
                + javaTypeMetadataHolder.getPlural().toLowerCase()
                + "\", "
                + getShortName(formBackingType)
                + "."
                + finderMetadataDetails.getFinderMethodMetadata()
                        .getMethodName().getSymbolName() + "("
                + methodParams.toString() + ", sortFieldName, sortOrder).getResultList());");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        
        if (dateFieldPresent) {
            bodyBuilder.appendFormalLine("addDateTimeFormatPatterns(uiModel);");
        }
        bodyBuilder.appendFormalLine("return \"" + controllerPath + "/list\";");

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), Modifier.PUBLIC, finderMethodName, JavaType.STRING,
                parameterTypes, newParamNames, bodyBuilder);
        methodBuilder.setAnnotations(annotations);
        return methodBuilder;
    }

    private String getShortName(final JavaType type) {
        return type.getNameIncludingTypeParameters(false,
                builder.getImportRegistrationResolver());
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

    private String uncapitalize(final String term) {
        // [ROO-1790] this is needed to adhere to the JavaBean naming
        // conventions (see JavaBean spec section 8.8)
        return Introspector.decapitalize(StringUtils.capitalize(term));
    }
}
