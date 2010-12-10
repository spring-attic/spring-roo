package org.springframework.roo.addon.web.mvc.controller;

import java.beans.Introspector;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.entity.RooIdentifier;
import org.springframework.roo.addon.finder.FinderMetadata;
import org.springframework.roo.addon.json.JsonMetadata;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
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
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataService;
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
	private BeanInfoMetadata beanInfoMetadata;
	private EntityMetadata entityMetadata;
	private MetadataService metadataService;
	private SortedSet<JavaType> specialDomainTypes;
	private String controllerPath;
	private String entityName;
	private Map<JavaSymbolName, DateTimeFormat> dateTypes;
	private Map<JavaType, String> pluralCache;
	private JsonMetadata jsonMetadata;
	private Logger log = Logger.getLogger(getClass().getName());
	private MemberDetailsScanner detailsScanner;

	public WebScaffoldMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, MetadataService metadataService, MemberDetailsScanner detailsScanner, WebScaffoldAnnotationValues annotationValues, BeanInfoMetadata beanInfoMetadata, EntityMetadata entityMetadata, FinderMetadata finderMetadata, ControllerOperations controllerOperations) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(beanInfoMetadata, "Bean info metadata required");
		Assert.notNull(entityMetadata, "Entity metadata required");
		Assert.notNull(finderMetadata, "Finder metadata required");
		Assert.notNull(controllerOperations, "Controller operations required");
		Assert.notNull(detailsScanner, "Member details scanner required");
		if (!isValid()) {
			return;
		}
		this.pluralCache = new HashMap<JavaType, String>();
		this.annotationValues = annotationValues;
		this.beanInfoMetadata = beanInfoMetadata;
		this.entityMetadata = entityMetadata;
		this.detailsScanner = detailsScanner;
		this.entityName = uncapitalize(beanInfoMetadata.getJavaBean().getSimpleTypeName());
		if (ReservedWords.RESERVED_JAVA_KEYWORDS.contains(this.entityName)) {
			this.entityName = "_" + entityName;
		}
		this.controllerPath = annotationValues.getPath();
		this.metadataService = metadataService;

		specialDomainTypes = getSpecialDomainTypes(beanInfoMetadata.getJavaBean());
		dateTypes = getDatePatterns();

		if (annotationValues.isRegisterConverters()) {
			builder.addField(getConversionServiceField());
		}
		if (annotationValues.create) {
			builder.addMethod(getCreateMethod());
			builder.addMethod(getCreateFormMethod());
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
		if (annotationValues.exposeFinders) { // No need for null check of entityMetadata.getDynamicFinders as it guarantees non-null (but maybe empty list)
			for (String finderName : entityMetadata.getDynamicFinders()) {
				builder.addMethod(getFinderFormMethod(finderMetadata.getDynamicFinderMethod(finderName, beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase())));
				builder.addMethod(getFinderMethod(finderMetadata.getDynamicFinderMethod(finderName, beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase())));
			}
		}
		if (specialDomainTypes.size() > 0) {
			for (MethodMetadata method : getPopulateMethods()) {
				builder.addMethod(method);
			}
		}
		if (annotationValues.isRegisterConverters()) {
			builder.addMethod(getRegisterConvertersMethod());
		}
		if (!dateTypes.isEmpty()) {
			builder.addMethod(getDateTimeFormatHelperMethod());
		}
		if (annotationValues.isExposeJson()) {
			// Decide if we want to build json support
			this.jsonMetadata = (JsonMetadata) metadataService.get(JsonMetadata.createIdentifier(beanInfoMetadata.getJavaBean(), Path.SRC_MAIN_JAVA));
			if (jsonMetadata != null) {
				builder.addMethod(getJsonShowMethod());
				builder.addMethod(getJsonListMethod());
				builder.addMethod(getJsonCreateMethod());
				builder.addMethod(getCreateFromJsonArrayMethod());
				builder.addMethod(getJsonUpdateMethod());
				builder.addMethod(getUpdateFromJsonArrayMethod());
				builder.addMethod(getJsonDeleteMethod());
				if (annotationValues.exposeFinders) {
					for (String finderName : entityMetadata.getDynamicFinders()) {
						builder.addMethod(getFinderJsonMethod(finderMetadata.getDynamicFinderMethod(finderName, beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase())));
					}
				}
			}
		}
		if (annotationValues.isCreate() || annotationValues.isUpdate()) {
			builder.addMethod(getEncodeUrlPathSegmentMethod());
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
	
	private FieldMetadataBuilder getConversionServiceField() {
		FieldMetadataBuilder builder = new FieldMetadataBuilder(getId(), 0, new JavaSymbolName("conversionService"), new JavaType("org.springframework.core.convert.support.GenericConversionService"), null);
		builder.addAnnotation(new AnnotationMetadataBuilder(new JavaType("org.springframework.beans.factory.annotation.Autowired")));
		return builder;
	}

	private MethodMetadata getDeleteMethod() {
		if (entityMetadata.getFindMethod() == null || entityMetadata.getRemoveMethod() == null) {
			// Mandatory input is missing (ROO-589)
			return null;
		}
		JavaSymbolName methodName = new JavaSymbolName("delete");

		List<AnnotationMetadata> typeAnnotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("value"), entityMetadata.getIdentifierField().getFieldName().getSymbolName()));
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
		paramTypes.add(new AnnotatedJavaType(entityMetadata.getIdentifierField().getFieldType(), typeAnnotations));
		paramTypes.add(new AnnotatedJavaType(new JavaType(Integer.class.getName()), firstResultAnnotations));
		paramTypes.add(new AnnotatedJavaType(new JavaType(Integer.class.getName()), maxResultsAnnotations));
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.Model"), null));
		
		MethodMetadata method = methodExists(methodName, paramTypes);
		if (method != null) return method;

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(entityMetadata.getIdentifierField().getFieldName().getSymbolName()));
		paramNames.add(new JavaSymbolName("page"));
		paramNames.add(new JavaSymbolName("size"));
		paramNames.add(new JavaSymbolName("model"));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/{" + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + "}"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("DELETE"))));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);

		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(beanInfoMetadata.getJavaBean().getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "." + entityMetadata.getFindMethod().getMethodName() + "(" + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + ")." + entityMetadata.getRemoveMethod().getMethodName() + "();");
		bodyBuilder.appendFormalLine("model.addAttribute(\"page\", (page == null) ? \"1\" : page.toString());");
		bodyBuilder.appendFormalLine("model.addAttribute(\"size\", (size == null) ? \"10\" : size.toString());");
		bodyBuilder.appendFormalLine("return \"redirect:/" + controllerPath + "?page=\" + ((page == null) ? \"1\" : page.toString()) + \"&size=\" + ((size == null) ? \"10\" : size.toString());");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getListMethod() {
		if (entityMetadata.getFindEntriesMethod() == null || entityMetadata.getCountMethod() == null || entityMetadata.getFindAllMethod() == null) {
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
		paramNames.add(new JavaSymbolName("model"));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("GET"))));
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes));

		String plural = getPlural(beanInfoMetadata.getJavaBean()).toLowerCase();

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (page != null || size != null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("int sizeNo = size == null ? 10 : size.intValue();");
		bodyBuilder.appendFormalLine("model.addAttribute(\"" + plural + "\", " + beanInfoMetadata.getJavaBean().getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "." + entityMetadata.getFindEntriesMethod().getMethodName() + "(page == null ? 0 : (page.intValue() - 1) * sizeNo, sizeNo));");
		bodyBuilder.appendFormalLine("float nrOfPages = (float) " + beanInfoMetadata.getJavaBean().getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "." + entityMetadata.getCountMethod().getMethodName() + "() / sizeNo;");
		bodyBuilder.appendFormalLine("model.addAttribute(\"maxPages\", (int) ((nrOfPages > (int) nrOfPages || nrOfPages == 0.0) ? nrOfPages + 1 : nrOfPages));");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("} else {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("model.addAttribute(\"" + plural + "\", " + beanInfoMetadata.getJavaBean().getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "." + entityMetadata.getFindAllMethod().getMethodName() + "());");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		if (!dateTypes.isEmpty()) {
			bodyBuilder.appendFormalLine("addDateTimeFormatPatterns(model);");
		}
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/list\";");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getShowMethod() {
		if (entityMetadata.getFindMethod() == null) {
			// Mandatory input is missing (ROO-589)
			return null;
		}

		JavaSymbolName methodName = new JavaSymbolName("show");

		List<AnnotationMetadata> typeAnnotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("value"), entityMetadata.getIdentifierField().getFieldName().getSymbolName()));
		AnnotationMetadataBuilder pathVariableAnnotation = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.PathVariable"), attributes);
		typeAnnotations.add(pathVariableAnnotation.build());

		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(entityMetadata.getIdentifierField().getFieldType(), typeAnnotations));
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.Model"), null));
		
		MethodMetadata method = methodExists(methodName, paramTypes);
		if (method != null) return method;

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(entityMetadata.getIdentifierField().getFieldName().getSymbolName()));
		paramNames.add(new JavaSymbolName("model"));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/{" + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + "}"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("GET"))));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		if (!dateTypes.isEmpty()) {
			bodyBuilder.appendFormalLine("addDateTimeFormatPatterns(model);");
		}
		bodyBuilder.appendFormalLine("model.addAttribute(\"" + entityName.toLowerCase() + "\", " + beanInfoMetadata.getJavaBean().getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "." + entityMetadata.getFindMethod().getMethodName() + "(" + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + "));");
		bodyBuilder.appendFormalLine("model.addAttribute(\"itemId\", " + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + ");");
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/show\";");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getCreateMethod() {
		if (entityMetadata.getPersistMethod() == null) {// || entityMetadata.getFindAllMethod() == null) {
			// Mandatory input is missing (ROO-589)
			return null;
		}
		JavaSymbolName methodName = new JavaSymbolName("create");
		
		List<AnnotationMetadata> typeAnnotations = new ArrayList<AnnotationMetadata>();
		AnnotationMetadataBuilder validAnnotation = new AnnotationMetadataBuilder(new JavaType("javax.validation.Valid"));
		typeAnnotations.add(validAnnotation.build());

		List<AnnotationMetadata> noAnnotations = new ArrayList<AnnotationMetadata>();

		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(beanInfoMetadata.getJavaBean(), typeAnnotations));
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.validation.BindingResult"), noAnnotations));
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.Model"), noAnnotations));
		paramTypes.add(new AnnotatedJavaType(new JavaType("javax.servlet.http.HttpServletRequest"), noAnnotations));

		MethodMetadata method = methodExists(methodName, paramTypes);
		if (method != null) return method;
		
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(entityName));
		paramNames.add(new JavaSymbolName("result"));
		paramNames.add(new JavaSymbolName("model"));
		paramNames.add(new JavaSymbolName("request"));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("POST"))));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (result.hasErrors()) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("model.addAttribute(\"" + entityName + "\", " + entityName + ");");
		if (!dateTypes.isEmpty()) {
			bodyBuilder.appendFormalLine("addDateTimeFormatPatterns(model);");
		}
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/create\";");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine(entityName + "." + entityMetadata.getPersistMethod().getMethodName() + "();");
		bodyBuilder.appendFormalLine("return \"redirect:/" + controllerPath + "/\" + encodeUrlPathSegment(" + entityName + "." + entityMetadata.getIdentifierAccessor().getMethodName() + "().toString(), request);");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getCreateFormMethod() {
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
		paramNames.add(new JavaSymbolName("model"));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("params"), "form"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("GET"))));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("model.addAttribute(\"" + entityName + "\", new " + beanInfoMetadata.getJavaBean().getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "());");
		if (!dateTypes.isEmpty()) {
			bodyBuilder.appendFormalLine("addDateTimeFormatPatterns(model);");
		}
		boolean listAdded = false;
		for (MethodMetadata accessorMethod : beanInfoMetadata.getPublicAccessors()) {
			if (specialDomainTypes.contains(accessorMethod.getReturnType())) {
				FieldMetadata field = beanInfoMetadata.getFieldForPropertyName(BeanInfoMetadata.getPropertyNameForJavaBeanMethod(accessorMethod));
				if (null != field && null != MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.NotNull"))) {
					EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(accessorMethod.getReturnType(), Path.SRC_MAIN_JAVA));
					if (entityMetadata != null) {
						if (!listAdded) {
							String listShort = new JavaType("java.util.List").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
							String arrayListShort = new JavaType("java.util.ArrayList").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
							bodyBuilder.appendFormalLine(listShort + " dependencies = new " + arrayListShort + "();");
							listAdded = true;
						}
						bodyBuilder.appendFormalLine("if (" + accessorMethod.getReturnType().getSimpleTypeName() + "." + entityMetadata.getCountMethod().getMethodName().getSymbolName() + "() == 0) {");
						bodyBuilder.indent();
						// Adding string array which has the fieldName at position 0 and the path at position 1
						bodyBuilder.appendFormalLine("dependencies.add(new String[]{\"" + field.getFieldName().getSymbolName() + "\", \"" + entityMetadata.getPlural().toLowerCase() + "\"});");
						bodyBuilder.indentRemove();
						bodyBuilder.appendFormalLine("}");
					}
				}
			}
		}
		if (listAdded) {
			bodyBuilder.appendFormalLine("model.addAttribute(\"dependencies\", dependencies);");
		}
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/create\";");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getUpdateMethod() {
		if (entityMetadata.getMergeMethod() == null) {// || entityMetadata.getFindAllMethod() == null) {
			// Mandatory input is missing (ROO-589)
			return null;
		}
		JavaSymbolName methodName = new JavaSymbolName("update");

		List<AnnotationMetadata> typeAnnotations = new ArrayList<AnnotationMetadata>();
		AnnotationMetadataBuilder validAnnotation = new AnnotationMetadataBuilder(new JavaType("javax.validation.Valid"));
		typeAnnotations.add(validAnnotation.build());

		List<AnnotationMetadata> noAnnotations = new ArrayList<AnnotationMetadata>();

		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(beanInfoMetadata.getJavaBean(), typeAnnotations));
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.validation.BindingResult"), noAnnotations));
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.Model"), noAnnotations));
		paramTypes.add(new AnnotatedJavaType(new JavaType("javax.servlet.http.HttpServletRequest"), noAnnotations));
		
		MethodMetadata method = methodExists(methodName, paramTypes);
		if (method != null) return method;

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(entityName));
		paramNames.add(new JavaSymbolName("result"));
		paramNames.add(new JavaSymbolName("model"));
		paramNames.add(new JavaSymbolName("request"));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("PUT"))));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (result.hasErrors()) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("model.addAttribute(\"" + entityName + "\", " + entityName + ");");
		if (!dateTypes.isEmpty()) {
			bodyBuilder.appendFormalLine("addDateTimeFormatPatterns(model);");
		}
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/update\";");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine(entityName + "." + entityMetadata.getMergeMethod().getMethodName() + "();");
		bodyBuilder.appendFormalLine("return \"redirect:/" + controllerPath + "/\" + encodeUrlPathSegment(" + entityName + "." +  entityMetadata.getIdentifierAccessor().getMethodName() + "().toString(), request);");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getUpdateFormMethod() {
		if (entityMetadata.getFindMethod() == null) {// || entityMetadata.getFindAllMethod() == null) {
			// Mandatory input is missing (ROO-589)
			return null;
		}
		JavaSymbolName methodName = new JavaSymbolName("updateForm");

		List<AnnotationMetadata> typeAnnotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("value"), entityMetadata.getIdentifierField().getFieldName().getSymbolName()));
		AnnotationMetadataBuilder pathVariableAnnotation = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.PathVariable"), attributes);
		typeAnnotations.add(pathVariableAnnotation.build());

		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(entityMetadata.getIdentifierField().getFieldType(), typeAnnotations));
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.Model"), null));
		
		MethodMetadata updateFormMethod = methodExists(methodName, paramTypes);
		if (updateFormMethod != null) return updateFormMethod;

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(entityMetadata.getIdentifierField().getFieldName().getSymbolName()));
		paramNames.add(new JavaSymbolName("model"));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/{" + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + "}"));
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("params"), "form"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("GET"))));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("model.addAttribute(\"" + entityName + "\", " + beanInfoMetadata.getJavaBean().getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "." + entityMetadata.getFindMethod().getMethodName() + "(" + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + "));");
		if (!dateTypes.isEmpty()) {
			bodyBuilder.appendFormalLine("addDateTimeFormatPatterns(model);");
		}
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/update\";");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getJsonShowMethod() {
		JavaSymbolName toJsonMethodName = jsonMetadata.getToJsonMethodName();
		if (toJsonMethodName == null || entityMetadata.getFindMethod() == null) {
			return null;
		}
		JavaSymbolName methodName = new JavaSymbolName("showJson");

		List<AnnotationMetadata> parameters = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("value"), entityMetadata.getIdentifierField().getFieldName().getSymbolName()));
		AnnotationMetadataBuilder pathVariableAnnotation = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.PathVariable"), attributes);
		parameters.add(pathVariableAnnotation.build());
		
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(entityMetadata.getIdentifierField().getFieldType(), parameters));
		
		MethodMetadata jsonShowMethod = methodExists(methodName, paramTypes);
		if (jsonShowMethod != null) return jsonShowMethod;

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(entityMetadata.getIdentifierField().getFieldName().getSymbolName()));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/{" + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + "}"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("GET"))));
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("headers"), "Accept=application/json"));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.ResponseBody")));

		String beanShortName = getShortName(beanInfoMetadata.getJavaBean());
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(beanShortName + " " + beanShortName.toLowerCase() + " = " + beanShortName + "." + entityMetadata.getFindMethod().getMethodName() + "(" + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + ");");
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
		if (fromJsonMethodName == null || entityMetadata.getPersistMethod() == null) {
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
		bodyBuilder.appendFormalLine(beanInfoMetadata.getJavaBean().getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "." + fromJsonMethodName.getSymbolName() + "(json)." + entityMetadata.getPersistMethod().getMethodName().getSymbolName() + "();");
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
		if (fromJsonArrayMethodName == null || entityMetadata.getPersistMethod() == null) {
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
		String beanName = beanInfoMetadata.getJavaBean().getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());

		List<JavaType> params = new ArrayList<JavaType>();
		params.add(beanInfoMetadata.getJavaBean());
		bodyBuilder.appendFormalLine("for (" + beanName + " " + entityName + ": " + beanName + "." + fromJsonArrayMethodName.getSymbolName() + "(json)) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(entityName + "." + entityMetadata.getPersistMethod().getMethodName().getSymbolName() + "();");
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
		if (toJsonArrayMethodName == null || entityMetadata.getFindAllMethod() == null) {
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
		String entityName = beanInfoMetadata.getJavaBean().getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		bodyBuilder.appendFormalLine("return " + entityName + "." + toJsonArrayMethodName.getSymbolName() + "(" + entityName + "." + entityMetadata.getFindAllMethod().getMethodName() + "());");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}
	
	private MethodMetadata getJsonUpdateMethod() {
		JavaSymbolName fromJsonMethodName = jsonMetadata.getFromJsonMethodName();
		if (fromJsonMethodName == null || entityMetadata.getMergeMethod() == null) {
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
		String beanShortName = beanInfoMetadata.getJavaBean().getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		bodyBuilder.appendFormalLine("if (" + beanShortName + "." + fromJsonMethodName.getSymbolName() + "(json)." + entityMetadata.getMergeMethod().getMethodName().getSymbolName() + "() == null) {");
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
		if (fromJsonArrayMethodName == null || entityMetadata.getMergeMethod() == null) {
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
		String beanName = beanInfoMetadata.getJavaBean().getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());

		List<JavaType> params = new ArrayList<JavaType>();
		params.add(beanInfoMetadata.getJavaBean());
		bodyBuilder.appendFormalLine("for (" + beanName + " " + entityName + ": " + beanName + "." + fromJsonArrayMethodName.getSymbolName() + "(json)) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("if (" + entityName + "." + entityMetadata.getMergeMethod().getMethodName().getSymbolName() + "() == null) {");
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
		if (entityMetadata.getFindMethod() == null || entityMetadata.getRemoveMethod() == null) {
			// Mandatory input is missing (ROO-589)
			return null;
		}
		JavaSymbolName methodName = new JavaSymbolName("deleteFromJson");

		List<AnnotationMetadata> typeAnnotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("value"), entityMetadata.getIdentifierField().getFieldName().getSymbolName()));
		AnnotationMetadataBuilder pathVariableAnnotation = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.PathVariable"), attributes);
		typeAnnotations.add(pathVariableAnnotation.build());

		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(entityMetadata.getIdentifierField().getFieldType(), typeAnnotations));
		
		MethodMetadata method = methodExists(methodName, paramTypes);
		if (method != null) return method;

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(entityMetadata.getIdentifierField().getFieldName().getSymbolName()));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/{" + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + "}"));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("DELETE"))));
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("headers"), "Accept=application/json"));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);

		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);

		String beanShortName = beanInfoMetadata.getJavaBean().getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(beanShortName + " " + beanShortName.toLowerCase() + " = " + beanShortName + "." + entityMetadata.getFindMethod().getMethodName() + "(" + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + ");");
		bodyBuilder.appendFormalLine("if (" + beanShortName.toLowerCase() + " == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return new " + getShortName(new JavaType("org.springframework.http.ResponseEntity")) + "<String>(" + getShortName(new JavaType("org.springframework.http.HttpStatus")) + ".NOT_FOUND);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine(beanShortName.toLowerCase() + "." + entityMetadata.getRemoveMethod().getMethodName() + "();");
		bodyBuilder.appendFormalLine("return new ResponseEntity<String>(" + new JavaType("org.springframework.http.HttpStatus").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".OK);");

		List<JavaType> typeParams = new ArrayList<JavaType>();
		typeParams.add(JavaType.STRING_OBJECT);
		JavaType returnType = new JavaType("org.springframework.http.ResponseEntity", 0, DataType.TYPE, null, typeParams);
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, paramTypes, paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}
	
	private MethodMetadata getFinderJsonMethod(MethodMetadata methodMetadata) {
		Assert.notNull(methodMetadata, "Method metadata required for finder");
		if (jsonMetadata.getToJsonArrayMethodName() == null) {
			return null;
		}
		JavaSymbolName finderMethodName = new JavaSymbolName("json" + StringUtils.capitalize(methodMetadata.getMethodName().getSymbolName()));

		List<AnnotatedJavaType> annotatedParamTypes = new ArrayList<AnnotatedJavaType>();
		List<JavaSymbolName> paramNames = methodMetadata.getParameterNames();
		List<JavaType> paramTypes = AnnotatedJavaType.convertFromAnnotatedJavaTypes(methodMetadata.getParameterTypes());

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		StringBuilder methodParams = new StringBuilder();

		for (int i = 0; i < paramTypes.size(); i++) {
			List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
			List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
			attributes.add(new StringAttributeValue(new JavaSymbolName("value"), uncapitalize(paramNames.get(i).getSymbolName())));
			if (paramTypes.get(i).equals(JavaType.BOOLEAN_PRIMITIVE) || paramTypes.get(i).equals(JavaType.BOOLEAN_OBJECT)) {
				attributes.add(new BooleanAttributeValue(new JavaSymbolName("required"), false));
			}
			AnnotationMetadataBuilder requestParamAnnotation = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestParam"), attributes);
			annotations.add(requestParamAnnotation.build());
			if (paramTypes.get(i).equals(new JavaType(Date.class.getName())) || paramTypes.get(i).equals(new JavaType(Calendar.class.getName()))) {
				JavaSymbolName fieldName = null;
				if (paramNames.get(i).getSymbolName().startsWith("max") || paramNames.get(i).getSymbolName().startsWith("min")) {
					fieldName = new JavaSymbolName(uncapitalize(paramNames.get(i).getSymbolName().substring(3)));
				} else {
					fieldName = paramNames.get(i);
				}
				FieldMetadata field = beanInfoMetadata.getFieldForPropertyName(fieldName);
				if (field != null) {
					AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("org.springframework.format.annotation.DateTimeFormat"));
					if (annotation != null) {
						annotations.add(annotation);
					}
				}
			}
			annotatedParamTypes.add(new AnnotatedJavaType(paramTypes.get(i), annotations));

			if (paramTypes.get(i).equals(JavaType.BOOLEAN_OBJECT)) {
				methodParams.append(paramNames.get(i) + " == null ? new Boolean(false) : " + paramNames.get(i) + ", ");
			} else {
				methodParams.append(paramNames.get(i) + ", ");
			}
		}

		if (methodParams.length() > 0) {
			methodParams.delete(methodParams.length() - 2, methodParams.length());
		}

		annotatedParamTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.Model"), new ArrayList<AnnotationMetadata>()));
		
		MethodMetadata existingMethod = methodExists(finderMethodName, annotatedParamTypes);
		if (existingMethod != null) return existingMethod;
		
		List<JavaSymbolName> newParamNames = new ArrayList<JavaSymbolName>();
		newParamNames.addAll(paramNames);
		newParamNames.add(new JavaSymbolName("model"));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("params"), "find=" + methodMetadata.getMethodName().getSymbolName().replaceFirst("find" + entityMetadata.getPlural(), "")));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("GET"))));
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("headers"), "Accept=application/json"));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);
		
		String shortBeanName = beanInfoMetadata.getJavaBean().getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		bodyBuilder.appendFormalLine("return " + shortBeanName + "." + jsonMetadata.getToJsonArrayMethodName().getSymbolName().toString() + "(" + shortBeanName + "." + methodMetadata.getMethodName().getSymbolName() + "(" + methodParams.toString() + ").getResultList());");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, finderMethodName, JavaType.STRING_OBJECT, annotatedParamTypes, newParamNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getFinderFormMethod(MethodMetadata methodMetadata) {
		if (entityMetadata.getFindAllMethod() == null) {
			// Mandatory input is missing (ROO-589)
			return null;
		}
		Assert.notNull(methodMetadata, "Method metadata required for finder");
		JavaSymbolName finderFormMethodName = new JavaSymbolName(methodMetadata.getMethodName().getSymbolName() + "Form");

		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		List<JavaType> types = AnnotatedJavaType.convertFromAnnotatedJavaTypes(methodMetadata.getParameterTypes());

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

		boolean needmodel = false;
		for (JavaType javaType : types) {
			EntityMetadata typeEntityMetadata = null;
			if (javaType.isCommonCollectionType() && isSpecialType(javaType.getParameters().get(0))) {
				javaType = javaType.getParameters().get(0);
				typeEntityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA));
			} else if (isEnumType(javaType)) {
				bodyBuilder.appendFormalLine("model.addAttribute(\"" + getPlural(javaType).toLowerCase() + "\", java.util.Arrays.asList(" + javaType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".class.getEnumConstants()));");
			} else if (isSpecialType(javaType)) {
				typeEntityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA));
			}
			if (typeEntityMetadata != null) {
				bodyBuilder.appendFormalLine("model.addAttribute(\"" + getPlural(javaType).toLowerCase() + "\", " + javaType.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "." + typeEntityMetadata.getFindAllMethod().getMethodName() + "());");
			}
			needmodel = true;
		}
		if (types.contains(new JavaType(Date.class.getName())) || types.contains(new JavaType(Calendar.class.getName()))) {
			bodyBuilder.appendFormalLine("addDateTimeFormatPatterns(model);");
		}
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/" + methodMetadata.getMethodName().getSymbolName() + "\";");

		if (needmodel) {
			paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.Model"), null));
			paramNames.add(new JavaSymbolName("model"));
		}
		
		MethodMetadata existingMethod = methodExists(finderFormMethodName, paramTypes);
		if (existingMethod != null) return existingMethod;
		
		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		List<StringAttributeValue> arrayValues = new ArrayList<StringAttributeValue>();
		arrayValues.add(new StringAttributeValue(new JavaSymbolName("value"), "find=" + methodMetadata.getMethodName().getSymbolName().replaceFirst("find" + entityMetadata.getPlural(), "")));
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

	private MethodMetadata getFinderMethod(MethodMetadata methodMetadata) {
		Assert.notNull(methodMetadata, "Method metadata required for finder");
		JavaSymbolName finderMethodName = new JavaSymbolName(methodMetadata.getMethodName().getSymbolName());

		List<AnnotatedJavaType> annotatedParamTypes = new ArrayList<AnnotatedJavaType>();
		List<JavaSymbolName> paramNames = methodMetadata.getParameterNames();
		List<JavaType> paramTypes = AnnotatedJavaType.convertFromAnnotatedJavaTypes(methodMetadata.getParameterTypes());

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		StringBuilder methodParams = new StringBuilder();

		for (int i = 0; i < paramTypes.size(); i++) {
			List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
			List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
			attributes.add(new StringAttributeValue(new JavaSymbolName("value"), uncapitalize(paramNames.get(i).getSymbolName())));
			if (paramTypes.get(i).equals(JavaType.BOOLEAN_PRIMITIVE) || paramTypes.get(i).equals(JavaType.BOOLEAN_OBJECT)) {
				attributes.add(new BooleanAttributeValue(new JavaSymbolName("required"), false));
			}
			AnnotationMetadataBuilder requestParamAnnotation = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestParam"), attributes);
			annotations.add(requestParamAnnotation.build());
			if (paramTypes.get(i).equals(new JavaType(Date.class.getName())) || paramTypes.get(i).equals(new JavaType(Calendar.class.getName()))) {
				JavaSymbolName fieldName = null;
				if (paramNames.get(i).getSymbolName().startsWith("max") || paramNames.get(i).getSymbolName().startsWith("min")) {
					fieldName = new JavaSymbolName(uncapitalize(paramNames.get(i).getSymbolName().substring(3)));
				} else {
					fieldName = paramNames.get(i);
				}
				FieldMetadata field = beanInfoMetadata.getFieldForPropertyName(fieldName);
				if (field != null) {
					AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("org.springframework.format.annotation.DateTimeFormat"));
					if (annotation != null) {
						annotations.add(annotation);
					}
				}
			}
			annotatedParamTypes.add(new AnnotatedJavaType(paramTypes.get(i), annotations));

			if (paramTypes.get(i).equals(JavaType.BOOLEAN_OBJECT)) {
				methodParams.append(paramNames.get(i) + " == null ? new Boolean(false) : " + paramNames.get(i) + ", ");
			} else {
				methodParams.append(paramNames.get(i) + ", ");
			}
		}

		if (methodParams.length() > 0) {
			methodParams.delete(methodParams.length() - 2, methodParams.length());
		}

		annotatedParamTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.Model"), new ArrayList<AnnotationMetadata>()));
		
		MethodMetadata existingMethod = methodExists(finderMethodName, annotatedParamTypes);
		if (existingMethod != null) return existingMethod;
		
		List<JavaSymbolName> newParamNames = new ArrayList<JavaSymbolName>();
		newParamNames.addAll(paramNames);
		newParamNames.add(new JavaSymbolName("model"));

		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("params"), "find=" + methodMetadata.getMethodName().getSymbolName().replaceFirst("find" + entityMetadata.getPlural(), "")));
		requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(new JavaType("org.springframework.web.bind.annotation.RequestMethod"), new JavaSymbolName("GET"))));
		AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes);
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(requestMapping);
		
		bodyBuilder.appendFormalLine("model.addAttribute(\"" + getPlural(beanInfoMetadata.getJavaBean()).toLowerCase() + "\", " + beanInfoMetadata.getJavaBean().getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "." + methodMetadata.getMethodName().getSymbolName() + "(" + methodParams.toString() + ").getResultList());");
		if (paramTypes.contains(new JavaType(Date.class.getName())) || paramTypes.contains(new JavaType(Calendar.class.getName()))) {
			bodyBuilder.appendFormalLine("addDateTimeFormatPatterns(model);");
		}
		bodyBuilder.appendFormalLine("return \"" + controllerPath + "/list\";");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, finderMethodName, JavaType.STRING_OBJECT, annotatedParamTypes, newParamNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getRegisterConvertersMethod() {
		JavaSymbolName registerConvertersMethodName = new JavaSymbolName("registerConverters");
		MethodMetadata registerConvertersMethod = methodExists(registerConvertersMethodName, new ArrayList<AnnotatedJavaType>());
		if (registerConvertersMethod != null) return registerConvertersMethod;

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		boolean converterPresent = false;

		List<JavaType> typesForConversion = new ArrayList<JavaType>(specialDomainTypes);
		Set<String> converterTypeSet = new HashSet<String>();
		typesForConversion.add(beanInfoMetadata.getJavaBean());
		for (int index = 0; index < typesForConversion.size(); index++) {
			JavaType conversionType = typesForConversion.get(index);
			BeanInfoMetadata typeBeanInfoMetadata = (BeanInfoMetadata) metadataService.get(BeanInfoMetadata.createIdentifier(conversionType , Path.SRC_MAIN_JAVA));
			EntityMetadata typeEntityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(conversionType, Path.SRC_MAIN_JAVA));
			List<MethodMetadata> elegibleMethods = new ArrayList<MethodMetadata>();
			int fieldCounter = 3;
			if (typeBeanInfoMetadata != null) {
				// Determine fields to be included in converter
				for (MethodMetadata accessor : typeBeanInfoMetadata.getPublicAccessors(false)) {
					if (fieldCounter == 0) {
						break;
					}
					if (typeEntityMetadata != null) {
						if (accessor.getMethodName().equals(typeEntityMetadata.getIdentifierAccessor().getMethodName()) || (typeEntityMetadata.getVersionAccessor() != null && accessor.getMethodName().equals(typeEntityMetadata.getVersionAccessor().getMethodName()))) {
							continue;
						}
					}
					FieldMetadata field = typeBeanInfoMetadata.getFieldForPropertyName(BeanInfoMetadata.getPropertyNameForJavaBeanMethod(accessor));
					if (field != null // Should not happen
						&& !field.getFieldType().isCommonCollectionType() && !field.getFieldType().isArray() // Exclude collections and arrays
						&& !getSpecialDomainTypes(conversionType).contains(field.getFieldType()) // Exclude references to other domain objects as they are too verbose
						&& !field.getFieldType().equals(JavaType.BOOLEAN_PRIMITIVE) && !field.getFieldType().equals(JavaType.BOOLEAN_OBJECT) /* Exclude boolean values as they would not be meaningful in this presentation */ ) {
						
						elegibleMethods.add(accessor);
						fieldCounter--;
					}
				}

				if (elegibleMethods.size() > 0) {
					JavaType converter = new JavaType("org.springframework.core.convert.converter.Converter");
					String conversionTypeFieldName = uncapitalize(conversionType.getSimpleTypeName());
					JavaSymbolName converterMethodName = new JavaSymbolName("get" + conversionType.getSimpleTypeName() + "Converter");
					if (!converterTypeSet.contains(conversionType.getFullyQualifiedTypeName()) && null == methodExists(converterMethodName, new ArrayList<AnnotatedJavaType>())) {
						converterTypeSet.add(conversionType.getFullyQualifiedTypeName());
						if (! conversionType.equals(beanInfoMetadata.getJavaBean())) {
							bodyBuilder.appendFormalLine("if (! conversionService.canConvert(" + conversionType.getSimpleTypeName() + ".class, String.class)) {");
							bodyBuilder.indent();
						}
						bodyBuilder.appendFormalLine("conversionService.addConverter(" + converterMethodName.getSymbolName() + "());");
						if (! conversionType.equals(beanInfoMetadata.getJavaBean())) {
							bodyBuilder.indentRemove();
							bodyBuilder.appendFormalLine("}");
						}
						
						converterPresent = true;
						
						// Register the converter method
						InvocableMemberBodyBuilder converterBodyBuilder = new InvocableMemberBodyBuilder();
						converterBodyBuilder.appendFormalLine("return new " + converter.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "<" + conversionType.getSimpleTypeName() + ", String>() {");
						converterBodyBuilder.indent();
						converterBodyBuilder.appendFormalLine("public String convert(" + conversionType.getSimpleTypeName() + " " + conversionTypeFieldName + ") {");
						converterBodyBuilder.indent();

						StringBuilder sb = new StringBuilder();
						sb.append("return new StringBuilder().append(").append(conversionTypeFieldName).append(".").append(elegibleMethods.get(0).getMethodName().getSymbolName()).append("()");
						if (isEnumType(elegibleMethods.get(0).getReturnType())) {
							sb.append(".name()");
						}
						sb.append(")");
						for (int i = 1; i < elegibleMethods.size(); i++) {
							sb.append(".append(\" \").append(").append(conversionTypeFieldName).append(".").append(elegibleMethods.get(i).getMethodName().getSymbolName()).append("()");
							if (isEnumType(elegibleMethods.get(i).getReturnType())) {
								sb.append(".name()");
							}
							sb.append(")");
						}
						sb.append(".toString();");

						converterBodyBuilder.appendFormalLine(sb.toString());
						converterBodyBuilder.indentRemove();
						converterBodyBuilder.appendFormalLine("}");
						converterBodyBuilder.indentRemove();
						converterBodyBuilder.appendFormalLine("};");
						List<JavaType> params = new ArrayList<JavaType>();
						params.add(conversionType);
						params.add(JavaType.STRING_OBJECT);
						JavaType parameterizedConverter = new JavaType("org.springframework.core.convert.converter.Converter", 0, DataType.TYPE, null, params);
						
						MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), 0, converterMethodName, parameterizedConverter, converterBodyBuilder);
						builder.addMethod(methodBuilder.build());
					}
				}
			}
		}
		
		if (converterPresent) {
			MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), 0, registerConvertersMethodName, JavaType.VOID_PRIMITIVE, bodyBuilder);
			methodBuilder.addAnnotation(new AnnotationMetadataBuilder(new JavaType("javax.annotation.PostConstruct")));

			return methodBuilder.build();
		}
		return null; //no converter to register
	}

	private MethodMetadata getDateTimeFormatHelperMethod() {
		JavaSymbolName addDateTimeFormatPatterns = new JavaSymbolName("addDateTimeFormatPatterns");

		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.springframework.ui.Model"), new ArrayList<AnnotationMetadata>()));
		
		MethodMetadata addDateTimeFormatPatternsMethod = methodExists(addDateTimeFormatPatterns, paramTypes);
		if (addDateTimeFormatPatternsMethod != null) return addDateTimeFormatPatternsMethod;

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("model"));

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		Iterator<Map.Entry<JavaSymbolName, DateTimeFormat>> it = dateTypes.entrySet().iterator();
		while (it.hasNext()) {
			Entry<JavaSymbolName, DateTimeFormat> entry = it.next();
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
			bodyBuilder.appendFormalLine("model.addAttribute(\"" + entityName + "_" + entry.getKey().getSymbolName().toLowerCase() + "_date_format\", " + pattern + ");");
		}

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), 0, addDateTimeFormatPatterns, JavaType.VOID_PRIMITIVE, paramTypes, paramNames, bodyBuilder);
		return methodBuilder.build();
	}

	private List<MethodMetadata> getPopulateMethods() {
		List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
		for (JavaType type : specialDomainTypes) {
			// There is a need to present a populator for self references (see ROO-1112)
			// if (type.equals(beanInfoMetadata.getJavaBean())) {
			// continue;
			// }

			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

			EntityMetadata typeEntityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(type, Path.SRC_MAIN_JAVA));
			if (typeEntityMetadata != null) {
				bodyBuilder.appendFormalLine("return " + type.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + "." + typeEntityMetadata.getFindAllMethod().getMethodName() + "();");
			} else if (isEnumType(type)) {
				JavaType arrays = new JavaType("java.util.Arrays");
				bodyBuilder.appendFormalLine("return " + arrays.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".asList(" + type.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + ".class.getEnumConstants());");
			} else if (isRooIdentifier(type)) {
				continue;
			} else {
				throw new IllegalStateException("Could not scaffold controller for type " + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + ", the referenced type " + type.getFullyQualifiedTypeName() + " cannot be handled");
			}

			JavaSymbolName populateMethodName = new JavaSymbolName("populate" + getPlural(type));
			MethodMetadata addReferenceDataMethod = methodExists(populateMethodName, new ArrayList<AnnotatedJavaType>());
			if (addReferenceDataMethod != null) continue;

			List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
			List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
			attributes.add(new StringAttributeValue(new JavaSymbolName("value"), getPlural(type).toLowerCase()));
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
		paramNames.add(new JavaSymbolName("request"));
		
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("String enc = request.getCharacterEncoding();");
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

	private SortedSet<JavaType> getSpecialDomainTypes(JavaType javaType) {
		SortedSet<JavaType> specialTypes = new TreeSet<JavaType>();
		BeanInfoMetadata bim = (BeanInfoMetadata) metadataService.get(BeanInfoMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA));
		if (bim == null) {
			// Unable to get metadata so it is not a JavaType in our project anyway.
			return specialTypes;
		}
		EntityMetadata em = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA));
		if (em == null) {
			// Unable to get entity metadata so it is not a Entity in our project anyway.
			return specialTypes;
		}
		for (MethodMetadata accessor : bim.getPublicAccessors(false)) {
			// Not interested in identifiers and version fields
			if (accessor.equals(em.getIdentifierAccessor()) || accessor.equals(em.getVersionAccessor())) {
				continue;
			}
			// Not interested in fields that are not exposed via a mutator
			FieldMetadata fieldMetadata = bim.getFieldForPropertyName(BeanInfoMetadata.getPropertyNameForJavaBeanMethod(accessor));
			if (fieldMetadata == null || !hasMutator(fieldMetadata, bim)) {
				continue;
			}
			JavaType type = accessor.getReturnType();
			if (type.isCommonCollectionType()) {
				for (JavaType genericType : type.getParameters()) {
					if (isSpecialType(genericType)) {
						specialTypes.add(genericType);
					}
				}
			} else {
				if (isSpecialType(type) && !isEmbeddedFieldType(fieldMetadata)) {
					specialTypes.add(type);
				}
			}
		}
		return specialTypes;
	}

	private Map<JavaSymbolName, DateTimeFormat> getDatePatterns() {
		Map<JavaSymbolName, DateTimeFormat> dates = new HashMap<JavaSymbolName, DateTimeFormat>();
		for (MethodMetadata accessor : beanInfoMetadata.getPublicAccessors(false)) {
			// Not interested in identifiers and version fields
			if (accessor.equals(entityMetadata.getIdentifierAccessor()) || accessor.equals(entityMetadata.getVersionAccessor())) {
				continue;
			}
			// Not interested in fields that are not exposed via a mutator
			FieldMetadata fieldMetadata = beanInfoMetadata.getFieldForPropertyName(BeanInfoMetadata.getPropertyNameForJavaBeanMethod(accessor));
			if (fieldMetadata == null || !hasMutator(fieldMetadata, beanInfoMetadata)) {
				continue;
			}
			JavaType type = accessor.getReturnType();

			if (type.getFullyQualifiedTypeName().equals(Date.class.getName()) || type.getFullyQualifiedTypeName().equals(Calendar.class.getName())) {
				AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(fieldMetadata.getAnnotations(), new JavaType("org.springframework.format.annotation.DateTimeFormat"));
				JavaSymbolName patternSymbol = new JavaSymbolName("pattern");
				JavaSymbolName styleSymbol = new JavaSymbolName("style");
				DateTimeFormat dateTimeFormat = null;
				if (annotation != null) {
					if (annotation.getAttributeNames().contains(styleSymbol)) {
						dateTimeFormat = DateTimeFormat.withStyle(annotation.getAttribute(styleSymbol).getValue().toString());
					} else if (annotation.getAttributeNames().contains(patternSymbol)) {
						dateTimeFormat = DateTimeFormat.withPattern(annotation.getAttribute(patternSymbol).getValue().toString());
					}
				}
				if (dateTimeFormat != null) {
					dates.put(fieldMetadata.getFieldName(), dateTimeFormat);
					for (String finder : entityMetadata.getDynamicFinders()) {
						if (finder.contains(StringUtils.capitalize(fieldMetadata.getFieldName().getSymbolName()) + "Between")) {
							dates.put(new JavaSymbolName("min" + StringUtils.capitalize(fieldMetadata.getFieldName().getSymbolName())), dateTimeFormat);
							dates.put(new JavaSymbolName("max" + StringUtils.capitalize(fieldMetadata.getFieldName().getSymbolName())), dateTimeFormat);
						}
					}
				} else {
					log.warning("It is recommended to use @DateTimeFormat(style=\"S-\") on " + beanInfoMetadata.getJavaBean().getSimpleTypeName() + "." + fieldMetadata.getFieldName() + " to use automatic date conversion in Spring MVC");
				}
			}
		}
		return dates;
	}
	
	private MethodMetadata methodExists(JavaSymbolName methodName, List<AnnotatedJavaType> parameters) {
		return MemberFindingUtils.getMethod(detailsScanner.getMemberDetails(getClass().getName(), governorTypeDetails), methodName, AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameters));
	}
	
	private String getShortName(JavaType type) {
		return type.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
	}

	private boolean isEnumType(JavaType type) {
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifierNamingUtils.createIdentifier(PhysicalTypeIdentifier.class.getName(), type, Path.SRC_MAIN_JAVA));
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

	private boolean isRooIdentifier(JavaType type) {
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(type, Path.SRC_MAIN_JAVA));
		if (physicalTypeMetadata == null) {
			return false;
		}
		ClassOrInterfaceTypeDetails cid = (ClassOrInterfaceTypeDetails) physicalTypeMetadata.getPhysicalTypeDetails();
		if (cid == null) {
			return false;
		}
		return null != MemberFindingUtils.getAnnotationOfType(cid.getAnnotations(), new JavaType(RooIdentifier.class.getName()));
	}

	private boolean hasMutator(FieldMetadata fieldMetadata, BeanInfoMetadata bim) {
		for (MethodMetadata mutator : bim.getPublicMutators()) {
			if (fieldMetadata.equals(bim.getFieldForPropertyName(BeanInfoMetadata.getPropertyNameForJavaBeanMethod(mutator)))) return true;
		}
		return false;
	}

	private boolean isSpecialType(JavaType javaType) {
		// We are only interested if the type is part of our application
		if (metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA)) != null) {
			return true;
		}
		return false;
	}

	private boolean isEmbeddedFieldType(FieldMetadata field) {
		return MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.Embedded")) != null;
	}

	private String getPlural(JavaType type) {
		if (pluralCache.get(type) != null) {
			return pluralCache.get(type);
		}
		PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(PluralMetadata.createIdentifier(type, Path.SRC_MAIN_JAVA));
		Assert.notNull(pluralMetadata, "Could not determine the plural for the '" + type.getFullyQualifiedTypeName() + "' type");
		if (!pluralMetadata.getPlural().equals(type.getSimpleTypeName())) {
			pluralCache.put(type, pluralMetadata.getPlural());
			return pluralMetadata.getPlural();
		}
		pluralCache.put(type, pluralMetadata.getPlural() + "Items");
		return pluralMetadata.getPlural() + "Items";
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
	
	private static class DateTimeFormat {
		public String style;
		public String pattern;

		public static DateTimeFormat withStyle(String style) {
			DateTimeFormat d = new DateTimeFormat();
			d.style = style;
			return d;
		}

		public static DateTimeFormat withPattern(String pattern) {
			DateTimeFormat d = new DateTimeFormat();
			d.pattern = pattern;
			return d;
		}
		
	}
	
	private String uncapitalize(String term) {
		// [ROO-1790] this is needed to adhere to the JavaBean naming conventions (see JavaBean spec section 8.8)
		return Introspector.decapitalize(StringUtils.capitalize(term));
	}
}