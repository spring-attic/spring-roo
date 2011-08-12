package org.springframework.roo.addon.web.mvc.controller.scaffold.json;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.roo.addon.json.JsonMetadata;
import org.springframework.roo.addon.web.mvc.controller.RooWebScaffold;
import org.springframework.roo.addon.web.mvc.controller.details.FinderMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldAnnotationValues;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
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
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
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

	public WebJsonMetadata(final String identifier, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final WebScaffoldAnnotationValues annotationValues, final MemberDetails memberDetails, final Map<String, MemberTypeAdditions> persistenceAdditions, final FieldMetadata identifierField, final String plural, final Set<FinderMetadataDetails> finderDetails, JsonMetadata jsonMetadata) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(persistenceAdditions, "Persistence additions required");
		Assert.notNull(memberDetails, "Member details required");
		Assert.notNull(finderDetails, "Array of dynamic finder methods cannot be null");
		Assert.notNull(jsonMetadata, "Json metadata required");
		if (!isValid()) {
			return;
		}
		this.annotationValues = annotationValues;
		this.entityName = JavaSymbolName.getReservedWordSaveName(annotationValues.getFormBackingObject()).getSymbolName();
		this.formBackingType = annotationValues.getFormBackingObject();
		this.memberDetails = memberDetails;

		this.jsonMetadata = jsonMetadata;
		
		MemberTypeAdditions findMethod = persistenceAdditions.get(PersistenceCustomDataKeys.FIND_METHOD.name());
		if (identifierField != null && findMethod != null) {
			builder.addMethod(getJsonShowMethod(identifierField, findMethod));
		}
		MemberTypeAdditions findAllMethod = persistenceAdditions.get(PersistenceCustomDataKeys.FIND_ALL_METHOD.name());
		if (findAllMethod != null) {
			builder.addMethod(getJsonListMethod(findAllMethod));
		}
		MemberTypeAdditions persistMethod = persistenceAdditions.get(PersistenceCustomDataKeys.PERSIST_METHOD.name());
		if (annotationValues.isCreate() && persistMethod != null) {
			builder.addMethod(getJsonCreateMethod(persistMethod));
			builder.addMethod(getCreateFromJsonArrayMethod(persistMethod));
		}
		MemberTypeAdditions mergeMethod = persistenceAdditions.get(PersistenceCustomDataKeys.MERGE_METHOD.name());
		if (annotationValues.isUpdate() && mergeMethod != null) {
			builder.addMethod(getJsonUpdateMethod(mergeMethod));
			builder.addMethod(getUpdateFromJsonArrayMethod(mergeMethod));
		}
		MemberTypeAdditions removeMethod = persistenceAdditions.get(PersistenceCustomDataKeys.REMOVE_METHOD.name());
		if (annotationValues.isDelete() && removeMethod != null && findMethod != null && identifierField != null) {
			builder.addMethod(getJsonDeleteMethod(removeMethod, identifierField, findMethod));
		}
		if (annotationValues.isExposeFinders() && !finderDetails.isEmpty()) {
			for (FinderMetadataDetails finder : finderDetails) {
				builder.addMethod(getFinderJsonMethod(finder, plural));
			}
		}
		
		itdTypeDetails = builder.build();
	}

	public WebScaffoldAnnotationValues getAnnotationValues() {
		return annotationValues;
	}

	private MethodMetadata getJsonShowMethod(final FieldMetadata identifierField, final MemberTypeAdditions findMethod) {
		JavaSymbolName toJsonMethodName = jsonMetadata.getToJsonMethodName();
		
		JavaSymbolName methodName = new JavaSymbolName("showJson");

		List<AnnotationMetadata> parameters = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("value"), identifierField.getFieldName().getSymbolName()));
		AnnotationMetadataBuilder pathVariableAnnotation = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.PathVariable"), attributes);
		parameters.add(pathVariableAnnotation.build());
		
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(identifierField.getFieldType(), parameters));
		
		MethodMetadata jsonShowMethod = methodExists(methodName, paramTypes);
		if (jsonShowMethod != null) return jsonShowMethod;

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(identifierField.getFieldName().getSymbolName()));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/{" + identifierField.getFieldName().getSymbolName() + "}"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("GET"))));
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("headers"), "Accept=application/json"));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.ResponseBody")));

		String beanShortName = getShortName(formBackingType);
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(beanShortName + " " + beanShortName.toLowerCase() + " = " + findMethod.getMethodCall() + ";");
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

	private MethodMetadata getJsonCreateMethod(final MemberTypeAdditions persistMethod) {
		JavaSymbolName fromJsonMethodName = jsonMetadata.getFromJsonMethodName();
		
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

		String formBackingTypeName = formBackingType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(formBackingTypeName + " " + JavaSymbolName.getReservedWordSaveName(formBackingType) + "=  " + formBackingTypeName + "." + fromJsonMethodName.getSymbolName() + "(json);");
		bodyBuilder.appendFormalLine(persistMethod.getMethodCall() + ";");
		String httpHeadersShortName = getShortName(new JavaType("org.springframework.http.HttpHeaders"));
		bodyBuilder.appendFormalLine(httpHeadersShortName + " headers= new " + httpHeadersShortName + "();");
		bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \"application/text\");");
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(headers, " + new JavaType("org.springframework.http.HttpStatus").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".CREATED);");

		JavaType returnType = new JavaType("org.springframework.http.ResponseEntity", 0, DataType.TYPE, null, Arrays.asList(JavaType.STRING_OBJECT));

//		persistMethod.copyAdditionsTo(builder, governorTypeDetails);
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getCreateFromJsonArrayMethod(MemberTypeAdditions persistMethod) {
		JavaSymbolName fromJsonArrayMethodName = jsonMetadata.getFromJsonArrayMethodName();
		
		JavaSymbolName methodName = new JavaSymbolName("createFromJsonArray");

		List<AnnotationMetadata> parameters = new ArrayList<AnnotationMetadata>();
		AnnotationMetadataBuilder requestBodyAnnotation = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestBody"));
		parameters.add(requestBodyAnnotation.build());

		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(JavaType.STRING_OBJECT, parameters));

		MethodMetadata createFromJsonArrayMethod = methodExists(methodName, paramTypes);
		if (createFromJsonArrayMethod != null) return createFromJsonArrayMethod;
		
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
		bodyBuilder.appendFormalLine(persistMethod.getMethodCall() + ";");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		String httpHeadersShortName = getShortName(new JavaType("org.springframework.http.HttpHeaders"));
		bodyBuilder.appendFormalLine(httpHeadersShortName + " headers= new " + httpHeadersShortName + "();");
		bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \"application/text\");");
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(headers, " + new JavaType("org.springframework.http.HttpStatus").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".CREATED);");

//		persistMethod.copyAdditionsTo(builder, governorTypeDetails);
		
		JavaType returnType = new JavaType("org.springframework.http.ResponseEntity", 0, DataType.TYPE, null, Arrays.asList(JavaType.STRING_OBJECT));
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getJsonListMethod(MemberTypeAdditions findAllMethod) {
		JavaSymbolName toJsonArrayMethodName = jsonMetadata.getToJsonArrayMethodName();
		
		// See if the type itself declared the method
		JavaSymbolName methodName = new JavaSymbolName("listJson");
		MethodMetadata listJsonMethod = MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName, null);
		if (listJsonMethod != null)  return listJsonMethod;

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
		JavaType list = new JavaType(List.class.getName(), 0, DataType.TYPE, null, Arrays.asList(formBackingType));
		bodyBuilder.appendFormalLine(list.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + " result = " + findAllMethod.getMethodCall() + ";");
		bodyBuilder.appendFormalLine("return new " + responseEntityShortName + "<String>(" + entityName + "." + toJsonArrayMethodName.getSymbolName() + "(result), headers, " +  httpStatusShortName + ".OK);");
		
//		findAllMethod.copyAdditionsTo(builder, governorTypeDetails);
		
		JavaType returnType = new JavaType("org.springframework.http.ResponseEntity", 0, DataType.TYPE, null, Arrays.asList(JavaType.STRING_OBJECT));
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}
	
	private MethodMetadata getJsonUpdateMethod(MemberTypeAdditions mergeMethod) {
		JavaSymbolName fromJsonMethodName = jsonMetadata.getFromJsonMethodName();
		
		JavaSymbolName methodName = new JavaSymbolName("updateFromJson");
		
		List<AnnotationMetadata> parameters = new ArrayList<AnnotationMetadata>();
		AnnotationMetadataBuilder requestBodyAnnotation = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestBody"));
		parameters.add(requestBodyAnnotation.build());
		
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(JavaType.STRING_OBJECT, parameters));
		
		MethodMetadata updateFromJsonMethod = methodExists(methodName, paramTypes);
		if (updateFromJsonMethod != null) return updateFromJsonMethod;
		
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
		String beanSymbolName = JavaSymbolName.getReservedWordSaveName(formBackingType).getSymbolName();
		String httpHeadersShortName = getShortName(new JavaType("org.springframework.http.HttpHeaders"));
		bodyBuilder.appendFormalLine(httpHeadersShortName + " headers= new " + httpHeadersShortName + "();");
		bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \"application/text\");");
		bodyBuilder.appendFormalLine(beanShortName + " " + beanSymbolName + " = " + beanShortName + "." + fromJsonMethodName.getSymbolName() + "(json);");
		bodyBuilder.appendFormalLine("if (" + mergeMethod.getMethodCall() + " == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(headers, " + new JavaType("org.springframework.http.HttpStatus").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".NOT_FOUND);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(headers, " + new JavaType("org.springframework.http.HttpStatus").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".OK);");
		
//		mergeMethod.copyAdditionsTo(builder, governorTypeDetails);
		JavaType returnType = new JavaType("org.springframework.http.ResponseEntity", 0, DataType.TYPE, null, Arrays.asList(JavaType.STRING_OBJECT));
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getUpdateFromJsonArrayMethod(MemberTypeAdditions mergeMethod) {
		JavaSymbolName fromJsonArrayMethodName = jsonMetadata.getFromJsonArrayMethodName();

		JavaSymbolName methodName = new JavaSymbolName("updateFromJsonArray");
		
		List<AnnotationMetadata> parameters = new ArrayList<AnnotationMetadata>();
		AnnotationMetadataBuilder requestBodyAnnotation = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestBody"));
		parameters.add(requestBodyAnnotation.build());
		
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(JavaType.STRING_OBJECT, parameters));
		
		MethodMetadata updateFromJsonArrayMethod = methodExists(methodName, paramTypes);
		if (updateFromJsonArrayMethod != null) return updateFromJsonArrayMethod;
		
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
		bodyBuilder.appendFormalLine("if (" + mergeMethod.getMethodCall() + " == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(headers, " + new JavaType("org.springframework.http.HttpStatus").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".NOT_FOUND);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(headers, " + new JavaType("org.springframework.http.HttpStatus").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".OK);");
		
//		mergeMethod.copyAdditionsTo(builder, governorTypeDetails);
		
		JavaType returnType = new JavaType("org.springframework.http.ResponseEntity", 0, DataType.TYPE, null, Arrays.asList(JavaType.STRING_OBJECT));	
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}
	
	private MethodMetadata getJsonDeleteMethod(MemberTypeAdditions removeMethod, FieldMetadata identifierField, MemberTypeAdditions findMethod) {
		JavaSymbolName methodName = new JavaSymbolName("deleteFromJson");

		List<AnnotationMetadata> typeAnnotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("value"), identifierField.getFieldName().getSymbolName()));
		AnnotationMetadataBuilder pathVariableAnnotation = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.PathVariable"), attributes);
		typeAnnotations.add(pathVariableAnnotation.build());

		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(identifierField.getFieldType(), typeAnnotations));
		
		MethodMetadata deleteFromJsonMethod = methodExists(methodName, paramTypes);
		if (deleteFromJsonMethod != null) return deleteFromJsonMethod;

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(identifierField.getFieldName().getSymbolName()));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/{" + identifierField.getFieldName().getSymbolName() + "}"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("DELETE"))));
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("headers"), "Accept=application/json"));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);

		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		String beanShortName = formBackingType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(beanShortName + " " + beanShortName.toLowerCase() + " = " + findMethod.getMethodCall() + ";");
		String httpHeadersShortName = getShortName(new JavaType("org.springframework.http.HttpHeaders"));
		bodyBuilder.appendFormalLine(httpHeadersShortName + " headers= new " + httpHeadersShortName + "();");
		bodyBuilder.appendFormalLine("headers.add(\"Content-Type\", \"application/text\");");
		bodyBuilder.appendFormalLine("if (" + beanShortName.toLowerCase() + " == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return new " + getShortName(new JavaType("org.springframework.http.ResponseEntity")) + "<String>(headers, " + getShortName(new JavaType("org.springframework.http.HttpStatus")) + ".NOT_FOUND);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine(removeMethod.getMethodCall() + ";");
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(headers, " + new JavaType("org.springframework.http.HttpStatus").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".OK);");

		JavaType returnType = new JavaType("org.springframework.http.ResponseEntity", 0, DataType.TYPE, null, Arrays.asList(JavaType.STRING_OBJECT));
		
//		removeMethod.copyAdditionsTo(builder, governorTypeDetails);
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}
	
	private MethodMetadata getFinderJsonMethod(FinderMetadataDetails finderDetails, String plural) {
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
			attributes.add(new StringAttributeValue(new JavaSymbolName("value"), StringUtils.uncapitalize(fieldName.getSymbolName())));
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
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("params"), "find=" + finderDetails.getFinderMethodMetadata().getMethodName().getSymbolName().replaceFirst("find" + plural, "")));
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
