package org.springframework.roo.addon.web.mvc.controller.scaffold.json;

import java.beans.Introspector;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;

import org.springframework.roo.addon.json.JsonMetadata;
import org.springframework.roo.addon.web.mvc.controller.RooWebScaffold;
import org.springframework.roo.addon.web.mvc.controller.details.FinderMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.details.JavaTypeMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.details.JavaTypePersistenceMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldAnnotationValues;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
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
import org.springframework.roo.classpath.itd.ItdSourceFileComposer;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.ReservedWords;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for Json functionality provided through {@link RooWebScaffold}.
 * 
 * @author Stefan Schmidt
 * @since 1.1.3
 */
public class WebJsonMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	private static final String PROVIDES_TYPE_STRING = WebJsonMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);

	private WebScaffoldAnnotationValues annotationValues;
	private String entityName;
	private JsonMetadata jsonMetadata;
	private MemberDetails memberDetails;
	private JavaType formBackingType;
	private JavaTypeMetadataDetails javaTypeMetadataHolder;

	public WebJsonMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, WebScaffoldAnnotationValues annotationValues, MemberDetails memberDetails, SortedMap<JavaType, JavaTypeMetadataDetails> specialDomainTypes, Set<FinderMetadataDetails> dynamicFinderMethods, JsonMetadata jsonMetadata) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(specialDomainTypes, "Special domain type map required");
		Assert.notNull(memberDetails, "Member details required");
		Assert.notNull(dynamicFinderMethods, "Array of dynamic finder methods cannot be null");
		Assert.notNull(jsonMetadata, "Json metadata required");
		if (!isValid()) {
			return;
		}
		this.annotationValues = annotationValues;
		this.entityName = uncapitalize(annotationValues.getFormBackingObject().getSimpleTypeName());
		if (ReservedWords.RESERVED_JAVA_KEYWORDS.contains(this.entityName)) {
			this.entityName = "_" + entityName;
		}
		this.formBackingType = annotationValues.getFormBackingObject();
		this.memberDetails = memberDetails;
		javaTypeMetadataHolder = specialDomainTypes.get(formBackingType);
		Assert.notNull(javaTypeMetadataHolder, "Metadata holder required for form backing type: " + formBackingType);

		this.jsonMetadata = jsonMetadata;
		builder.addMethod(getJsonShowMethod());
		builder.addMethod(getJsonListMethod());
		if (annotationValues.isCreate()) {
			builder.addMethod(getJsonCreateMethod());
			builder.addMethod(getCreateFromJsonArrayMethod());
		} 
		if (annotationValues.isUpdate()) {
			builder.addMethod(getJsonUpdateMethod());
			builder.addMethod(getUpdateFromJsonArrayMethod());
		}
		if (annotationValues.isDelete()) {
			builder.addMethod(getJsonDeleteMethod());
		}
		if (annotationValues.isExposeFinders() && dynamicFinderMethods.size() > 0) {
			for (FinderMetadataDetails finder : new TreeSet<FinderMetadataDetails>(dynamicFinderMethods)) {
				builder.addMethod(getFinderJsonMethod(finder));
			}
		}
		
		itdTypeDetails = builder.build();

		new ItdSourceFileComposer(itdTypeDetails);
	}

	public WebScaffoldAnnotationValues getAnnotationValues() {
		return annotationValues;
	}

	private MethodMetadata getJsonShowMethod() {
		JavaSymbolName toJsonMethodName = jsonMetadata.getToJsonMethodName();
		JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataHolder = javaTypeMetadataHolder.getPersistenceDetails();
		if (toJsonMethodName == null || javaTypePersistenceMetadataHolder == null || javaTypePersistenceMetadataHolder.getFindMethod() == null) {
			// Mandatory input is missing (ROO-589)
			return null;
		}
		
		JavaSymbolName methodName = new JavaSymbolName("showJson");

		List<AnnotationMetadata> parameters = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("value"), javaTypePersistenceMetadataHolder.getIdentifierField().getFieldName().getSymbolName()));
		AnnotationMetadataBuilder pathVariableAnnotation = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.PathVariable"), attributes);
		parameters.add(pathVariableAnnotation.build());
		
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(javaTypePersistenceMetadataHolder.getIdentifierField().getFieldType(), parameters));
		
		MethodMetadata jsonShowMethod = methodExists(methodName, paramTypes);
		if (jsonShowMethod != null) return jsonShowMethod;

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(javaTypePersistenceMetadataHolder.getIdentifierField().getFieldName().getSymbolName()));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/{" + javaTypePersistenceMetadataHolder.getIdentifierField().getFieldName().getSymbolName() + "}"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("GET"))));
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("headers"), "Accept=application/json"));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.ResponseBody")));

		String beanShortName = getShortName(formBackingType);
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(beanShortName + " " + beanShortName.toLowerCase() + " = " + beanShortName + "." + javaTypePersistenceMetadataHolder.getFindMethod().getMethodName() + "(" + javaTypePersistenceMetadataHolder.getIdentifierField().getFieldName().getSymbolName() + ");");
		String httpHeadersShortName = getShortName(new JavaType("org.springframework.http.HttpHeaders"));
		String responseEntityShortName = getShortName(new JavaType("org.springframework.http.ResponseEntity"));
		String httpStatusShortName = getShortName(new JavaType("org.springframework.http.HttpStatus"));
		bodyBuilder.appendFormalLine(httpHeadersShortName + " headers = new " + httpHeadersShortName + "();");
		bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \"application/text; charset=utf-8\");");
		bodyBuilder.appendFormalLine("if (" + beanShortName.toLowerCase() + " == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return new " + responseEntityShortName + "<String>(headers, " + httpStatusShortName + ".NOT_FOUND);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return new " + responseEntityShortName + "<String>(" + beanShortName.toLowerCase() + "." + toJsonMethodName.getSymbolName() + "(), headers, " +  httpStatusShortName + ".OK);");

		JavaType returnType = new JavaType("org.springframework.http.ResponseEntity", 0, DataType.TYPE, null, Arrays.asList(JavaType.STRING_OBJECT));
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getJsonCreateMethod() {
		JavaSymbolName fromJsonMethodName = jsonMetadata.getFromJsonMethodName();
		JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataHolder = javaTypeMetadataHolder.getPersistenceDetails();
		if (fromJsonMethodName == null || javaTypePersistenceMetadataHolder == null || javaTypePersistenceMetadataHolder.getPersistMethod() == null) {
			return null;
		}
		
		JavaSymbolName methodName = new JavaSymbolName("createFromJson");
		
		List<AnnotationMetadata> parameters = new ArrayList<AnnotationMetadata>();
		AnnotationMetadataBuilder requestBodyAnnotation = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestBody"));
		parameters.add(requestBodyAnnotation.build());
		
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(JavaType.STRING_OBJECT, parameters));
		
		MethodMetadata jsonCreateMethod = methodExists(methodName, paramTypes);
		if (jsonCreateMethod != null) return jsonCreateMethod;

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("json"));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("POST"))));
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("headers"), "Accept=application/json"));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(formBackingType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "." + fromJsonMethodName.getSymbolName() + "(json)." + javaTypePersistenceMetadataHolder.getPersistMethod().getMethodName().getSymbolName() + "();");
		String httpHeadersShortName = getShortName(new JavaType("org.springframework.http.HttpHeaders"));
		bodyBuilder.appendFormalLine(httpHeadersShortName + " headers= new " + httpHeadersShortName + "();");
		bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \"application/text\");");
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(headers, " + new JavaType("org.springframework.http.HttpStatus").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".CREATED);");

		JavaType returnType = new JavaType("org.springframework.http.ResponseEntity", 0, DataType.TYPE, null, Arrays.asList(JavaType.STRING_OBJECT));

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getCreateFromJsonArrayMethod() {
		JavaSymbolName fromJsonArrayMethodName = jsonMetadata.getFromJsonArrayMethodName();
		JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataHolder = javaTypeMetadataHolder.getPersistenceDetails();
		if (fromJsonArrayMethodName == null || javaTypePersistenceMetadataHolder == null || javaTypePersistenceMetadataHolder.getPersistMethod() == null) {
			return null;
		}
		JavaSymbolName methodName = new JavaSymbolName("createFromJsonArray");

		List<AnnotationMetadata> parameters = new ArrayList<AnnotationMetadata>();
		AnnotationMetadataBuilder requestBodyAnnotation = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestBody"));
		parameters.add(requestBodyAnnotation.build());

		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(JavaType.STRING_OBJECT, parameters));

		MethodMetadata existingMethod = methodExists(methodName, paramTypes);
		if (existingMethod != null) return existingMethod;
		
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("json"));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/jsonArray"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("POST"))));
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("headers"), "Accept=application/json"));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		String beanName = formBackingType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());

		List<JavaType> params = new ArrayList<JavaType>();
		params.add(formBackingType);
		bodyBuilder.appendFormalLine("for (" + beanName + " " + entityName + ": " + beanName + "." + fromJsonArrayMethodName.getSymbolName() + "(json)) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(entityName + "." + javaTypePersistenceMetadataHolder.getPersistMethod().getMethodName().getSymbolName() + "();");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		String httpHeadersShortName = getShortName(new JavaType("org.springframework.http.HttpHeaders"));
		bodyBuilder.appendFormalLine(httpHeadersShortName + " headers= new " + httpHeadersShortName + "();");
		bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \"application/text\");");
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(headers, " + new JavaType("org.springframework.http.HttpStatus").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".CREATED);");

		JavaType returnType = new JavaType("org.springframework.http.ResponseEntity", 0, DataType.TYPE, null, Arrays.asList(JavaType.STRING_OBJECT));
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getJsonListMethod() {
		JavaSymbolName toJsonArrayMethodName = jsonMetadata.getToJsonArrayMethodName();
		JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataHolder = javaTypeMetadataHolder.getPersistenceDetails();
		if (toJsonArrayMethodName == null || javaTypePersistenceMetadataHolder == null || javaTypePersistenceMetadataHolder.getFindAllMethod() == null) {
			return null;
		}
		
		// See if the type itself declared the method
		JavaSymbolName methodName = new JavaSymbolName("listJson");
		MethodMetadata result = MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName, null);
		if (result != null) {
			return result;
		}
		
		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("headers"), "Accept=application/json"));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);
		
		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.ResponseBody")));
		
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		String entityName = formBackingType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		String httpHeadersShortName = getShortName(new JavaType("org.springframework.http.HttpHeaders"));
		String responseEntityShortName = getShortName(new JavaType("org.springframework.http.ResponseEntity"));
		String httpStatusShortName = getShortName(new JavaType("org.springframework.http.HttpStatus"));
		bodyBuilder.appendFormalLine(httpHeadersShortName + " headers = new " + httpHeadersShortName + "();");
		bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \"application/text; charset=utf-8\");");
		bodyBuilder.appendFormalLine("return new " + responseEntityShortName + "<String>(" + entityName + "." + toJsonArrayMethodName.getSymbolName() + "(" + entityName + "." + javaTypePersistenceMetadataHolder.getFindAllMethod().getMethodName() + "()), headers, " +  httpStatusShortName + ".OK);");
		
		JavaType returnType = new JavaType("org.springframework.http.ResponseEntity", 0, DataType.TYPE, null, Arrays.asList(JavaType.STRING_OBJECT));
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}
	
	private MethodMetadata getJsonUpdateMethod() {
		JavaSymbolName fromJsonMethodName = jsonMetadata.getFromJsonMethodName();
		JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataHolder = javaTypeMetadataHolder.getPersistenceDetails();
		if (fromJsonMethodName == null || javaTypePersistenceMetadataHolder == null || javaTypePersistenceMetadataHolder.getMergeMethod() == null) {
			return null;
		}
		JavaSymbolName methodName = new JavaSymbolName("updateFromJson");
		
		List<AnnotationMetadata> parameters = new ArrayList<AnnotationMetadata>();
		AnnotationMetadataBuilder requestBodyAnnotation = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestBody"));
		parameters.add(requestBodyAnnotation.build());
		
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(JavaType.STRING_OBJECT, parameters));
		
		MethodMetadata jsonCreateMethod = methodExists(methodName, paramTypes);
		if (jsonCreateMethod != null) return jsonCreateMethod;
		
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("json"));
		
		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("PUT"))));
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("headers"), "Accept=application/json"));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);
		
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		String beanShortName = formBackingType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		String httpHeadersShortName = getShortName(new JavaType("org.springframework.http.HttpHeaders"));
		bodyBuilder.appendFormalLine(httpHeadersShortName + " headers= new " + httpHeadersShortName + "();");
		bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \"application/text\");");
		bodyBuilder.appendFormalLine("if (" + beanShortName + "." + fromJsonMethodName.getSymbolName() + "(json)." + javaTypePersistenceMetadataHolder.getMergeMethod().getMethodName().getSymbolName() + "() == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(headers, " + new JavaType("org.springframework.http.HttpStatus").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".NOT_FOUND);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(headers, " + new JavaType("org.springframework.http.HttpStatus").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".OK);");
		
		JavaType returnType = new JavaType("org.springframework.http.ResponseEntity", 0, DataType.TYPE, null, Arrays.asList(JavaType.STRING_OBJECT));
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getUpdateFromJsonArrayMethod() {
		JavaSymbolName fromJsonArrayMethodName = jsonMetadata.getFromJsonArrayMethodName();
		JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataHolder = javaTypeMetadataHolder.getPersistenceDetails();
		if (fromJsonArrayMethodName == null || javaTypePersistenceMetadataHolder == null || javaTypePersistenceMetadataHolder.getMergeMethod() == null) {
			return null;
		}
		
		JavaSymbolName methodName = new JavaSymbolName("updateFromJsonArray");
		
		List<AnnotationMetadata> parameters = new ArrayList<AnnotationMetadata>();
		AnnotationMetadataBuilder requestBodyAnnotation = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestBody"));
		parameters.add(requestBodyAnnotation.build());
		
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(JavaType.STRING_OBJECT, parameters));
		
		MethodMetadata existingMethod = methodExists(methodName, paramTypes);
		if (existingMethod != null) return existingMethod;
		
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("json"));
		
		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/jsonArray"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("PUT"))));
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("headers"), "Accept=application/json"));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);
		
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		String beanName = formBackingType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		
		List<JavaType> params = new ArrayList<JavaType>();
		params.add(formBackingType);
		String httpHeadersShortName = getShortName(new JavaType("org.springframework.http.HttpHeaders"));
		bodyBuilder.appendFormalLine(httpHeadersShortName + " headers= new " + httpHeadersShortName + "();");
		bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \"application/text\");");
		bodyBuilder.appendFormalLine("for (" + beanName + " " + entityName + ": " + beanName + "." + fromJsonArrayMethodName.getSymbolName() + "(json)) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("if (" + entityName + "." + javaTypePersistenceMetadataHolder.getMergeMethod().getMethodName().getSymbolName() + "() == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(headers, " + new JavaType("org.springframework.http.HttpStatus").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".NOT_FOUND);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(headers, " + new JavaType("org.springframework.http.HttpStatus").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".OK);");
		
		JavaType returnType = new JavaType("org.springframework.http.ResponseEntity", 0, DataType.TYPE, null, Arrays.asList(JavaType.STRING_OBJECT));	
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}
	
	private MethodMetadata getJsonDeleteMethod() {
		JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataHolder = javaTypeMetadataHolder.getPersistenceDetails();
		if (javaTypePersistenceMetadataHolder == null || javaTypePersistenceMetadataHolder.getRemoveMethod() == null  || javaTypePersistenceMetadataHolder.getFindMethod() == null) {
			return null;
		}
		
		JavaSymbolName methodName = new JavaSymbolName("deleteFromJson");

		List<AnnotationMetadata> typeAnnotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("value"), javaTypePersistenceMetadataHolder.getIdentifierField().getFieldName().getSymbolName()));
		AnnotationMetadataBuilder pathVariableAnnotation = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.PathVariable"), attributes);
		typeAnnotations.add(pathVariableAnnotation.build());

		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(javaTypePersistenceMetadataHolder.getIdentifierField().getFieldType(), typeAnnotations));
		
		MethodMetadata method = methodExists(methodName, paramTypes);
		if (method != null) return method;

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(javaTypePersistenceMetadataHolder.getIdentifierField().getFieldName().getSymbolName()));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/{" + javaTypePersistenceMetadataHolder.getIdentifierField().getFieldName().getSymbolName() + "}"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("DELETE"))));
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("headers"), "Accept=application/json"));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);

		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		String beanShortName = formBackingType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(beanShortName + " " + beanShortName.toLowerCase() + " = " + beanShortName + "." + javaTypePersistenceMetadataHolder.getFindMethod().getMethodName() + "(" + javaTypePersistenceMetadataHolder.getIdentifierField().getFieldName().getSymbolName() + ");");
		String httpHeadersShortName = getShortName(new JavaType("org.springframework.http.HttpHeaders"));
		bodyBuilder.appendFormalLine(httpHeadersShortName + " headers= new " + httpHeadersShortName + "();");
		bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \"application/text\");");
		bodyBuilder.appendFormalLine("if (" + beanShortName.toLowerCase() + " == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return new " + getShortName(new JavaType("org.springframework.http.ResponseEntity")) + "<String>(headers, " + getShortName(new JavaType("org.springframework.http.HttpStatus")) + ".NOT_FOUND);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine(beanShortName.toLowerCase() + "." + javaTypePersistenceMetadataHolder.getRemoveMethod().getMethodName() + "();");
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(headers, " + new JavaType("org.springframework.http.HttpStatus").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".OK);");

		JavaType returnType = new JavaType("org.springframework.http.ResponseEntity", 0, DataType.TYPE, null, Arrays.asList(JavaType.STRING_OBJECT));
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}
	
	private MethodMetadata getFinderJsonMethod(FinderMetadataDetails finderDetails) {
		Assert.notNull(finderDetails, "Method metadata required for finder");
		if (jsonMetadata.getToJsonArrayMethodName() == null) {
			return null;
		}
		JavaSymbolName finderMethodName = new JavaSymbolName("json" + StringUtils.capitalize(finderDetails.getFinderMethodMetadata().getMethodName().getSymbolName()));

		List<AnnotatedJavaType> annotatedParamTypes = new ArrayList<AnnotatedJavaType>();
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		StringBuilder methodParams = new StringBuilder();
		
		for (FieldMetadata field: finderDetails.getFinderMethodParamFields()) {
			JavaSymbolName fieldName = field.getFieldName();
			List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
			List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
			attributes.add(new StringAttributeValue(new JavaSymbolName("value"), uncapitalize(fieldName.getSymbolName())));
			if (field.getFieldType().equals(JavaType.BOOLEAN_PRIMITIVE) || field.getFieldType().equals(JavaType.BOOLEAN_OBJECT)) {
				attributes.add(new BooleanAttributeValue(new JavaSymbolName("required"), false));
			}
			AnnotationMetadataBuilder requestParamAnnotation = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestParam"), attributes);
			annotations.add(requestParamAnnotation.build());
			if (field.getFieldType().equals(new JavaType(Date.class.getName())) || field.getFieldType().equals(new JavaType(Calendar.class.getName()))) {
				AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("org.springframework.format.annotation.DateTimeFormat"));
				if (annotation != null) {
					annotations.add(annotation);
				}
			}
			paramNames.add(fieldName);
			annotatedParamTypes.add(new AnnotatedJavaType(field.getFieldType(), annotations));

			if (field.getFieldType().equals(JavaType.BOOLEAN_OBJECT)) {
				methodParams.append(field.getFieldName() + " == null ? new Boolean(false) : " + field.getFieldName() + ", ");
			} else {
				methodParams.append(field.getFieldName() + ", ");
			}
		}

		if (methodParams.length() > 0) {
			methodParams.delete(methodParams.length() - 2, methodParams.length());
		}
		
		MethodMetadata existingMethod = methodExists(finderMethodName, annotatedParamTypes);
		if (existingMethod != null) return existingMethod;
		
		List<JavaSymbolName> newParamNames = new ArrayList<JavaSymbolName>();
		newParamNames.addAll(paramNames);

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("params"), "find=" + finderDetails.getFinderMethodMetadata().getMethodName().getSymbolName().replaceFirst("find" + javaTypeMetadataHolder.getPlural(), "")));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("GET"))));
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("headers"), "Accept=application/json"));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);
		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.ResponseBody")));
		String shortBeanName = formBackingType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		String httpHeadersShortName = getShortName(new JavaType("org.springframework.http.HttpHeaders"));
		String responseEntityShortName = getShortName(new JavaType("org.springframework.http.ResponseEntity"));
		String httpStatusShortName = getShortName(new JavaType("org.springframework.http.HttpStatus"));
		bodyBuilder.appendFormalLine(httpHeadersShortName + " headers = new " + httpHeadersShortName + "();");
		bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \"application/text; charset=utf-8\");");
		bodyBuilder.appendFormalLine("return new " + responseEntityShortName + "<String>(" + shortBeanName + "." + jsonMetadata.getToJsonArrayMethodName().getSymbolName().toString() + "(" + shortBeanName + "." + finderDetails.getFinderMethodMetadata().getMethodName().getSymbolName() + "(" + methodParams.toString() + ").getResultList()), headers, " +  httpStatusShortName + ".OK);");

		JavaType returnType = new JavaType("org.springframework.http.ResponseEntity", 0, DataType.TYPE, null, Arrays.asList(JavaType.STRING_OBJECT));
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, finderMethodName, returnType, annotatedParamTypes, newParamNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}
	
	private MethodMetadata methodExists(JavaSymbolName methodName, List<AnnotatedJavaType> parameters) {
		return MemberFindingUtils.getMethod(memberDetails, methodName, AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameters));
	}
	
	private String getShortName(JavaType type) {
		return type.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
	}
	
	private String uncapitalize(String term) {
		// [ROO-1790] this is needed to adhere to the JavaBean naming conventions (see JavaBean spec section 8.8)
		return Introspector.decapitalize(StringUtils.capitalize(term));
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
