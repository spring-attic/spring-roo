package org.springframework.roo.addon.jsf;

import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;
import static org.springframework.roo.addon.jsf.JsfJavaType.CONVERTER;
import static org.springframework.roo.addon.jsf.JsfJavaType.DATE_TIME_CONVERTER;
import static org.springframework.roo.addon.jsf.JsfJavaType.DISPLAY_CREATE_DIALOG;
import static org.springframework.roo.addon.jsf.JsfJavaType.DISPLAY_LIST;
import static org.springframework.roo.addon.jsf.JsfJavaType.EL_CONTEXT;
import static org.springframework.roo.addon.jsf.JsfJavaType.EXPRESSION_FACTORY;
import static org.springframework.roo.addon.jsf.JsfJavaType.FACES_CONTEXT;
import static org.springframework.roo.addon.jsf.JsfJavaType.FACES_MESSAGE;
import static org.springframework.roo.addon.jsf.JsfJavaType.HTML_OUTPUT_TEXT;
import static org.springframework.roo.addon.jsf.JsfJavaType.HTML_PANEL_GRID;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_AUTO_COMPLETE;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_CALENDAR;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_CLOSE_EVENT;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_FILE_UPLOAD;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_FILE_UPLOAD_EVENT;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_INPUT_TEXT;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_UPLOADED_FILE;
import static org.springframework.roo.addon.jsf.JsfJavaType.REQUEST_SCOPED;
import static org.springframework.roo.addon.jsf.JsfJavaType.SESSION_SCOPED;
import static org.springframework.roo.addon.jsf.JsfJavaType.UI_COMPONENT;
import static org.springframework.roo.addon.jsf.JsfJavaType.VIEW_SCOPED;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.MERGE_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.PERSIST_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.REMOVE_METHOD;
import static org.springframework.roo.model.RooJavaType.ROO_UPLOADED_FILE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooJsfManagedBean}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public class JsfManagedBeanMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	
	// Constants
	static final String CONVERTER_FIELD_CUSTOM_DATA_KEY = "converterField";
	private static final String LIST = "java.util.List";
	private static final String PROVIDES_TYPE_STRING = JsfManagedBeanMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private static final String CREATE_DIALOG_VISIBLE = "createDialogVisible";
	
	// Fields
	private JavaType entity;
	private Map<FieldMetadata, MethodMetadata> locatedFieldsAndAccessors;
	private Iterable<JavaType> enumTypes;
	private final Set<FieldMetadata> autoCompleteFields = new LinkedHashSet<FieldMetadata>();
	private String plural;

	private enum Action {
		CREATE, EDIT, VIEW;
	};

	/**
	 * Constructor
	 *
	 * @param identifier
	 * @param aspectName
	 * @param governorPhysicalTypeMetadata
	 * @param annotationValues
	 * @param plural
	 * @param crudAdditions the additions this metadata should make in order to
	 * invoke the target entity type's CRUD methods (required)
	 * @param locatedFieldsAndAccessors
	 * @param enumTypes
	 * @param identifierAccessor the entity id's accessor (getter) method (can be <code>null</code>)
	 */
	public JsfManagedBeanMetadata(final String identifier, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final JsfManagedBeanAnnotationValues annotationValues, final String plural, final Map<MethodMetadataCustomDataKey, MemberTypeAdditions> crudAdditions, final Map<FieldMetadata, MethodMetadata> locatedFieldsAndAccessors, final Iterable<JavaType> enumTypes, final MethodMetadata identifierAccessor) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' is invalid");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.isTrue(StringUtils.hasText(plural), "Plural required");
		Assert.notNull(crudAdditions, "Crud additions map required");
		Assert.notNull(locatedFieldsAndAccessors, "Located fields and accessors map required");
		Assert.notNull(enumTypes, "Enumerated types required");
		
		if (!isValid()) {
			return;
		}
		
		this.entity = annotationValues.getEntity();
		this.plural = plural;
		this.locatedFieldsAndAccessors = locatedFieldsAndAccessors;
		this.enumTypes = enumTypes;
		
		final MemberTypeAdditions findAllMethod = crudAdditions.get(FIND_ALL_METHOD);
		final MemberTypeAdditions mergeMethod = crudAdditions.get(MERGE_METHOD);
		final MemberTypeAdditions persistMethod = crudAdditions.get(PERSIST_METHOD);
		final MemberTypeAdditions removeMethod = crudAdditions.get(REMOVE_METHOD);
		if (identifierAccessor == null || findAllMethod == null || mergeMethod == null || persistMethod == null || removeMethod == null) {
			valid = false;
			return;
		}
		final Set<FieldMetadata> rooUploadedFileFields = getRooUploadedFileFields();
		final List<MethodMetadata> converterMethods = getConverterMethods();

		// Add @ManagedBean annotation if required
		builder.addAnnotation(getManagedBeanAnnotation());

		// Add @SessionScoped annotation if required
		builder.addAnnotation(getScopeAnnotation());

		// Add fields
		builder.addField(getNameField());
		builder.addField(getSelectedEntityField());
		builder.addField(getAllEntitiesField());
		builder.addField(getColumnsField());
		builder.addField(getPanelGridField(Action.CREATE));
		builder.addField(getPanelGridField(Action.EDIT));
		builder.addField(getPanelGridField(Action.VIEW));
		builder.addField(getBooleanField(new JavaSymbolName(CREATE_DIALOG_VISIBLE)));
		
		for (final FieldMetadata rooUploadedFileField : rooUploadedFileFields) {
			builder.addField(getUploadedFileField(rooUploadedFileField));
		}

		// Add methods
		builder.addMethod(getInitMethod(converterMethods, findAllMethod));
		builder.addMethod(getNameAccessorMethod());
		builder.addMethod(getColumnsAccessorMethod());
		builder.addMethod(getSelectedEntityAccessorMethod());
		builder.addMethod(getSelectedEntityMutatorMethod());
		builder.addMethod(getAllEntitiesAccessorMethod());
		builder.addMethod(getAllEntitiesMutatorMethod());
		builder.addMethod(getFindAllEntitiesMethod(findAllMethod));
		builder.addMethod(getPanelGridAccessorMethod(Action.CREATE));
		builder.addMethod(getPanelGridMutatorMethod(Action.CREATE));
		builder.addMethod(getPanelGridAccessorMethod(Action.EDIT));
		builder.addMethod(getPanelGridMutatorMethod(Action.EDIT));
		builder.addMethod(getPanelGridAccessorMethod(Action.VIEW));
		builder.addMethod(getPanelGridMutatorMethod(Action.VIEW));
		builder.addMethod(getPopulatePanelMethod(Action.CREATE));
		builder.addMethod(getPopulatePanelMethod(Action.EDIT)); 
		builder.addMethod(getPopulatePanelMethod(Action.VIEW)); 
	
		for (final FieldMetadata rooUploadedFileField : rooUploadedFileFields) {
			builder.addMethod(getFileUploadListenerMethod(rooUploadedFileField));
			builder.addMethod(getUploadedFileAccessorMethod(rooUploadedFileField));
			builder.addMethod(getUploadedFileMutatorMethod(rooUploadedFileField));
		}
		
		for (final FieldMetadata autoCompleteField : autoCompleteFields) {
			builder.addMethod(getEnumAutoCompleteMethod(autoCompleteField));
		}

		builder.addMethod(getBooleanAccessorMethod(CREATE_DIALOG_VISIBLE));
		builder.addMethod(getBooleanMutatorMethod(CREATE_DIALOG_VISIBLE));
		builder.addMethod(getDisplayListMethod());
		builder.addMethod(getDisplayCreateDialogMethod());
		builder.addMethod(getPersistMethod(mergeMethod, persistMethod, identifierAccessor));
		builder.addMethod(getDeleteMethod(removeMethod));
		builder.addMethod(getResetMethod());
		builder.addMethod(getHandleDialogCloseMethod());
		builder.addInnerType(getConverterInnerType(converterMethods, findAllMethod));

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	private AnnotationMetadata getManagedBeanAnnotation() {
		return getTypeAnnotation(new JavaType("javax.faces.bean.ManagedBean"));
	}

	private AnnotationMetadata getScopeAnnotation() {
		if (hasScopeAnnotation()) { 
			return null;
		}
		final AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(SESSION_SCOPED);
		return annotationBuilder.build();
	}
	
	private boolean hasScopeAnnotation() {
		return (MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, SESSION_SCOPED) != null 
			|| MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, VIEW_SCOPED) != null 
			|| MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, REQUEST_SCOPED) != null);
	}
	
	private FieldMetadata getSelectedEntityField() {
		final JavaSymbolName fieldName = new JavaSymbolName(getEntityName());
		final FieldMetadata field = MemberFindingUtils.getField(governorTypeDetails, fieldName);
		if (field != null) return field;
		
		final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), PRIVATE, new ArrayList<AnnotationMetadataBuilder>(), fieldName, entity);
		return fieldBuilder.build();
	}

	private FieldMetadata getAllEntitiesField() {
		final JavaSymbolName fieldName = new JavaSymbolName("all" + plural);
		final FieldMetadata field = MemberFindingUtils.getField(governorTypeDetails, fieldName);
		if (field != null) return field;

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(new JavaType(LIST));

		final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), PRIVATE, new ArrayList<AnnotationMetadataBuilder>(), fieldName, getEntityListType());
		return fieldBuilder.build();
	}

	private JavaType getEntityListType() {
		return new JavaType(LIST, 0, DataType.TYPE, null, Arrays.asList(entity));
	}
	
	private FieldMetadata getNameField() {
		final JavaSymbolName fieldName = new JavaSymbolName("name");
		final FieldMetadata field = MemberFindingUtils.getField(governorTypeDetails, fieldName);
		if (field != null) return field;

		final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), PRIVATE, fieldName, JavaType.STRING, "\"" + plural + "\"");
		return fieldBuilder.build();
	}

	private FieldMetadata getColumnsField() {
		final JavaSymbolName fieldName = new JavaSymbolName("columns");
		final FieldMetadata field = MemberFindingUtils.getField(governorTypeDetails, fieldName);
		if (field != null) return field;

		final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), PRIVATE, new ArrayList<AnnotationMetadataBuilder>(), fieldName, getColumnsListType());
		return fieldBuilder.build();
	}

	private JavaType getColumnsListType() {
		final List<JavaType> parameterTypes = new ArrayList<JavaType>();
		parameterTypes.add(JavaType.STRING);
		return new JavaType(LIST, 0, DataType.TYPE, null, parameterTypes);
	}

	private FieldMetadata getPanelGridField(final Action panelType) {
		final JavaSymbolName fieldName = new JavaSymbolName(StringUtils.toLowerCase(panelType.name()) + "PanelGrid");
		final FieldMetadata field = MemberFindingUtils.getField(governorTypeDetails, fieldName);
		if (field != null) return field;

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(HTML_PANEL_GRID);

		final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), PRIVATE, new ArrayList<AnnotationMetadataBuilder>(), fieldName, HTML_PANEL_GRID);
		return fieldBuilder.build();
	}
	
	private FieldMetadata getUploadedFileField(final FieldMetadata rooUploadedFileField) {
		final JavaSymbolName fieldName = rooUploadedFileField.getFieldName();
		final FieldMetadata field = MemberFindingUtils.getField(governorTypeDetails, fieldName);
		if (field != null) return field;
		
		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(PRIMEFACES_UPLOADED_FILE);
		
		final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), PRIVATE, new ArrayList<AnnotationMetadataBuilder>(), fieldName, PRIMEFACES_UPLOADED_FILE);
		return fieldBuilder.build();
	}

	private MethodMetadata getInitMethod(final List<MethodMetadata> converterMethods, final MemberTypeAdditions findAllAdditions) {
		final JavaSymbolName methodName = new JavaSymbolName("init");
		final MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(new JavaType("java.util.ArrayList"));

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("all" + plural + " = " + findAllAdditions.getMethodCall() + ";");
		findAllAdditions.copyAdditionsTo(builder, governorTypeDetails);
		bodyBuilder.appendFormalLine("columns = new ArrayList<String>();");
		for (final MethodMetadata converterMethod : converterMethods) {
			final String fieldName = StringUtils.uncapitalize(BeanInfoUtils.getPropertyNameForJavaBeanMethod(converterMethod).getSymbolName());
			bodyBuilder.appendFormalLine("columns.add(\"" + fieldName + "\");");
		}
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.VOID_PRIMITIVE, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		methodBuilder.addAnnotation(new AnnotationMetadataBuilder(new JavaType("javax.annotation.PostConstruct")));
		return methodBuilder.build();
	}
	
	private MethodMetadata getColumnsAccessorMethod() {
		final JavaSymbolName methodName = new JavaSymbolName("getColumns");
		final MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return columns;");

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, getColumnsListType(), new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getSelectedEntityAccessorMethod() {
		final String fieldName = getEntityName();
		final JavaSymbolName methodName = new JavaSymbolName("get" + StringUtils.capitalize(fieldName));
		final MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (" + fieldName + " == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(fieldName + " = new " + entity.getSimpleTypeName() + "();");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return " + fieldName + ";");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, entity, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getSelectedEntityMutatorMethod() {
		final String fieldName = getEntityName();
		final JavaSymbolName methodName = new JavaSymbolName("set" + StringUtils.capitalize(fieldName));
		final List<JavaType> parameterTypes = Arrays.asList(entity);
		final MethodMetadata method = methodExists(methodName, parameterTypes);
		if (method != null) return method;

		final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
		parameterNames.add(new JavaSymbolName(fieldName));
		
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("this." + fieldName + " = " + fieldName + ";");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getAllEntitiesAccessorMethod() {
		final JavaSymbolName methodName = new JavaSymbolName("getAll" + plural);
		final MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		final JavaSymbolName fieldName = new JavaSymbolName("all" + plural);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return " + fieldName + ";");

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, getEntityListType(), new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getAllEntitiesMutatorMethod() {
		final JavaSymbolName methodName = new JavaSymbolName("setAll" + plural);
		final List<JavaType> parameterTypes = new ArrayList<JavaType>();
		parameterTypes.add(new JavaType(LIST, 0, DataType.TYPE, null, Arrays.asList(entity)));
		final MethodMetadata method = methodExists(methodName, parameterTypes);
		if (method != null) return method;

		final JavaSymbolName fieldName = new JavaSymbolName("all" + plural);
		final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
		parameterNames.add(fieldName);

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(new JavaType(LIST));

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("this." + fieldName + " = " + fieldName + ";");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getFindAllEntitiesMethod(final MemberTypeAdditions findAllMethod) {
		final JavaSymbolName methodName = new JavaSymbolName("findAll" + plural);
		final MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		final JavaSymbolName fieldName = new JavaSymbolName("all" + plural);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(fieldName + " = " + findAllMethod.getMethodCall() + ";");
		findAllMethod.copyAdditionsTo(builder, governorTypeDetails);
		bodyBuilder.appendFormalLine("return null;");

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.STRING, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getNameAccessorMethod() {
		final JavaSymbolName methodName = new JavaSymbolName("getName");
		final MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return name;");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.STRING, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getPanelGridAccessorMethod(final Action action) {
		final String fieldName = StringUtils.toLowerCase(action.name()) + "PanelGrid";
		final JavaSymbolName methodName = new JavaSymbolName("get" + StringUtils.capitalize(fieldName));
		final MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(HTML_PANEL_GRID);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (" + fieldName + " == null) {");
		bodyBuilder.indent();
		switch (action) {
			case CREATE:
				bodyBuilder.appendFormalLine(fieldName + " = populateCreatePanel();");
				break;
			case EDIT:
				bodyBuilder.appendFormalLine(fieldName + " = populateEditPanel();");
				break;
			default:
				bodyBuilder.appendFormalLine(fieldName + " = populateViewPanel();");
				break;
		}
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return " + fieldName + ";");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, HTML_PANEL_GRID, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getPanelGridMutatorMethod(final Action action) {
		final String fieldName = StringUtils.toLowerCase(action.name()) + "PanelGrid";
		final JavaSymbolName methodName = new JavaSymbolName("set" + StringUtils.capitalize(fieldName));
		final List<JavaType> parameterTypes = Arrays.asList(HTML_PANEL_GRID);
		final MethodMetadata method = methodExists(methodName, parameterTypes);
		if (method != null) return method;

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(HTML_PANEL_GRID);
		
		final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
		parameterNames.add(new JavaSymbolName(fieldName));

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("this." + fieldName + " = " + fieldName + ";");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getPopulatePanelMethod(final Action action) {
		JavaSymbolName methodName;
		String fieldSuffix1;
		String fieldSuffix2;
		switch (action) {
			case CREATE:
				fieldSuffix1 = "CreateOutput";
				fieldSuffix2 = "CreateInput";
				methodName = new JavaSymbolName("populateCreatePanel");
				break;
			case EDIT:
				fieldSuffix1 = "EditOutput";
				fieldSuffix2 = "EditInput";
				methodName = new JavaSymbolName("populateEditPanel");
				break;
			default:
				fieldSuffix1 = "Label";
				fieldSuffix2 = "Value";
				methodName = new JavaSymbolName("populateViewPanel");
				break;
		}
		final MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;
		
		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(EL_CONTEXT);
		imports.addImport(EXPRESSION_FACTORY);
		imports.addImport(FACES_CONTEXT);
		imports.addImport(HTML_PANEL_GRID);
		imports.addImport(HTML_OUTPUT_TEXT);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("FacesContext facesContext = FacesContext.getCurrentInstance();");
		bodyBuilder.appendFormalLine("ExpressionFactory expressionFactory = facesContext.getApplication().getExpressionFactory();");
		bodyBuilder.appendFormalLine("ELContext elContext = facesContext.getELContext();");
		bodyBuilder.appendFormalLine("");
		imports.addImport(HTML_PANEL_GRID);
		imports.addImport(HTML_OUTPUT_TEXT);

		bodyBuilder.appendFormalLine("HtmlPanelGrid htmlPanelGrid = " + getComponentCreationStr("HtmlPanelGrid"));
		bodyBuilder.appendFormalLine("");

		for (final FieldMetadata field : locatedFieldsAndAccessors.keySet()) {
			final JavaType fieldType = field.getFieldType();
			final String fieldName = field.getFieldName().getSymbolName();
			final String fieldLabelVar = fieldName + fieldSuffix1;
			final String fieldValueVar = fieldName + fieldSuffix2;
			final String converterName = fieldValueVar + "Converter";

			bodyBuilder.appendFormalLine("HtmlOutputText " + fieldLabelVar + " = " + getComponentCreationStr("HtmlOutputText"));
			bodyBuilder.appendFormalLine(fieldLabelVar + ".setId(\"" + fieldLabelVar + "\");");
			bodyBuilder.appendFormalLine(fieldLabelVar + ".setValue(\"" + fieldName + ": \");");
			bodyBuilder.appendFormalLine(fieldLabelVar + ".setStyle(\"font-weight:bold\");");
			bodyBuilder.appendFormalLine("htmlPanelGrid.getChildren().add(" + fieldLabelVar + ");");
			bodyBuilder.appendFormalLine("");
			
			if (isRooUploadFileField(field)) {
				imports.addImport(PRIMEFACES_FILE_UPLOAD);
				imports.addImport(PRIMEFACES_FILE_UPLOAD_EVENT);
				imports.addImport(PRIMEFACES_UPLOADED_FILE);
				final JavaSymbolName fileUploadMethodName = getFileUploadMethodName(field.getFieldName());
				bodyBuilder.appendFormalLine("FileUpload " + fieldValueVar + " = " + getComponentCreationStr("FileUpload"));
				bodyBuilder.appendFormalLine(fieldValueVar + ".setFileUploadListener(expressionFactory.createMethodExpression(elContext, \"#{" + getEntityName() + "Bean." + fileUploadMethodName + "}\", void.class, new Class[] { FileUploadEvent.class }));");
				bodyBuilder.appendFormalLine(fieldValueVar + ".setMode(\"advanced\");");
				bodyBuilder.appendFormalLine(fieldValueVar + ".setUpdate(\"messages\");");
			} else if (isEnum(field)) {
				if (action == Action.VIEW) {
					bodyBuilder.appendFormalLine("HtmlOutputText " + fieldValueVar + " = " + getComponentCreationStr("HtmlOutputText"));
					bodyBuilder.appendFormalLine(getValueExpressionStr(fieldValueVar, fieldName, "String"));
				} else {
					imports.addImport(PRIMEFACES_AUTO_COMPLETE);
					imports.addImport(fieldType);
					bodyBuilder.appendFormalLine("AutoComplete " + fieldValueVar + " = " + getComponentCreationStr("AutoComplete"));
					bodyBuilder.appendFormalLine(getValueExpressionStr(fieldValueVar, fieldName, fieldType.getSimpleTypeName()));
					bodyBuilder.appendFormalLine(fieldValueVar + ".setCompleteMethod(expressionFactory.createMethodExpression(elContext, \"#{" + getEntityName() + "Bean.complete" + StringUtils.capitalize(fieldName) + "}\", List.class, new Class[] { String.class }));");
					autoCompleteFields.add(field);
				}
			} else if (isDateField(fieldType)) {
				if (action == Action.VIEW) {
					imports.addImport(DATE_TIME_CONVERTER);
					bodyBuilder.appendFormalLine("HtmlOutputText " + fieldValueVar + " = " + getComponentCreationStr("HtmlOutputText"));
					bodyBuilder.appendFormalLine(getValueExpressionStr(fieldValueVar, fieldName, "String"));
					bodyBuilder.appendFormalLine("DateTimeConverter " + converterName + " = new DateTimeConverter();");
					bodyBuilder.appendFormalLine(converterName + ".setPattern(\"dd/MM/yyyy\");");
					bodyBuilder.appendFormalLine(fieldValueVar + ".setConverter(" + converterName + ");");
				} else {
					imports.addImport(PRIMEFACES_CALENDAR);
					imports.addImport(new JavaType("java.util.Date"));
					bodyBuilder.appendFormalLine("Calendar " + fieldValueVar + " = " + getComponentCreationStr("Calendar"));
					bodyBuilder.appendFormalLine(getValueExpressionStr(fieldValueVar, fieldName, "Date"));
					bodyBuilder.appendFormalLine(fieldValueVar + ".setNavigator(true);");
					bodyBuilder.appendFormalLine(fieldValueVar + ".setEffect(\"slideDown\");");
					bodyBuilder.appendFormalLine(fieldValueVar + ".setPattern(\"dd/MM/yyyy\");");
				}
			} else {
				imports.addImport(PRIMEFACES_INPUT_TEXT);
				if (action == Action.VIEW) {
					bodyBuilder.appendFormalLine("HtmlOutputText " + fieldValueVar + " = " + getComponentCreationStr("HtmlOutputText"));
				} else {
					bodyBuilder.appendFormalLine("InputText " + fieldValueVar + " = " + getComponentCreationStr("InputText"));
				}
				bodyBuilder.appendFormalLine(getValueExpressionStr(fieldValueVar, fieldName, "String"));
			}
			bodyBuilder.appendFormalLine(fieldValueVar + ".setId(\"" + fieldValueVar + "\");");
			bodyBuilder.appendFormalLine("htmlPanelGrid.getChildren().add(" + fieldValueVar + ");");	
			bodyBuilder.appendFormalLine("");
		}
		
		bodyBuilder.appendFormalLine("return htmlPanelGrid;");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, HTML_PANEL_GRID, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}

	/**
	 * Indicates whether the given field contains an enum value
	 * 
	 * @param field the field to check (required)
	 * @return see above
	 */
	private boolean isEnum(final FieldMetadata field) {
		for (final JavaType enumType : enumTypes) {
			if (field.getFieldType().equals(enumType)) {
				return true;
			}
		}
		return false;
	}
	
	private MethodMetadata getFileUploadListenerMethod(final FieldMetadata rooUploadedFileField) {
		final JavaSymbolName methodName = getFileUploadMethodName(rooUploadedFileField.getFieldName());
		final List<JavaType> parameterTypes = Arrays.asList(PRIMEFACES_FILE_UPLOAD_EVENT);
		final MethodMetadata method = methodExists(methodName, parameterTypes);
		if (method != null) return method;

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(FACES_MESSAGE);
		imports.addImport(PRIMEFACES_FILE_UPLOAD_EVENT);

		final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
		parameterNames.add(new JavaSymbolName("event"));
		
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("set" + StringUtils.capitalize(rooUploadedFileField.getFieldName().getSymbolName()) + "(event.getFile());");
		bodyBuilder.appendFormalLine("FacesContext facesContext = FacesContext.getCurrentInstance();");
		bodyBuilder.appendFormalLine("FacesMessage msg = new FacesMessage(\"Successful\", event.getFile().getFileName() + \" is uploaded.\");"); 
		bodyBuilder.appendFormalLine("facesContext.addMessage(null, msg);");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getUploadedFileAccessorMethod(final FieldMetadata rooUploadedFileField) {
		final JavaSymbolName methodName = new JavaSymbolName("get" + StringUtils.capitalize(rooUploadedFileField.getFieldName().getSymbolName()));
		final MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(PRIMEFACES_UPLOADED_FILE);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return " + rooUploadedFileField.getFieldName().getSymbolName() + ";");

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, PRIMEFACES_UPLOADED_FILE, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getUploadedFileMutatorMethod(final FieldMetadata rooUploadedFileField) {
		final JavaSymbolName methodName = new JavaSymbolName("set" + StringUtils.capitalize(rooUploadedFileField.getFieldName().getSymbolName()));
		final List<JavaType> parameterTypes = Arrays.asList(PRIMEFACES_UPLOADED_FILE);
		final MethodMetadata method = methodExists(methodName, parameterTypes);
		if (method != null) return method;

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(PRIMEFACES_UPLOADED_FILE);

		final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
		parameterNames.add(rooUploadedFileField.getFieldName());
		
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("this." + rooUploadedFileField.getFieldName().getSymbolName() + " = " + rooUploadedFileField.getFieldName().getSymbolName() + ";");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getEnumAutoCompleteMethod(final FieldMetadata autoCompleteField) {
		final JavaSymbolName methodName = new JavaSymbolName("complete" + StringUtils.capitalize(autoCompleteField.getFieldName().getSymbolName()));
		final List<JavaType> parameterTypes = Arrays.asList(JavaType.STRING);
		final MethodMetadata method = methodExists(methodName, parameterTypes);
		if (method != null) return method;

		final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
		parameterNames.add(new JavaSymbolName("query"));

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		final String simpleTypeName = autoCompleteField.getFieldType().getSimpleTypeName();
		bodyBuilder.appendFormalLine("List<" + simpleTypeName + "> suggestions = new ArrayList<" + simpleTypeName + ">();");
		bodyBuilder.appendFormalLine("for (" + simpleTypeName + " " + StringUtils.uncapitalize(simpleTypeName) + " : " + simpleTypeName + ".values()) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("if (" + StringUtils.uncapitalize(simpleTypeName) + ".name().toLowerCase().startsWith(query.toLowerCase())) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("suggestions.add(" + StringUtils.uncapitalize(simpleTypeName) + ");");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return suggestions;");

		final JavaType returnType = new JavaType(LIST, 0, DataType.TYPE, null, Arrays.asList(autoCompleteField.getFieldType()));
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getDisplayCreateDialogMethod() {
		final JavaSymbolName methodName = new JavaSymbolName(DISPLAY_CREATE_DIALOG);
		final MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(CREATE_DIALOG_VISIBLE + " = true;");
		bodyBuilder.appendFormalLine("return \"" + StringUtils.uncapitalize(entity.getSimpleTypeName()) + "\";");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.STRING, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getDisplayListMethod() {
		final JavaSymbolName methodName = new JavaSymbolName(DISPLAY_LIST);
		final MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(CREATE_DIALOG_VISIBLE + " = false;");
		bodyBuilder.appendFormalLine("return \"" + StringUtils.uncapitalize(entity.getSimpleTypeName()) + "\";");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.STRING, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getPersistMethod(final MemberTypeAdditions mergeMethod, final MemberTypeAdditions persistMethod, final MethodMetadata identifierAccessor) {
		final JavaSymbolName methodName = new JavaSymbolName("persist");
		final MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(FACES_MESSAGE);

		final String fieldName = getEntityName();
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("String message = \"\";");
		bodyBuilder.appendFormalLine("if (" + fieldName + "." +  identifierAccessor.getMethodName().getSymbolName() + "() != null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(mergeMethod.getMethodCall() + ";");
		mergeMethod.copyAdditionsTo(builder, governorTypeDetails);
		bodyBuilder.appendFormalLine("message = \"Successfully updated\";");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("} else {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(persistMethod.getMethodCall() + ";");
		persistMethod.copyAdditionsTo(builder, governorTypeDetails);
		bodyBuilder.appendFormalLine("message = \"Successfully created\";");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("FacesMessage facesMessage = new FacesMessage(message);");
		bodyBuilder.appendFormalLine("FacesContext.getCurrentInstance().addMessage(null, facesMessage);");
		bodyBuilder.appendFormalLine("reset();");
		bodyBuilder.appendFormalLine("return findAll" + plural + "();");

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.STRING, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getDeleteMethod(final MemberTypeAdditions removeMethod) {
		final JavaSymbolName methodName = new JavaSymbolName("delete");
		final MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(removeMethod.getMethodCall() + ";");
		removeMethod.copyAdditionsTo(builder, governorTypeDetails);
		bodyBuilder.appendFormalLine("FacesMessage facesMessage = new FacesMessage(\"Successfully deleted\");");
		bodyBuilder.appendFormalLine("FacesContext.getCurrentInstance().addMessage(null, facesMessage);");
		bodyBuilder.appendFormalLine("reset();");
		bodyBuilder.appendFormalLine("return findAll" + plural + "();");

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.STRING, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getResetMethod() {
		final JavaSymbolName methodName = new JavaSymbolName("reset");
		final MethodMetadata method = methodExists(methodName,  new ArrayList<JavaType>());
		if (method != null) return method;

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(getEntityName() + " = null;");
		bodyBuilder.appendFormalLine(CREATE_DIALOG_VISIBLE + " = false;");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.VOID_PRIMITIVE, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getHandleDialogCloseMethod() {
		final JavaSymbolName methodName = new JavaSymbolName("handleDialogClose");
		final List<JavaType> parameterTypes = Arrays.asList(PRIMEFACES_CLOSE_EVENT);
		final MethodMetadata method = methodExists(methodName, parameterTypes);
		if (method != null) return method;

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(PRIMEFACES_CLOSE_EVENT);
		
		final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
		parameterNames.add(new JavaSymbolName("event"));

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("reset();");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	private String getEntityName() {
		return JavaSymbolName.getReservedWordSafeName(entity).getSymbolName();
	}
	
	private JavaSymbolName getFileUploadMethodName(final JavaSymbolName fieldName) {
		return new JavaSymbolName("handleFileUploadFor" + StringUtils.capitalize(fieldName.getSymbolName()));
	}

	private FieldMetadata getBooleanField(final JavaSymbolName fieldName) {
		final FieldMetadata field = MemberFindingUtils.getField(governorTypeDetails, fieldName);
		if (field != null) return field;
		
		final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), PRIVATE, fieldName, JavaType.BOOLEAN_PRIMITIVE, Boolean.FALSE.toString());
		return fieldBuilder.build();
	}

	private MethodMetadata getBooleanAccessorMethod(final String fieldName) {
		final JavaSymbolName methodName = new JavaSymbolName("is" + StringUtils.capitalize(fieldName));
		final MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return " + fieldName + ";");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.BOOLEAN_PRIMITIVE, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getBooleanMutatorMethod(final String fieldName) {
		final JavaSymbolName methodName = new JavaSymbolName("set" + StringUtils.capitalize(fieldName));
		final List<JavaType> parameterTypes = Arrays.asList(JavaType.BOOLEAN_PRIMITIVE);
		final MethodMetadata method = methodExists(methodName, parameterTypes);
		if (method != null) return method;

		final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
		parameterNames.add(new JavaSymbolName(fieldName));
		
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("this." + fieldName + " = " + fieldName + ";");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	private Set<FieldMetadata> getRooUploadedFileFields() {
		final Set<FieldMetadata> rooUploadedFileFields = new LinkedHashSet<FieldMetadata>();
		for (final FieldMetadata rooUploadedFileField : this.locatedFieldsAndAccessors.keySet()) {
			if (isRooUploadFileField(rooUploadedFileField)) {
				rooUploadedFileFields.add(rooUploadedFileField);
			}
		}
		return rooUploadedFileFields;
	}
	
	private boolean isRooUploadFileField(final FieldMetadata rooUploadedFileField) {
		for (final AnnotationMetadata annotation : rooUploadedFileField.getAnnotations()) {
			if (annotation.getAnnotationType().equals(ROO_UPLOADED_FILE)) {
				return true;
			}
		}
		return false;
	}
	
	private ClassOrInterfaceTypeDetails getConverterInnerType(final List<MethodMetadata> converterMethods, final MemberTypeAdditions findAllMethod) {
		final String simpleTypeName = entity.getSimpleTypeName();
		final JavaType innerType = new JavaType(simpleTypeName + "Converter");
		if (MemberFindingUtils.getDeclaredInnerType(governorTypeDetails, innerType) != null) {
			return null;
		}

		if (converterMethods.isEmpty()) {
			return null;
		}
		
		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(UI_COMPONENT);
		imports.addImport(CONVERTER);
		imports.addImport(FACES_CONTEXT);

		final List<JavaType> parameterTypes = new ArrayList<JavaType>();
		parameterTypes.add(FACES_CONTEXT);
		parameterTypes.add(UI_COMPONENT);

		final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
		parameterNames.add(new JavaSymbolName("context"));
		parameterNames.add(new JavaSymbolName("component"));
		parameterNames.add(new JavaSymbolName("value"));

		final String typeName = StringUtils.uncapitalize(simpleTypeName);
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

		// Create getAsObject method
		final List<JavaType> getAsObjectParameterTypes = new ArrayList<JavaType>(parameterTypes);
		getAsObjectParameterTypes.add(JavaType.STRING);
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(getEntityListType().getNameIncludingTypeParameters(false, imports) + " " + StringUtils.uncapitalize(plural) + " = " + findAllMethod.getMethodCall() + ";");
		findAllMethod.copyAdditionsTo(builder, governorTypeDetails);
		bodyBuilder.appendFormalLine("for (" + simpleTypeName + " " + typeName + " : " + StringUtils.uncapitalize(plural) + ") {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(new StringBuilder("String ").append(typeName).append("Str = ").append(getBuilderString(converterMethods)).toString());
		bodyBuilder.appendFormalLine("if (" + typeName +"Str.equals(value)) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return " + typeName + ";");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return null;");
		bodyBuilder.indentRemove();
		
		final MethodMetadataBuilder getAsObjectMethod = new MethodMetadataBuilder(getId(), PUBLIC, new JavaSymbolName("getAsObject"), new JavaType("java.lang.Object"), AnnotatedJavaType.convertFromJavaTypes(getAsObjectParameterTypes), parameterNames, bodyBuilder);
		
		// Create getAsString method
		final List<JavaType> getAsStringParameterTypes = new ArrayList<JavaType>(parameterTypes);
		getAsStringParameterTypes.add(new JavaType("java.lang.Object"));
		bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(simpleTypeName + " " + typeName + " = (" + simpleTypeName + ") value;" );
		bodyBuilder.appendFormalLine(new StringBuilder("return ").append(getBuilderString(converterMethods)).toString());
		bodyBuilder.indentRemove();
		
		final MethodMetadataBuilder getAsStringMethod = new MethodMetadataBuilder(getId(), PUBLIC, new JavaSymbolName("getAsString"), JavaType.STRING, AnnotatedJavaType.convertFromJavaTypes(getAsStringParameterTypes), parameterNames, bodyBuilder);

		final ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(getId(), PUBLIC | STATIC, innerType, PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.addImplementsType(CONVERTER);
		typeDetailsBuilder.addMethod(getAsObjectMethod);
		typeDetailsBuilder.addMethod(getAsStringMethod);

		return typeDetailsBuilder.build();
	}

	private MethodMetadata methodExists(final JavaSymbolName methodName, final List<JavaType> parameterTypes) {
		return MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName, parameterTypes);
	}
	
	private List<MethodMetadata> getConverterMethods() {
		final List<MethodMetadata> converterMethods = new LinkedList<MethodMetadata>();
		for (final FieldMetadata field : locatedFieldsAndAccessors.keySet()) {
			if (field.getCustomData() != null && field.getCustomData().keySet().contains(CONVERTER_FIELD_CUSTOM_DATA_KEY)) {
				converterMethods.add(locatedFieldsAndAccessors.get(field));
			}
		}
		return converterMethods;
	}

	/**
	 * TODO does this need to be called twice by the same method?
	 * 
	 * @param converterMethods
	 * @return
	 */
	private String getBuilderString(final List<MethodMetadata> converterMethods) {
		final StringBuilder sb = new StringBuilder("new StringBuilder()");
		for (int i = 0; i < converterMethods.size(); i++) {
			if (i > 0) {
				sb.append(".append(\" \")");
			}
			sb.append(".append(").append(StringUtils.uncapitalize(entity.getSimpleTypeName())).append(".").append(converterMethods.get(i).getMethodName().getSymbolName()).append("())");
		}
		sb.append(".toString();");
		return sb.toString();
	}
	
	private String getComponentCreationStr(final String componentName) {
		return new StringBuilder().append("(").append(componentName).append(") facesContext.getApplication().createComponent(").append(componentName).append(".COMPONENT_TYPE);").toString();
	}
	
	private String getValueExpressionStr(final String inputFieldVar, final String fieldName, final String className) {
		return inputFieldVar + ".setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{" + StringUtils.uncapitalize(entity.getSimpleTypeName()) + "Bean." + getEntityName() + "." + fieldName + "}\", " + className + ".class));";
	}

	private boolean isDateField(final JavaType fieldType) {
		return fieldType.equals(new JavaType("java.util.Date")) || fieldType.equals(new JavaType("java.util.Calendar")) || fieldType.equals(new JavaType("java.util.GregorianCalendar"));
	}

	public String toString() {
		final ToStringCreator tsc = new ToStringCreator(this);
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
	
	public static final String createIdentifier(final JavaType javaType, final Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static final JavaType getJavaType(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static final Path getPath(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
}
