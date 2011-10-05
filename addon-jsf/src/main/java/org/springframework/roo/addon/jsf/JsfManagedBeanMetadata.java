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
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_SELECT_BOOLEAN_CHECKBOX;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_SLIDER;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_SPINNER;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_UPLOADED_FILE;
import static org.springframework.roo.addon.jsf.JsfJavaType.REQUEST_SCOPED;
import static org.springframework.roo.addon.jsf.JsfJavaType.SESSION_SCOPED;
import static org.springframework.roo.addon.jsf.JsfJavaType.VIEW_SCOPED;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.MERGE_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.PERSIST_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.REMOVE_METHOD;
import static org.springframework.roo.model.JavaType.BOOLEAN_PRIMITIVE;
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
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.FieldMetadata;
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
	private static final JavaSymbolName NAME = new JavaSymbolName("name");
	private static final JavaSymbolName CREATE_DIALOG_VISIBLE = new JavaSymbolName("createDialogVisible");
	private static final JavaSymbolName DATA_VISIBLE = new JavaSymbolName("dataVisible");
	private static final JavaSymbolName COLUMNS = new JavaSymbolName("columns");
	private static final String HTML_PANEL_GRID_ID = "htmlPanelGrid";

	// Fields
	private JavaType entity;
	private Set<JsfFieldHolder> locatedFields;
	private String plural;
	private JavaSymbolName entityName;
	private final Set<FieldMetadata> autoCompleteEnumFields = new LinkedHashSet<FieldMetadata>();
	private final Set<JsfFieldHolder> autoCompleteApplicationTypeFields = new LinkedHashSet<JsfFieldHolder>();

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
		this.plural = plural;
		this.locatedFields = locatedFields;

		final MemberTypeAdditions findAllMethod = crudAdditions.get(FIND_ALL_METHOD);
		final MemberTypeAdditions mergeMethod = crudAdditions.get(MERGE_METHOD);
		final MemberTypeAdditions persistMethod = crudAdditions.get(PERSIST_METHOD);
		final MemberTypeAdditions removeMethod = crudAdditions.get(REMOVE_METHOD);
		if (identifierAccessor == null || findAllMethod == null || mergeMethod == null || persistMethod == null || removeMethod == null || this.locatedFields.isEmpty() || entity == null) {
			valid = false;
			return;
		}

		final Set<FieldMetadata> rooUploadedFileFields = getRooUploadedFileFields();
		final JavaSymbolName allEntitiesFieldName = new JavaSymbolName("all" + plural);
		final JavaType entityListType = getListType(entity);
		entityName = JavaSymbolName.getReservedWordSafeName(entity);

		// Add @ManagedBean annotation if required
		builder.addAnnotation(getManagedBeanAnnotation());

		// Add @SessionScoped annotation if required
		builder.addAnnotation(getScopeAnnotation());

		// Add fields
		builder.addField(getField(PRIVATE, NAME, STRING, "\"" + plural + "\""));
		builder.addField(getField(entityName, entity));
		builder.addField(getField(allEntitiesFieldName, entityListType));
		builder.addField(getField(PRIVATE, DATA_VISIBLE, BOOLEAN_PRIMITIVE, Boolean.FALSE.toString()));
		builder.addField(getField(COLUMNS, getListType(STRING)));
		builder.addField(getPanelGridField(Action.CREATE));
		builder.addField(getPanelGridField(Action.EDIT));
		builder.addField(getPanelGridField(Action.VIEW));
		builder.addField(getField(PRIVATE, CREATE_DIALOG_VISIBLE, BOOLEAN_PRIMITIVE, Boolean.FALSE.toString()));

		for (final FieldMetadata rooUploadedFileField : rooUploadedFileFields) {
			builder.addField(getField(rooUploadedFileField.getFieldName(), PRIMEFACES_UPLOADED_FILE));
		}

		// Add methods
		builder.addMethod(getInitMethod(findAllMethod));
		builder.addMethod(getAccessorMethod(NAME, STRING));
		builder.addMethod(getAccessorMethod(COLUMNS, getListType(STRING)));
		builder.addMethod(getSelectedEntityAccessorMethod());
		builder.addMethod(getMutatorMethod(entityName, entity));
		builder.addMethod(getAccessorMethod(allEntitiesFieldName, entityListType));
		builder.addMethod(getMutatorMethod(allEntitiesFieldName, entityListType));
		builder.addMethod(getFindAllEntitiesMethod(allEntitiesFieldName, findAllMethod));
		builder.addMethod(getAccessorMethod(DATA_VISIBLE, BOOLEAN_PRIMITIVE));
		builder.addMethod(getMutatorMethod(DATA_VISIBLE, BOOLEAN_PRIMITIVE));
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
			builder.addMethod(getAccessorMethod(rooUploadedFileField.getFieldName(), rooUploadedFileField.getFieldType()));
			builder.addMethod(getMutatorMethod(rooUploadedFileField.getFieldName(), rooUploadedFileField.getFieldType()));
		}

		for (final FieldMetadata field : autoCompleteEnumFields) {
			builder.addMethod(getAutoCompleteEnumMethod(field));
		}

		for (final JsfFieldHolder jsfFieldHolder : autoCompleteApplicationTypeFields) {
			builder.addMethod(getAutoCompleteApplicationTypeMethod(jsfFieldHolder));
		}

		builder.addMethod(getAccessorMethod(CREATE_DIALOG_VISIBLE, BOOLEAN_PRIMITIVE));
		builder.addMethod(getMutatorMethod(CREATE_DIALOG_VISIBLE, BOOLEAN_PRIMITIVE));
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
		return (governorTypeDetails.getAnnotation(SESSION_SCOPED) != null
			|| governorTypeDetails.getAnnotation(VIEW_SCOPED) != null
			|| governorTypeDetails.getAnnotation(REQUEST_SCOPED) != null);
	}

	private JavaType getListType(final JavaType parameterType) {
		return new JavaType(LIST.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(parameterType));
	}

	private FieldMetadata getPanelGridField(final Action panelType) {
		return getField(new JavaSymbolName(StringUtils.toLowerCase(panelType.name()) + "PanelGrid"), HTML_PANEL_GRID);
	}

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

	private MethodMetadata getSelectedEntityAccessorMethod() {
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (" + entityName.getSymbolName() + " == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(entityName.getSymbolName() + " = new " + entity.getSimpleTypeName() + "();");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return " + entityName.getSymbolName() + ";");
		return getAccessorMethod(entityName, entity, bodyBuilder);
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
			final String fieldName = field.getFieldName().getSymbolName();
			final String fieldLabelId = fieldName + suffix1;
			final boolean nullable = isNullable(field);
			final String requiredFlag = action != Action.VIEW && !nullable ? " * " : "   ";

			// Field label
			bodyBuilder.appendFormalLine("HtmlOutputText " + fieldLabelId + " = " + getComponentCreation("HtmlOutputText"));
			bodyBuilder.appendFormalLine(fieldLabelId + ".setId(\"" + fieldLabelId + "\");");
			bodyBuilder.appendFormalLine(fieldLabelId + ".setValue(\"" + fieldName + ":" + requiredFlag + "\");");
			if (action == Action.VIEW) {
				bodyBuilder.appendFormalLine(fieldLabelId + ".setStyle(\"font-weight:bold\");");
			}
			bodyBuilder.appendFormalLine(getAddToPanelText(fieldLabelId));
			bodyBuilder.appendFormalLine("");

			// Field value
			String fieldValueId = fieldName + suffix2;
			String converterName = fieldValueId + "Converter";
			final String htmlOutputTextStr = "HtmlOutputText " + fieldValueId + " = " + getComponentCreation("HtmlOutputText");
			final String inputTextStr = "InputText " + fieldValueId + " = " + getComponentCreation("InputText");
			final String componentIdStr = fieldValueId + ".setId(\"" + fieldValueId + "\");";
			final String requiredStr = fieldValueId + ".setRequired(" + !nullable + ");";

			if (isRooUploadFileField(field)) {
				imports.addImport(PRIMEFACES_FILE_UPLOAD);
				imports.addImport(PRIMEFACES_FILE_UPLOAD_EVENT);
				imports.addImport(PRIMEFACES_UPLOADED_FILE);
				final JavaSymbolName fileUploadMethodName = getFileUploadMethodName(field.getFieldName());
				bodyBuilder.appendFormalLine("FileUpload " + fieldValueId + " = " + getComponentCreation("FileUpload"));
				bodyBuilder.appendFormalLine(componentIdStr);
				bodyBuilder.appendFormalLine(fieldValueId + ".setFileUploadListener(expressionFactory.createMethodExpression(elContext, \"#{" + entityName + "Bean." + fileUploadMethodName + "}\", void.class, new Class[] { FileUploadEvent.class }));");
				bodyBuilder.appendFormalLine(fieldValueId + ".setMode(\"advanced\");");
				bodyBuilder.appendFormalLine(fieldValueId + ".setUpdate(\"messages\");");
			} else if (fieldType.equals(JavaType.BOOLEAN_OBJECT) || fieldType.equals(JavaType.BOOLEAN_PRIMITIVE)) {
				if (action == Action.VIEW) {
					bodyBuilder.appendFormalLine(htmlOutputTextStr);
					bodyBuilder.appendFormalLine(getValueExpression(fieldName, fieldValueId));
				} else {
					imports.addImport(PRIMEFACES_SELECT_BOOLEAN_CHECKBOX);
					bodyBuilder.appendFormalLine("SelectBooleanCheckbox " + fieldValueId + " = " + getComponentCreation("SelectBooleanCheckbox"));
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getValueExpression(fieldValueId, fieldName, fieldType.getSimpleTypeName()));
					bodyBuilder.appendFormalLine(requiredStr);
				}
			} else if (jsfFieldHolder.isEnumerated()) {
				if (action == Action.VIEW) {
					bodyBuilder.appendFormalLine(htmlOutputTextStr);
					bodyBuilder.appendFormalLine(getValueExpression(fieldName, fieldValueId));
				} else {
					imports.addImport(PRIMEFACES_AUTO_COMPLETE);
					imports.addImport(fieldType);
					bodyBuilder.appendFormalLine("AutoComplete " + fieldValueId + " = " + getComponentCreation("AutoComplete"));
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getValueExpression(fieldValueId, fieldName, fieldType.getSimpleTypeName()));
					bodyBuilder.appendFormalLine(fieldValueId + ".setCompleteMethod(expressionFactory.createMethodExpression(elContext, \"#{" + entityName + "Bean.complete" + StringUtils.capitalize(fieldName) + "}\", List.class, new Class[] { String.class }));");
					bodyBuilder.appendFormalLine(fieldValueId + ".setDropdown(true);");
					bodyBuilder.appendFormalLine(requiredStr);
					autoCompleteEnumFields.add(field);
				}
			} else if (JdkJavaType.isDateField(fieldType)) {
				if (action == Action.VIEW) {
					imports.addImport(DATE_TIME_CONVERTER);
					bodyBuilder.appendFormalLine(htmlOutputTextStr);
					bodyBuilder.appendFormalLine(getValueExpression(fieldName, fieldValueId));
					bodyBuilder.appendFormalLine("DateTimeConverter " + converterName + " = new DateTimeConverter();");
					bodyBuilder.appendFormalLine(converterName + ".setPattern(\"dd/MM/yyyy\");");
					bodyBuilder.appendFormalLine(fieldValueId + ".setConverter(" + converterName + ");");
				} else {
					imports.addImport(PRIMEFACES_CALENDAR);
					imports.addImport(DATE);
					bodyBuilder.appendFormalLine("Calendar " + fieldValueId + " = " + getComponentCreation("Calendar"));
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getValueExpression(fieldValueId, fieldName, "Date"));
					bodyBuilder.appendFormalLine(fieldValueId + ".setNavigator(true);");
					bodyBuilder.appendFormalLine(fieldValueId + ".setEffect(\"slideDown\");");
					bodyBuilder.appendFormalLine(fieldValueId + ".setPattern(\"dd/MM/yyyy\");");
					bodyBuilder.appendFormalLine(requiredStr);
				}
			} else if (JdkJavaType.isIntegerType(fieldType)) {
				if (action == Action.VIEW) {
					bodyBuilder.appendFormalLine(htmlOutputTextStr);
					bodyBuilder.appendFormalLine(getValueExpression(fieldName, fieldValueId));
				} else {
					imports.addImport(PRIMEFACES_INPUT_TEXT);
					imports.addImport(PRIMEFACES_SLIDER);
					imports.addImport(PRIMEFACES_SPINNER);
					if (fieldType.equals(JdkJavaType.BIG_INTEGER)) {
						imports.addImport(fieldType);
					}
					bodyBuilder.appendFormalLine("Spinner " + fieldValueId + " = " + getComponentCreation("Spinner"));
			//		bodyBuilder.appendFormalLine(inputTextStr);
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getValueExpression(fieldValueId, fieldName, fieldType.getSimpleTypeName()));
					bodyBuilder.appendFormalLine(requiredStr);

					BigDecimal minValue = NumberUtils.max(getMinOrMax(field, MIN), getMinOrMax(field, DECIMAL_MIN));
					BigDecimal maxValue = NumberUtils.min(getMinOrMax(field, MAX), getMinOrMax(field, DECIMAL_MAX));
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
			//		bodyBuilder.appendFormalLine("Slider " + fieldValueId + "Slider = " + getComponentCreationStr("Slider"));
			//		bodyBuilder.appendFormalLine(fieldValueId + "Slider.setFor(\"" + fieldValueId + "\");");
			//		bodyBuilder.appendFormalLine(fieldValueId + "Spinner.setFor(\"" + fieldValueId + "\");");
			//		bodyBuilder.appendFormalLine("");

			//		final String fieldPanelGrid = fieldValueId + "PanelGrid";
			//		bodyBuilder.appendFormalLine("HtmlPanelGrid " + fieldPanelGrid + " = " + getComponentCreationStr("HtmlPanelGrid"));
			//		bodyBuilder.appendFormalLine(fieldPanelGrid + ".getChildren().add(" + fieldValueId + ");");
			//		bodyBuilder.appendFormalLine(fieldPanelGrid + ".getChildren().add(" + fieldValueId + "Slider);");
			//		fieldValueId = fieldPanelGrid;
				}
			} else if (JdkJavaType.isDecimalType(fieldType)) {
				if (action == Action.VIEW) {
					bodyBuilder.appendFormalLine(htmlOutputTextStr);
					bodyBuilder.appendFormalLine(getValueExpression(fieldName, fieldValueId));
				} else {
					imports.addImport(PRIMEFACES_INPUT_TEXT);
					if (fieldType.equals(JdkJavaType.BIG_DECIMAL)) {
						imports.addImport(fieldType);
					}

					bodyBuilder.appendFormalLine(inputTextStr);
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getValueExpression(fieldValueId, fieldName, fieldType.getSimpleTypeName()));
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
					bodyBuilder.appendFormalLine(getValueExpression(fieldName, fieldValueId));
				} else {
					imports.addImport(PRIMEFACES_INPUT_TEXT);
					bodyBuilder.appendFormalLine(inputTextStr);
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getValueExpression(fieldValueId, fieldName, fieldType.getSimpleTypeName()));
					bodyBuilder.appendFormalLine(requiredStr);

					Integer minValue = getSizeMinOrMax(field, "min");
					BigDecimal maxValue = NumberUtils.min(getSizeMinOrMax(field, "max"), getColumnLength(field));
					if (minValue != null || maxValue != null) {
						bodyBuilder.append(getLengthValdatorString(fieldValueId, minValue, maxValue));
					}
				}
		//	} else if (jsfFieldHolder.isCommonCollectionType()) {


			} else if (jsfFieldHolder.isApplicationType()) {
				JavaType converterType = new JavaType(destination.getPackage().getFullyQualifiedPackageName() + "." + fieldType.getSimpleTypeName() + "Converter");
				if (action == Action.VIEW) {
					bodyBuilder.appendFormalLine(htmlOutputTextStr);
					bodyBuilder.appendFormalLine(getValueExpression(fieldValueId, fieldName, fieldType.getSimpleTypeName()));
					bodyBuilder.appendFormalLine(fieldValueId + ".setConverter(new " + converterType.getSimpleTypeName() + "());");
				} else {
					imports.addImport(PRIMEFACES_AUTO_COMPLETE);
					imports.addImport(fieldType);
					imports.addImport(new JavaType(converterName));
					bodyBuilder.appendFormalLine("AutoComplete " + fieldValueId + " = " + getComponentCreation("AutoComplete"));
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getValueExpression(fieldValueId, fieldName, fieldType.getSimpleTypeName()));
					bodyBuilder.appendFormalLine(fieldValueId + ".setCompleteMethod(expressionFactory.createMethodExpression(elContext, \"#{" + entityName + "Bean.complete" + StringUtils.capitalize(fieldName) + "}\", List.class, new Class[] { String.class }));");
					bodyBuilder.appendFormalLine(fieldValueId + ".setDropdown(true);");
					bodyBuilder.appendFormalLine(fieldValueId + ".setValueExpression(\"var\", expressionFactory.createValueExpression(elContext, \""+ fieldName + "\", " + fieldType.getSimpleTypeName() + ".class));");
					bodyBuilder.appendFormalLine(fieldValueId + ".setValueExpression(\"itemLabel\", expressionFactory.createValueExpression(elContext, \"#{" + fieldName + ".displayName}\", String.class));");
					bodyBuilder.appendFormalLine(fieldValueId + ".setValueExpression(\"itemValue\", expressionFactory.createValueExpression(elContext, \"#{" + fieldName + "}\", " + fieldType.getSimpleTypeName() + ".class));");
					bodyBuilder.appendFormalLine(fieldValueId + ".setConverter(new " + converterType.getSimpleTypeName() + "());");
					bodyBuilder.appendFormalLine(requiredStr);
					autoCompleteApplicationTypeFields.add(jsfFieldHolder);
				}
			} else {
				if (action == Action.VIEW) {
					bodyBuilder.appendFormalLine(htmlOutputTextStr);
					bodyBuilder.appendFormalLine(getValueExpression(fieldName, fieldValueId));
				} else {
					imports.addImport(PRIMEFACES_INPUT_TEXT);
					bodyBuilder.appendFormalLine(inputTextStr);
					bodyBuilder.appendFormalLine(componentIdStr);
					bodyBuilder.appendFormalLine(getValueExpression(fieldValueId, fieldName, fieldType.getSimpleTypeName()));
					bodyBuilder.appendFormalLine(requiredStr);
				}
			}

			if (action != Action.VIEW) {
				bodyBuilder.appendFormalLine(getAddToPanelText(fieldValueId));
				fieldValueId = fieldName + suffix2;

				// Add message for input field
				imports.addImport(PRIMEFACES_MESSAGE);
				bodyBuilder.appendFormalLine("");
				bodyBuilder.appendFormalLine("Message " + fieldValueId + "Message = " + getComponentCreation("Message"));
				bodyBuilder.appendFormalLine(fieldValueId + "Message.setId(\"" + fieldLabelId + "Message\");");
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

	private String getAddToPanelText(final String componentId) {
		return HTML_PANEL_GRID_ID + ".getChildren().add(" + componentId + ");";
	}

	private boolean isNullable(final FieldMetadata field) {
		return MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), NOT_NULL) == null;
	}

	private BigDecimal getMinOrMax(final FieldMetadata field, final JavaType annotationType) {
		AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), annotationType);
		if (annotation != null && annotation.getAttribute(new JavaSymbolName("value")) != null) {
			return new BigDecimal(String.valueOf(annotation.getAttribute(new JavaSymbolName("value")).getValue()));
		}
		return null;
	}

	private Integer getSizeMinOrMax(final FieldMetadata field, final String attrName) {
		AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), SIZE);
		if (annotation != null && annotation.getAttribute(new JavaSymbolName(attrName)) != null) {
			return (Integer) annotation.getAttribute(new JavaSymbolName(attrName)).getValue();
		}
		return null;
	}

	private Integer getColumnLength(final FieldMetadata field) {
		@SuppressWarnings("unchecked") Map<String, Object> values = (Map<String, Object>) field.getCustomData().get(CustomDataKeys.COLUMN_FIELD);
		if (values != null && values.containsKey("length")) {
			return (Integer) values.get("length");
		}
		return null;
	}

	public String getLongRangeValdatorString(final String fieldValueId, final BigDecimal minValue, final BigDecimal maxValue) {
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

	public String getDoubleRangeValdatorString(final String fieldValueId, final BigDecimal minValue, final BigDecimal maxValue) {
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

	public String getLengthValdatorString(final String fieldValueId, final Number minValue, final Number maxValue) {
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
		if (getGovernorMethod(methodName, parameterType) != null) {
			return null;
		}

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(FACES_MESSAGE);

		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("event"));

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("set" + StringUtils.capitalize(rooUploadedFileField.getFieldName().getSymbolName()) + "(event.getFile());");
		bodyBuilder.appendFormalLine("FacesContext facesContext = FacesContext.getCurrentInstance();");
		bodyBuilder.appendFormalLine("FacesMessage msg = new FacesMessage(\"Successful\", event.getFile().getFileName() + \" is uploaded.\");");
		bodyBuilder.appendFormalLine("facesContext.addMessage(null, msg);");

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

		final JavaType returnType = new JavaType(LIST.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(autoCompleteField.getFieldType()));

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
		bodyBuilder.appendFormalLine("if (" + StringUtils.uncapitalize(simpleTypeName) + "." + jsfFieldHolder.getDisplayMethod() + ".toLowerCase().startsWith(query.toLowerCase())) {");
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
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(entityName.getSymbolName() + " = new " + entity.getSimpleTypeName() + "();");
		bodyBuilder.appendFormalLine(CREATE_DIALOG_VISIBLE + " = true;");
		bodyBuilder.appendFormalLine("return \"" + entityName.getSymbolName() + "\";");
		return getMethod(PUBLIC, new JavaSymbolName(DISPLAY_CREATE_DIALOG), STRING, null, null, bodyBuilder);
	}

	private MethodMetadata getDisplayListMethod() {
		return getMethod(PUBLIC, new JavaSymbolName(DISPLAY_LIST), STRING, null, null, InvocableMemberBodyBuilder.getInstance().appendFormalLine(CREATE_DIALOG_VISIBLE + " = false;").appendFormalLine("return \"" + entityName.getSymbolName() + "\";"));
	}

	private MethodMetadata getPersistMethod(final MemberTypeAdditions mergeMethod, final MemberTypeAdditions persistMethod, final MethodMetadata identifierAccessor) {
		final JavaSymbolName methodName = new JavaSymbolName("persist");
		final MethodMetadata method = getGovernorMethod(methodName);
		if (method != null) return method;

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(FACES_MESSAGE);
		imports.addImport(PRIMEFACES_REQUEST_CONTEXT);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("String message = \"\";");
		bodyBuilder.appendFormalLine("if (" + entityName.getSymbolName() + "." +  identifierAccessor.getMethodName().getSymbolName() + "() != null) {");
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
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(entityName.getSymbolName() + " = null;");
		bodyBuilder.appendFormalLine(CREATE_DIALOG_VISIBLE + " = false;");
		return getMethod(PUBLIC, new JavaSymbolName("reset"), JavaType.VOID_PRIMITIVE, null, null, bodyBuilder);
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

	private JavaSymbolName getFileUploadMethodName(final JavaSymbolName fieldName) {
		return new JavaSymbolName("handleFileUploadFor" + StringUtils.capitalize(fieldName.getSymbolName()));
	}

	private Set<FieldMetadata> getRooUploadedFileFields() {
		final Set<FieldMetadata> rooUploadedFileFields = new LinkedHashSet<FieldMetadata>();
		for (final JsfFieldHolder jsfFieldHolder : locatedFields) {
			final FieldMetadata field = jsfFieldHolder.getField();
			if (isRooUploadFileField(field)) {
				rooUploadedFileFields.add(field);
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

	private String getComponentCreation(final String componentName) {
		return new StringBuilder().append("(").append(componentName).append(") facesContext.getApplication().createComponent(").append(componentName).append(".COMPONENT_TYPE);").toString();
	}

	private String getValueExpression(final String inputFieldVar, final String fieldName, final String className) {
		return inputFieldVar + ".setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{" + entityName.getSymbolName() + "Bean." + entityName.getSymbolName() + "." + fieldName + "}\", " + className + ".class));";
	}

	private String getValueExpression(final String fieldName, final String fieldValueId) {
		return getValueExpression(fieldValueId, fieldName, "String");
	}

	@Override
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
