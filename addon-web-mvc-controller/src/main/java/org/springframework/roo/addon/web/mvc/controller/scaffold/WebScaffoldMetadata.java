package org.springframework.roo.addon.web.mvc.controller.scaffold;

import java.beans.Introspector;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;

import org.springframework.roo.addon.json.JsonMetadata;
import org.springframework.roo.addon.web.mvc.controller.RooWebScaffold;
import org.springframework.roo.addon.web.mvc.controller.details.DateTimeFormatDetails;
import org.springframework.roo.addon.web.mvc.controller.details.FinderMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.details.JavaTypeMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.details.JavaTypePersistenceMetadataDetails;
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
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.BooleanAttributeValue;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.itd.ItdSourceFileComposer;
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
 * Metadata for {@link RooWebScaffold}.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
public class WebScaffoldMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	private static final String PROVIDES_TYPE_STRING = WebScaffoldMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);

	private WebScaffoldAnnotationValues annotationValues;
	private String controllerPath;
	private String entityName;
	private Map<JavaSymbolName, DateTimeFormatDetails> dateTypes;
	private JsonMetadata jsonMetadata;
	private List<MethodMetadata> existingMethods;
	private JavaType formBackingType;
	private Map<JavaType, JavaTypeMetadataDetails> specialDomainTypes;
	private JavaTypeMetadataDetails javaTypeMetadataHolder;

	public WebScaffoldMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, WebScaffoldAnnotationValues annotationValues, List<MethodMetadata> existingMethods, SortedMap<JavaType, JavaTypeMetadataDetails> specialDomainTypes, List<JavaTypeMetadataDetails> dependentTypes, Map<JavaSymbolName, DateTimeFormatDetails> dateTypes, Set<FinderMetadataDetails> dynamicFinderMethods, JsonMetadata jsonMetadata) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(specialDomainTypes, "Special domain type map required");
		Assert.notNull(dynamicFinderMethods, "Finder methods required");
		Assert.notNull(existingMethods, "List of existing methods required");
		Assert.notNull(dependentTypes, "Dependent types list required");
		if (!isValid()) {
			return;
		}
		this.annotationValues = annotationValues;
		this.entityName = uncapitalize(annotationValues.getFormBackingObject().getSimpleTypeName());
		if (ReservedWords.RESERVED_JAVA_KEYWORDS.contains(this.entityName)) {
			this.entityName = "_" + entityName;
		}
		this.controllerPath = annotationValues.getPath();
		this.formBackingType = annotationValues.getFormBackingObject();
		this.existingMethods = existingMethods;
		this.specialDomainTypes = specialDomainTypes;
		javaTypeMetadataHolder = specialDomainTypes.get(formBackingType);
		Assert.notNull(javaTypeMetadataHolder, "Metadata holder required for form backing type: " + formBackingType);

		this.dateTypes = dateTypes;

		if (annotationValues.create) {
			builder.addMethod(getCreateMethod());
			builder.addMethod(getCreateFormMethod(dependentTypes));
		}
		builder.addMethod(getShowMethod());
		builder.addMethod(getListMethod());
		if (annotationValues.update) {
			builder.addMethod(getUpdateMethod());
			builder.addMethod(getUpdateFormMethod());
		}
		if (annotationValues.delete) {
			builder.addMethod(getDeleteMethod());
		}
		if (annotationValues.exposeFinders && dynamicFinderMethods.size() > 0) { // No need for null check of entityMetadata.getDynamicFinders as it guarantees non-null (but maybe empty list)
			for (FinderMetadataDetails finder : new TreeSet<FinderMetadataDetails>(dynamicFinderMethods)) {
				builder.addMethod(getFinderFormMethod(finder));
				builder.addMethod(getFinderMethod(finder));
			}
		}
		if (specialDomainTypes.size() > 0) {
			for (MethodMetadata method : getPopulateMethods()) {
				builder.addMethod(method);
			}
		}
		if (!dateTypes.isEmpty()) {
			builder.addMethod(getDateTimeFormatHelperMethod());
		}
		if (jsonMetadata != null) {
			this.jsonMetadata = jsonMetadata;
			builder.addMethod(getJsonShowMethod());
			builder.addMethod(getJsonListMethod());
			builder.addMethod(getJsonCreateMethod());
			builder.addMethod(getCreateFromJsonArrayMethod());
			builder.addMethod(getJsonUpdateMethod());
			builder.addMethod(getUpdateFromJsonArrayMethod());
			builder.addMethod(getJsonDeleteMethod());
			if (annotationValues.exposeFinders && dynamicFinderMethods.size() > 0) {
				for (FinderMetadataDetails finder : new TreeSet<FinderMetadataDetails>(dynamicFinderMethods)) {
					builder.addMethod(getFinderJsonMethod(finder));
				}
			}
		}
		if (annotationValues.isCreate() || annotationValues.isUpdate()) {
			builder.addMethod(getEncodeUrlPathSegmentMethod());
		}
		
		itdTypeDetails = builder.build();

		new ItdSourceFileComposer(itdTypeDetails);
	}

	public WebScaffoldAnnotationValues getAnnotationValues() {
		return annotationValues;
	}
	
	private MethodMetadata getDeleteMethod() {
		JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataHolder = javaTypeMetadataHolder.getPersistenceDetails();
		if (javaTypePersistenceMetadataHolder == null || javaTypePersistenceMetadataHolder.getFindMethod() == null || javaTypePersistenceMetadataHolder.getRemoveMethod() == null) {
			// Mandatory input is missing (ROO-589)
			return null;
		}
		JavaSymbolName methodName = new JavaSymbolName("delete");

		List<AnnotationMetadata> typeAnnotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("value"), javaTypePersistenceMetadataHolder.getIdentifierField().getFieldName().getSymbolName()));
		AnnotationMetadataBuilder pathVariableAnnotation = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.PathVariable"), attributes);
		typeAnnotations.add(pathVariableAnnotation.build());

		List<AnnotationAttributeValue<?>> firstResultAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		firstResultAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "page"));
		firstResultAttributes.add(new BooleanAttributeValue(new JavaSymbolName("required"), false));
		List<AnnotationMetadata> firstResultAnnotations = new ArrayList<AnnotationMetadata>();
		AnnotationMetadataBuilder firstResultAnnotation = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestParam"), firstResultAttributes);
		firstResultAnnotations.add(firstResultAnnotation.build());

		List<AnnotationAttributeValue<?>> maxResultsAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		maxResultsAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "size"));
		maxResultsAttributes.add(new BooleanAttributeValue(new JavaSymbolName("required"), false));
		List<AnnotationMetadata> maxResultsAnnotations = new ArrayList<AnnotationMetadata>();
		AnnotationMetadataBuilder maxResultAnnotation = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestParam"), maxResultsAttributes);
		maxResultsAnnotations.add(maxResultAnnotation.build());

		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(javaTypePersistenceMetadataHolder.getIdentifierField().getFieldType(), typeAnnotations));
		paramTypes.add(new AnnotatedJavaType(new JavaType(Integer.class.getName()), firstResultAnnotations));
		paramTypes.add(new AnnotatedJavaType(new JavaType(Integer.class.getName()), maxResultsAnnotations));
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.Model"), null));
		
		MethodMetadata method = methodExists(methodName, paramTypes);
		if (method != null) return method;

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(javaTypePersistenceMetadataHolder.getIdentifierField().getFieldName().getSymbolName()));
		paramNames.add(new JavaSymbolName("page"));
		paramNames.add(new JavaSymbolName("size"));
		paramNames.add(new JavaSymbolName("uiModel"));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/{" + javaTypePersistenceMetadataHolder.getIdentifierField().getFieldName().getSymbolName() + "}"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("DELETE"))));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);

		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(formBackingType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "." + javaTypePersistenceMetadataHolder.getFindMethod().getMethodName() + "(" + javaTypePersistenceMetadataHolder.getIdentifierField().getFieldName().getSymbolName() + ")." + javaTypePersistenceMetadataHolder.getRemoveMethod().getMethodName() + "();");
		bodyBuilder.appendFormalLine("uiModel.asMap().clear();");
		bodyBuilder.appendFormalLine("uiModel.addAttribute(\"page\", (page == null) ? \"1\" : page.toString());");
		bodyBuilder.appendFormalLine("uiModel.addAttribute(\"size\", (size == null) ? \"10\" : size.toString());");
		bodyBuilder.appendFormalLine("return \"redirect:/" + controllerPath + "\";");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getListMethod() {
		JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataHolder = javaTypeMetadataHolder.getPersistenceDetails();
		if (javaTypePersistenceMetadataHolder == null || javaTypePersistenceMetadataHolder.getFindEntriesMethod() == null || javaTypePersistenceMetadataHolder.getCountMethod() == null  || javaTypePersistenceMetadataHolder.getFindAllMethod() == null) {
			// Mandatory input is missing (ROO-589)
			return null;
		}

		JavaSymbolName methodName = new JavaSymbolName("list");

		List<AnnotationAttributeValue<?>> firstResultAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		firstResultAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "page"));
		firstResultAttributes.add(new BooleanAttributeValue(new JavaSymbolName("required"), false));
		List<AnnotationMetadata> firstResultAnnotations = new ArrayList<AnnotationMetadata>();
		AnnotationMetadataBuilder firstResultAnnotation = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestParam"), firstResultAttributes);
		firstResultAnnotations.add(firstResultAnnotation.build());

		List<AnnotationAttributeValue<?>> maxResultsAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		maxResultsAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "size"));
		maxResultsAttributes.add(new BooleanAttributeValue(new JavaSymbolName("required"), false));
		List<AnnotationMetadata> maxResultsAnnotations = new ArrayList<AnnotationMetadata>();
		AnnotationMetadataBuilder maxResultAnnotation = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestParam"), maxResultsAttributes);
		maxResultsAnnotations.add(maxResultAnnotation.build());

		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(new JavaType(Integer.class.getName()), firstResultAnnotations));
		paramTypes.add(new AnnotatedJavaType(new JavaType(Integer.class.getName()), maxResultsAnnotations));
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.Model"), null));
		
		MethodMetadata method = methodExists(methodName, paramTypes);
		if (method != null) {
			return method;
		}

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("page"));
		paramNames.add(new JavaSymbolName("size"));
		paramNames.add(new JavaSymbolName("uiModel"));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("GET"))));
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes));

		String plural = javaTypeMetadataHolder.getPlural().toLowerCase();

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (page != null || size != null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("int sizeNo = size == null ? 10 : size.intValue();");
		bodyBuilder.appendFormalLine("uiModel.addAttribute(\"" + plural + "\", " + formBackingType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "." + javaTypePersistenceMetadataHolder.getFindEntriesMethod().getMethodName() + "(page == null ? 0 : (page.intValue() - 1) * sizeNo, sizeNo));");
		bodyBuilder.appendFormalLine("float nrOfPages = (float) " + formBackingType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "." + javaTypePersistenceMetadataHolder.getCountMethod().getMethodName() + "() / sizeNo;");
		bodyBuilder.appendFormalLine("uiModel.addAttribute(\"maxPages\", (int) ((nrOfPages > (int) nrOfPages || nrOfPages == 0.0) ? nrOfPages + 1 : nrOfPages));");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("} else {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("uiModel.addAttribute(\"" + plural + "\", " + formBackingType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "." + javaTypePersistenceMetadataHolder.getFindAllMethod().getMethodName() + "());");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		if (!dateTypes.isEmpty()) {
			bodyBuilder.appendFormalLine("addDateTimeFormatPatterns(uiModel);");
		}
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/list\";");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getShowMethod() {
		JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataHolder = javaTypeMetadataHolder.getPersistenceDetails();
		if (javaTypePersistenceMetadataHolder == null || javaTypePersistenceMetadataHolder.getFindMethod() == null) {
			// Mandatory input is missing (ROO-589)
			return null;
		}

		JavaSymbolName methodName = new JavaSymbolName("show");

		List<AnnotationMetadata> typeAnnotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("value"), javaTypePersistenceMetadataHolder.getIdentifierField().getFieldName().getSymbolName()));
		AnnotationMetadataBuilder pathVariableAnnotation = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.PathVariable"), attributes);
		typeAnnotations.add(pathVariableAnnotation.build());

		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(javaTypePersistenceMetadataHolder.getIdentifierField().getFieldType(), typeAnnotations));
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.Model"), null));
		
		MethodMetadata method = methodExists(methodName, paramTypes);
		if (method != null) return method;

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(javaTypePersistenceMetadataHolder.getIdentifierField().getFieldName().getSymbolName()));
		paramNames.add(new JavaSymbolName("uiModel"));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/{" + javaTypePersistenceMetadataHolder.getIdentifierField().getFieldName().getSymbolName() + "}"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("GET"))));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		if (!dateTypes.isEmpty()) {
			bodyBuilder.appendFormalLine("addDateTimeFormatPatterns(uiModel);");
		}
		bodyBuilder.appendFormalLine("uiModel.addAttribute(\"" + entityName.toLowerCase() + "\", " + formBackingType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "." + javaTypePersistenceMetadataHolder.getFindMethod().getMethodName() + "(" + javaTypePersistenceMetadataHolder.getIdentifierField().getFieldName().getSymbolName() + "));");
		bodyBuilder.appendFormalLine("uiModel.addAttribute(\"itemId\", " + javaTypePersistenceMetadataHolder.getIdentifierField().getFieldName().getSymbolName() + ");");
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/show\";");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getCreateMethod() {
		JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataHolder = javaTypeMetadataHolder.getPersistenceDetails();
		if (javaTypePersistenceMetadataHolder == null || javaTypePersistenceMetadataHolder.getPersistMethod() == null) {
			// Mandatory input is missing (ROO-589)
			return null;
		}
		
		JavaSymbolName methodName = new JavaSymbolName("create");
		
		List<AnnotationMetadata> typeAnnotations = new ArrayList<AnnotationMetadata>();
		AnnotationMetadataBuilder validAnnotation = new AnnotationMetadataBuilder(new JavaType("javax.validation.Valid"));
		typeAnnotations.add(validAnnotation.build());

		List<AnnotationMetadata> noAnnotations = new ArrayList<AnnotationMetadata>();

		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(formBackingType, typeAnnotations));
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.validation.BindingResult"), noAnnotations));
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.Model"), noAnnotations));
		paramTypes.add(new AnnotatedJavaType(new JavaType("javax.servlet.http.HttpServletRequest"), noAnnotations));

		MethodMetadata method = methodExists(methodName, paramTypes);
		if (method != null) return method;
		
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(entityName));
		paramNames.add(new JavaSymbolName("bindingResult"));
		paramNames.add(new JavaSymbolName("uiModel"));
		paramNames.add(new JavaSymbolName("httpServletRequest"));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("POST"))));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (bindingResult.hasErrors()) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("uiModel.addAttribute(\"" + entityName + "\", " + entityName + ");");
		if (!dateTypes.isEmpty()) {
			bodyBuilder.appendFormalLine("addDateTimeFormatPatterns(uiModel);");
		}
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/create\";");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("uiModel.asMap().clear();");
		bodyBuilder.appendFormalLine(entityName + "." + javaTypePersistenceMetadataHolder.getPersistMethod().getMethodName() + "();");
		bodyBuilder.appendFormalLine("return \"redirect:/" + controllerPath + "/\" + encodeUrlPathSegment(" + entityName + "." + javaTypePersistenceMetadataHolder.getIdentifierAccessorMethod().getMethodName() + "().toString(), httpServletRequest);");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getCreateFormMethod(List<JavaTypeMetadataDetails> dependentTypes) {
//		if (entityMetadata.getFindAllMethod() == null) {
//			// Mandatory input is missing (ROO-589)
//			return null;
//		}
		JavaSymbolName methodName = new JavaSymbolName("createForm");

		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.Model"), null));

		MethodMetadata method = methodExists(methodName, paramTypes);
		if (method != null) return method;
		
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("uiModel"));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("params"), "form"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("GET"))));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("uiModel.addAttribute(\"" + entityName + "\", new " + formBackingType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "());");
		if (!dateTypes.isEmpty()) {
			bodyBuilder.appendFormalLine("addDateTimeFormatPatterns(uiModel);");
		}
		boolean listAdded = false;
		for (JavaTypeMetadataDetails dependentType: dependentTypes) {
			if (dependentType.getPersistenceDetails().getCountMethod() == null) {
				continue;
			}
			if (!listAdded) {
				String listShort = new JavaType("java.util.List").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
				String arrayListShort = new JavaType("java.util.ArrayList").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
				bodyBuilder.appendFormalLine(listShort + " dependencies = new " + arrayListShort + "();");
				listAdded = true;
			}
			bodyBuilder.appendFormalLine("if (" + getShortName(dependentType.getJavaType()) + "." + dependentType.getPersistenceDetails().getCountMethod().getMethodName().getSymbolName() + "() == 0) {");
			bodyBuilder.indent();
			// Adding string array which has the fieldName at position 0 and the path at position 1
			bodyBuilder.appendFormalLine("dependencies.add(new String[]{\"" + dependentType.getJavaType().getSimpleTypeName().toLowerCase() + "\", \"" + dependentType.getPlural().toLowerCase() + "\"});");
			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("}");
		}
		if (listAdded) {
			bodyBuilder.appendFormalLine("uiModel.addAttribute(\"dependencies\", dependencies);");
		}
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/create\";");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getUpdateMethod() {
		JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataHolder = javaTypeMetadataHolder.getPersistenceDetails();
		if (javaTypePersistenceMetadataHolder == null || javaTypePersistenceMetadataHolder.getMergeMethod() == null) {
			// Mandatory input is missing (ROO-589)
			return null;
		}
		JavaSymbolName methodName = new JavaSymbolName("update");

		List<AnnotationMetadata> typeAnnotations = new ArrayList<AnnotationMetadata>();
		AnnotationMetadataBuilder validAnnotation = new AnnotationMetadataBuilder(new JavaType("javax.validation.Valid"));
		typeAnnotations.add(validAnnotation.build());

		List<AnnotationMetadata> noAnnotations = new ArrayList<AnnotationMetadata>();

		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(formBackingType, typeAnnotations));
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.validation.BindingResult"), noAnnotations));
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.Model"), noAnnotations));
		paramTypes.add(new AnnotatedJavaType(new JavaType("javax.servlet.http.HttpServletRequest"), noAnnotations));
		
		MethodMetadata method = methodExists(methodName, paramTypes);
		if (method != null) return method;

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(entityName));
		paramNames.add(new JavaSymbolName("bindingResult"));
		paramNames.add(new JavaSymbolName("uiModel"));
		paramNames.add(new JavaSymbolName("httpServletRequest"));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("PUT"))));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (bindingResult.hasErrors()) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("uiModel.addAttribute(\"" + entityName + "\", " + entityName + ");");
		if (!dateTypes.isEmpty()) {
			bodyBuilder.appendFormalLine("addDateTimeFormatPatterns(uiModel);");
		}
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/update\";");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("uiModel.asMap().clear();");
		bodyBuilder.appendFormalLine(entityName + "." + javaTypePersistenceMetadataHolder.getMergeMethod().getMethodName() + "();");
		bodyBuilder.appendFormalLine("return \"redirect:/" + controllerPath + "/\" + encodeUrlPathSegment(" + entityName + "." +  javaTypePersistenceMetadataHolder.getIdentifierAccessorMethod().getMethodName() + "().toString(), httpServletRequest);");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getUpdateFormMethod() {
		JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataHolder = javaTypeMetadataHolder.getPersistenceDetails();
		if (javaTypePersistenceMetadataHolder == null || javaTypePersistenceMetadataHolder.getFindMethod() == null) {
			// Mandatory input is missing (ROO-589)
			return null;
		}
		JavaSymbolName methodName = new JavaSymbolName("updateForm");

		List<AnnotationMetadata> typeAnnotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("value"), javaTypePersistenceMetadataHolder.getIdentifierField().getFieldName().getSymbolName()));
		AnnotationMetadataBuilder pathVariableAnnotation = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.PathVariable"), attributes);
		typeAnnotations.add(pathVariableAnnotation.build());

		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(javaTypePersistenceMetadataHolder.getIdentifierField().getFieldType(), typeAnnotations));
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.Model"), null));
		
		MethodMetadata updateFormMethod = methodExists(methodName, paramTypes);
		if (updateFormMethod != null) return updateFormMethod;

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(javaTypePersistenceMetadataHolder.getIdentifierField().getFieldName().getSymbolName()));
		paramNames.add(new JavaSymbolName("uiModel"));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/{" + javaTypePersistenceMetadataHolder.getIdentifierField().getFieldName().getSymbolName() + "}"));
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("params"), "form"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("GET"))));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("uiModel.addAttribute(\"" + entityName + "\", " + formBackingType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "." + javaTypePersistenceMetadataHolder.getFindMethod().getMethodName() + "(" + javaTypePersistenceMetadataHolder.getIdentifierField().getFieldName().getSymbolName() + "));");
		if (!dateTypes.isEmpty()) {
			bodyBuilder.appendFormalLine("addDateTimeFormatPatterns(uiModel);");
		}
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/update\";");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
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
		bodyBuilder.appendFormalLine("if (" + beanShortName.toLowerCase() + " == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return new " + getShortName(new JavaType("org.springframework.http.ResponseEntity")) + "<String>(" + getShortName(new JavaType("org.springframework.http.HttpStatus")) + ".NOT_FOUND);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return " + beanShortName.toLowerCase() + "." + toJsonMethodName.getSymbolName() + "();");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, new JavaType("java.lang.Object"), paramTypes, paramNames, bodyBuilder);
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
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(" + new JavaType("org.springframework.http.HttpStatus").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".CREATED);");

		List<JavaType> typeParams = new ArrayList<JavaType>();
		typeParams.add(JavaType.STRING_OBJECT);
		JavaType returnType = new JavaType("org.springframework.http.ResponseEntity", 0, DataType.TYPE, null, typeParams);

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
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(" + new JavaType("org.springframework.http.HttpStatus").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".CREATED);");

		List<JavaType> typeParams = new ArrayList<JavaType>();
		typeParams.add(JavaType.STRING_OBJECT);
		JavaType returnType = new JavaType("org.springframework.http.ResponseEntity", 0, DataType.TYPE, null, typeParams);

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
		bodyBuilder.appendFormalLine("return " + entityName + "." + toJsonArrayMethodName.getSymbolName() + "(" + entityName + "." + javaTypePersistenceMetadataHolder.getFindAllMethod().getMethodName() + "());");
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, bodyBuilder);
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
		bodyBuilder.appendFormalLine("if (" + beanShortName + "." + fromJsonMethodName.getSymbolName() + "(json)." + javaTypePersistenceMetadataHolder.getMergeMethod().getMethodName().getSymbolName() + "() == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(" + new JavaType("org.springframework.http.HttpStatus").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".NOT_FOUND);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(" + new JavaType("org.springframework.http.HttpStatus").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".OK);");
		
		List<JavaType> typeParams = new ArrayList<JavaType>();
		typeParams.add(JavaType.STRING_OBJECT);
		JavaType returnType = new JavaType("org.springframework.http.ResponseEntity", 0, DataType.TYPE, null, typeParams);
		
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
		bodyBuilder.appendFormalLine("for (" + beanName + " " + entityName + ": " + beanName + "." + fromJsonArrayMethodName.getSymbolName() + "(json)) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("if (" + entityName + "." + javaTypePersistenceMetadataHolder.getMergeMethod().getMethodName().getSymbolName() + "() == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(" + new JavaType("org.springframework.http.HttpStatus").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".NOT_FOUND);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(" + new JavaType("org.springframework.http.HttpStatus").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".OK);");
		
		List<JavaType> typeParams = new ArrayList<JavaType>();
		typeParams.add(JavaType.STRING_OBJECT);
		JavaType returnType = new JavaType("org.springframework.http.ResponseEntity", 0, DataType.TYPE, null, typeParams);
		
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
		bodyBuilder.appendFormalLine("if (" + beanShortName.toLowerCase() + " == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return new " + getShortName(new JavaType("org.springframework.http.ResponseEntity")) + "<String>(" + getShortName(new JavaType("org.springframework.http.HttpStatus")) + ".NOT_FOUND);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine(beanShortName.toLowerCase() + "." + javaTypePersistenceMetadataHolder.getRemoveMethod().getMethodName() + "();");
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(" + new JavaType("org.springframework.http.HttpStatus").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".OK);");

		List<JavaType> typeParams = new ArrayList<JavaType>();
		typeParams.add(JavaType.STRING_OBJECT);
		JavaType returnType = new JavaType("org.springframework.http.ResponseEntity", 0, DataType.TYPE, null, typeParams);
		
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
		
		String shortBeanName = formBackingType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		bodyBuilder.appendFormalLine("return " + shortBeanName + "." + jsonMetadata.getToJsonArrayMethodName().getSymbolName().toString() + "(" + shortBeanName + "." + finderDetails.getFinderMethodMetadata().getMethodName().getSymbolName() + "(" + methodParams.toString() + ").getResultList());");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, finderMethodName, JavaType.STRING_OBJECT, annotatedParamTypes, newParamNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getFinderFormMethod(FinderMetadataDetails finder) {
		Assert.notNull(finder, "Method metadata required for finder");
		JavaSymbolName finderFormMethodName = new JavaSymbolName(finder.getFinderMethodMetadata().getMethodName().getSymbolName() + "Form");

		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		List<JavaType> types = AnnotatedJavaType.convertFromAnnotatedJavaTypes(finder.getFinderMethodMetadata().getParameterTypes());

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

		boolean needmodel = false;
		for (JavaType javaType : types) {
			JavaTypeMetadataDetails typeMd = specialDomainTypes.get(javaType);
			JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataHolder = null;
			if (javaType.isCommonCollectionType()) {
				JavaTypeMetadataDetails paramTypeMd = specialDomainTypes.get(javaType.getParameters().get(0));
				if (paramTypeMd != null && paramTypeMd.isApplicationType()) {
					javaType = javaType.getParameters().get(0);
					javaTypePersistenceMetadataHolder = paramTypeMd.getPersistenceDetails();
				}
			} else if (typeMd != null && typeMd.isEnumType()) {
				bodyBuilder.appendFormalLine("uiModel.addAttribute(\"" + typeMd.getPlural().toLowerCase() + "\", java.util.Arrays.asList(" + javaType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".class.getEnumConstants()));");
			} else if (typeMd != null && typeMd.isApplicationType()) {
				javaTypePersistenceMetadataHolder = typeMd.getPersistenceDetails();
			}
			if (typeMd != null && javaTypePersistenceMetadataHolder != null && javaTypePersistenceMetadataHolder.getFindAllMethod() != null) {
				bodyBuilder.appendFormalLine("uiModel.addAttribute(\"" + typeMd.getPlural().toLowerCase() + "\", " + javaType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "." + javaTypePersistenceMetadataHolder.getFindAllMethod().getMethodName() + "());");
			}
			needmodel = true;
		}
		if (types.contains(new JavaType(Date.class.getName())) || types.contains(new JavaType(Calendar.class.getName()))) {
			bodyBuilder.appendFormalLine("addDateTimeFormatPatterns(uiModel);");
		}
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/" + finder.getFinderMethodMetadata().getMethodName().getSymbolName() + "\";");

		if (needmodel) {
			paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.Model"), null));
			paramNames.add(new JavaSymbolName("uiModel"));
		}
		
		MethodMetadata existingMethod = methodExists(finderFormMethodName, paramTypes);
		if (existingMethod != null) return existingMethod;
		
		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		List<StringAttributeValue> arrayValues = new ArrayList<StringAttributeValue>();
		arrayValues.add(new StringAttributeValue(new JavaSymbolName("value"), "find=" + finder.getFinderMethodMetadata().getMethodName().getSymbolName().replaceFirst("find" + javaTypeMetadataHolder.getPlural(), "")));
		arrayValues.add(new StringAttributeValue(new JavaSymbolName("value"), "form"));
		requestMappingAttributes.add(new ArrayAttributeValue<StringAttributeValue>(new JavaSymbolName("params"), arrayValues));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("GET"))));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, finderFormMethodName, JavaType.STRING_OBJECT, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getFinderMethod(FinderMetadataDetails finderMetadataDetails) {
		Assert.notNull(finderMetadataDetails, "Method metadata required for finder");
		JavaSymbolName finderMethodName = new JavaSymbolName(finderMetadataDetails.getFinderMethodMetadata().getMethodName().getSymbolName());

		List<AnnotatedJavaType> annotatedParamTypes = new ArrayList<AnnotatedJavaType>();
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		StringBuilder methodParams = new StringBuilder();

		boolean dateFieldPresent = false;
		for (FieldMetadata field: finderMetadataDetails.getFinderMethodParamFields()) {
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
				dateFieldPresent = true;
				JavaType dateTimeFormat = new JavaType("org.springframework.format.annotation.DateTimeFormat");
				AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), dateTimeFormat);
				if (annotation != null) {
					dateTimeFormat.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
					annotations.add(annotation);
				}
			}
			paramNames.add(fieldName);
			annotatedParamTypes.add(new AnnotatedJavaType(field.getFieldType(), annotations));

			if (field.getFieldType().equals(JavaType.BOOLEAN_OBJECT)) {
				methodParams.append(fieldName + " == null ? new Boolean(false) : " + fieldName + ", ");
			} else {
				methodParams.append(fieldName + ", ");
			}
		}

		if (methodParams.length() > 0) {
			methodParams.delete(methodParams.length() - 2, methodParams.length());
		}

		annotatedParamTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.Model"), new ArrayList<AnnotationMetadata>()));
		
		MethodMetadata existingMethod = methodExists(finderMethodName, annotatedParamTypes);
		if (existingMethod != null) {
			return existingMethod;
		}
		List<JavaSymbolName> newParamNames = new ArrayList<JavaSymbolName>();
		newParamNames.addAll(paramNames);
		newParamNames.add(new JavaSymbolName("uiModel"));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("params"), "find=" + finderMetadataDetails.getFinderMethodMetadata().getMethodName().getSymbolName().replaceFirst("find" + javaTypeMetadataHolder.getPlural(), "")));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("GET"))));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);
		
		bodyBuilder.appendFormalLine("uiModel.addAttribute(\"" + javaTypeMetadataHolder.getPlural().toLowerCase() + "\", " + formBackingType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "." + finderMetadataDetails.getFinderMethodMetadata().getMethodName().getSymbolName() + "(" + methodParams.toString() + ").getResultList());");
		if (dateFieldPresent) {
			bodyBuilder.appendFormalLine("addDateTimeFormatPatterns(uiModel);");
		}
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/list\";");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, finderMethodName, JavaType.STRING_OBJECT, annotatedParamTypes, newParamNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private List<MethodMetadata> getPopulateMethods() {
		List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
		for (JavaType type : specialDomainTypes.keySet()) {
			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			JavaTypeMetadataDetails javaTypeMd = specialDomainTypes.get(type);
			JavaTypePersistenceMetadataDetails javaTypePersistenceMd = javaTypeMd.getPersistenceDetails();
			if (javaTypePersistenceMd != null && javaTypePersistenceMd.getFindAllMethod() != null) {
				bodyBuilder.appendFormalLine("return " + type.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "." + javaTypePersistenceMd.getFindAllMethod().getMethodName() + "();");
			} else if (javaTypeMd.isEnumType()) {
				JavaType arrays = new JavaType("java.util.Arrays");
				bodyBuilder.appendFormalLine("return " + arrays.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".asList(" + type.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".class.getEnumConstants());");
			} else if (javaTypePersistenceMd != null && javaTypePersistenceMd.isRooIdentifier()) {
				continue;
			} else {
				throw new IllegalStateException("Unable to scaffold controller for type " + formBackingType.getFullyQualifiedTypeName() + ". The referenced type " + type.getFullyQualifiedTypeName() + " cannot be handled");
			}

			JavaSymbolName populateMethodName = new JavaSymbolName("populate" + javaTypeMd.getPlural());
			MethodMetadata addReferenceDataMethod = methodExists(populateMethodName, new ArrayList<AnnotatedJavaType>());
			if (addReferenceDataMethod != null) continue;

			List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
			List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
			attributes.add(new StringAttributeValue(new JavaSymbolName("value"), javaTypeMd.getPlural().toLowerCase()));
			annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.ModelAttribute"), attributes));

			List<JavaType> typeParams = new ArrayList<JavaType>();
			typeParams.add(type);
			JavaType returnType = new JavaType("java.util.Collection", 0, DataType.TYPE, null, typeParams);

			MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, populateMethodName, returnType, bodyBuilder);
			methodBuilder.setAnnotations(annotations);
			methods.add(methodBuilder.build());
		}
		return methods;
	}
	
	private MethodMetadata getEncodeUrlPathSegmentMethod() {
		JavaSymbolName encodeUrlPathSegment = new JavaSymbolName("encodeUrlPathSegment");
		
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(JavaType.STRING_OBJECT, new ArrayList<AnnotationMetadata>()));
		paramTypes.add(new AnnotatedJavaType(new JavaType("javax.servlet.http.HttpServletRequest"), new ArrayList<AnnotationMetadata>()));
		
		MethodMetadata encodeUrlPathSegmentMethod = methodExists(encodeUrlPathSegment, paramTypes);
		if (encodeUrlPathSegmentMethod != null) return encodeUrlPathSegmentMethod;
		
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("pathSegment"));
		paramNames.add(new JavaSymbolName("httpServletRequest"));
		
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("String enc = httpServletRequest.getCharacterEncoding();");
		bodyBuilder.appendFormalLine("if (enc == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("enc = " + new JavaType("org.springframework.web.util.WebUtils").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".DEFAULT_CHARACTER_ENCODING;");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("try {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("pathSegment = " + new JavaType("org.springframework.web.util.UriUtils").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".encodePathSegment(pathSegment, enc);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("catch (" + new JavaType("java.io.UnsupportedEncodingException").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + " uee) {}");
		bodyBuilder.appendFormalLine("return pathSegment;");
		
		return new MethodMetadataBuilder(getId(), 0, encodeUrlPathSegment, JavaType.STRING_OBJECT, paramTypes, paramNames, bodyBuilder).build();
	}
	
	private MethodMetadata getDateTimeFormatHelperMethod() {
		JavaSymbolName addDateTimeFormatPatterns = new JavaSymbolName("addDateTimeFormatPatterns");

		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.Model"), new ArrayList<AnnotationMetadata>()));
		
		MethodMetadata addDateTimeFormatPatternsMethod = methodExists(addDateTimeFormatPatterns, paramTypes);
		if (addDateTimeFormatPatternsMethod != null) return addDateTimeFormatPatternsMethod;

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("uiModel"));

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		Iterator<Map.Entry<JavaSymbolName, DateTimeFormatDetails>> it = dateTypes.entrySet().iterator();
		while (it.hasNext()) {
			Entry<JavaSymbolName, DateTimeFormatDetails> entry = it.next();
			String pattern;
			if (entry.getValue().pattern != null) {
				pattern = "\"" + entry.getValue().pattern + "\"";
			} else {
				JavaType dateTimeFormat = new JavaType("org.joda.time.format.DateTimeFormat");
				String dateTimeFormatSimple = dateTimeFormat.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
				JavaType localeContextHolder = new JavaType("org.springframework.context.i18n.LocaleContextHolder");
				String localeContextHolderSimple = localeContextHolder.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
				pattern = dateTimeFormatSimple + ".patternForStyle(\"" + entry.getValue().style + "\", " + localeContextHolderSimple + ".getLocale())";
			}
			bodyBuilder.appendFormalLine("uiModel.addAttribute(\"" + entityName + "_" + entry.getKey().getSymbolName().toLowerCase() + "_date_format\", " + pattern + ");");
		}

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), 0, addDateTimeFormatPatterns, JavaType.VOID_PRIMITIVE, paramTypes, paramNames, bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata methodExists(JavaSymbolName methodName, List<AnnotatedJavaType> parameters) {
		for (MethodMetadata method: existingMethods) {
			if (method.getMethodName().equals(methodName)) {
				return method;
			}
		}
		return null;
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
