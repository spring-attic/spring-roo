package org.springframework.roo.addon.solr;

import static org.springframework.roo.model.JavaType.INT_OBJECT;
import static org.springframework.roo.model.SpringJavaType.MODEL_MAP;
import static org.springframework.roo.model.SpringJavaType.REQUEST_MAPPING;
import static org.springframework.roo.model.SpringJavaType.REQUEST_PARAM;
import static org.springframework.roo.model.SpringJavaType.RESPONSE_BODY;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldAnnotationValues;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
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
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Metadata for {@link RooSolrWebSearchable}.
 *
 * @author Stefan Schmidt
 * @since 1.1
 */
public class SolrWebSearchMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

	// Constants
	private static final String PROVIDES_TYPE_STRING = SolrWebSearchMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);

	public SolrWebSearchMetadata(final String identifier, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final SolrWebSearchAnnotationValues annotationValues, final WebScaffoldAnnotationValues webScaffoldAnnotationValues, final SolrSearchAnnotationValues solrSearchAnnotationValues) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.notNull(webScaffoldAnnotationValues, "Web scaffold annotation values required");
		Assert.notNull(annotationValues, "Solr web searchable annotation values required");
		Assert.notNull(solrSearchAnnotationValues, "Solr search annotation values required");
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");

		if (!isValid()) {
			return;
		}

		if (annotationValues.getSearchMethod() != null && annotationValues.getSearchMethod().length() > 0) {
			builder.addMethod(getSearchMethod(annotationValues, solrSearchAnnotationValues, webScaffoldAnnotationValues));
		}
		if (annotationValues.getAutoCompleteMethod() != null && annotationValues.getAutoCompleteMethod().length() > 0) {
			builder.addMethod(getAutocompleteMethod(annotationValues, solrSearchAnnotationValues, webScaffoldAnnotationValues));
		}

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	private MethodMetadata getSearchMethod(final SolrWebSearchAnnotationValues solrWebSearchAnnotationValues, final SolrSearchAnnotationValues searchAnnotationValues, final WebScaffoldAnnotationValues webScaffoldAnnotationValues) {
		JavaType targetObject = webScaffoldAnnotationValues.getFormBackingObject();
		Assert.notNull(targetObject, "Could not aquire form backing object for the '" + webScaffoldAnnotationValues.getGovernorTypeDetails().getName().getFullyQualifiedTypeName() + "' controller");

		JavaSymbolName methodName = new JavaSymbolName(solrWebSearchAnnotationValues.getSearchMethod());
		MethodMetadata method = methodExists(methodName);
		if (method != null) return method;

		List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
		List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

		parameterTypes.add(new AnnotatedJavaType(new JavaType("String"), getRequestParamAnnotation("q", false)));
		parameterNames.add(new JavaSymbolName("q"));

		parameterTypes.add(new AnnotatedJavaType(new JavaType("String"), getRequestParamAnnotation("fq", false)));
		parameterNames.add(new JavaSymbolName("facetQuery"));

		parameterTypes.add(new AnnotatedJavaType(new JavaType("Integer"), getRequestParamAnnotation("page", false)));
		parameterNames.add(new JavaSymbolName("page"));

		parameterTypes.add(new AnnotatedJavaType(new JavaType("Integer"), getRequestParamAnnotation("size", false)));
		parameterNames.add(new JavaSymbolName("size"));

		parameterTypes.add(new AnnotatedJavaType(MODEL_MAP));
		parameterNames.add(new JavaSymbolName("modelMap"));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("params"), "search"));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(REQUEST_MAPPING, requestMappingAttributes);

		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		String solrQuerySimpleName = new JavaType("org.apache.solr.client.solrj.SolrQuery").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (q != null && q.length() != 0) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(solrQuerySimpleName + " solrQuery = new " + solrQuerySimpleName + "(\"" + webScaffoldAnnotationValues.getFormBackingObject().getSimpleTypeName().toLowerCase() + "_solrsummary_t:\" + q.toLowerCase());");

		bodyBuilder.appendFormalLine("if (page != null) solrQuery.setStart(page);");
		bodyBuilder.appendFormalLine("if (size != null) solrQuery.setRows(size);");
		bodyBuilder.appendFormalLine("modelMap.addAttribute(\"searchResults\", " + targetObject.getFullyQualifiedTypeName() + "." + searchAnnotationValues.getSearchMethod() + "(solrQuery).getResults());");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return \"" + webScaffoldAnnotationValues.getPath() + "/search\";");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getAutocompleteMethod(final SolrWebSearchAnnotationValues solrWebSearchAnnotationValues, final SolrSearchAnnotationValues searchAnnotationValues, final WebScaffoldAnnotationValues webScaffoldAnnotationValues) {
		JavaSymbolName methodName = new JavaSymbolName(solrWebSearchAnnotationValues.getAutoCompleteMethod());

		MethodMetadata method = methodExists(methodName);
		if (method != null) return method;

		List<AnnotationAttributeValue<?>> reqMapAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		reqMapAttributes.add(new StringAttributeValue(new JavaSymbolName("params"), "autocomplete"));

		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(new AnnotationMetadataBuilder(REQUEST_MAPPING, reqMapAttributes));
		annotations.add(new AnnotationMetadataBuilder(RESPONSE_BODY));

		List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
		List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

		parameterTypes.add(new AnnotatedJavaType(JavaType.STRING, getRequestParamAnnotation("q", true)));
		parameterNames.add(new JavaSymbolName("q"));

		parameterTypes.add(new AnnotatedJavaType(JavaType.STRING, getRequestParamAnnotation("facetFields", true)));
		parameterNames.add(new JavaSymbolName("facetFields"));

		parameterTypes.add(new AnnotatedJavaType(INT_OBJECT, getRequestParamAnnotation("rows", false)));
		parameterNames.add(new JavaSymbolName("rows"));

		String solrQuerySimpleName = new JavaType("org.apache.solr.client.solrj.SolrQuery").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		String facetFieldSimpleName = new JavaType("org.apache.solr.client.solrj.response.FacetField").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());

		String queryResponseSimpleName = new JavaType("org.apache.solr.client.solrj.response.QueryResponse").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("StringBuilder dojo = new StringBuilder(\"{identifier:'id',label:'label',items:[\");");
		bodyBuilder.appendFormalLine(solrQuerySimpleName + " solrQuery = new SolrQuery(q.toLowerCase());");
		bodyBuilder.appendFormalLine("solrQuery.setRows(rows == null ? 10 : rows);");
		bodyBuilder.appendFormalLine("solrQuery.setFacetMinCount(1);");
		bodyBuilder.appendFormalLine("solrQuery.addFacetField(facetFields.split(\",\"));");
		bodyBuilder.appendFormalLine(queryResponseSimpleName + " response = " + webScaffoldAnnotationValues.getFormBackingObject().getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "." + searchAnnotationValues.getSearchMethod() + "(solrQuery);");
		bodyBuilder.appendFormalLine("for (" + facetFieldSimpleName + " field: response.getFacetFields()) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("if (response.getResults().get(0) != null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("Object fieldValue = response.getResults().get(0).getFieldValue(field.getName());");
		bodyBuilder.appendFormalLine("if (fieldValue != null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("dojo.append(\"{label:'\").append(fieldValue).append(\" (\").append(field.getValueCount()).append(\")\").append(\"',\").append(\"id:'\").append(field.getName()).append(\"'},\");");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("dojo.append(\"]}\");");
		bodyBuilder.appendFormalLine("return dojo.toString();");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private AnnotationMetadata getRequestParamAnnotation(final String paramName, final boolean required) {
		List<AnnotationAttributeValue<?>> attributeValue = new ArrayList<AnnotationAttributeValue<?>>();
		if (!required) {
			attributeValue.add(new BooleanAttributeValue(new JavaSymbolName("required"), false));
		}
		attributeValue.add(new StringAttributeValue(new JavaSymbolName("value"), paramName));
		return new AnnotationMetadataBuilder(REQUEST_PARAM, attributeValue).build();
	}

	private MethodMetadata methodExists(final JavaSymbolName methodName) {
		// We have no access to method parameter information, so we scan by name alone and treat any match as authoritative
		// We do not scan the superclass, as the caller is expected to know we'll only scan the current class
		for (MethodMetadata method : governorTypeDetails.getDeclaredMethods()) {
			if (method.getMethodName().equals(methodName)) {
				// Found a method of the expected name; we won't check method parameters though
				return method;
			}
		}
		return null;
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
}