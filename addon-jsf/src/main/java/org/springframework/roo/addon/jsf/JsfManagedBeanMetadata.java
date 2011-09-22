package org.springframework.roo.addon.jsf;

import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PUBLIC;
import static org.springframework.roo.addon.jsf.JsfJavaType.DATE_TIME_CONVERTER;
import static org.springframework.roo.addon.jsf.JsfJavaType.DISPLAY_CREATE_DIALOG;
import static org.springframework.roo.addon.jsf.JsfJavaType.DISPLAY_LIST;
import static org.springframework.roo.addon.jsf.JsfJavaType.DOUBLE_RANGE_VALIDATOR;
import static org.springframework.roo.addon.jsf.JsfJavaType.EL_CONTEXT;
import static org.springframework.roo.addon.jsf.JsfJavaType.EXPRESSION_FACTORY;
import static org.springframework.roo.addon.jsf.JsfJavaType.FACES_CONTEXT;
import static org.springframework.roo.addon.jsf.JsfJavaType.FACES_MESSAGE;
import static org.springframework.roo.addon.jsf.JsfJavaType.HTML_OUTPUT_TEXT;
import static org.springframework.roo.addon.jsf.JsfJavaType.HTML_PANEL_GRID;
import static org.springframework.roo.addon.jsf.JsfJavaType.LENGTH_VALIDATOR;
import static org.springframework.roo.addon.jsf.JsfJavaType.LONG_RANGE_VALIDATOR;
import static org.springframework.roo.addon.jsf.JsfJavaType.MANAGED_BEAN;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_AUTO_COMPLETE;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_CALENDAR;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_CLOSE_EVENT;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_FILE_UPLOAD;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_FILE_UPLOAD_EVENT;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_INPUT_TEXT;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_MESSAGE;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_REQUEST_CONTEXT;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_SLIDER;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_UPLOADED_FILE;
import static org.springframework.roo.addon.jsf.JsfJavaType.REQUEST_SCOPED;
import static org.springframework.roo.addon.jsf.JsfJavaType.SESSION_SCOPED;
import static org.springframework.roo.addon.jsf.JsfJavaType.VIEW_SCOPED;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.MERGE_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.PERSIST_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.REMOVE_METHOD;
import static org.springframework.roo.model.JavaType.STRING;
import static org.springframework.roo.model.JdkJavaType.ARRAY_LIST;
import static org.springframework.roo.model.JdkJavaType.DATE;
import static org.springframework.roo.model.JdkJavaType.LIST;
import static org.springframework.roo.model.JdkJavaType.POST_CONSTRUCT;
import static org.springframework.roo.model.Jsr303JavaType.DECIMAL_MAX;
import static org.springframework.roo.model.Jsr303JavaType.DECIMAL_MIN;
import static org.springframework.roo.model.Jsr303JavaType.MAX;
import static org.springframework.roo.model.Jsr303JavaType.MIN;
import static org.springframework.roo.model.Jsr303JavaType.NOT_NULL;
import static org.springframework.roo.model.Jsr303JavaType.SIZE;
import static org.springframework.roo.model.RooJavaType.ROO_UPLOADED_FILE;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.details.BeanInfoUtils;
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
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.NumberUtils;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooJsfManagedBean}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public class JsfManagedBeanMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	
	// Constants
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
		builder.addMethod(getInitMethod(findAllMethod));
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

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	private AnnotationMetadata getManagedBeanAnnotation() {
		return getTypeAnnotation(MANAGED_BEAN);
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
		imports.addImport(LIST);

		final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), PRIVATE, new ArrayList<AnnotationMetadataBuilder>(), fieldName, getEntityListType());
		return fieldBuilder.build();
	}

	private JavaType getEntityListType() {
		return new JavaType(LIST.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(entity));
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
		final List<JavaType> parameterTypes = Arrays.asList(JavaType.STRING);
		return new JavaType(LIST.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, parameterTypes);
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

	private MethodMetadata getInitMethod(final MemberTypeAdditions findAllAdditions) {
		final JavaSymbolName methodName = new JavaSymbolName("init");
		final MethodMetadata method = getGovernorMethod(methodName);
		if (method != null) return method;

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(ARRAY_LIST);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("all" + plural + " = " + findAllAdditions.getMethodCall() + ";");
		findAllAdditions.copyAdditionsTo(builder, governorTypeDetails);
		bodyBuilder.appendFormalLine("columns = new ArrayList<String>();");
		for (final MethodMetadata listViewMethod : getListViewMethods()) {
			final String fieldName = StringUtils.uncapitalize(BeanInfoUtils.getPropertyNameForJavaBeanMethod(listViewMethod).getSymbolName());
			bodyBuilder.appendFormalLine("columns.add(\"" + fieldName + "\");");
		}
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.VOID_PRIMITIVE, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		methodBuilder.addAnnotation(new AnnotationMetadataBuilder(POST_CONSTRUCT));
		return methodBuilder.build();
	}
	
	private MethodMetadata getColumnsAccessorMethod() {
		final JavaSymbolName methodName = new JavaSymbolName("getColumns");
		final MethodMetadata method = getGovernorMethod(methodName);
		if (method != null) return method;

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return columns;");

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, getColumnsListType(), new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getSelectedEntityAccessorMethod() {
		final String fieldName = getEntityName();
		final JavaSymbolName methodName = new JavaSymbolName("get" + StringUtils.capitalize(fieldName));
		final MethodMetadata method = getGovernorMethod(methodName);
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
		final JavaType parameterType = entity;
		final MethodMetadata method = getGovernorMethod(methodName, parameterType);
		if (method != null) return method;

		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName(fieldName));
		
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("this." + fieldName + " = " + fieldName + ";");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getAllEntitiesAccessorMethod() {
		final JavaSymbolName methodName = new JavaSymbolName("getAll" + plural);
		final MethodMetadata method = getGovernorMethod(methodName);
		if (method != null) return method;

		final JavaSymbolName fieldName = new JavaSymbolName("all" + plural);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return " + fieldName + ";");

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, getEntityListType(), new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getAllEntitiesMutatorMethod() {
		final JavaSymbolName methodName = new JavaSymbolName("setAll" + plural);
		final JavaType entityListParameter = new JavaType(LIST.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(entity));
		final MethodMetadata method = getGovernorMethod(methodName, entityListParameter);
		if (method != null) return method;

		final JavaSymbolName fieldName = new JavaSymbolName("all" + plural);
		final List<JavaSymbolName> parameterNames = Arrays.asList(fieldName);

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(LIST);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("this." + fieldName + " = " + fieldName + ";");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(entityListParameter), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getFindAllEntitiesMethod(final MemberTypeAdditions findAllMethod) {
		final JavaSymbolName methodName = new JavaSymbolName("findAll" + plural);
		final MethodMetadata method = getGovernorMethod(methodName);
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
		final MethodMetadata method = getGovernorMethod(methodName);
		if (method != null) return method;

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return name;");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.STRING, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getPanelGridAccessorMethod(final Action action) {
		final String fieldName = StringUtils.toLowerCase(action.name()) + "PanelGrid";
		final JavaSymbolName methodName = new JavaSymbolName("get" + StringUtils.capitalize(fieldName));
		final MethodMetadata method = getGovernorMethod(methodName);
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
		final JavaType parameterType = HTML_PANEL_GRID;
		final MethodMetadata method = getGovernorMethod(methodName, parameterType);
		if (method != null) return method;

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(HTML_PANEL_GRID);
		
		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName(fieldName));

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("this." + fieldName + " = " + fieldName + ";");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getPopulatePanelMethod(final Action action) {
		JavaSymbolName methodName;
		String suffix1;
		String suffix2;
		switch (action) {
			case CREATE:
				suffix1 = "CreateOutput";
				suffix2 = "CreateInput";
				methodName = new JavaSymbolName("populateCreatePanel");
				break;
			case EDIT:
				suffix1 = "EditOutput";
				suffix2 = "EditInput";
				methodName = new JavaSymbolName("populateEditPanel");
				break;
			default:
				suffix1 = "Label";
				suffix2 = "Value";
				methodName = new JavaSymbolName("populateViewPanel");
				break;
		}
		final MethodMetadata method = getGovernorMethod(methodName);
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
			final String fieldLabelId = fieldName + suffix1;
			final boolean nullable = isNullable(field);
			
			// Field label
			bodyBuilder.appendFormalLine("HtmlOutputText " + fieldLabelId + " = " + getComponentCreationStr("HtmlOutputText"));
			bodyBuilder.appendFormalLine(fieldLabelId + ".setId(\"" + fieldLabelId + "\");");
			bodyBuilder.appendFormalLine(fieldLabelId + ".setValue(\"" + fieldName + ":" + (nullable ? "   " : " * ") + "\");");
			// bodyBuilder.appendFormalLine(fieldLabelVar + ".setStyle(\"font-weight:bold\");");
			bodyBuilder.appendFormalLine(getAddToPanelText(fieldLabelId));
			bodyBuilder.appendFormalLine("");

			// Field value
			String fieldValueId = fieldName + suffix2;
			final String converterName = fieldValueId + "Converter";
			final String htmlOutputTextStr = "HtmlOutputText " + fieldValueId + " = " + getComponentCreationStr("HtmlOutputText");
			final String inputTextStr = "InputText " + fieldValueId + " = " + getComponentCreationStr("InputText");
			final String componentIdStr = fieldValueId + ".setId(\"" + fieldValueId + "\");";
			final String requiredStr = fieldValueId + ".setRequired(" + !nullable + ");";

			if (isRooUploadFileField(field)) {
				imports.addImport(PRIMEFACES_FILE_UPLOAD);
				imports.addImport(PRIMEFACES_FILE_UPLOAD_EVENT);
				imports.addImport(PRIMEFACES_UPLOADED_FILE);
				final JavaSymbolName fileUploadMethodName = getFileUploadMethodName(field.getFieldName());
				bodyBuilder.appendFormalLine("FileUpload " + fieldValueId + " = " + getComponentCreationStr("FileUpload"));
				bodyBuilder.appendFormalLine(componentIdStr);
				bodyBuilder.appendFormalLine(fieldValueId + ".setFileUploadListener(expressionFactory.createMethodExpression(elContext, \"#{" + getEntityName() + "Bean." + fileUploadMethodName + "}\", void.class, new Class[] { FileUploadEvent.class }));");
				bodyBuilder.appendFormalLine(fieldValueId + ".setMode(\"advanced\");");
				bodyBuilder.appendFormalLine(fieldValueId + ".setUpdate(\"messages\");");
			} else if (isEnum(field)) {
				if (action == Action.VIEW) {
					bodyBuilder.appendFormalLine(htmlOutputTextStr);
					bodyBuilder.appendFormalLine(getValueExpressionStr(fieldValueId, fieldName, "String"));
				} else {
					imports.addImport(PRIMEFACES_AUTO_COMPLETE);
					imports.addImport(fieldType);
					bodyBuilder.appendFormalLine("AutoComplete " + fieldValueId + " = " + getComponentCreationStr("AutoComplete"));
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getValueExpressionStr(fieldValueId, fieldName, fieldType.getSimpleTypeName()));
					bodyBuilder.appendFormalLine(fieldValueId + ".setCompleteMethod(expressionFactory.createMethodExpression(elContext, \"#{" + getEntityName() + "Bean.complete" + StringUtils.capitalize(fieldName) + "}\", List.class, new Class[] { String.class }));");
					bodyBuilder.appendFormalLine(requiredStr);
					autoCompleteFields.add(field);
				}
			} else if (JdkJavaType.isDateField(fieldType)) {
				if (action == Action.VIEW) {
					imports.addImport(DATE_TIME_CONVERTER);
					bodyBuilder.appendFormalLine(htmlOutputTextStr);
					bodyBuilder.appendFormalLine(getValueExpressionStr(fieldValueId, fieldName, "String"));
					bodyBuilder.appendFormalLine("DateTimeConverter " + converterName + " = new DateTimeConverter();");
					bodyBuilder.appendFormalLine(converterName + ".setPattern(\"dd/MM/yyyy\");");
					bodyBuilder.appendFormalLine(fieldValueId + ".setConverter(" + converterName + ");");
				} else {
					imports.addImport(PRIMEFACES_CALENDAR);
					imports.addImport(DATE);
					bodyBuilder.appendFormalLine("Calendar " + fieldValueId + " = " + getComponentCreationStr("Calendar"));
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getValueExpressionStr(fieldValueId, fieldName, "Date"));
					bodyBuilder.appendFormalLine(fieldValueId + ".setNavigator(true);");
					bodyBuilder.appendFormalLine(fieldValueId + ".setEffect(\"slideDown\");");
					bodyBuilder.appendFormalLine(fieldValueId + ".setPattern(\"dd/MM/yyyy\");");
					bodyBuilder.appendFormalLine(requiredStr);
				}
			} else if (JdkJavaType.isIntegerType(fieldType)) {
				if (action == Action.VIEW) {
					bodyBuilder.appendFormalLine(htmlOutputTextStr);
				} else {
					imports.addImport(PRIMEFACES_INPUT_TEXT);
					imports.addImport(PRIMEFACES_SLIDER);
					if (fieldType.equals(JdkJavaType.BIG_INTEGER)) {
						imports.addImport(fieldType);
					}
					bodyBuilder.appendFormalLine(inputTextStr);
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getValueExpressionStr(fieldValueId, fieldName, fieldType.getSimpleTypeName()));
					bodyBuilder.appendFormalLine(requiredStr);

					BigDecimal minValue = NumberUtils.max(getMinOrMax(field, MIN), getMinOrMax(field, DECIMAL_MIN));
					BigDecimal maxValue = NumberUtils.min(getMinOrMax(field, MAX), getMinOrMax(field, DECIMAL_MAX));
					if (minValue != null || maxValue != null) {
						bodyBuilder.append(getLongRangeValdatorString(fieldValueId, minValue, maxValue));
					}
					bodyBuilder.appendFormalLine("");
					bodyBuilder.appendFormalLine("Slider " + fieldValueId + "Slider = " + getComponentCreationStr("Slider"));
					bodyBuilder.appendFormalLine(fieldValueId + "Slider.setFor(\"" + fieldValueId + "\");");
					bodyBuilder.appendFormalLine("");
				
					final String fieldPanelGrid = fieldValueId + "PanelGrid";
					bodyBuilder.appendFormalLine("HtmlPanelGrid " + fieldPanelGrid + " = " + getComponentCreationStr("HtmlPanelGrid"));
					bodyBuilder.appendFormalLine(fieldPanelGrid + ".getChildren().add(" + fieldValueId + ");");
					bodyBuilder.appendFormalLine(fieldPanelGrid + ".getChildren().add(" + fieldValueId + "Slider);");
					fieldValueId = fieldPanelGrid;
				}
			} else if (JdkJavaType.isDecimalType(fieldType)) {
				if (action == Action.VIEW) {
					bodyBuilder.appendFormalLine(htmlOutputTextStr);
				} else {
					imports.addImport(PRIMEFACES_INPUT_TEXT);
					if (fieldType.equals(JdkJavaType.BIG_DECIMAL)) {
						imports.addImport(fieldType);
					}

					bodyBuilder.appendFormalLine(inputTextStr);
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getValueExpressionStr(fieldValueId, fieldName, fieldType.getSimpleTypeName()));
					bodyBuilder.appendFormalLine(requiredStr);

					BigDecimal minValue = NumberUtils.max(getMinOrMax(field, MIN), getMinOrMax(field, DECIMAL_MIN));
					BigDecimal maxValue = NumberUtils.min(getMinOrMax(field, MAX), getMinOrMax(field, DECIMAL_MAX));
					if (minValue != null || maxValue != null) {
						bodyBuilder.append(getDoubleRangeValdatorString(fieldValueId, minValue, maxValue));
					}
				}
			} else if (fieldType.equals(STRING)){
				if (action == Action.VIEW) {
					bodyBuilder.appendFormalLine(htmlOutputTextStr);
				} else {
					imports.addImport(PRIMEFACES_INPUT_TEXT);
					bodyBuilder.appendFormalLine(inputTextStr);
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getValueExpressionStr(fieldValueId, fieldName, fieldType.getSimpleTypeName()));
					bodyBuilder.appendFormalLine(requiredStr);
					
					Integer minValue = getSizeMinOrMax(field, "min");
					BigDecimal maxValue = NumberUtils.min(getSizeMinOrMax(field, "max"), getColumnLength(field));
					if (minValue != null || maxValue != null) {
						bodyBuilder.append(getLengthValdatorString(fieldValueId, minValue, maxValue));
					}
				}
			} else {
				if (action == Action.VIEW) {
					bodyBuilder.appendFormalLine(htmlOutputTextStr);
				} else {
					imports.addImport(PRIMEFACES_INPUT_TEXT);
					bodyBuilder.appendFormalLine(inputTextStr);
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getValueExpressionStr(fieldValueId, fieldName, fieldType.getSimpleTypeName()));
					bodyBuilder.appendFormalLine(requiredStr);
				}
			}

			if (action != Action.VIEW) {
				bodyBuilder.appendFormalLine(getAddToPanelText(fieldValueId));
				fieldValueId = fieldName + suffix2;

				// Add message for input field
				imports.addImport(PRIMEFACES_MESSAGE);
				bodyBuilder.appendFormalLine("");
				bodyBuilder.appendFormalLine("Message " + fieldValueId + "Message = " + getComponentCreationStr("Message"));
				bodyBuilder.appendFormalLine(fieldValueId + "Message.setId(\"" + fieldLabelId + "Message\");");
				bodyBuilder.appendFormalLine(fieldValueId + "Message.setFor(\"" + fieldValueId + "\");");
				bodyBuilder.appendFormalLine(fieldValueId + "Message.setDisplay(\"icon\");");
				bodyBuilder.appendFormalLine(getAddToPanelText(fieldValueId + "Message"));
			} else {
				bodyBuilder.appendFormalLine(getAddToPanelText(fieldValueId));
			}

			bodyBuilder.appendFormalLine("");
		}
		
		bodyBuilder.appendFormalLine("return htmlPanelGrid;");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, HTML_PANEL_GRID, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}

	private String getAddToPanelText(final String componentId) {
		return "htmlPanelGrid.getChildren().add(" + componentId + ");";
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
	
	private boolean isNullable(final FieldMetadata field) {
		return MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), NOT_NULL) == null;
	}
	
	private BigDecimal getMinOrMax(final FieldMetadata field, JavaType annotationType) {
		AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), annotationType);
		if (annotation != null && annotation.getAttribute(new JavaSymbolName("value")) != null) {
			return new BigDecimal(String.valueOf(annotation.getAttribute(new JavaSymbolName("value")).getValue()));
		}
		return null;
	}
	
	private Integer getSizeMinOrMax(final FieldMetadata field, String attrName) {
		AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), SIZE);
		if (annotation != null && annotation.getAttribute(new JavaSymbolName(attrName)) != null) {
			return (Integer) annotation.getAttribute(new JavaSymbolName(attrName)).getValue();
		}
		return null;
	}

	private Integer getColumnLength(final FieldMetadata field) {
		@SuppressWarnings("unchecked") Map<String, Object> values = (Map<String, Object>) field.getCustomData().get(PersistenceCustomDataKeys.COLUMN_FIELD);
		if (values != null && values.containsKey("length")) {
			return (Integer) values.get("length");
		}
		return null;
	}

	public String getLongRangeValdatorString(String fieldValueId, BigDecimal minValue, BigDecimal maxValue) {
		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(LONG_RANGE_VALIDATOR);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("LongRangeValidator " + fieldValueId + "Validator = new LongRangeValidator();");
		if (minValue != null) {
			bodyBuilder.appendFormalLine(fieldValueId + "Validator.setMinimum(" + minValue.longValue() + ");");
		}
		if (maxValue != null) {
			bodyBuilder.appendFormalLine(fieldValueId + "Validator.setMaximum(" + maxValue.longValue() + ");");
		}
		bodyBuilder.appendFormalLine(fieldValueId + ".addValidator(" + fieldValueId + "Validator);");
		return bodyBuilder.getOutput();
	}
	
	public String getDoubleRangeValdatorString(String fieldValueId, BigDecimal minValue, BigDecimal maxValue) {
		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(DOUBLE_RANGE_VALIDATOR);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("DoubleRangeValidator " + fieldValueId + "Validator = new DoubleRangeValidator();");
		if (minValue != null) {
			bodyBuilder.appendFormalLine(fieldValueId + "Validator.setMinimum(" + minValue.doubleValue() + ");");
		}
		if (maxValue != null) {
			bodyBuilder.appendFormalLine(fieldValueId + "Validator.setMaximum(" + maxValue.doubleValue() + ");");
		}
		bodyBuilder.appendFormalLine(fieldValueId + ".addValidator(" + fieldValueId + "Validator);");
		return bodyBuilder.getOutput();
	}

	public String getLengthValdatorString(String fieldValueId, Number minValue, Number maxValue) {
		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(LENGTH_VALIDATOR);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("LengthValidator " + fieldValueId + "Validator = new LengthValidator();");
		if (minValue != null) {
			bodyBuilder.appendFormalLine(fieldValueId + "Validator.setMinimum(" + minValue.intValue() + ");");
		}
		if (maxValue != null) {
			bodyBuilder.appendFormalLine(fieldValueId + "Validator.setMaximum(" + maxValue.intValue() + ");");
		}
		bodyBuilder.appendFormalLine(fieldValueId + ".addValidator(" + fieldValueId + "Validator);");
		return bodyBuilder.getOutput();
	}

	private MethodMetadata getFileUploadListenerMethod(final FieldMetadata rooUploadedFileField) {
		final JavaSymbolName methodName = getFileUploadMethodName(rooUploadedFileField.getFieldName());
		final JavaType parameterType = PRIMEFACES_FILE_UPLOAD_EVENT;
		final MethodMetadata method = getGovernorMethod(methodName, parameterType);
		if (method != null) return method;

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(FACES_MESSAGE);
		imports.addImport(PRIMEFACES_FILE_UPLOAD_EVENT);

		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("event"));
		
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("set" + StringUtils.capitalize(rooUploadedFileField.getFieldName().getSymbolName()) + "(event.getFile());");
		bodyBuilder.appendFormalLine("FacesContext facesContext = FacesContext.getCurrentInstance();");
		bodyBuilder.appendFormalLine("FacesMessage msg = new FacesMessage(\"Successful\", event.getFile().getFileName() + \" is uploaded.\");"); 
		bodyBuilder.appendFormalLine("facesContext.addMessage(null, msg);");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getUploadedFileAccessorMethod(final FieldMetadata rooUploadedFileField) {
		final JavaSymbolName methodName = new JavaSymbolName("get" + StringUtils.capitalize(rooUploadedFileField.getFieldName().getSymbolName()));
		final MethodMetadata method = getGovernorMethod(methodName);
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
		final JavaType parameterType = PRIMEFACES_UPLOADED_FILE;
		final MethodMetadata method = getGovernorMethod(methodName, parameterType);
		if (method != null) return method;

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(PRIMEFACES_UPLOADED_FILE);

		final List<JavaSymbolName> parameterNames = Arrays.asList(rooUploadedFileField.getFieldName());
		
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("this." + rooUploadedFileField.getFieldName().getSymbolName() + " = " + rooUploadedFileField.getFieldName().getSymbolName() + ";");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getEnumAutoCompleteMethod(final FieldMetadata autoCompleteField) {
		final JavaSymbolName methodName = new JavaSymbolName("complete" + StringUtils.capitalize(autoCompleteField.getFieldName().getSymbolName()));
		final JavaType parameterType = JavaType.STRING;
		final MethodMetadata method = getGovernorMethod(methodName, parameterType);
		if (method != null) return method;

		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("query"));

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

		final JavaType returnType = new JavaType(LIST.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(autoCompleteField.getFieldType()));
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getDisplayCreateDialogMethod() {
		final JavaSymbolName methodName = new JavaSymbolName(DISPLAY_CREATE_DIALOG);
		final MethodMetadata method = getGovernorMethod(methodName);
		if (method != null) return method;

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(CREATE_DIALOG_VISIBLE + " = true;");
		bodyBuilder.appendFormalLine("return \"" + StringUtils.uncapitalize(entity.getSimpleTypeName()) + "\";");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.STRING, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getDisplayListMethod() {
		final JavaSymbolName methodName = new JavaSymbolName(DISPLAY_LIST);
		final MethodMetadata method = getGovernorMethod(methodName);
		if (method != null) return method;

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(CREATE_DIALOG_VISIBLE + " = false;");
		bodyBuilder.appendFormalLine("return \"" + StringUtils.uncapitalize(entity.getSimpleTypeName()) + "\";");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.STRING, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getPersistMethod(final MemberTypeAdditions mergeMethod, final MemberTypeAdditions persistMethod, final MethodMetadata identifierAccessor) {
		final JavaSymbolName methodName = new JavaSymbolName("persist");
		final MethodMetadata method = getGovernorMethod(methodName);
		if (method != null) return method;

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(FACES_MESSAGE);
		imports.addImport(PRIMEFACES_REQUEST_CONTEXT);

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
		bodyBuilder.appendFormalLine("RequestContext context = RequestContext.getCurrentInstance();");
		bodyBuilder.appendFormalLine("context.execute(\"createDialog.hide()\");");
		bodyBuilder.appendFormalLine("context.execute(\"editDialog.hide()\");");
		bodyBuilder.appendFormalLine("");
		bodyBuilder.appendFormalLine("FacesMessage facesMessage = new FacesMessage(message);");
		bodyBuilder.appendFormalLine("FacesContext.getCurrentInstance().addMessage(null, facesMessage);");
		bodyBuilder.appendFormalLine("reset();");
		bodyBuilder.appendFormalLine("return findAll" + plural + "();");

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.STRING, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getDeleteMethod(final MemberTypeAdditions removeMethod) {
		final JavaSymbolName methodName = new JavaSymbolName("delete");
		final MethodMetadata method = getGovernorMethod(methodName);
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
		final MethodMetadata method = getGovernorMethod(methodName);
		if (method != null) return method;

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(getEntityName() + " = null;");
		bodyBuilder.appendFormalLine(CREATE_DIALOG_VISIBLE + " = false;");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.VOID_PRIMITIVE, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getHandleDialogCloseMethod() {
		final JavaSymbolName methodName = new JavaSymbolName("handleDialogClose");
		final JavaType parameterType = PRIMEFACES_CLOSE_EVENT;
		final MethodMetadata method = getGovernorMethod(methodName, parameterType);
		if (method != null) return method;

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(PRIMEFACES_CLOSE_EVENT);
		
		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("event"));

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("reset();");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder);
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
		final MethodMetadata method = getGovernorMethod(methodName);
		if (method != null) return method;

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return " + fieldName + ";");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.BOOLEAN_PRIMITIVE, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getBooleanMutatorMethod(final String fieldName) {
		final JavaSymbolName methodName = new JavaSymbolName("set" + StringUtils.capitalize(fieldName));
		final JavaType parameterType = JavaType.BOOLEAN_PRIMITIVE;
		final MethodMetadata method = getGovernorMethod(methodName, parameterType);
		if (method != null) return method;

		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName(fieldName));
		
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("this." + fieldName + " = " + fieldName + ";");
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder);
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
	
	private List<MethodMetadata> getListViewMethods() {
		final List<MethodMetadata> listViewMethod = new ArrayList<MethodMetadata>();
		for (final FieldMetadata field : locatedFieldsAndAccessors.keySet()) {
			if (field.getCustomData() != null && field.getCustomData().keySet().contains(JsfManagedBeanMetadataProvider.LIST_VIEW_FIELD_CUSTOM_DATA_KEY)) {
				listViewMethod.add(locatedFieldsAndAccessors.get(field));
			}
		}
		return listViewMethod;
	}
	
	private String getComponentCreationStr(final String componentName) {
		return new StringBuilder().append("(").append(componentName).append(") facesContext.getApplication().createComponent(").append(componentName).append(".COMPONENT_TYPE);").toString();
	}
	
	private String getValueExpressionStr(final String inputFieldVar, final String fieldName, final String className) {
		return inputFieldVar + ".setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{" + StringUtils.uncapitalize(entity.getSimpleTypeName()) + "Bean." + getEntityName() + "." + fieldName + "}\", " + className + ".class));";
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
