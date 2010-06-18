package org.springframework.roo.addon.solr;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.addon.web.mvc.controller.WebScaffoldMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.DefaultMethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.BooleanAttributeValue;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataService;
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
 *
 */
public class SolrWebSearchMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

	private static final String PROVIDES_TYPE_STRING = SolrWebSearchMetadata.class.getName(); 
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	
	private String controllerPath;
	private MetadataService metadataService;
	private WebScaffoldMetadata webScaffoldMetadata;
	private SolrWebSearchAnnotationValues annotationValues;

	public SolrWebSearchMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, MetadataService metadataService, SolrWebSearchAnnotationValues annotationValues, WebScaffoldMetadata webScaffoldMetadata) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(webScaffoldMetadata, "Web scaffold metadata required");
		Assert.notNull(annotationValues, "Solr web searchable annotation values required");
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");

		if (!isValid()) {
			return;
		}

		controllerPath = webScaffoldMetadata.getAnnotationValues().getPath();
		this.webScaffoldMetadata = webScaffoldMetadata;
		this.metadataService = metadataService;
		this.annotationValues = annotationValues;
		
		if (annotationValues.getSearchMethod() != null && annotationValues.getSearchMethod().length() > 0) {
			builder.addMethod(getSearchMethod());
		}
		if (annotationValues.getAutoCompleteMethod() != null && annotationValues.getAutoCompleteMethod().length() > 0) {
			builder.addMethod(getAutocompleteMethod());
		}
		
		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}
	
	private MethodMetadata getSearchMethod() {
		JavaType targetObject = webScaffoldMetadata.getAnnotationValues().getFormBackingObject();
		Assert.notNull(targetObject, "Could not aquire form backing object for the '" + WebScaffoldMetadata.getJavaType(webScaffoldMetadata.getId()).getFullyQualifiedTypeName() + "' controller");
		
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getSearchMethod());
		MethodMetadata method = methodExists(methodName);
		if (method != null) return method;

		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		
		paramTypes.add(new AnnotatedJavaType(new JavaType("String"), getRequestParamAnnotation("q", false)));
		paramNames.add(new JavaSymbolName("q"));
		
		paramTypes.add(new AnnotatedJavaType(new JavaType("String"), getRequestParamAnnotation("fq", false)));
		paramNames.add(new JavaSymbolName("facetQuery"));
		
		paramTypes.add(new AnnotatedJavaType(new JavaType("Integer"), getRequestParamAnnotation("page", false)));
		paramNames.add(new JavaSymbolName("page"));
		
		paramTypes.add(new AnnotatedJavaType(new JavaType("Integer"), getRequestParamAnnotation("size", false)));
		paramNames.add(new JavaSymbolName("size"));
		
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.ModelMap"), null));	
		paramNames.add(new JavaSymbolName("modelMap"));
		
		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("params"), "search"));
		AnnotationMetadata requestMapping = new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
				
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		annotations.add(requestMapping);

		String solrQuerySimpleName = new JavaType("org.apache.solr.client.solrj.SolrQuery").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder(); 	
		bodyBuilder.appendFormalLine("if (q != null && q.length() != 0) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(solrQuerySimpleName + " solrQuery = new " + solrQuerySimpleName + "(\"" + webScaffoldMetadata.getAnnotationValues().getFormBackingObject().getSimpleTypeName().toLowerCase() + "_solrsummary_t:\" + q.toLowerCase());");

		bodyBuilder.appendFormalLine("if (page != null) solrQuery.setStart(page);");
		bodyBuilder.appendFormalLine("if (size != null) solrQuery.setRows(size);");		
		bodyBuilder.appendFormalLine("modelMap.addAttribute(\"searchResults\", " + targetObject.getFullyQualifiedTypeName() + "." + getSolrMetadata().getAnnotationValues().getSearchMethod() + "(solrQuery).getResults());");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/search\";");
		
		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, methodName, new JavaType(String.class.getName()), paramTypes, paramNames, annotations, null, bodyBuilder.getOutput());
	}
	
	private MethodMetadata getAutocompleteMethod() {
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getAutoCompleteMethod());		
		
		MethodMetadata method = methodExists(methodName);
		if (method != null) return method;
		
		List<AnnotationAttributeValue<?>> reqMapAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		reqMapAttributes.add(new StringAttributeValue(new JavaSymbolName("params"), "autocomplete"));
		
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		annotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), reqMapAttributes));
		annotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.ResponseBody"), new ArrayList<AnnotationAttributeValue<?>>()));
		
		JavaType string = new JavaType(String.class.getName());
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		
		paramTypes.add(new AnnotatedJavaType(string, getRequestParamAnnotation("q", true)));
		paramNames.add(new JavaSymbolName("q"));
		
		paramTypes.add(new AnnotatedJavaType(string, getRequestParamAnnotation("facetFields", true)));
		paramNames.add(new JavaSymbolName("facetFields"));
		
		paramTypes.add(new AnnotatedJavaType(new JavaType(Integer.class.getName()), getRequestParamAnnotation("rows", false)));
		paramNames.add(new JavaSymbolName("rows"));
	
		String solrQuerySimpleName = new JavaType("org.apache.solr.client.solrj.SolrQuery").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		String facetFieldSimpleName = new JavaType("org.apache.solr.client.solrj.response.FacetField").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());

		String queryResponseSimpleName = new JavaType("org.apache.solr.client.solrj.response.QueryResponse").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("StringBuilder dojo = new StringBuilder(\"{identifier:'id',label:'label',items:[\");");
		bodyBuilder.appendFormalLine(solrQuerySimpleName + " solrQuery = new SolrQuery(q.toLowerCase());");
		bodyBuilder.appendFormalLine("solrQuery.setRows(rows == null ? 10 : rows);");
		bodyBuilder.appendFormalLine("solrQuery.setFacetMinCount(1);");
		bodyBuilder.appendFormalLine("solrQuery.addFacetField(facetFields.split(\",\"));");
		bodyBuilder.appendFormalLine(queryResponseSimpleName + " response = " + webScaffoldMetadata.getAnnotationValues().getFormBackingObject().getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "." + getSolrMetadata().getAnnotationValues().getSearchMethod() + "(solrQuery);");
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
		
		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, methodName, string, paramTypes, paramNames, annotations, new ArrayList<JavaType>(), bodyBuilder.getOutput());
	}
	
	private SolrMetadata getSolrMetadata() {
		JavaType targetObject = webScaffoldMetadata.getAnnotationValues().getFormBackingObject();
		Assert.notNull(targetObject, "Could not aquire form backing object for the '" + WebScaffoldMetadata.getJavaType(webScaffoldMetadata.getId()).getFullyQualifiedTypeName() + "' controller");
		
		SolrMetadata solrMetadata = (SolrMetadata) metadataService.get(SolrMetadata.createIdentifier(targetObject, Path.SRC_MAIN_JAVA));
		Assert.notNull(solrMetadata, "Could not determine SolrMetadata for type '" + targetObject.getFullyQualifiedTypeName() + "'");
		return solrMetadata;
	}
	
	private List<AnnotationMetadata> getRequestParamAnnotation(String paramName, boolean required) {
		List<AnnotationAttributeValue<?>> attributeValue = new ArrayList<AnnotationAttributeValue<?>>();
		if (!required) {
			attributeValue.add(new BooleanAttributeValue(new JavaSymbolName("required"), false));
		}
		attributeValue.add(new StringAttributeValue(new JavaSymbolName("value"), paramName));
		List<AnnotationMetadata> paramAnnotations = new ArrayList<AnnotationMetadata>();
		paramAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.RequestParam"), attributeValue));
		return paramAnnotations;
	}
	
	private MethodMetadata methodExists(JavaSymbolName methodName) {
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

	public static final String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}
	
	public static final String createIdentifier(JavaType javaType, Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static final JavaType getJavaType(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static final Path getPath(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
}