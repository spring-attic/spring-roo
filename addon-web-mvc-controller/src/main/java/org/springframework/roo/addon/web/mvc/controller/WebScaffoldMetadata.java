package org.springframework.roo.addon.web.mvc.controller;

import java.lang.reflect.Modifier;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.finder.FinderMetadata;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.DefaultMethodMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.BooleanAttributeValue;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.itd.ItdSourceFileComposer;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Metadata for {@link RooWebScaffold}.
 * 
 * 
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
public class WebScaffoldMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

	private static final String PROVIDES_TYPE_STRING = WebScaffoldMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);

	private WebScaffoldAnnotationValues annotationValues;
	private BeanInfoMetadata beanInfoMetadata;
	private EntityMetadata entityMetadata;
	private MetadataService metadataService;
	private SortedSet<JavaType> specialDomainTypes;
	private String controllerPath;
	private String entityName;
	
	private boolean typeExposesDateField = false;
	
	public WebScaffoldMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, MetadataService metadataService, WebScaffoldAnnotationValues annotationValues, BeanInfoMetadata beanInfoMetadata, EntityMetadata entityMetadata, FinderMetadata finderMetadata, ControllerOperations controllerOperations) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(beanInfoMetadata, "Bean info metadata required");
		Assert.notNull(entityMetadata, "Entity metadata required");
		Assert.notNull(entityMetadata, "Finder metadata required");
		Assert.notNull(controllerOperations, "Controller operations required");
		if (!isValid()) {
			return;
		}

		this.annotationValues = annotationValues;
		this.beanInfoMetadata = beanInfoMetadata;
		this.entityMetadata = entityMetadata;
		this.entityName = beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase();
		this.controllerPath = annotationValues.getPath();
		this.metadataService = metadataService;
		
		//now figure out if we need to create any custom property editors
		
		SortedSet<JavaType> editorsToBeCreated = getEditorToBeCreated();		
		if (editorsToBeCreated.size() > 0) { 			
			controllerOperations.createPropertyEditors(editorsToBeCreated);
		}
		
		specialDomainTypes = getSpecialDomainTypes();
		
		if (annotationValues.create) {
			builder.addMethod(getCreateMethod());
			builder.addMethod(getCreateFormMethod());
		}		
//		if (annotationValues.show) {
			builder.addMethod(getShowMethod());
//		}
//		if (annotationValues.list) {
			builder.addMethod(getListMethod());
//		}
		if (annotationValues.update) {
			builder.addMethod(getUpdateMethod());
			builder.addMethod(getUpdateFormMethod());
		}	
		if (annotationValues.delete) {
			builder.addMethod(getDeleteMethod());
		} 
		if (typeExposesDateField) {
			builder.addMethod(getInitBinderMethod());
		}		
		if (annotationValues.exposeFinders) {			
			for (String finderName : entityMetadata.getDynamicFinders()) {
				builder.addMethod(getFinderFormMethod(finderMetadata.getDynamicFinderMethod(finderName)));
				builder.addMethod(getFinderMethod(finderMetadata.getDynamicFinderMethod(finderName)));
			}
		}
		
		itdTypeDetails = builder.build();
		
		new ItdSourceFileComposer(itdTypeDetails);		
	}
	
	public String getIdentifierForBeanInfoMetadata() {
		return beanInfoMetadata.getId();
	}
	
	public String getIdentifierForEntityMetadata() {
		return entityMetadata.getId();
	}
	
	public WebScaffoldAnnotationValues getAnnotationValues() {
		return annotationValues;
	}

	private MethodMetadata getInitBinderMethod() {
		JavaSymbolName methodName = new JavaSymbolName("initBinder");
		
		for (MethodMetadata method : governorTypeDetails.getDeclaredMethods()) {
			for (AnnotationMetadata annotation : method.getAnnotations()) {
				if(annotation.getAnnotationType().equals(new JavaType("org.springframework.web.bind.annotation.InitBinder"))) {
					//do nothing if the governor already has a custom init binder
					return method;
				}
			}
		}
		
		List<AnnotationMetadata> typeAnnotations = new ArrayList<AnnotationMetadata>();

		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.web.bind.WebDataBinder"), typeAnnotations));	
		
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("binder"));
		
		List<AnnotationAttributeValue<?>> initBinderAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		AnnotationMetadata initBinder = new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.InitBinder"), initBinderAttributes);
		
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		annotations.add(initBinder);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("binder.registerCustomEditor(java.util.Date.class, new org.springframework.beans.propertyeditors.CustomDateEditor(new java.text.SimpleDateFormat(\"" + annotationValues.getDateFormat().toPattern() + "\"), true));");
		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, paramTypes, paramNames, annotations, bodyBuilder.getOutput());
	}

	private MethodMetadata getDeleteMethod() {
		JavaSymbolName methodName = new JavaSymbolName("delete");		
		
		MethodMetadata method = methodExists(methodName);
		if (method != null) return method;
		
		List<AnnotationMetadata> typeAnnotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("value"), entityMetadata.getIdentifierField().getFieldName().getSymbolName()));
		typeAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.PathVariable"), attributes));
		
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(entityMetadata.getIdentifierField().getFieldType(), typeAnnotations));	
		
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(entityMetadata.getIdentifierField().getFieldName().getSymbolName()));
		
		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/" + controllerPath + "/{" + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + "}"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("DELETE"))));
		AnnotationMetadata requestMapping = new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		annotations.add(requestMapping);
				
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		if (!entityMetadata.getIdentifierField().getFieldType().isPrimitive()) {
			bodyBuilder.appendFormalLine("if (" + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + " == null) throw new IllegalArgumentException(\"An Identifier is required\");");
		}
		bodyBuilder.appendFormalLine(beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + "." + entityMetadata.getFindMethod().getMethodName() + "(" + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + ")." + entityMetadata.getRemoveMethod().getMethodName() + "();");
		bodyBuilder.appendFormalLine("return \"redirect:/" + controllerPath + "\";");

		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, methodName, new JavaType(String.class.getName()), paramTypes, paramNames, annotations, bodyBuilder.getOutput());
	}

	private MethodMetadata getListMethod() {
		JavaSymbolName methodName = new JavaSymbolName("list");		
		
		MethodMetadata method = methodExists(methodName);
		if (method != null) return method;
		
		List<AnnotationAttributeValue<?>> firstResultAttributeValue = new ArrayList<AnnotationAttributeValue<?>>();
		firstResultAttributeValue.add(new StringAttributeValue(new JavaSymbolName("value"), "page"));
		firstResultAttributeValue.add(new BooleanAttributeValue(new JavaSymbolName("required"), false));		
		
		List<AnnotationMetadata> paramAnnotationsFirstResult = new ArrayList<AnnotationMetadata>();
		paramAnnotationsFirstResult.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.RequestParam"), firstResultAttributeValue));
		
		List<AnnotationAttributeValue<?>> maxResultsAttributeValue = new ArrayList<AnnotationAttributeValue<?>>();
		maxResultsAttributeValue.add(new StringAttributeValue(new JavaSymbolName("value"), "size"));
		maxResultsAttributeValue.add(new BooleanAttributeValue(new JavaSymbolName("required"), false));		
		
		List<AnnotationMetadata> paramAnnotationsMaxResults = new ArrayList<AnnotationMetadata>();
		paramAnnotationsMaxResults.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.RequestParam"), maxResultsAttributeValue));
		
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(new JavaType("Integer"), paramAnnotationsFirstResult));
		paramTypes.add(new AnnotatedJavaType(new JavaType("Integer"), paramAnnotationsMaxResults));
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.ModelMap"), null));	
		
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();		
		paramNames.add(new JavaSymbolName("page"));
		paramNames.add(new JavaSymbolName("size"));
		paramNames.add(new JavaSymbolName("modelMap"));
		
		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/" + controllerPath));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("GET"))));
		AnnotationMetadata requestMapping = new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
				
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		annotations.add(requestMapping);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder(); 		
		bodyBuilder.appendFormalLine("if(page != null || size != null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("int sizeNo = size == null ? 10 : size.intValue();");
		bodyBuilder.appendFormalLine("modelMap.addAttribute(\"" + entityMetadata.getPlural().toLowerCase() + "\", " + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + "." + entityMetadata.getFindEntriesMethod().getMethodName() + "(page == null ? 0 : (page.intValue() - 1) * sizeNo, sizeNo));");
		bodyBuilder.appendFormalLine("float nrOfPages = (float) " + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + "." + entityMetadata.getCountMethod().getMethodName() + "() / sizeNo;");
		bodyBuilder.appendFormalLine("modelMap.addAttribute(\"maxPages\", (int) ((nrOfPages > (int) nrOfPages || nrOfPages == 0.0) ? nrOfPages + 1 : nrOfPages));");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("} else {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("modelMap.addAttribute(\"" + entityMetadata.getPlural().toLowerCase() + "\", " + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + "." + entityMetadata.getFindAllMethod().getMethodName() + "());");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/list\";");

		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, methodName, new JavaType(String.class.getName()), paramTypes, paramNames, annotations, bodyBuilder.getOutput());
	}

	private MethodMetadata getShowMethod() {
		JavaSymbolName methodName = new JavaSymbolName("show");		
		
		MethodMetadata method = methodExists(methodName);
		if (method != null) return method;
		
		List<AnnotationMetadata> typeAnnotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("value"), entityMetadata.getIdentifierField().getFieldName().getSymbolName()));
		typeAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.PathVariable"), attributes));
		
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(entityMetadata.getIdentifierField().getFieldType(), typeAnnotations));	
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.ModelMap"), null));	
		
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(entityMetadata.getIdentifierField().getFieldName().getSymbolName()));
		paramNames.add(new JavaSymbolName("modelMap"));
		
		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/" + controllerPath + "/{" + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + "}"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("GET"))));
		AnnotationMetadata requestMapping = new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		annotations.add(requestMapping);
				
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		if (!entityMetadata.getIdentifierField().getFieldType().isPrimitive()) {
			bodyBuilder.appendFormalLine("if (" + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + " == null) throw new IllegalArgumentException(\"An Identifier is required\");");
		}
		bodyBuilder.appendFormalLine("modelMap.addAttribute(\"" + entityName + "\", " + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + "." + entityMetadata.getFindMethod().getMethodName() + "(" + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + "));");
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/show\";");

		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, methodName, new JavaType(String.class.getName()), paramTypes, paramNames, annotations, bodyBuilder.getOutput());
	}

	private MethodMetadata getCreateMethod() {		
		JavaSymbolName methodName = new JavaSymbolName("create");
		
		MethodMetadata method = methodExists(methodName);
		if (method != null) return method;
		
		List<AnnotationMetadata> typeAnnotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("value"), entityName));
		typeAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.ModelAttribute"), attributes));
		
		List<AnnotationMetadata> noAnnotations = new ArrayList<AnnotationMetadata>();
		
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(beanInfoMetadata.getJavaBean(), typeAnnotations));	
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.validation.BindingResult"), noAnnotations));
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.ModelMap"), noAnnotations));
		
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(entityName));
		paramNames.add(new JavaSymbolName("result"));
		paramNames.add(new JavaSymbolName("modelMap"));
		
		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/" + controllerPath));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("POST"))));
		AnnotationMetadata requestMapping = new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		annotations.add(requestMapping);
				
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (" + entityName + " == null) throw new IllegalArgumentException(\"A " + entityName+ " is required\");");
		bodyBuilder.appendFormalLine("for(javax.validation.ConstraintViolation<" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + "> constraint : javax.validation.Validation.buildDefaultValidatorFactory().getValidator().validate(" + entityName + ")) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("result.rejectValue(constraint.getPropertyPath().toString(), \"" + entityName + ".error.\" + constraint.getPropertyPath(), constraint.getMessage());");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("if (result.hasErrors()) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("modelMap.addAllAttributes(result.getAllErrors());");
		bodyBuilder.appendFormalLine("modelMap.addAttribute(\"" + entityName + "\", " + entityName + ");");
		if(specialDomainTypes.size() > 0) {
			for (JavaType type: specialDomainTypes) {
				EntityMetadata typeEntityMetadata = (EntityMetadata) metadataService.get(entityMetadata.createIdentifier(type, Path.SRC_MAIN_JAVA));
				if (typeEntityMetadata != null) {
					bodyBuilder.appendFormalLine("modelMap.addAttribute(\"" + typeEntityMetadata.getPlural().toLowerCase() + "\", " + type.getFullyQualifiedTypeName() + "." + typeEntityMetadata.getFindAllMethod().getMethodName() + "());");
				} else if(isEnumType(type)){
					bodyBuilder.appendFormalLine("modelMap.addAttribute(\"_" + type.getSimpleTypeName().toLowerCase() + "\", " + type.getFullyQualifiedTypeName() + ".class.getEnumConstants());");
				}				
			}
		}
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/create\";");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine(entityName + "." + entityMetadata.getPersistMethod().getMethodName() + "();");
		bodyBuilder.appendFormalLine("return \"redirect:/" + controllerPath + "/\" + " + entityName + "." + entityMetadata.getIdentifierAccessor().getMethodName() + "();");
		
		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, methodName, new JavaType(String.class.getName()), paramTypes, paramNames, annotations, bodyBuilder.getOutput());
	}
	
	private MethodMetadata getCreateFormMethod() {
		JavaSymbolName methodName = new JavaSymbolName("createForm");
		
		MethodMetadata method = methodExists(methodName);
		if (method != null) return method;
		
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.ModelMap"), null));	
		
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("modelMap"));
		
		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/" + controllerPath + "/form"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("GET"))));
		AnnotationMetadata requestMapping = new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		annotations.add(requestMapping);
				
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("modelMap.addAttribute(\"" + entityName + "\", new " + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + "());");
		if(specialDomainTypes.size() > 0) {
			for (JavaType type: specialDomainTypes) {
				EntityMetadata typeEntityMetadata = (EntityMetadata) metadataService.get(entityMetadata.createIdentifier(type, Path.SRC_MAIN_JAVA));
				if (typeEntityMetadata != null) {
					bodyBuilder.appendFormalLine("modelMap.addAttribute(\"" + typeEntityMetadata.getPlural().toLowerCase() + "\", " + type.getFullyQualifiedTypeName() + "." + typeEntityMetadata.getFindAllMethod().getMethodName() + "());");
				} else if(isEnumType(type)){
					bodyBuilder.appendFormalLine("modelMap.addAttribute(\"_" + type.getSimpleTypeName().toLowerCase() + "\", " + type.getFullyQualifiedTypeName() + ".class.getEnumConstants());");
				}				
			}
		}
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/create\";");
		
		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, methodName, new JavaType(String.class.getName()), paramTypes, paramNames, annotations, bodyBuilder.getOutput());
	}


	
	private MethodMetadata getUpdateMethod() {		
		JavaSymbolName methodName = new JavaSymbolName("update");
		
		MethodMetadata method = methodExists(methodName);
		if (method != null) return method;

		List<AnnotationMetadata> typeAnnotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("value"), entityName));
		typeAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.ModelAttribute"), attributes));
		
		List<AnnotationMetadata> noAnnotations = new ArrayList<AnnotationMetadata>();
		
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(beanInfoMetadata.getJavaBean(), typeAnnotations));	
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.validation.BindingResult"), noAnnotations));
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.ModelMap"), noAnnotations));
		
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(entityName));
		paramNames.add(new JavaSymbolName("result"));
		paramNames.add(new JavaSymbolName("modelMap"));
		
		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("PUT"))));
		AnnotationMetadata requestMapping = new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		annotations.add(requestMapping);
				
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (" + entityName + " == null) throw new IllegalArgumentException(\"A " + entityName+ " is required\");");
		bodyBuilder.appendFormalLine("for(javax.validation.ConstraintViolation<" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + "> constraint : javax.validation.Validation.buildDefaultValidatorFactory().getValidator().validate(" + entityName + ")) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("result.rejectValue(constraint.getPropertyPath().toString(), \"" + entityName + ".error.\" + constraint.getPropertyPath(), constraint.getMessage());");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("if (result.hasErrors()) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("modelMap.addAllAttributes(result.getAllErrors());");
		bodyBuilder.appendFormalLine("modelMap.addAttribute(\"" + entityName + "\", " + entityName + ");");
		if(specialDomainTypes.size() > 0) {
			for (JavaType type: specialDomainTypes) {
				EntityMetadata typeEntityMetadata = (EntityMetadata) metadataService.get(entityMetadata.createIdentifier(type, Path.SRC_MAIN_JAVA));
				if (typeEntityMetadata != null) {
					bodyBuilder.appendFormalLine("modelMap.addAttribute(\"" + typeEntityMetadata.getPlural().toLowerCase() + "\", " + type.getFullyQualifiedTypeName() + "." + typeEntityMetadata.getFindAllMethod().getMethodName() + "());");
				} else if(isEnumType(type)){
					bodyBuilder.appendFormalLine("modelMap.addAttribute(\"_" + type.getSimpleTypeName().toLowerCase() + "\", " + type.getFullyQualifiedTypeName() + ".class.getEnumConstants());");
				}				
			}
		}
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/update\";");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine(entityName + "." + entityMetadata.getMergeMethod().getMethodName() + "();");
		bodyBuilder.appendFormalLine("return \"redirect:/" + controllerPath + "/\" + " + entityName + "." + entityMetadata.getIdentifierAccessor().getMethodName() + "();");
		
		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, methodName, new JavaType(String.class.getName()), paramTypes, paramNames, annotations, bodyBuilder.getOutput());
	}
	
	private MethodMetadata getUpdateFormMethod() {
		JavaSymbolName methodName = new JavaSymbolName("updateForm");
		
		MethodMetadata updateFormMethod = methodExists(methodName);
		if (updateFormMethod != null) return updateFormMethod;
		
		List<AnnotationMetadata> typeAnnotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("value"), entityMetadata.getIdentifierField().getFieldName().getSymbolName()));
		typeAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.PathVariable"), attributes));		
		
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(entityMetadata.getIdentifierField().getFieldType(), typeAnnotations));	
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.ModelMap"), null));	
		
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(entityMetadata.getIdentifierField().getFieldName().getSymbolName()));
		paramNames.add(new JavaSymbolName("modelMap"));
		
		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/" + controllerPath + "/{" + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + "}/form"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("GET"))));
		AnnotationMetadata requestMapping = new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		annotations.add(requestMapping);
				
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		if (!entityMetadata.getIdentifierField().getFieldType().isPrimitive()) {
			bodyBuilder.appendFormalLine("if (" + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + " == null) throw new IllegalArgumentException(\"An Identifier is required\");");
		}
		bodyBuilder.appendFormalLine("modelMap.addAttribute(\"" + entityName + "\", " + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + "." + entityMetadata.getFindMethod().getMethodName() + "(" + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + "));");
		if(specialDomainTypes.size() > 0) {
			for (JavaType type: specialDomainTypes) {
				EntityMetadata typeEntityMetadata = (EntityMetadata) metadataService.get(entityMetadata.createIdentifier(type, Path.SRC_MAIN_JAVA));
				if (typeEntityMetadata != null) {
					bodyBuilder.appendFormalLine("modelMap.addAttribute(\"" + typeEntityMetadata.getPlural().toLowerCase() + "\", " + type.getFullyQualifiedTypeName() + "." + typeEntityMetadata.getFindAllMethod().getMethodName() + "());");
				} else if (isEnumType(type)){
					bodyBuilder.appendFormalLine("modelMap.addAttribute(\"_" + type.getSimpleTypeName().toLowerCase() + "\", " + type.getFullyQualifiedTypeName() + ".class.getEnumConstants());");
				}
			}
		}
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/update\";");

		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, methodName, new JavaType(String.class.getName()), paramTypes, paramNames, annotations, bodyBuilder.getOutput());
	}
	
	private MethodMetadata getFinderFormMethod(MethodMetadata methodMetadata) {
		Assert.notNull(methodMetadata, "Method metadata required for finder");
		JavaSymbolName finderFormMethodName = new JavaSymbolName(methodMetadata.getMethodName().getSymbolName() + "Form");
		MethodMetadata finderFormMethod = methodExists(finderFormMethodName);
		if (finderFormMethod != null) return finderFormMethod;
		
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		List<JavaType> types = AnnotatedJavaType.convertFromAnnotatedJavaTypes(methodMetadata.getParameterTypes());
		
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		
		boolean needModelMap = false;
		for (JavaType javaType : types) {
			if (isSpecialType(javaType)) {
				EntityMetadata typeEntityMetadata = (EntityMetadata) metadataService.get(entityMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA));
				if (typeEntityMetadata != null) {
					bodyBuilder.appendFormalLine("modelMap.addAttribute(\"" + typeEntityMetadata.getPlural().toLowerCase() + "\", " + javaType.getFullyQualifiedTypeName() + "." + typeEntityMetadata.getFindAllMethod().getMethodName() + "());");
				}
				needModelMap = true;
			}
		}		
		
		if (needModelMap) {
			paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.ModelMap"), null));		
			paramNames.add(new JavaSymbolName("modelMap"));
		}
		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "find/" + methodMetadata.getMethodName().getSymbolName().replaceFirst("find" + entityMetadata.getPlural(), "") + "/form"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("GET"))));
		AnnotationMetadata requestMapping = new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		annotations.add(requestMapping);
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/" + methodMetadata.getMethodName().getSymbolName() + "\";");

		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, finderFormMethodName, new JavaType(String.class.getName()), paramTypes, paramNames, annotations, bodyBuilder.getOutput());
	}	
	
	private MethodMetadata getFinderMethod(MethodMetadata methodMetadata) {
		Assert.notNull(methodMetadata, "Method metadata required for finder");
		JavaSymbolName finderMethodName = new JavaSymbolName(methodMetadata.getMethodName().getSymbolName());
		MethodMetadata finderMethod = methodExists(finderMethodName);
		if (finderMethod != null) return finderMethod;
		
		List<AnnotatedJavaType> annotatedParamTypes = new ArrayList<AnnotatedJavaType>();
		List<JavaSymbolName> paramNames = methodMetadata.getParameterNames();
		List<JavaType> paramTypes = AnnotatedJavaType.convertFromAnnotatedJavaTypes(methodMetadata.getParameterTypes());	

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();	
		StringBuilder methodParams = new StringBuilder();
		
		for (int i = 0; i < paramTypes.size(); i++) {
			List<AnnotationMetadata> pathVariable = new ArrayList<AnnotationMetadata>();
			List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
			attributes.add(new StringAttributeValue(new JavaSymbolName("value"), paramNames.get(i).getSymbolName().toLowerCase()));
			pathVariable.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.RequestParam"), attributes));					
			annotatedParamTypes.add(new AnnotatedJavaType(paramTypes.get(i), pathVariable));
			if (!paramTypes.get(i).isPrimitive()) {
				bodyBuilder.appendFormalLine("if(" + paramNames.get(i).getSymbolName() + " == null" + (paramTypes.get(i).equals(new JavaType(String.class.getName())) ? " || " + paramNames.get(i).getSymbolName() + ".length() == 0" : "") + ") throw new IllegalArgumentException(\"A " + paramNames.get(i).getSymbolNameCapitalisedFirstLetter() + " is required.\");");
			}
			methodParams.append(paramNames.get(i) + ", ");
		}				
		
		if(methodParams.length() > 0) {
			methodParams.delete(methodParams.length() - 2, methodParams.length());
		}
		
		annotatedParamTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.ModelMap"), new ArrayList<AnnotationMetadata>()));
		List<JavaSymbolName> newParamNames = new ArrayList<JavaSymbolName>();
		newParamNames.addAll(paramNames);
		newParamNames.add(new JavaSymbolName("modelMap"));
		
		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "find/" + methodMetadata.getMethodName().getSymbolName().replaceFirst("find" + entityMetadata.getPlural(), "")));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("GET"))));
		AnnotationMetadata requestMapping = new DefaultAnnotationMetadata(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		annotations.add(requestMapping);	
		
		bodyBuilder.appendFormalLine("modelMap.addAttribute(\"" + entityMetadata.getPlural().toLowerCase() + "\", " + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + "." + methodMetadata.getMethodName().getSymbolName() + "(" + methodParams.toString() + ").getResultList());");
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/list\";");

		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, finderMethodName, new JavaType(String.class.getName()), annotatedParamTypes, newParamNames, annotations, bodyBuilder.getOutput());
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
	
	private SortedSet<JavaType> getEditorToBeCreated() {
		SortedSet<JavaType> editorTypes = new TreeSet<JavaType>();
		
		for (MethodMetadata accessor : beanInfoMetadata.getPublicAccessors(false)) {
			
			//not interested in identifiers and version fields
			if (accessor.equals(entityMetadata.getIdentifierAccessor()) || accessor.equals(entityMetadata.getVersionAccessor())) {
				continue;
			}
			
			//not interested in fields that are not exposed via a mutator
			FieldMetadata fieldMetadata = beanInfoMetadata.getFieldForPropertyName(beanInfoMetadata.getPropertyNameForJavaBeanMethod(accessor));
			if(fieldMetadata == null || !hasMutator(fieldMetadata)) {
				continue;
			}
			JavaType type = accessor.getReturnType();
			
			if(type.isCommonCollectionType()) {
				for (JavaType genericType : type.getParameters()) {
					if(isTypeElegibleForEditorCreation(genericType)) {					
						editorTypes.add(genericType);	
					} else if (genericType.equals(new JavaType(Date.class.getName()))) {
						typeExposesDateField = true;
					}
				}		
			} else {
				if(isTypeElegibleForEditorCreation(type)) {
					editorTypes.add(type);
				} else if (type.equals(new JavaType(Date.class.getName()))) {
					typeExposesDateField = true;
				}
			}
		}
		return editorTypes;
	}
	
	private SortedSet<JavaType> getSpecialDomainTypes() {
		SortedSet<JavaType> editorTypes = new TreeSet<JavaType>();
		
		for (MethodMetadata accessor : beanInfoMetadata.getPublicAccessors(false)) {
			
			//not interested in identifiers and version fields
			if (accessor.equals(entityMetadata.getIdentifierAccessor()) || accessor.equals(entityMetadata.getVersionAccessor())) {
				continue;
			}
			
			//not interested in fields that are not exposed via a mutator
			FieldMetadata fieldMetadata = beanInfoMetadata.getFieldForPropertyName(beanInfoMetadata.getPropertyNameForJavaBeanMethod(accessor));
			if(fieldMetadata == null || !hasMutator(fieldMetadata)) {
				continue;
			}
			JavaType type = accessor.getReturnType();
			
			if(type.isCommonCollectionType()) {
				for (JavaType genericType : type.getParameters()) {
					if(isSpecialType(genericType)) {					
						editorTypes.add(genericType);	
					} else if (genericType.equals(new JavaType(Date.class.getName()))) {
						typeExposesDateField = true;
					}
				}		
			} else {
				if(isSpecialType(type)) {
					editorTypes.add(type);
				} else if (type.equals(new JavaType(Date.class.getName()))) {
					typeExposesDateField = true;
				}
			}
		}
		return editorTypes;
	}
	
	private boolean isEnumType(JavaType type) {
		PhysicalTypeMetadata physicalTypeMetadata  = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifierNamingUtils.createIdentifier(PhysicalTypeIdentifier.class.getName(), type, Path.SRC_MAIN_JAVA));
		if (physicalTypeMetadata != null) {
			PhysicalTypeDetails details = physicalTypeMetadata.getPhysicalTypeDetails();
			if (details != null) {
				if (details.getPhysicalTypeCategory().equals(PhysicalTypeCategory.ENUMERATION)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean hasMutator(FieldMetadata fieldMetadata) {
		Assert.notNull(fieldMetadata, "Field metadata required");
		for (MethodMetadata mutator : beanInfoMetadata.getPublicMutators()) {
			if (fieldMetadata.equals(beanInfoMetadata.getFieldForPropertyName(beanInfoMetadata.getPropertyNameForJavaBeanMethod(mutator)))) return true;
		}
		return false;
	}
	
	private boolean isTypeElegibleForEditorCreation(JavaType javaType) {
		String editorPhysicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(new JavaType(javaType.getFullyQualifiedTypeName() + "Editor"), Path.SRC_MAIN_JAVA);
		//we are only interested if the type is part of our application and if no editor exists for it already
		if (isSpecialType(javaType) && metadataService.get(editorPhysicalTypeIdentifier) == null) {
		  return true;
		}		
		return false;
	}
	
	private boolean isSpecialType(JavaType javaType) {
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		//we are only interested if the type is part of our application and if no editor exists for it already
		if (metadataService.get(physicalTypeIdentifier) != null) {
		  return true;
		}		
		return false;
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