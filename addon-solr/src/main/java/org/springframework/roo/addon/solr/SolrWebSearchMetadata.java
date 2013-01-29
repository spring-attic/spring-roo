package org.springframework.roo.addon.solr;

import static org.springframework.roo.model.JavaType.INT_OBJECT;
import static org.springframework.roo.model.SpringJavaType.MODEL_MAP;
import static org.springframework.roo.model.SpringJavaType.REQUEST_MAPPING;
import static org.springframework.roo.model.SpringJavaType.REQUEST_PARAM;
import static org.springframework.roo.model.SpringJavaType.RESPONSE_BODY;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldAnnotationValues;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.BooleanAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooSolrWebSearchable}.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public class SolrWebSearchMetadata extends
        AbstractItdTypeDetailsProvidingMetadataItem {

    private static final String PROVIDES_TYPE_STRING = SolrWebSearchMetadata.class
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

    public SolrWebSearchMetadata(final String identifier,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final SolrWebSearchAnnotationValues annotationValues,
            final WebScaffoldAnnotationValues webScaffoldAnnotationValues,
            final SolrSearchAnnotationValues solrSearchAnnotationValues) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
        Validate.notNull(webScaffoldAnnotationValues,
                "Web scaffold annotation values required");
        Validate.notNull(annotationValues,
                "Solr web searchable annotation values required");
        Validate.notNull(solrSearchAnnotationValues,
                "Solr search annotation values required");
        Validate.isTrue(
                isValid(identifier),
                "Metadata identification string '%s' does not appear to be a valid",
                identifier);

        if (!isValid()) {
            return;
        }

        if (annotationValues.getSearchMethod() != null
                && annotationValues.getSearchMethod().length() > 0) {
            builder.addMethod(getSearchMethod(annotationValues,
                    solrSearchAnnotationValues, webScaffoldAnnotationValues));
        }
        if (annotationValues.getAutoCompleteMethod() != null
                && annotationValues.getAutoCompleteMethod().length() > 0) {
            builder.addMethod(getAutocompleteMethod(annotationValues,
                    solrSearchAnnotationValues, webScaffoldAnnotationValues));
        }

        // Create a representation of the desired output ITD
        itdTypeDetails = builder.build();
    }

    private MethodMetadataBuilder getAutocompleteMethod(
            final SolrWebSearchAnnotationValues solrWebSearchAnnotationValues,
            final SolrSearchAnnotationValues searchAnnotationValues,
            final WebScaffoldAnnotationValues webScaffoldAnnotationValues) {
        final JavaSymbolName methodName = new JavaSymbolName(
                solrWebSearchAnnotationValues.getAutoCompleteMethod());
        if (governorHasMethodWithSameName(methodName)) {
            return null;
        }

        final List<AnnotationAttributeValue<?>> reqMapAttributes = new ArrayList<AnnotationAttributeValue<?>>();
        reqMapAttributes.add(new StringAttributeValue(new JavaSymbolName(
                "params"), "autocomplete"));

        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        annotations.add(new AnnotationMetadataBuilder(REQUEST_MAPPING,
                reqMapAttributes));
        annotations.add(new AnnotationMetadataBuilder(RESPONSE_BODY));

        final List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
        final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

        parameterTypes.add(new AnnotatedJavaType(JavaType.STRING,
                getRequestParamAnnotation("q", true)));
        parameterNames.add(new JavaSymbolName("q"));

        parameterTypes.add(new AnnotatedJavaType(JavaType.STRING,
                getRequestParamAnnotation("facetFields", true)));
        parameterNames.add(new JavaSymbolName("facetFields"));

        parameterTypes.add(new AnnotatedJavaType(INT_OBJECT,
                getRequestParamAnnotation("rows", false)));
        parameterNames.add(new JavaSymbolName("rows"));

        final String solrQuerySimpleName = new JavaType(
                "org.apache.solr.client.solrj.SolrQuery")
                .getNameIncludingTypeParameters(false,
                        builder.getImportRegistrationResolver());
        final String facetFieldSimpleName = new JavaType(
                "org.apache.solr.client.solrj.response.FacetField")
                .getNameIncludingTypeParameters(false,
                        builder.getImportRegistrationResolver());

        final String queryResponseSimpleName = new JavaType(
                "org.apache.solr.client.solrj.response.QueryResponse")
                .getNameIncludingTypeParameters(false,
                        builder.getImportRegistrationResolver());

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder
                .appendFormalLine("StringBuilder dojo = new StringBuilder(\"{identifier:'id',label:'label',items:[\");");
        bodyBuilder.appendFormalLine(solrQuerySimpleName
                + " solrQuery = new SolrQuery(q.toLowerCase());");
        bodyBuilder
                .appendFormalLine("solrQuery.setRows(rows == null ? 10 : rows);");
        bodyBuilder.appendFormalLine("solrQuery.setFacetMinCount(1);");
        bodyBuilder
                .appendFormalLine("solrQuery.addFacetField(facetFields.split(\",\"));");
        bodyBuilder.appendFormalLine(queryResponseSimpleName
                + " response = "
                + webScaffoldAnnotationValues.getFormBackingObject()
                        .getNameIncludingTypeParameters(false,
                                builder.getImportRegistrationResolver()) + "."
                + searchAnnotationValues.getSearchMethod() + "(solrQuery);");
        bodyBuilder.appendFormalLine("for (" + facetFieldSimpleName
                + " field: response.getFacetFields()) {");
        bodyBuilder.indent();
        bodyBuilder
                .appendFormalLine("if (response.getResults().get(0) != null) {");
        bodyBuilder.indent();
        bodyBuilder
                .appendFormalLine("Object fieldValue = response.getResults().get(0).getFieldValue(field.getName());");
        bodyBuilder.appendFormalLine("if (fieldValue != null) {");
        bodyBuilder.indent();
        bodyBuilder
                .appendFormalLine("dojo.append(\"{label:'\").append(fieldValue).append(\" (\").append(field.getValueCount()).append(\")\").append(\"',\").append(\"id:'\").append(field.getName()).append(\"'},\");");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder.appendFormalLine("dojo.append(\"]}\");");
        bodyBuilder.appendFormalLine("return dojo.toString();");

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), Modifier.PUBLIC, methodName, JavaType.STRING,
                parameterTypes, parameterNames, bodyBuilder);
        methodBuilder.setAnnotations(annotations);
        return methodBuilder;
    }

    private AnnotationMetadata getRequestParamAnnotation(
            final String paramName, final boolean required) {
        final List<AnnotationAttributeValue<?>> attributeValue = new ArrayList<AnnotationAttributeValue<?>>();
        if (!required) {
            attributeValue.add(new BooleanAttributeValue(new JavaSymbolName(
                    "required"), false));
        }
        attributeValue.add(new StringAttributeValue(
                new JavaSymbolName("value"), paramName));
        return new AnnotationMetadataBuilder(REQUEST_PARAM, attributeValue)
                .build();
    }

    private MethodMetadataBuilder getSearchMethod(
            final SolrWebSearchAnnotationValues solrWebSearchAnnotationValues,
            final SolrSearchAnnotationValues searchAnnotationValues,
            final WebScaffoldAnnotationValues webScaffoldAnnotationValues) {
        final JavaType targetObject = webScaffoldAnnotationValues
                .getFormBackingObject();
        Validate.notNull(targetObject,
                "Could not aquire form backing object for the '%s' controller",
                webScaffoldAnnotationValues.getGovernorTypeDetails().getName()
                        .getFullyQualifiedTypeName());

        final JavaSymbolName methodName = new JavaSymbolName(
                solrWebSearchAnnotationValues.getSearchMethod());
        if (governorHasMethodWithSameName(methodName)) {
            return null;
        }

        final List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
        final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

        parameterTypes.add(new AnnotatedJavaType(new JavaType("String"),
                getRequestParamAnnotation("q", false)));
        parameterNames.add(new JavaSymbolName("q"));

        parameterTypes.add(new AnnotatedJavaType(new JavaType("String"),
                getRequestParamAnnotation("fq", false)));
        parameterNames.add(new JavaSymbolName("facetQuery"));

        parameterTypes.add(new AnnotatedJavaType(new JavaType("Integer"),
                getRequestParamAnnotation("page", false)));
        parameterNames.add(new JavaSymbolName("page"));

        parameterTypes.add(new AnnotatedJavaType(new JavaType("Integer"),
                getRequestParamAnnotation("size", false)));
        parameterNames.add(new JavaSymbolName("size"));

        parameterTypes.add(new AnnotatedJavaType(MODEL_MAP));
        parameterNames.add(new JavaSymbolName("modelMap"));

        final List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
        requestMappingAttributes.add(new StringAttributeValue(
                new JavaSymbolName("params"), "search"));
        final AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(
                REQUEST_MAPPING, requestMappingAttributes);

        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        annotations.add(requestMapping);

        final String solrQuerySimpleName = new JavaType(
                "org.apache.solr.client.solrj.SolrQuery")
                .getNameIncludingTypeParameters(false,
                        builder.getImportRegistrationResolver());
        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("if (q != null && q.length() != 0) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine(solrQuerySimpleName
                + " solrQuery = new "
                + solrQuerySimpleName
                + "(\""
                + webScaffoldAnnotationValues.getFormBackingObject()
                        .getSimpleTypeName().toLowerCase()
                + "_solrsummary_t:\" + q.toLowerCase());");

        bodyBuilder
                .appendFormalLine("if (page != null) solrQuery.setStart(page);");
        bodyBuilder
                .appendFormalLine("if (size != null) solrQuery.setRows(size);");
        bodyBuilder
                .appendFormalLine("modelMap.addAttribute(\"searchResults\", "
                        + targetObject.getFullyQualifiedTypeName() + "."
                        + searchAnnotationValues.getSearchMethod()
                        + "(solrQuery).getResults());");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder.appendFormalLine("return \""
                + webScaffoldAnnotationValues.getPath() + "/search\";");

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), Modifier.PUBLIC, methodName, JavaType.STRING,
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