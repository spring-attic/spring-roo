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
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_COMMAND_BUTTON;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_DEFAULT_STREAMED_CONTENT;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_FILE_DOWNLOAD_ACTION_LISTENER;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_FILE_UPLOAD;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_FILE_UPLOAD_EVENT;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_INPUT_TEXT;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_MESSAGE;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_REQUEST_CONTEXT;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_SELECT_BOOLEAN_CHECKBOX;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_SELECT_MANY_MENU;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_SPINNER;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_STREAMED_CONTENT;
import static org.springframework.roo.addon.jsf.JsfJavaType.REQUEST_SCOPED;
import static org.springframework.roo.addon.jsf.JsfJavaType.SESSION_SCOPED;
import static org.springframework.roo.addon.jsf.JsfJavaType.UI_SELECT_ITEM;
import static org.springframework.roo.addon.jsf.JsfJavaType.UI_SELECT_ITEMS;
import static org.springframework.roo.addon.jsf.JsfJavaType.VIEW_SCOPED;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.MERGE_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.PERSIST_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.REMOVE_METHOD;
import static org.springframework.roo.model.JavaType.BOOLEAN_PRIMITIVE;
import static org.springframework.roo.model.JavaType.STRING;
import static org.springframework.roo.model.JdkJavaType.ARRAY_LIST;
import static org.springframework.roo.model.JdkJavaType.BYTE_ARRAY_INPUT_STREAM;
import static org.springframework.roo.model.JdkJavaType.DATE;
import static org.springframework.roo.model.JdkJavaType.HASH_SET;
import static org.springframework.roo.model.JdkJavaType.LIST;
import static org.springframework.roo.model.JdkJavaType.POST_CONSTRUCT;
import static org.springframework.roo.model.Jsr303JavaType.DECIMAL_MAX;
import static org.springframework.roo.model.Jsr303JavaType.DECIMAL_MIN;
import static org.springframework.roo.model.Jsr303JavaType.MAX;
import static org.springframework.roo.model.Jsr303JavaType.MIN;
import static org.springframework.roo.model.Jsr303JavaType.NOT_NULL;
import static org.springframework.roo.model.Jsr303JavaType.SIZE;
import static org.springframework.roo.model.RooJavaType.ROO_UPLOADED_FILE;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
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
	private static final JavaSymbolName NAME = new JavaSymbolName("name");
	private static final JavaSymbolName CREATE_DIALOG_VISIBLE = new JavaSymbolName("createDialogVisible");
	private static final JavaSymbolName DATA_VISIBLE = new JavaSymbolName("dataVisible");
	private static final JavaSymbolName COLUMNS = new JavaSymbolName("columns");
	private static final String HTML_PANEL_GRID_ID = "htmlPanelGrid";
	
	// Fields
	private Set<JsfFieldHolder> locatedFields;
	private JavaType entity;
	private String beanName;
	private String plural;
	private JavaSymbolName entityName;
	private final List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
	private final List<MethodMetadata> methods = new ArrayList<MethodMetadata>();

	private enum Action {
		CREATE, EDIT, VIEW;
	};

	public JsfManagedBeanMetadata(final String identifier, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final JsfManagedBeanAnnotationValues annotationValues, final String plural, final Map<MethodMetadataCustomDataKey, MemberTypeAdditions> crudAdditions, final Set<JsfFieldHolder> locatedFields, final MethodMetadata identifierAccessor) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' is invalid");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.isTrue(StringUtils.hasText(plural), "Plural required");
		Assert.notNull(crudAdditions, "Crud additions map required");
		Assert.notNull(locatedFields, "Located fields required");

		if (!isValid()) {
			return;
		}

		entity = annotationValues.getEntity();

		final MemberTypeAdditions findAllMethod = crudAdditions.get(FIND_ALL_METHOD);
		final MemberTypeAdditions mergeMethod = crudAdditions.get(MERGE_METHOD);
		final MemberTypeAdditions persistMethod = crudAdditions.get(PERSIST_METHOD);
		final MemberTypeAdditions removeMethod = crudAdditions.get(REMOVE_METHOD);
		if (identifierAccessor == null || findAllMethod == null || mergeMethod == null || persistMethod == null || removeMethod == null || locatedFields.isEmpty() || entity == null) {
			valid = false;
			return;
		}

		this.locatedFields = locatedFields;
		beanName = annotationValues.getBeanName();
		this.plural = plural;
		entityName = JavaSymbolName.getReservedWordSafeName(entity);

		final JavaSymbolName allEntitiesFieldName = new JavaSymbolName("all" + plural);
		final JavaType entityListType = getListType(entity);

		// Add @ManagedBean annotation if required
		builder.addAnnotation(getManagedBeanAnnotation(annotationValues.getBeanName()));

		// Add @SessionScoped annotation if required
		builder.addAnnotation(getScopeAnnotation());

		// Add fields
		fields.add(getField(PRIVATE, NAME, STRING, "\"" + plural + "\""));
		fields.add(getField(entityName, entity));
		fields.add(getField(allEntitiesFieldName, entityListType));
		fields.add(getField(PRIVATE, DATA_VISIBLE, BOOLEAN_PRIMITIVE, Boolean.FALSE.toString()));
		fields.add(getField(COLUMNS, getListType(STRING)));
		fields.add(getPanelGridField(Action.CREATE));
		fields.add(getPanelGridField(Action.EDIT));
		fields.add(getPanelGridField(Action.VIEW));
		fields.add(getField(PRIVATE, CREATE_DIALOG_VISIBLE, BOOLEAN_PRIMITIVE, Boolean.FALSE.toString()));

		// Add methods
		methods.add(getInitMethod(findAllMethod));
		methods.add(getAccessorMethod(NAME, STRING));
		methods.add(getAccessorMethod(COLUMNS, getListType(STRING)));
		methods.add(getAccessorMethod(allEntitiesFieldName, entityListType));
		methods.add(getMutatorMethod(allEntitiesFieldName, entityListType));
		methods.add(getFindAllEntitiesMethod(allEntitiesFieldName, findAllMethod));
		methods.add(getAccessorMethod(DATA_VISIBLE, BOOLEAN_PRIMITIVE));
		methods.add(getMutatorMethod(DATA_VISIBLE, BOOLEAN_PRIMITIVE));
		methods.add(getPanelGridAccessorMethod(Action.CREATE));
		methods.add(getPanelGridMutatorMethod(Action.CREATE));
		methods.add(getPanelGridAccessorMethod(Action.EDIT));
		methods.add(getPanelGridMutatorMethod(Action.EDIT));
		methods.add(getPanelGridAccessorMethod(Action.VIEW));
		methods.add(getPanelGridMutatorMethod(Action.VIEW));
		methods.add(getPopulatePanelMethod(Action.CREATE));
		methods.add(getPopulatePanelMethod(Action.EDIT));
		methods.add(getPopulatePanelMethod(Action.VIEW));

		methods.add(getEntityAccessorMethod());
		methods.add(getEntityMutatorMethod());

		addOtherFieldsAndMethods();

		methods.add(getAccessorMethod(CREATE_DIALOG_VISIBLE, BOOLEAN_PRIMITIVE));
		methods.add(getMutatorMethod(CREATE_DIALOG_VISIBLE, BOOLEAN_PRIMITIVE));
		methods.add(getDisplayListMethod());
		methods.add(getDisplayCreateDialogMethod());
		methods.add(getPersistMethod(mergeMethod, persistMethod, identifierAccessor));
		methods.add(getDeleteMethod(removeMethod));
		methods.add(getResetMethod());
		methods.add(getHandleDialogCloseMethod());

		// Add fields first to builder followed by methods
		for (FieldMetadata field : fields) {
			builder.addField(field);
		}
		for (MethodMetadata method : methods) {
			builder.addMethod(method);
		}

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	private void addOtherFieldsAndMethods() {
		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		for (JsfFieldHolder jsfFieldHolder : locatedFields) {
			if (jsfFieldHolder.isApplicationType()) {
				methods.add(getAutoCompleteApplicationTypeMethod(jsfFieldHolder));
			} else if (jsfFieldHolder.isEnumerated()) {
				methods.add(getAutoCompleteEnumMethod(jsfFieldHolder.getField()));
			} else if (jsfFieldHolder.isGenericType()) {
				final String fieldName = jsfFieldHolder.getField().getFieldName().getSymbolName();
				final Map<JavaType, String> genericTypes = jsfFieldHolder.getGenericTypes();
				final JavaType genericType = genericTypes.keySet().iterator().next();
				final JavaSymbolName selectedFieldName = new JavaSymbolName(getSelectedFieldName(fieldName));
				final JavaType listType = getListType(genericType);

				fields.add(getField(selectedFieldName, listType));
				methods.add(getAccessorMethod(selectedFieldName, listType));

				imports.addImport(HASH_SET);

				final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
				bodyBuilder.appendFormalLine("if (" + selectedFieldName.getSymbolName() + " != null) {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(entityName.getSymbolName() + ".set" + StringUtils.capitalize(fieldName) + "(new HashSet<" + genericType.getSimpleTypeName() + ">(" + selectedFieldName + "));");
				bodyBuilder.indentRemove();
				bodyBuilder.appendFormalLine("}");
				bodyBuilder.appendFormalLine("this." + selectedFieldName.getSymbolName() + " = " + selectedFieldName.getSymbolName() + ";");
				methods.add(getMutatorMethod(selectedFieldName, listType, bodyBuilder));
			} else if (jsfFieldHolder.isUploadFileField()) {
				imports.addImport(PRIMEFACES_STREAMED_CONTENT);
				imports.addImport(PRIMEFACES_DEFAULT_STREAMED_CONTENT);
				imports.addImport(BYTE_ARRAY_INPUT_STREAM);

				final FieldMetadata field = jsfFieldHolder.getField();
				final String fieldName = field.getFieldName().getSymbolName();
				final JavaSymbolName streamedContentFieldName = new JavaSymbolName(fieldName + "StreamedContent");

				fields.add(getField(streamedContentFieldName, PRIMEFACES_STREAMED_CONTENT));
				methods.add(getFileUploadListenerMethod(field));

				final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
				final AnnotationMetadata annotation = field.getAnnotation(ROO_UPLOADED_FILE);
				final String contentType = (String) annotation.getAttribute("contentType").getValue();
				String fileName = (String) annotation.getAttribute("fileName").getValue();
				fileName.replace('/', File.separatorChar);
				fileName.replace('\\', File.separatorChar);

				bodyBuilder.appendFormalLine("return new DefaultStreamedContent(new ByteArrayInputStream(" + entityName.getSymbolName() + ".get" + StringUtils.capitalize(fieldName) + "()), \"" + contentType + "\", \"" + fileName + "\");");
				methods.add(getAccessorMethod(streamedContentFieldName, PRIMEFACES_STREAMED_CONTENT, bodyBuilder));

				methods.add(getMutatorMethod(streamedContentFieldName, PRIMEFACES_STREAMED_CONTENT));
			}
		}
	}

	private AnnotationMetadata getManagedBeanAnnotation(String beanName) {
		AnnotationMetadata annotation = getTypeAnnotation(MANAGED_BEAN);
		if (annotation == null) {
			return null;
		}
		final AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(annotation);
		annotationBuilder.addStringAttribute("name", beanName);
		return annotationBuilder.build();
	}

	private AnnotationMetadata getScopeAnnotation() {
		if (hasScopeAnnotation()) {
			return null;
		}
		final AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(SESSION_SCOPED);
		return annotationBuilder.build();
	}

	private boolean hasScopeAnnotation() {
		return (governorTypeDetails.getAnnotation(SESSION_SCOPED) != null || governorTypeDetails.getAnnotation(VIEW_SCOPED) != null || governorTypeDetails.getAnnotation(REQUEST_SCOPED) != null);
	}

	private JavaType getListType(final JavaType parameterType) {
		return new JavaType(LIST.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(parameterType));
	}

	private FieldMetadata getPanelGridField(final Action panelType) {
		return getField(new JavaSymbolName(StringUtils.toLowerCase(panelType.name()) + "PanelGrid"), HTML_PANEL_GRID);
	}

	// Methods

	private MethodMetadata getInitMethod(final MemberTypeAdditions findAllAdditions) {
		final JavaSymbolName methodName = new JavaSymbolName("init");
		if (getGovernorMethod(methodName) != null) {
			return null;
		}

		findAllAdditions.copyAdditionsTo(builder, governorTypeDetails);

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(ARRAY_LIST);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("all" + plural + " = " + findAllAdditions.getMethodCall() + ";");
		bodyBuilder.appendFormalLine("columns = new ArrayList<String>();");
		for (final JsfFieldHolder jsfFieldHolder : locatedFields) {
			FieldMetadata field = jsfFieldHolder.getField();
			if (field.getCustomData() != null && field.getCustomData().keySet().contains(JsfManagedBeanMetadataProvider.LIST_VIEW_FIELD_CUSTOM_DATA_KEY)) {
				bodyBuilder.appendFormalLine("columns.add(\"" + field.getFieldName().getSymbolName() + "\");");
			}
		}

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.VOID_PRIMITIVE, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		methodBuilder.addAnnotation(new AnnotationMetadataBuilder(POST_CONSTRUCT));
		return methodBuilder.build();
	}

	private MethodMetadata getEntityAccessorMethod() {
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (" + entityName.getSymbolName() + " == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(entityName.getSymbolName() + " = new " + entity.getSimpleTypeName() + "();");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return " + entityName.getSymbolName() + ";");
		return getAccessorMethod(entityName, entity, bodyBuilder);
	}

	private MethodMetadata getEntityMutatorMethod() {
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		for (JsfFieldHolder jsfFieldHolder : locatedFields) {
			if (!jsfFieldHolder.isGenericType()) {
				continue;
			}

			final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
			imports.addImport(ARRAY_LIST);

			final String fieldName = jsfFieldHolder.getField().getFieldName().getSymbolName();
			final Map.Entry<JavaType, String> entry = jsfFieldHolder.getGenericTypes().entrySet().iterator().next();
			final JavaType genericType = entry.getKey();
			final String genericTypeBeanName = entry.getValue();
			final String genericTypePlural = jsfFieldHolder.getGenericTypePlural();
			
			bodyBuilder.appendFormalLine("if (" + entityName.getSymbolName() + " != null && " + entityName.getSymbolName() + ".get" + (StringUtils.hasText(genericTypeBeanName) ? genericTypePlural : StringUtils.capitalize(fieldName)) + "() != null) {");
			bodyBuilder.indent();
			bodyBuilder.appendFormalLine(getSelectedFieldName(fieldName) + " = new ArrayList<" + genericType.getSimpleTypeName() + ">(" + entityName.getSymbolName() + ".get" + StringUtils.capitalize(fieldName) + "());");
			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("}");
		}
		
		bodyBuilder.appendFormalLine("this." + entityName.getSymbolName() + " = " + entityName.getSymbolName() + ";");
		return getMutatorMethod(entityName, entity, bodyBuilder);
	}

	private MethodMetadata getFindAllEntitiesMethod(final JavaSymbolName fieldName, final MemberTypeAdditions findAllMethod) {
		final JavaSymbolName methodName = new JavaSymbolName("findAll" + plural);
		if (getGovernorMethod(methodName) != null) {
			return null;
		}

		findAllMethod.copyAdditionsTo(builder, governorTypeDetails);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(fieldName.getSymbolName() + " = " + findAllMethod.getMethodCall() + ";");
		bodyBuilder.appendFormalLine(DATA_VISIBLE + " = !" + fieldName.getSymbolName() + ".isEmpty();");
		bodyBuilder.appendFormalLine("return null;");

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.STRING, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getPanelGridAccessorMethod(final Action action) {
		final String fieldName = StringUtils.toLowerCase(action.name()) + "PanelGrid";
		final JavaSymbolName methodName = BeanInfoUtils.getAccessorMethodName(new JavaSymbolName(fieldName), false);
		if (getGovernorMethod(methodName) != null) {
			return null;
		}

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
		return getMutatorMethod(new JavaSymbolName(StringUtils.toLowerCase(action.name()) + "PanelGrid"), HTML_PANEL_GRID);
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

		if (getGovernorMethod(methodName) != null) {
			return null;
		}

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

		bodyBuilder.appendFormalLine("HtmlPanelGrid " + HTML_PANEL_GRID_ID + " = " + getComponentCreation("HtmlPanelGrid"));
		bodyBuilder.appendFormalLine("");

		for (final JsfFieldHolder jsfFieldHolder : locatedFields) {
			final FieldMetadata field = jsfFieldHolder.getField();
			final JavaType fieldType = field.getFieldType();
			final String simpleTypeName = fieldType.getSimpleTypeName();
			final String fieldName = field.getFieldName().getSymbolName();
			final String fieldLabelId = fieldName + suffix1;
			
			final BigDecimal minValue = NumberUtils.max(getMinOrMax(field, MIN), getMinOrMax(field, DECIMAL_MIN));
			final BigDecimal maxValue = NumberUtils.min(getMinOrMax(field, MAX), getMinOrMax(field, DECIMAL_MAX));
			final Integer sizeMinValue = getSizeMinOrMax(field, "min");
			final BigDecimal sizeMaxValue = NumberUtils.min(getSizeMinOrMax(field, "max"), getColumnLength(field));
			final boolean required = action != Action.VIEW && (!isNullable(field) || minValue != null || maxValue != null || sizeMinValue != null || sizeMaxValue != null);

			// Field label
			bodyBuilder.appendFormalLine("HtmlOutputText " + fieldLabelId + " = " + getComponentCreation("HtmlOutputText"));
			bodyBuilder.appendFormalLine(fieldLabelId + ".setId(\"" + fieldLabelId + "\");");
			bodyBuilder.appendFormalLine(fieldLabelId + ".setValue(\"" + fieldName + ": " + (required ? "* " : "  ") + "\");");
			bodyBuilder.appendFormalLine(getAddToPanelText(fieldLabelId));
			bodyBuilder.appendFormalLine("");

			// Field value
			final String fieldValueId = fieldName + suffix2;
			final String converterName = fieldValueId + "Converter";
			final String htmlOutputTextStr = "HtmlOutputText " + fieldValueId + " = " + getComponentCreation("HtmlOutputText");
			final String inputTextStr = "InputText " + fieldValueId + " = " + getComponentCreation("InputText");
			final String componentIdStr = fieldValueId + ".setId(\"" + fieldValueId + "\");";
			final String requiredStr = fieldValueId + ".setRequired(" + required + ");";

			if (jsfFieldHolder.isUploadFileField()) {
				AnnotationMetadata annotation = field.getAnnotation(ROO_UPLOADED_FILE);
				final String contentType = (String) annotation.getAttribute("contentType").getValue();
				final String allowedType = UploadedFileContentType.getFileExtension(contentType);
				final AnnotationAttributeValue<?> autoUploadAttr = annotation.getAttribute("autoUpload");
				if (action == Action.VIEW) {
					imports.addImport(PRIMEFACES_FILE_DOWNLOAD_ACTION_LISTENER);
					imports.addImport(PRIMEFACES_COMMAND_BUTTON);
					imports.addImport(PRIMEFACES_STREAMED_CONTENT);
					bodyBuilder.appendFormalLine("CommandButton " + fieldValueId + " = " + getComponentCreation("CommandButton"));
					bodyBuilder.appendFormalLine(fieldValueId + ".addActionListener(new FileDownloadActionListener(expressionFactory.createValueExpression(elContext, \"#{" + beanName + "." + fieldName + "StreamedContent}\", StreamedContent.class), null));");
					bodyBuilder.appendFormalLine(fieldValueId + ".setValue(\"Download\");");
					bodyBuilder.appendFormalLine(fieldValueId + ".setAjax(false);");
			} else {
					imports.addImport(PRIMEFACES_FILE_UPLOAD);
					imports.addImport(PRIMEFACES_FILE_UPLOAD_EVENT);
					bodyBuilder.appendFormalLine("FileUpload " + fieldValueId + " = " + getComponentCreation("FileUpload"));
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(fieldValueId + ".setFileUploadListener(expressionFactory.createMethodExpression(elContext, \"#{" + beanName + "." + getFileUploadMethodName(fieldName) + "}\", void.class, new Class[] { FileUploadEvent.class }));");
					bodyBuilder.appendFormalLine(fieldValueId + ".setMode(\"advanced\");");
					if (autoUploadAttr != null && (Boolean) autoUploadAttr.getValue()) {
						bodyBuilder.appendFormalLine(fieldValueId + ".setAuto(true);");
					}
					bodyBuilder.appendFormalLine(fieldValueId + ".setAllowTypes(\"/(\\\\.|\\\\/)(" + getAllowTypeRegex(allowedType)  + ")$/\");");
				}
			} else if (fieldType.equals(JavaType.BOOLEAN_OBJECT) || fieldType.equals(JavaType.BOOLEAN_PRIMITIVE)) {
				if (action == Action.VIEW) {
					bodyBuilder.appendFormalLine(htmlOutputTextStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldName, fieldValueId));
				} else {
					imports.addImport(PRIMEFACES_SELECT_BOOLEAN_CHECKBOX);
					bodyBuilder.appendFormalLine("SelectBooleanCheckbox " + fieldValueId + " = " + getComponentCreation("SelectBooleanCheckbox"));
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldValueId, fieldName, simpleTypeName));
					bodyBuilder.appendFormalLine(requiredStr);
				}
			} else if (jsfFieldHolder.isEnumerated()) {
				if (action == Action.VIEW) {
					bodyBuilder.appendFormalLine(htmlOutputTextStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldName, fieldValueId));
				} else {
					imports.addImport(PRIMEFACES_AUTO_COMPLETE);
					imports.addImport(fieldType);
					bodyBuilder.appendFormalLine("AutoComplete " + fieldValueId + " = " + getComponentCreation("AutoComplete"));
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldValueId, fieldName, simpleTypeName));
					bodyBuilder.appendFormalLine(getSetCompleteMethod(fieldName, fieldValueId));
					bodyBuilder.appendFormalLine(fieldValueId + ".setDropdown(true);");
					bodyBuilder.appendFormalLine(requiredStr);
				}
			} else if (JdkJavaType.isDateField(fieldType)) {
				if (action == Action.VIEW) {
					imports.addImport(DATE_TIME_CONVERTER);
					bodyBuilder.appendFormalLine(htmlOutputTextStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldName, fieldValueId));
					bodyBuilder.appendFormalLine("DateTimeConverter " + converterName + " = new DateTimeConverter();");
					bodyBuilder.appendFormalLine(converterName + ".setPattern(\"dd/MM/yyyy\");");
					bodyBuilder.appendFormalLine(fieldValueId + ".setConverter(" + converterName + ");");
				} else {
					imports.addImport(PRIMEFACES_CALENDAR);
					imports.addImport(DATE);
					bodyBuilder.appendFormalLine("Calendar " + fieldValueId + " = " + getComponentCreation("Calendar"));
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldValueId, fieldName, "Date"));
					bodyBuilder.appendFormalLine(fieldValueId + ".setNavigator(true);");
					bodyBuilder.appendFormalLine(fieldValueId + ".setEffect(\"slideDown\");");
					bodyBuilder.appendFormalLine(fieldValueId + ".setPattern(\"dd/MM/yyyy\");");
					bodyBuilder.appendFormalLine(requiredStr);
				}
			} else if (JdkJavaType.isIntegerType(fieldType)) {
				if (action == Action.VIEW) {
					bodyBuilder.appendFormalLine(htmlOutputTextStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldName, fieldValueId));
				} else {
					imports.addImport(PRIMEFACES_INPUT_TEXT);
					imports.addImport(PRIMEFACES_SPINNER);
					if (fieldType.equals(JdkJavaType.BIG_INTEGER)) {
						imports.addImport(fieldType);
					}
					bodyBuilder.appendFormalLine("Spinner " + fieldValueId + " = " + getComponentCreation("Spinner"));
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldValueId, fieldName, simpleTypeName));
					bodyBuilder.appendFormalLine(requiredStr);

					if (minValue != null || maxValue != null) {
						if (minValue != null) {
							bodyBuilder.appendFormalLine(fieldValueId + ".setMin(" + minValue.doubleValue() + ");");
						}
						if (maxValue != null) {
							bodyBuilder.appendFormalLine(fieldValueId + ".setMax(" + maxValue.doubleValue() + ");");
						}
						bodyBuilder.append(getLongRangeValdatorString(fieldValueId, minValue, maxValue));
					}
					bodyBuilder.appendFormalLine("");
				}
			} else if (JdkJavaType.isDecimalType(fieldType)) {
				if (action == Action.VIEW) {
					bodyBuilder.appendFormalLine(htmlOutputTextStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldName, fieldValueId));
				} else {
					imports.addImport(PRIMEFACES_INPUT_TEXT);
					if (fieldType.equals(JdkJavaType.BIG_DECIMAL)) {
						imports.addImport(fieldType);
					}

					bodyBuilder.appendFormalLine(inputTextStr);
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldValueId, fieldName, simpleTypeName));
					bodyBuilder.appendFormalLine(requiredStr);

					if (minValue != null || maxValue != null) {
						bodyBuilder.append(getDoubleRangeValdatorString(fieldValueId, minValue, maxValue));
					}
				}
			} else if (fieldType.equals(STRING)) {
				if (action == Action.VIEW) {
					bodyBuilder.appendFormalLine(htmlOutputTextStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldName, fieldValueId));
				} else {
					imports.addImport(PRIMEFACES_INPUT_TEXT);
					bodyBuilder.appendFormalLine(inputTextStr);
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldValueId, fieldName, simpleTypeName));
					bodyBuilder.appendFormalLine(requiredStr);

					if (sizeMinValue != null || sizeMaxValue != null) {
						bodyBuilder.append(getLengthValdatorString(fieldValueId, sizeMinValue, sizeMaxValue));
					}
				}
			} else if (jsfFieldHolder.isGenericType()) {
				final Map.Entry<JavaType, String> entry = jsfFieldHolder.getGenericTypes().entrySet().iterator().next();
				final JavaType genericType = entry.getKey();
				final String genericTypeBeanName = entry.getValue();
				final String genericTypeFieldName = StringUtils.uncapitalize(genericType.getSimpleTypeName());
				
				imports.addImport(PRIMEFACES_SELECT_MANY_MENU);
				imports.addImport(UI_SELECT_ITEMS);
				imports.addImport(UI_SELECT_ITEM);
				imports.addImport(fieldType);

				if (StringUtils.hasText(genericTypeBeanName)) {
					JavaType converterType = new JavaType(destination.getPackage().getFullyQualifiedPackageName() + "." + genericType.getSimpleTypeName() + "Converter");
					imports.addImport(converterType);

					final String genericTypePlural = jsfFieldHolder.getGenericTypePlural();

					bodyBuilder.appendFormalLine("SelectManyMenu " + fieldValueId + " = " + getComponentCreation("SelectManyMenu"));
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(fieldValueId + ".setConverter(new " + converterType.getSimpleTypeName() + "());");
					bodyBuilder.appendFormalLine(fieldValueId + ".setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{" + beanName + "." + getSelectedFieldName(fieldName) + "}\", List.class));");
					bodyBuilder.appendFormalLine("UISelectItems " + fieldValueId + "Items = (UISelectItems) facesContext.getApplication().createComponent(UISelectItems.COMPONENT_TYPE);");
					if (action == Action.VIEW) {
						bodyBuilder.appendFormalLine(fieldValueId + ".setReadonly(true);");
						bodyBuilder.appendFormalLine(fieldValueId + ".setDisabled(true);");
						bodyBuilder.appendFormalLine(fieldValueId + "Items.setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{" + beanName + "." + entityName.getSymbolName() + "." + fieldName + "}\", " + fieldType.getSimpleTypeName() + ".class));");
					} else {
						bodyBuilder.appendFormalLine(fieldValueId + "Items.setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{" + genericTypeBeanName + ".all" + StringUtils.capitalize(genericTypePlural) + "}\", List.class));");
						bodyBuilder.appendFormalLine(requiredStr);
					}
					bodyBuilder.appendFormalLine(fieldValueId + "Items.setValueExpression(\"var\", expressionFactory.createValueExpression(elContext, \"" + genericTypeFieldName + "\", String.class));");
					bodyBuilder.appendFormalLine(fieldValueId + "Items.setValueExpression(\"itemLabel\", expressionFactory.createValueExpression(elContext, \"#{" + genericTypeFieldName + ".displayString}\", String.class));");
					bodyBuilder.appendFormalLine(fieldValueId + "Items.setValueExpression(\"itemValue\", expressionFactory.createValueExpression(elContext, \"#{" + genericTypeFieldName + "}\", " + genericType.getSimpleTypeName() + ".class));");
					bodyBuilder.appendFormalLine(fieldValueId + ".getChildren().add(" + fieldValueId + "Items);");
					
				} else {
					// Generic type is an enum
					bodyBuilder.appendFormalLine("SelectManyMenu " + fieldValueId + " = " + getComponentCreation("SelectManyMenu"));
					bodyBuilder.appendFormalLine(componentIdStr);
					if (action == Action.VIEW) {
						bodyBuilder.appendFormalLine(fieldValueId + ".setReadonly(true);");
						bodyBuilder.appendFormalLine(fieldValueId + ".setDisabled(true);");
						bodyBuilder.appendFormalLine(fieldValueId + ".setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{" + beanName + "." + getSelectedFieldName(fieldName) + "}\", List.class));");
						bodyBuilder.appendFormalLine("UISelectItems " + fieldValueId + "Items = (UISelectItems) facesContext.getApplication().createComponent(UISelectItems.COMPONENT_TYPE);");
						bodyBuilder.appendFormalLine(fieldValueId + "Items.setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{" + beanName + "." + entityName.getSymbolName() + "." + fieldName + "}\", " + fieldType.getSimpleTypeName() + ".class));");
						bodyBuilder.appendFormalLine(fieldValueId + "Items.setValueExpression(\"var\", expressionFactory.createValueExpression(elContext, \"" + genericTypeFieldName + "\", String.class));");
						bodyBuilder.appendFormalLine(fieldValueId + "Items.setValueExpression(\"itemLabel\", expressionFactory.createValueExpression(elContext, \"#{" + genericTypeFieldName + "}\", String.class));");
						bodyBuilder.appendFormalLine(fieldValueId + "Items.setValueExpression(\"itemValue\", expressionFactory.createValueExpression(elContext, \"#{" + genericTypeFieldName + "}\", " + genericType.getSimpleTypeName() + ".class));");
						bodyBuilder.appendFormalLine(fieldValueId + ".getChildren().add(" + fieldValueId + "Items);");
					} else {
						bodyBuilder.appendFormalLine(fieldValueId + ".setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{" + beanName + "." + getSelectedFieldName(fieldName) + "}\", List.class));");
						bodyBuilder.appendFormalLine(requiredStr);
						bodyBuilder.appendFormalLine("UISelectItem " + fieldValueId + "Item;");
						bodyBuilder.appendFormalLine("for (" +  genericType.getSimpleTypeName() + " " + StringUtils.uncapitalize(genericType.getSimpleTypeName()) + " : " + genericType.getSimpleTypeName() + ".values()) {");
						bodyBuilder.indent();
						bodyBuilder.appendFormalLine(fieldValueId + "Item = (UISelectItem) facesContext.getApplication().createComponent(UISelectItem.COMPONENT_TYPE);");
						bodyBuilder.appendFormalLine(fieldValueId + "Item.setItemLabel(" + StringUtils.uncapitalize(genericType.getSimpleTypeName()) + ".name());");
						bodyBuilder.appendFormalLine(fieldValueId + "Item.setItemValue(" + StringUtils.uncapitalize(genericType.getSimpleTypeName()) + ".name());");
						bodyBuilder.appendFormalLine(fieldValueId + ".getChildren().add(" + fieldValueId + "Item);");
						bodyBuilder.indentRemove();
						bodyBuilder.appendFormalLine("}");
					}
				}

			} else if (jsfFieldHolder.isApplicationType()) {
				JavaType converterType = new JavaType(destination.getPackage().getFullyQualifiedPackageName() + "." + simpleTypeName + "Converter");
				if (action == Action.VIEW) {
					bodyBuilder.appendFormalLine(htmlOutputTextStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldValueId, fieldName, simpleTypeName));
					bodyBuilder.appendFormalLine(fieldValueId + ".setConverter(new " + converterType.getSimpleTypeName() + "());");
				} else {
					imports.addImport(PRIMEFACES_AUTO_COMPLETE);
					imports.addImport(fieldType);
					imports.addImport(new JavaType(converterName));
					bodyBuilder.appendFormalLine("AutoComplete " + fieldValueId + " = " + getComponentCreation("AutoComplete"));
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldValueId, fieldName, simpleTypeName));
					bodyBuilder.appendFormalLine(getSetCompleteMethod(fieldName, fieldValueId));
					bodyBuilder.appendFormalLine(fieldValueId + ".setDropdown(true);");
					bodyBuilder.appendFormalLine(fieldValueId + ".setValueExpression(\"var\", expressionFactory.createValueExpression(elContext, \"" + fieldName + "\", String.class));");
					bodyBuilder.appendFormalLine(fieldValueId + ".setValueExpression(\"itemLabel\", expressionFactory.createValueExpression(elContext, \"#{" + fieldName + ".displayString}\", String.class));");
					bodyBuilder.appendFormalLine(fieldValueId + ".setValueExpression(\"itemValue\", expressionFactory.createValueExpression(elContext, \"#{" + fieldName + "}\", " + simpleTypeName + ".class));");
					bodyBuilder.appendFormalLine(fieldValueId + ".setConverter(new " + converterType.getSimpleTypeName() + "());");
					bodyBuilder.appendFormalLine(requiredStr);
				}
			} else {
				if (action == Action.VIEW) {
					bodyBuilder.appendFormalLine(htmlOutputTextStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldName, fieldValueId));
				} else {
					imports.addImport(PRIMEFACES_INPUT_TEXT);
					bodyBuilder.appendFormalLine(inputTextStr);
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getSetValueExpression(fieldValueId, fieldName, simpleTypeName));
					bodyBuilder.appendFormalLine(requiredStr);
				}
			}

			if (action != Action.VIEW) {
				bodyBuilder.appendFormalLine(getAddToPanelText(fieldValueId));
				// Add message for input field
				imports.addImport(PRIMEFACES_MESSAGE);
				bodyBuilder.appendFormalLine("");
				bodyBuilder.appendFormalLine("Message " + fieldValueId + "Message = " + getComponentCreation("Message"));
				bodyBuilder.appendFormalLine(fieldValueId + "Message.setId(\"" + fieldValueId + "Message\");");
				bodyBuilder.appendFormalLine(fieldValueId + "Message.setFor(\"" + fieldValueId + "\");");
				bodyBuilder.appendFormalLine(fieldValueId + "Message.setDisplay(\"icon\");");
				bodyBuilder.appendFormalLine(getAddToPanelText(fieldValueId + "Message"));
			} else {
				bodyBuilder.appendFormalLine(getAddToPanelText(fieldValueId));
			}

			bodyBuilder.appendFormalLine("");
		}

		bodyBuilder.appendFormalLine("return " + HTML_PANEL_GRID_ID + ";");

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, HTML_PANEL_GRID, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}

	private String getAllowTypeRegex(String allowedType) {
		StringBuilder builder = new StringBuilder();
		char[] value = allowedType.toCharArray();
		for (int i = 0; i < value.length; i++) {
			builder.append("[").append(Character.toLowerCase(value[i])).append(Character.toUpperCase(value[i])).append("]");
		}
		if (allowedType.equals(UploadedFileContentType.JPG.name())) {
			builder.append("|[jJ][pP][eE][gG]");
		}
		return builder.toString();
	}

	private String getSelectedFieldName(final String fieldName) {
		return "selected" + StringUtils.capitalize(fieldName);
	}

	private String getAddToPanelText(final String componentId) {
		return HTML_PANEL_GRID_ID + ".getChildren().add(" + componentId + ");";
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
		@SuppressWarnings("unchecked")
		Map<String, Object> values = (Map<String, Object>) field.getCustomData().get(CustomDataKeys.COLUMN_FIELD);
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

	private MethodMetadata getFileUploadListenerMethod(final FieldMetadata field) {
		final String fieldName = field.getFieldName().getSymbolName();
		final JavaSymbolName methodName = getFileUploadMethodName(fieldName);
		final JavaType parameterType = PRIMEFACES_FILE_UPLOAD_EVENT;
		if (getGovernorMethod(methodName, parameterType) != null) {
			return null;
		}

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(FACES_CONTEXT);
		imports.addImport(FACES_MESSAGE);
		imports.addImport(PRIMEFACES_FILE_UPLOAD_EVENT);

		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("event"));

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(entityName + ".set" + StringUtils.capitalize(fieldName) + "(event.getFile().getContents());");
		bodyBuilder.appendFormalLine("FacesContext facesContext = FacesContext.getCurrentInstance();");
		bodyBuilder.appendFormalLine("FacesMessage facesMessage = new FacesMessage(\"Successful\", event.getFile().getFileName() + \" is uploaded.\");");
		bodyBuilder.appendFormalLine("facesContext.addMessage(null, facesMessage);");

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getAutoCompleteEnumMethod(final FieldMetadata autoCompleteField) {
		final JavaSymbolName methodName = new JavaSymbolName("complete" + StringUtils.capitalize(autoCompleteField.getFieldName().getSymbolName()));
		final JavaType parameterType = STRING;
		if (getGovernorMethod(methodName, parameterType) != null) {
			return null;
		}

		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("query"));
		final JavaType returnType = new JavaType(LIST.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(autoCompleteField.getFieldType()));

		final String simpleTypeName = autoCompleteField.getFieldType().getSimpleTypeName();
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
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

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getAutoCompleteApplicationTypeMethod(final JsfFieldHolder jsfFieldHolder) {
		final FieldMetadata field = jsfFieldHolder.getField();
		final JavaSymbolName methodName = new JavaSymbolName("complete" + StringUtils.capitalize(field.getFieldName().getSymbolName()));
		final JavaType parameterType = STRING;
		if (getGovernorMethod(methodName, parameterType) != null) {
			return null;
		}

		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("query"));
		final MemberTypeAdditions findAllMethod = jsfFieldHolder.getCrudAdditions().get(FIND_ALL_METHOD);
		final String simpleTypeName = field.getFieldType().getSimpleTypeName();

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("List<" + simpleTypeName + "> suggestions = new ArrayList<" + simpleTypeName + ">();");
		bodyBuilder.appendFormalLine("for (" + simpleTypeName + " " + StringUtils.uncapitalize(simpleTypeName) + " : " + findAllMethod.getMethodCall() + ") {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("if (" + StringUtils.uncapitalize(simpleTypeName) + ".getDisplayString().toLowerCase().startsWith(query.toLowerCase())) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("suggestions.add(" + StringUtils.uncapitalize(simpleTypeName) + ");");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return suggestions;");

		final JavaType returnType = new JavaType(LIST.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(field.getFieldType()));

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getDisplayCreateDialogMethod() {
		final JavaSymbolName methodName = new JavaSymbolName(DISPLAY_CREATE_DIALOG);
		final MethodMetadata method = getGovernorMethod(methodName);
		if (method != null) {
			return method;
		}

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(entityName.getSymbolName() + " = new " + entity.getSimpleTypeName() + "();");
		bodyBuilder.appendFormalLine(CREATE_DIALOG_VISIBLE + " = true;");
		bodyBuilder.appendFormalLine("return \"" + entityName.getSymbolName() + "\";");
		return getMethod(PUBLIC, methodName, STRING, null, null, bodyBuilder);
	}

	private MethodMetadata getDisplayListMethod() {
		return getMethod(PUBLIC, new JavaSymbolName(DISPLAY_LIST), STRING, null, null, InvocableMemberBodyBuilder.getInstance().appendFormalLine(CREATE_DIALOG_VISIBLE + " = false;").appendFormalLine("return \"" + entityName.getSymbolName() + "\";"));
	}

	private MethodMetadata getPersistMethod(final MemberTypeAdditions mergeMethod, final MemberTypeAdditions persistMethod, final MethodMetadata identifierAccessor) {
		final JavaSymbolName methodName = new JavaSymbolName("persist");
		final MethodMetadata method = getGovernorMethod(methodName);
		if (method != null) {
			return method;
		}

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(FACES_MESSAGE);
		imports.addImport(PRIMEFACES_REQUEST_CONTEXT);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("String message = \"\";");
		bodyBuilder.appendFormalLine("if (" + entityName.getSymbolName() + "." + identifierAccessor.getMethodName().getSymbolName() + "() != null) {");
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

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, STRING, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getDeleteMethod(final MemberTypeAdditions removeMethod) {
		final JavaSymbolName methodName = new JavaSymbolName("delete");
		if (getGovernorMethod(methodName) != null) {
			return null;
		}

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(removeMethod.getMethodCall() + ";");
		removeMethod.copyAdditionsTo(builder, governorTypeDetails);
		bodyBuilder.appendFormalLine("FacesMessage facesMessage = new FacesMessage(\"Successfully deleted\");");
		bodyBuilder.appendFormalLine("FacesContext.getCurrentInstance().addMessage(null, facesMessage);");
		bodyBuilder.appendFormalLine("reset();");
		bodyBuilder.appendFormalLine("return findAll" + plural + "();");

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, STRING, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getResetMethod() {
		final JavaSymbolName methodName = new JavaSymbolName("reset");
		if (getGovernorMethod(methodName) != null) {
			return null;
		}

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(entityName.getSymbolName() + " = null;");
		for (JsfFieldHolder jsfFieldHolder : locatedFields) {
			if (!jsfFieldHolder.isGenericType()) {
				continue;
			}
			
			final Map.Entry<JavaType, String> entry = jsfFieldHolder.getGenericTypes().entrySet().iterator().next();
			final String genericTypeBeanName = entry.getValue();
			final String genericTypePlural = jsfFieldHolder.getGenericTypePlural();
			final JavaSymbolName fieldName = new JavaSymbolName(getSelectedFieldName(StringUtils.hasText(genericTypeBeanName) ? genericTypePlural : jsfFieldHolder.getField().getFieldName().getSymbolName()));
			bodyBuilder.appendFormalLine(fieldName.getSymbolName() + " = null;");
		}
		bodyBuilder.appendFormalLine(CREATE_DIALOG_VISIBLE + " = false;");
		return getMethod(PUBLIC, methodName, JavaType.VOID_PRIMITIVE, null, null, bodyBuilder);
	}

	private MethodMetadata getHandleDialogCloseMethod() {
		final JavaSymbolName methodName = new JavaSymbolName("handleDialogClose");
		final JavaType parameterType = PRIMEFACES_CLOSE_EVENT;
		if (getGovernorMethod(methodName, parameterType) != null) {
			return null;
		}

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(PRIMEFACES_CLOSE_EVENT);

		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("event"));

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("reset();");

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	private JavaSymbolName getFileUploadMethodName(final String fieldName) {
		return new JavaSymbolName("handleFileUploadFor" + StringUtils.capitalize(fieldName));
	}

	private String getComponentCreation(final String componentName) {
		return new StringBuilder().append("(").append(componentName).append(") facesContext.getApplication().createComponent(").append(componentName).append(".COMPONENT_TYPE);").toString();
	}

	private String getSetValueExpression(final String inputFieldVar, final String fieldName, final String className) {
		return inputFieldVar + ".setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{" + beanName + "." + entityName.getSymbolName() + "." + fieldName + "}\", " + className + ".class));";
	}

	private String getSetValueExpression(final String fieldName, String fieldValueId) {
		return getSetValueExpression(fieldValueId, fieldName, "String");
	}

	private String getSetCompleteMethod(final String fieldName, String fieldValueId) {
		return fieldValueId + ".setCompleteMethod(expressionFactory.createMethodExpression(elContext, \"#{" + beanName + ".complete" + StringUtils.capitalize(fieldName) + "}\", List.class, new Class[] { String.class }));";
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
