package org.springframework.roo.addon.jsf.managedbean;

import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PUBLIC;
import static org.springframework.roo.addon.jsf.JsfJavaType.APPLICATION;
import static org.springframework.roo.addon.jsf.JsfJavaType.APPLICATION_SCOPED;
import static org.springframework.roo.addon.jsf.JsfJavaType.DATE_TIME_CONVERTER;
import static org.springframework.roo.addon.jsf.JsfJavaType.DISPLAY_CREATE_DIALOG;
import static org.springframework.roo.addon.jsf.JsfJavaType.DISPLAY_LIST;
import static org.springframework.roo.addon.jsf.JsfJavaType.DOUBLE_RANGE_VALIDATOR;
import static org.springframework.roo.addon.jsf.JsfJavaType.EL_CONTEXT;
import static org.springframework.roo.addon.jsf.JsfJavaType.ENUM_CONVERTER;
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
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_INPUT_TEXTAREA;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_MESSAGE;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_OUTPUT_LABEL;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_REQUEST_CONTEXT;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_SELECT_BOOLEAN_CHECKBOX;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_SELECT_MANY_MENU;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_SPINNER;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_STREAMED_CONTENT;
import static org.springframework.roo.addon.jsf.JsfJavaType.REGEX_VALIDATOR;
import static org.springframework.roo.addon.jsf.JsfJavaType.REQUEST_SCOPED;
import static org.springframework.roo.addon.jsf.JsfJavaType.SESSION_SCOPED;
import static org.springframework.roo.addon.jsf.JsfJavaType.UI_COMPONENT;
import static org.springframework.roo.addon.jsf.JsfJavaType.UI_SELECT_ITEM;
import static org.springframework.roo.addon.jsf.JsfJavaType.UI_SELECT_ITEMS;
import static org.springframework.roo.addon.jsf.JsfJavaType.VIEW_SCOPED;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.MANY_TO_MANY_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.MERGE_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.ONE_TO_MANY_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.ONE_TO_ONE_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.PERSIST_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.REMOVE_METHOD;
import static org.springframework.roo.model.JavaType.BOOLEAN_OBJECT;
import static org.springframework.roo.model.JavaType.BOOLEAN_PRIMITIVE;
import static org.springframework.roo.model.JavaType.STRING;
import static org.springframework.roo.model.JavaType.VOID_PRIMITIVE;
import static org.springframework.roo.model.JdkJavaType.ARRAY_LIST;
import static org.springframework.roo.model.JdkJavaType.BYTE_ARRAY_INPUT_STREAM;
import static org.springframework.roo.model.JdkJavaType.DATE;
import static org.springframework.roo.model.JdkJavaType.HASH_SET;
import static org.springframework.roo.model.JdkJavaType.LIST;
import static org.springframework.roo.model.JdkJavaType.POST_CONSTRUCT;
import static org.springframework.roo.model.JpaJavaType.MANY_TO_MANY;
import static org.springframework.roo.model.JpaJavaType.ONE_TO_MANY;
import static org.springframework.roo.model.JpaJavaType.ONE_TO_ONE;
import static org.springframework.roo.model.Jsr303JavaType.DECIMAL_MAX;
import static org.springframework.roo.model.Jsr303JavaType.DECIMAL_MIN;
import static org.springframework.roo.model.Jsr303JavaType.FUTURE;
import static org.springframework.roo.model.Jsr303JavaType.MAX;
import static org.springframework.roo.model.Jsr303JavaType.MIN;
import static org.springframework.roo.model.Jsr303JavaType.NOT_NULL;
import static org.springframework.roo.model.Jsr303JavaType.PAST;
import static org.springframework.roo.model.Jsr303JavaType.PATTERN;
import static org.springframework.roo.model.Jsr303JavaType.SIZE;
import static org.springframework.roo.model.RooJavaType.ROO_UPLOADED_FILE;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
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
import org.springframework.roo.classpath.operations.jsr303.UploadedFileContentType;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.CustomData;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooJsfManagedBean}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public class JsfManagedBeanMetadata extends
        AbstractItdTypeDetailsProvidingMetadataItem {

    private enum Action {
        CREATE, EDIT, VIEW;
    }

    static final String APPLICATION_TYPE_FIELDS_KEY = "applicationTypeFieldsKey";
    static final String APPLICATION_TYPE_KEY = "applicationTypeKey";
    private static final JavaSymbolName COLUMNS = new JavaSymbolName("columns");
    private static final JavaSymbolName CREATE_DIALOG_VISIBLE = new JavaSymbolName(
            "createDialogVisible");
    static final String CRUD_ADDITIONS_KEY = "crudAdditionsKey";
    private static final JavaSymbolName DATA_VISIBLE = new JavaSymbolName(
            "dataVisible");
    static final String ENUMERATED_KEY = "enumeratedKey";

    private static final String HTML_PANEL_GRID_ID = "htmlPanelGrid";
    static final String LIST_VIEW_FIELD_KEY = "listViewFieldKey";
    private static final JavaSymbolName NAME = new JavaSymbolName("name");
    static final String PARAMETER_TYPE_KEY = "parameterTypeKey";
    static final String PARAMETER_TYPE_MANAGED_BEAN_NAME_KEY = "parameterTypeManagedBeanNameKey";
    static final String PARAMETER_TYPE_PLURAL_KEY = "parameterTypePluralKey";
    private static final String PROVIDES_TYPE_STRING = JsfManagedBeanMetadata.class
            .getName();
    private static final String PROVIDES_TYPE = MetadataIdentificationUtils
            .create(PROVIDES_TYPE_STRING);

    public static String createIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return PhysicalTypeIdentifierNamingUtils.createIdentifier(
                PROVIDES_TYPE_STRING, javaType, path);
    }

    public static JavaType getJavaType(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getJavaType(
                PROVIDES_TYPE_STRING, metadataIdentificationString);
    }

    public static String getMetadataIdentiferType() {
        return PROVIDES_TYPE;
    }

    public static LogicalPath getPath(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING,
                metadataIdentificationString);
    }

    public static boolean isValid(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING,
                metadataIdentificationString);
    }

    private String beanName;
    private final List<FieldMetadataBuilder> builderFields = new ArrayList<FieldMetadataBuilder>();;
    private final List<MethodMetadataBuilder> builderMethods = new ArrayList<MethodMetadataBuilder>();
    private JavaType entity;
    private JavaSymbolName entityName;
    private Set<FieldMetadata> locatedFields;
    private String plural;
    private JavaType messageFactory;

    public JsfManagedBeanMetadata(
            final String identifier,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final JsfManagedBeanAnnotationValues annotationValues,
            final String plural,
            final Map<MethodMetadataCustomDataKey, MemberTypeAdditions> crudAdditions,
            final Set<FieldMetadata> locatedFields,
            final MethodMetadata identifierAccessor) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
        Validate.isTrue(isValid(identifier),
                "Metadata identification string '%s' is invalid", identifier);
        Validate.notNull(annotationValues, "Annotation values required");
        Validate.notBlank(plural, "Plural required");
        Validate.notNull(crudAdditions, "Crud additions map required");
        Validate.notNull(locatedFields, "Located fields required");

        if (!isValid()) {
            return;
        }

        entity = annotationValues.getEntity();

        final MemberTypeAdditions findAllMethod = crudAdditions
                .get(FIND_ALL_METHOD);
        final MemberTypeAdditions mergeMethod = crudAdditions.get(MERGE_METHOD);
        final MemberTypeAdditions persistMethod = crudAdditions
                .get(PERSIST_METHOD);
        final MemberTypeAdditions removeMethod = crudAdditions
                .get(REMOVE_METHOD);
        if (identifierAccessor == null || findAllMethod == null
                || mergeMethod == null || persistMethod == null
                || removeMethod == null || entity == null) {
            valid = false;
            return;
        }

        this.locatedFields = locatedFields;
        beanName = annotationValues.getBeanName();
        this.plural = plural;
        entityName = JavaSymbolName.getReservedWordSafeName(entity);
        messageFactory = new JavaType(destination.getPackage()
                .getFullyQualifiedPackageName() + ".util.MessageFactory");

        final JavaSymbolName allEntitiesFieldName = new JavaSymbolName("all"
                + plural);
        final JavaType entityListType = getListType(entity);

        // Add @ManagedBean annotation if required
        builder.addAnnotation(getManagedBeanAnnotation(annotationValues
                .getBeanName()));

        // Add @SessionScoped annotation if required
        builder.addAnnotation(getScopeAnnotation());

        // Add builderFields
        builderFields
                .add(getField(PRIVATE, NAME, STRING, "\"" + plural + "\""));
        builderFields.add(getField(entityName, entity));
        builderFields.add(getField(allEntitiesFieldName, entityListType));
        builderFields.add(getField(PRIVATE, DATA_VISIBLE, BOOLEAN_PRIMITIVE,
                Boolean.FALSE.toString()));
        builderFields.add(getField(COLUMNS, getListType(STRING)));
        builderFields.add(getPanelGridField(Action.CREATE));
        builderFields.add(getPanelGridField(Action.EDIT));
        builderFields.add(getPanelGridField(Action.VIEW));
        builderFields.add(getField(PRIVATE, CREATE_DIALOG_VISIBLE,
                BOOLEAN_PRIMITIVE, Boolean.FALSE.toString()));

        // Add builderMethods
        builderMethods.add(getInitMethod(identifierAccessor));
        builderMethods.add(getAccessorMethod(NAME, STRING));
        builderMethods.add(getAccessorMethod(COLUMNS, getListType(STRING)));
        builderMethods.add(getAccessorMethod(allEntitiesFieldName,
                entityListType));
        builderMethods.add(getMutatorMethod(allEntitiesFieldName,
                entityListType));
        builderMethods.add(getFindAllEntitiesMethod(allEntitiesFieldName,
                findAllMethod));
        builderMethods.add(getAccessorMethod(DATA_VISIBLE, BOOLEAN_PRIMITIVE));
        builderMethods.add(getMutatorMethod(DATA_VISIBLE, BOOLEAN_PRIMITIVE));
        builderMethods.add(getPanelGridAccessorMethod(Action.CREATE));
        builderMethods.add(getPanelGridMutatorMethod(Action.CREATE));
        builderMethods.add(getPanelGridAccessorMethod(Action.EDIT));
        builderMethods.add(getPanelGridMutatorMethod(Action.EDIT));
        builderMethods.add(getPanelGridAccessorMethod(Action.VIEW));
        builderMethods.add(getPanelGridMutatorMethod(Action.VIEW));
        builderMethods.add(getPopulatePanelMethod(Action.CREATE));
        builderMethods.add(getPopulatePanelMethod(Action.EDIT));
        builderMethods.add(getPopulatePanelMethod(Action.VIEW));

        builderMethods.add(getEntityAccessorMethod());
        builderMethods.add(getMutatorMethod(entityName, entity));

        addOtherFieldsAndMethods();

        builderMethods.add(getOnEditMethod());
        builderMethods.add(getAccessorMethod(CREATE_DIALOG_VISIBLE,
                BOOLEAN_PRIMITIVE));
        builderMethods.add(getMutatorMethod(CREATE_DIALOG_VISIBLE,
                BOOLEAN_PRIMITIVE));
        builderMethods.add(getDisplayListMethod());
        builderMethods.add(getDisplayCreateDialogMethod());
        builderMethods.add(getPersistMethod(mergeMethod, persistMethod,
                identifierAccessor));
        builderMethods.add(getDeleteMethod(removeMethod));
        builderMethods.add(getResetMethod());
        builderMethods.add(getHandleDialogCloseMethod());

        // Add builderFields first to builder followed by builderMethods
        for (final FieldMetadataBuilder fieldBuilder : builderFields) {
            builder.addField(fieldBuilder);
        }
        for (final MethodMetadataBuilder method : builderMethods) {
            builder.addMethod(method);
        }

        // Create a representation of the desired output ITD
        itdTypeDetails = builder.build();
    }

    private void addOtherFieldsAndMethods() {
        for (final FieldMetadata field : locatedFields) {
            final CustomData customData = field.getCustomData();

            if (customData.keySet().contains(APPLICATION_TYPE_KEY)) {
                builderMethods.add(getAutoCompleteApplicationTypeMethod(field));
            }
            else if (customData.keySet().contains(ENUMERATED_KEY)) {
                builderMethods.add(getAutoCompleteEnumMethod(field));
            }
            else if (field.getCustomData().keySet()
                    .contains(PARAMETER_TYPE_KEY)) {
                final String fieldName = field.getFieldName().getSymbolName();
                final JavaType parameterType = (JavaType) field.getCustomData()
                        .get(PARAMETER_TYPE_KEY);
                final JavaSymbolName selectedFieldName = new JavaSymbolName(
                        getSelectedFieldName(fieldName));
                final JavaType listType = getListType(parameterType);

                builderFields.add(getField(selectedFieldName, listType));
                builderMethods.add(getAccessorMethod(selectedFieldName,
                        listType));

                builder.getImportRegistrationResolver().addImport(HASH_SET);

                final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
                bodyBuilder.appendFormalLine("if ("
                        + selectedFieldName.getSymbolName() + " != null) {");
                bodyBuilder.indent();
                bodyBuilder.appendFormalLine(entityName.getSymbolName()
                        + ".set" + StringUtils.capitalize(fieldName)
                        + "(new HashSet<" + parameterType.getSimpleTypeName()
                        + ">(" + selectedFieldName + "));");
                bodyBuilder.indentRemove();
                bodyBuilder.appendFormalLine("}");
                bodyBuilder.appendFormalLine("this."
                        + selectedFieldName.getSymbolName() + " = "
                        + selectedFieldName.getSymbolName() + ";");
                builderMethods.add(getMutatorMethod(selectedFieldName,
                        listType, bodyBuilder));
            }
            else if (field.getAnnotation(ROO_UPLOADED_FILE) != null) {
                builder.getImportRegistrationResolver().addImports(
                        PRIMEFACES_STREAMED_CONTENT,
                        PRIMEFACES_DEFAULT_STREAMED_CONTENT,
                        BYTE_ARRAY_INPUT_STREAM);

                final String fieldName = field.getFieldName().getSymbolName();
                final JavaSymbolName streamedContentFieldName = new JavaSymbolName(
                        fieldName + "StreamedContent");

                builderMethods.add(getFileUploadListenerMethod(field));

                final AnnotationMetadata annotation = field
                        .getAnnotation(ROO_UPLOADED_FILE);
                final String contentType = (String) annotation.getAttribute(
                        "contentType").getValue();
                final String fileExtension = StringUtils
                        .lowerCase(UploadedFileContentType.getFileExtension(
                                contentType).name());

                final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
                bodyBuilder.appendFormalLine("if ("
                        + entityName.getSymbolName() + " != null && "
                        + entityName.getSymbolName() + ".get"
                        + StringUtils.capitalize(fieldName) + "() != null) {");
                bodyBuilder.indent();
                bodyBuilder
                        .appendFormalLine("return new DefaultStreamedContent(new ByteArrayInputStream("
                                + entityName.getSymbolName()
                                + ".get"
                                + StringUtils.capitalize(fieldName)
                                + "()), \""
                                + contentType
                                + "\", \""
                                + fieldName
                                + "."
                                + fileExtension + "\");");
                bodyBuilder.indentRemove();
                bodyBuilder.appendFormalLine("}");
                bodyBuilder
                        .appendFormalLine("return new DefaultStreamedContent(new ByteArrayInputStream(\"\".getBytes()));");
                builderMethods.add(getAccessorMethod(streamedContentFieldName,
                        PRIMEFACES_STREAMED_CONTENT, bodyBuilder));
            }
        }
    }

    private String getAddChildToComponent(final String componentId,
            final String childComponentId) {
        return componentId + ".getChildren().add(" + childComponentId + ");";
    }

    private String getAddToPanelText(final String componentId) {
        return getAddChildToComponent(HTML_PANEL_GRID_ID, componentId);
    }

    private String getAllowTypeRegex(final String allowedType) {
        final StringBuilder builder = new StringBuilder();
        final char[] value = allowedType.toCharArray();
        for (final char element : value) {
            builder.append("[").append(Character.toLowerCase(element))
                    .append(Character.toUpperCase(element)).append("]");
        }
        if (allowedType.equals(UploadedFileContentType.JPG.name())) {
            builder.append("|[jJ][pP][eE][gG]");
        }
        return builder.toString();
    }

    private String getAutoCcompleteItemLabelValue(final FieldMetadata field,
            final String fieldName) {
        final StringBuilder sb = new StringBuilder();
        @SuppressWarnings("unchecked")
        final List<FieldMetadata> applicationTypeFields = (List<FieldMetadata>) field
                .getCustomData().get(APPLICATION_TYPE_FIELDS_KEY);
        for (final FieldMetadata applicationTypeField : applicationTypeFields) {
            sb.append("#{")
                    .append(fieldName)
                    .append(".")
                    .append(applicationTypeField.getFieldName().getSymbolName())
                    .append("} ");
        }
        return sb.length() > 0 ? sb.toString().trim() : fieldName;
    }

    private MethodMetadataBuilder getAutoCompleteApplicationTypeMethod(
            final FieldMetadata field) {
        final JavaSymbolName methodName = new JavaSymbolName("complete"
                + StringUtils.capitalize(field.getFieldName().getSymbolName()));
        final JavaType parameterType = STRING;
        if (governorHasMethod(methodName, parameterType)) {
            return null;
        }

        builder.getImportRegistrationResolver().addImports(LIST, ARRAY_LIST);

        final List<JavaSymbolName> parameterNames = Arrays
                .asList(new JavaSymbolName("query"));

        @SuppressWarnings("unchecked")
        final Map<MethodMetadataCustomDataKey, MemberTypeAdditions> crudAdditions = (Map<MethodMetadataCustomDataKey, MemberTypeAdditions>) field
                .getCustomData().get(CRUD_ADDITIONS_KEY);
        final MemberTypeAdditions findAllMethod = crudAdditions
                .get(FIND_ALL_METHOD);
        findAllMethod.copyAdditionsTo(builder, governorTypeDetails);
        final String simpleTypeName = field.getFieldType().getSimpleTypeName();

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("List<" + simpleTypeName
                + "> suggestions = new ArrayList<" + simpleTypeName + ">();");
        bodyBuilder.appendFormalLine("for (" + simpleTypeName + " "
                + StringUtils.uncapitalize(simpleTypeName) + " : "
                + findAllMethod.getMethodCall() + ") {");
        bodyBuilder.indent();

        final StringBuilder sb = new StringBuilder();
        @SuppressWarnings("unchecked")
        final List<FieldMetadata> applicationTypeFields = (List<FieldMetadata>) field
                .getCustomData().get(APPLICATION_TYPE_FIELDS_KEY);
        for (int i = 0; i < applicationTypeFields.size(); i++) {
            final JavaSymbolName accessorMethodName = BeanInfoUtils
                    .getAccessorMethodName(applicationTypeFields.get(i));
            if (i > 0) {
                sb.append(" + ").append(" \" \" ").append(" + ");
            }
            sb.append(StringUtils.uncapitalize(simpleTypeName)).append(".")
                    .append(accessorMethodName).append("()");
        }
        bodyBuilder.appendFormalLine("String "
                + StringUtils.uncapitalize(simpleTypeName)
                + "Str = String.valueOf(" + sb.toString().trim() + ");");

        bodyBuilder.appendFormalLine("if ("
                + StringUtils.uncapitalize(simpleTypeName)
                + "Str.toLowerCase().startsWith(query.toLowerCase())) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine("suggestions.add("
                + StringUtils.uncapitalize(simpleTypeName) + ");");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder.appendFormalLine("return suggestions;");

        final JavaType returnType = new JavaType(
                LIST.getFullyQualifiedTypeName(), 0, DataType.TYPE, null,
                Arrays.asList(field.getFieldType()));

        return new MethodMetadataBuilder(getId(), PUBLIC, methodName,
                returnType,
                AnnotatedJavaType.convertFromJavaTypes(parameterType),
                parameterNames, bodyBuilder);
    }

    private MethodMetadataBuilder getAutoCompleteEnumMethod(
            final FieldMetadata autoCompleteField) {
        final JavaSymbolName methodName = new JavaSymbolName("complete"
                + StringUtils.capitalize(autoCompleteField.getFieldName()
                        .getSymbolName()));
        final JavaType parameterType = STRING;
        if (governorHasMethod(methodName, parameterType)) {
            return null;
        }

        builder.getImportRegistrationResolver().addImports(LIST, ARRAY_LIST);

        final List<JavaSymbolName> parameterNames = Arrays
                .asList(new JavaSymbolName("query"));
        final JavaType returnType = new JavaType(
                LIST.getFullyQualifiedTypeName(), 0, DataType.TYPE, null,
                Arrays.asList(autoCompleteField.getFieldType()));

        final String simpleTypeName = autoCompleteField.getFieldType()
                .getSimpleTypeName();
        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("List<" + simpleTypeName
                + "> suggestions = new ArrayList<" + simpleTypeName + ">();");
        bodyBuilder.appendFormalLine("for (" + simpleTypeName + " "
                + StringUtils.uncapitalize(simpleTypeName) + " : "
                + simpleTypeName + ".values()) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine("if ("
                + StringUtils.uncapitalize(simpleTypeName)
                + ".name().toLowerCase().startsWith(query.toLowerCase())) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine("suggestions.add("
                + StringUtils.uncapitalize(simpleTypeName) + ");");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder.appendFormalLine("return suggestions;");

        return new MethodMetadataBuilder(getId(), PUBLIC, methodName,
                returnType,
                AnnotatedJavaType.convertFromJavaTypes(parameterType),
                parameterNames, bodyBuilder);
    }

    private Integer getColumnLength(final FieldMetadata field) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> values = (Map<String, Object>) field
                .getCustomData().get(CustomDataKeys.COLUMN_FIELD);
        if (values != null && values.containsKey("length")) {
            return (Integer) values.get("length");
        }
        return null;
    }

    private String getComponentCreation(final String componentName) {
        return new StringBuilder().append("(").append(componentName)
                .append(") application.createComponent(").append(componentName)
                .append(".COMPONENT_TYPE);").toString();
    }

    private MethodMetadataBuilder getDeleteMethod(
            final MemberTypeAdditions removeMethod) {
        final JavaSymbolName methodName = new JavaSymbolName("delete");
        if (governorHasMethod(methodName)) {
            return null;
        }

        builder.getImportRegistrationResolver().addImports(FACES_MESSAGE,
                FACES_CONTEXT, messageFactory);

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine(removeMethod.getMethodCall() + ";");
        removeMethod.copyAdditionsTo(builder, governorTypeDetails);
        bodyBuilder
                .appendFormalLine("FacesMessage facesMessage = MessageFactory.getMessage(\"message_successfully_deleted\", \""
                        + entity.getSimpleTypeName() + "\");");
        bodyBuilder
                .appendFormalLine("FacesContext.getCurrentInstance().addMessage(null, facesMessage);");
        bodyBuilder.appendFormalLine("reset();");
        bodyBuilder.appendFormalLine("return findAll" + plural + "();");

        return new MethodMetadataBuilder(getId(), PUBLIC, methodName, STRING,
                new ArrayList<AnnotatedJavaType>(),
                new ArrayList<JavaSymbolName>(), bodyBuilder);
    }

    private MethodMetadataBuilder getDisplayCreateDialogMethod() {
        final JavaSymbolName methodName = new JavaSymbolName(
                DISPLAY_CREATE_DIALOG);
        if (governorHasMethod(methodName)) {
            return null;
        }

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine(entityName.getSymbolName() + " = new "
                + entity.getSimpleTypeName() + "();");
        bodyBuilder.appendFormalLine(CREATE_DIALOG_VISIBLE + " = true;");
        bodyBuilder.appendFormalLine("return \"" + entityName.getSymbolName()
                + "\";");
        return getMethod(PUBLIC, methodName, STRING, null, null, bodyBuilder);
    }

    private MethodMetadataBuilder getDisplayListMethod() {
        final JavaSymbolName methodName = new JavaSymbolName(DISPLAY_LIST);
        if (governorHasMethod(methodName)) {
            return null;
        }

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine(CREATE_DIALOG_VISIBLE + " = false;");
        bodyBuilder.appendFormalLine("findAll" + plural + "();");
        bodyBuilder.appendFormalLine("return \"" + entityName.getSymbolName()
                + "\";");
        return getMethod(PUBLIC, methodName, STRING, null, null, bodyBuilder);
    }

    public String getDoubleRangeValdatorString(final String fieldValueId,
            final BigDecimal minValue, final BigDecimal maxValue) {
        builder.getImportRegistrationResolver().addImport(
                DOUBLE_RANGE_VALIDATOR);

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("DoubleRangeValidator " + fieldValueId
                + "Validator = new DoubleRangeValidator();");
        if (minValue != null) {
            bodyBuilder.appendFormalLine(fieldValueId + "Validator.setMinimum("
                    + minValue.doubleValue() + ");");
        }
        if (maxValue != null) {
            bodyBuilder.appendFormalLine(fieldValueId + "Validator.setMaximum("
                    + maxValue.doubleValue() + ");");
        }
        bodyBuilder.appendFormalLine(fieldValueId + ".addValidator("
                + fieldValueId + "Validator);");
        return bodyBuilder.getOutput();
    }

    private MethodMetadataBuilder getEntityAccessorMethod() {
        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("if (" + entityName.getSymbolName()
                + " == null) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine(entityName.getSymbolName() + " = new "
                + entity.getSimpleTypeName() + "();");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder.appendFormalLine("return " + entityName.getSymbolName()
                + ";");
        return getAccessorMethod(entityName, entity, bodyBuilder);
    }

    private MethodMetadataBuilder getFileUploadListenerMethod(
            final FieldMetadata field) {
        final String fieldName = field.getFieldName().getSymbolName();
        final JavaSymbolName methodName = getFileUploadMethodName(fieldName);
        final JavaType parameterType = PRIMEFACES_FILE_UPLOAD_EVENT;
        if (governorHasMethod(methodName, parameterType)) {
            return null;
        }

        builder.getImportRegistrationResolver().addImports(FACES_CONTEXT,
                FACES_MESSAGE, PRIMEFACES_FILE_UPLOAD_EVENT, messageFactory);

        final List<JavaSymbolName> parameterNames = Arrays
                .asList(new JavaSymbolName("event"));

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine(entityName + ".set"
                + StringUtils.capitalize(fieldName)
                + "(event.getFile().getContents());");
        bodyBuilder
                .appendFormalLine("FacesMessage facesMessage = MessageFactory.getMessage(\"message_successfully_uploaded\", event.getFile().getFileName());");
        bodyBuilder
                .appendFormalLine("FacesContext.getCurrentInstance().addMessage(null, facesMessage);");

        return new MethodMetadataBuilder(getId(), PUBLIC, methodName,
                JavaType.VOID_PRIMITIVE,
                AnnotatedJavaType.convertFromJavaTypes(parameterType),
                parameterNames, bodyBuilder);
    }

    private JavaSymbolName getFileUploadMethodName(final String fieldName) {
        return new JavaSymbolName("handleFileUploadFor"
                + StringUtils.capitalize(fieldName));
    }

    private MethodMetadataBuilder getFindAllEntitiesMethod(
            final JavaSymbolName allEntitiesFieldName,
            final MemberTypeAdditions findAllMethod) {
        final JavaSymbolName methodName = new JavaSymbolName("findAll" + plural);
        if (governorHasMethod(methodName)) {
            return null;
        }

        findAllMethod.copyAdditionsTo(builder, governorTypeDetails);

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine(allEntitiesFieldName.getSymbolName()
                + " = " + findAllMethod.getMethodCall() + ";");
        bodyBuilder.appendFormalLine(DATA_VISIBLE + " = !"
                + allEntitiesFieldName.getSymbolName() + ".isEmpty();");
        bodyBuilder.appendFormalLine("return null;");

        return new MethodMetadataBuilder(getId(), PUBLIC, methodName,
                JavaType.STRING, new ArrayList<AnnotatedJavaType>(),
                new ArrayList<JavaSymbolName>(), bodyBuilder);
    }

    private MethodMetadataBuilder getHandleDialogCloseMethod() {
        final JavaSymbolName methodName = new JavaSymbolName(
                "handleDialogClose");
        final JavaType parameterType = PRIMEFACES_CLOSE_EVENT;
        if (governorHasMethod(methodName, parameterType)) {
            return null;
        }

        builder.getImportRegistrationResolver().addImport(
                PRIMEFACES_CLOSE_EVENT);

        final List<JavaSymbolName> parameterNames = Arrays
                .asList(new JavaSymbolName("event"));

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("reset();");

        return new MethodMetadataBuilder(getId(), PUBLIC, methodName,
                VOID_PRIMITIVE,
                AnnotatedJavaType.convertFromJavaTypes(parameterType),
                parameterNames, bodyBuilder);
    }

    private MethodMetadataBuilder getInitMethod(
            final MethodMetadata identifierAccessor) {
        final JavaSymbolName methodName = new JavaSymbolName("init");
        if (governorHasMethod(methodName)) {
            return null;
        }

        builder.getImportRegistrationResolver().addImport(ARRAY_LIST);

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("columns = new ArrayList<String>();");
        for (final FieldMetadata field : locatedFields) {
            if (field.getCustomData().keySet().contains(LIST_VIEW_FIELD_KEY)) {
                bodyBuilder.appendFormalLine("columns.add(\""
                        + field.getFieldName().getSymbolName() + "\");");
            }
        }

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), PUBLIC, methodName, JavaType.VOID_PRIMITIVE,
                new ArrayList<AnnotatedJavaType>(),
                new ArrayList<JavaSymbolName>(), bodyBuilder);
        methodBuilder.addAnnotation(new AnnotationMetadataBuilder(
                POST_CONSTRUCT));
        return methodBuilder;
    }

    public String getLengthValdatorString(final String fieldValueId,
            final Number minValue, final Number maxValue) {
        builder.getImportRegistrationResolver().addImport(LENGTH_VALIDATOR);

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("LengthValidator " + fieldValueId
                + "Validator = new LengthValidator();");
        if (minValue != null) {
            bodyBuilder.appendFormalLine(fieldValueId + "Validator.setMinimum("
                    + minValue.intValue() + ");");
        }
        if (maxValue != null) {
            bodyBuilder.appendFormalLine(fieldValueId + "Validator.setMaximum("
                    + maxValue.intValue() + ");");
        }
        bodyBuilder.appendFormalLine(fieldValueId + ".addValidator("
                + fieldValueId + "Validator);");
        return bodyBuilder.getOutput();
    }

    private JavaType getListType(final JavaType parameterType) {
        return new JavaType(LIST.getFullyQualifiedTypeName(), 0, DataType.TYPE,
                null, Arrays.asList(parameterType));
    }

    public String getLongRangeValdatorString(final String fieldValueId,
            final BigDecimal minValue, final BigDecimal maxValue) {
        builder.getImportRegistrationResolver().addImport(LONG_RANGE_VALIDATOR);

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("LongRangeValidator " + fieldValueId
                + "Validator = new LongRangeValidator();");
        if (minValue != null) {
            bodyBuilder.appendFormalLine(fieldValueId + "Validator.setMinimum("
                    + minValue.longValue() + ");");
        }
        if (maxValue != null) {
            bodyBuilder.appendFormalLine(fieldValueId + "Validator.setMaximum("
                    + maxValue.longValue() + ");");
        }
        bodyBuilder.appendFormalLine(fieldValueId + ".addValidator("
                + fieldValueId + "Validator);");
        return bodyBuilder.getOutput();
    }

    private AnnotationMetadata getManagedBeanAnnotation(final String beanName) {
        final AnnotationMetadata annotation = getTypeAnnotation(MANAGED_BEAN);
        if (annotation == null) {
            return null;
        }
        final AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(
                annotation);
        annotationBuilder.addStringAttribute("name", beanName);
        return annotationBuilder.build();
    }

    private BigDecimal getMinOrMaxValue(final FieldMetadata field,
            final JavaType annotationType) {
        final AnnotationMetadata annotation = MemberFindingUtils
                .getAnnotationOfType(field.getAnnotations(), annotationType);
        if (annotation != null
                && annotation.getAttribute(new JavaSymbolName("value")) != null) {
            return new BigDecimal(String.valueOf(annotation.getAttribute(
                    new JavaSymbolName("value")).getValue()));
        }
        return null;
    }

    private MethodMetadataBuilder getOnEditMethod() {
        final JavaSymbolName methodName = new JavaSymbolName("onEdit");
        if (governorHasMethod(methodName)) {
            return null;
        }

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        for (final FieldMetadata field : locatedFields) {
            final CustomData customData = field.getCustomData();
            if (!customData.keySet().contains(PARAMETER_TYPE_KEY)) {
                continue;
            }

            builder.getImportRegistrationResolver().addImport(ARRAY_LIST);

            final String fieldName = field.getFieldName().getSymbolName();
            final JavaType parameterType = (JavaType) customData
                    .get(PARAMETER_TYPE_KEY);
            final String entityAccessorMethodCall = entityName.getSymbolName()
                    + ".get" + StringUtils.capitalize(fieldName) + "()";

            bodyBuilder
                    .appendFormalLine("if (" + entityName.getSymbolName()
                            + " != null && " + entityAccessorMethodCall
                            + " != null) {");
            bodyBuilder.indent();
            bodyBuilder.appendFormalLine(getSelectedFieldName(fieldName)
                    + " = new ArrayList<" + parameterType.getSimpleTypeName()
                    + ">(" + entityAccessorMethodCall + ");");
            bodyBuilder.indentRemove();
            bodyBuilder.appendFormalLine("}");
        }
        bodyBuilder.appendFormalLine("return null;");

        return new MethodMetadataBuilder(getId(), PUBLIC, methodName,
                JavaType.STRING, new ArrayList<AnnotatedJavaType>(),
                new ArrayList<JavaSymbolName>(), bodyBuilder);
    }

    private MethodMetadataBuilder getPanelGridAccessorMethod(final Action action) {
        final String fieldName = StringUtils.lowerCase(action.name())
                + "PanelGrid";
        final JavaSymbolName methodName = BeanInfoUtils.getAccessorMethodName(
                new JavaSymbolName(fieldName), HTML_PANEL_GRID);
        if (governorHasMethod(methodName)) {
            return null;
        }

        builder.getImportRegistrationResolver().addImport(HTML_PANEL_GRID);

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        switch (action) {
        case CREATE:
            bodyBuilder.appendFormalLine("if (" + fieldName + " == null) {");
            bodyBuilder.indent();
            bodyBuilder.appendFormalLine(fieldName
                    + " = populateCreatePanel();");
            bodyBuilder.indentRemove();
            bodyBuilder.appendFormalLine("}");
            bodyBuilder.appendFormalLine("return " + fieldName + ";");
            break;
        case EDIT:
            bodyBuilder.appendFormalLine("if (" + fieldName + " == null) {");
            bodyBuilder.indent();
            bodyBuilder.appendFormalLine(fieldName + " = populateEditPanel();");
            bodyBuilder.indentRemove();
            bodyBuilder.appendFormalLine("}");
            bodyBuilder.appendFormalLine("return " + fieldName + ";");
            break;
        default:
            bodyBuilder.appendFormalLine("return populateViewPanel();");
            break;
        }

        return new MethodMetadataBuilder(getId(), PUBLIC, methodName,
                HTML_PANEL_GRID, new ArrayList<AnnotatedJavaType>(),
                new ArrayList<JavaSymbolName>(), bodyBuilder);
    }

    private FieldMetadataBuilder getPanelGridField(final Action panelType) {
        return getField(
                new JavaSymbolName(StringUtils.lowerCase(panelType.name())
                        + "PanelGrid"), HTML_PANEL_GRID);
    }

    private MethodMetadataBuilder getPanelGridMutatorMethod(final Action action) {
        return getMutatorMethod(
                new JavaSymbolName(StringUtils.lowerCase(action.name())
                        + "PanelGrid"), HTML_PANEL_GRID);
    }

    private MethodMetadataBuilder getPersistMethod(
            final MemberTypeAdditions mergeMethod,
            final MemberTypeAdditions persistMethod,
            final MethodMetadata identifierAccessor) {
        final JavaSymbolName methodName = new JavaSymbolName("persist");
        if (governorHasMethod(methodName)) {
            return null;
        }

        builder.getImportRegistrationResolver().addImports(FACES_MESSAGE,
                PRIMEFACES_REQUEST_CONTEXT, FACES_CONTEXT, messageFactory);

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("String message = \"\";");
        bodyBuilder.appendFormalLine("if (" + entityName.getSymbolName() + "."
                + identifierAccessor.getMethodName().getSymbolName()
                + "() != null) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine(mergeMethod.getMethodCall() + ";");
        mergeMethod.copyAdditionsTo(builder, governorTypeDetails);
        bodyBuilder
                .appendFormalLine("message = \"message_successfully_updated\";");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("} else {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine(persistMethod.getMethodCall() + ";");
        persistMethod.copyAdditionsTo(builder, governorTypeDetails);
        bodyBuilder
                .appendFormalLine("message = \"message_successfully_created\";");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder
                .appendFormalLine("RequestContext context = RequestContext.getCurrentInstance();");
        bodyBuilder
                .appendFormalLine("context.execute(\"createDialogWidget.hide()\");");
        bodyBuilder
                .appendFormalLine("context.execute(\"editDialogWidget.hide()\");");
        bodyBuilder.appendFormalLine("");
        bodyBuilder
                .appendFormalLine("FacesMessage facesMessage = MessageFactory.getMessage(message, \""
                        + entity.getSimpleTypeName() + "\");");
        bodyBuilder
                .appendFormalLine("FacesContext.getCurrentInstance().addMessage(null, facesMessage);");
        bodyBuilder.appendFormalLine("reset();");
        bodyBuilder.appendFormalLine("return findAll" + plural + "();");

        return new MethodMetadataBuilder(getId(), PUBLIC, methodName, STRING,
                new ArrayList<AnnotatedJavaType>(),
                new ArrayList<JavaSymbolName>(), bodyBuilder);
    }

    private MethodMetadataBuilder getPopulatePanelMethod(final Action action) {
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

        if (governorHasMethod(methodName)) {
            return null;
        }

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId());
        methodBuilder.setModifier(PUBLIC);
        methodBuilder.setMethodName(methodName);
        methodBuilder.setReturnType(HTML_PANEL_GRID);
        methodBuilder.setParameterTypes(new ArrayList<AnnotatedJavaType>());
        methodBuilder.setParameterNames(new ArrayList<JavaSymbolName>());

        builder.getImportRegistrationResolver().addImports(FACES_CONTEXT,
                HTML_PANEL_GRID);

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder
                .appendFormalLine("FacesContext facesContext = FacesContext.getCurrentInstance();");
        bodyBuilder.appendFormalLine(APPLICATION.getFullyQualifiedTypeName()
                + " application = facesContext.getApplication();");

        if (locatedFields.isEmpty()) {
            bodyBuilder.appendFormalLine("return "
                    + getComponentCreation("HtmlPanelGrid"));
            methodBuilder.setBodyBuilder(bodyBuilder);
            return methodBuilder;
        }

        builder.getImportRegistrationResolver().addImports(EL_CONTEXT,
                EXPRESSION_FACTORY, HTML_OUTPUT_TEXT, PRIMEFACES_OUTPUT_LABEL);

        bodyBuilder
                .appendFormalLine("ExpressionFactory expressionFactory = application.getExpressionFactory();");
        bodyBuilder
                .appendFormalLine("ELContext elContext = facesContext.getELContext();");
        bodyBuilder.appendFormalLine("");
        bodyBuilder.appendFormalLine("HtmlPanelGrid " + HTML_PANEL_GRID_ID
                + " = " + getComponentCreation("HtmlPanelGrid"));
        bodyBuilder.appendFormalLine("");

        for (final FieldMetadata field : locatedFields) {
            final CustomData customData = field.getCustomData();
            final JavaType fieldType = field.getFieldType();
            final String simpleTypeName = fieldType.getSimpleTypeName();
            final String fieldName = field.getFieldName().getSymbolName();
            final String fieldLabelId = fieldName + suffix1;
            final String fieldValueId = fieldName + suffix2;

            final BigDecimal minValue = ObjectUtils.max(
                    getMinOrMaxValue(field, MIN),
                    getMinOrMaxValue(field, DECIMAL_MIN));

            final BigDecimal maxValue = ObjectUtils.min(
                    getMinOrMaxValue(field, MAX),
                    getMinOrMaxValue(field, DECIMAL_MAX));

            final Integer sizeMinValue = getSizeMinOrMax(field, "min");

            final Integer min = ObjectUtils.min(getSizeMinOrMax(field, "max"),
                    getColumnLength(field));
            final BigDecimal sizeMaxValue = min != null ? new BigDecimal(min)
                    : null;

            final boolean required = action != Action.VIEW
                    && (!isNullable(field) || minValue != null
                            || maxValue != null || sizeMinValue != null || sizeMaxValue != null);
            final boolean isTextarea = sizeMinValue != null
                    && sizeMinValue.intValue() > 30 || sizeMaxValue != null
                    && sizeMaxValue.intValue() > 30
                    || customData.keySet().contains(CustomDataKeys.LOB_FIELD);

            final boolean isUIComponent = isUIComponent(field, fieldType,
                    customData);

            // Field label
            if (action.equals(Action.VIEW) || !isUIComponent) {
                bodyBuilder.appendFormalLine("HtmlOutputText " + fieldLabelId
                        + " = " + getComponentCreation("HtmlOutputText"));
            }
            else {
                bodyBuilder.appendFormalLine("OutputLabel " + fieldLabelId
                        + " = " + getComponentCreation("OutputLabel"));
                bodyBuilder.appendFormalLine(fieldLabelId + ".setFor(\""
                        + fieldValueId + "\");");
            }
            bodyBuilder.appendFormalLine(fieldLabelId + ".setId(\""
                    + fieldLabelId + "\");");
            bodyBuilder.appendFormalLine(fieldLabelId + ".setValue(\""
                    + field.getFieldName().getReadableSymbolName() + ":\");");
            bodyBuilder.appendFormalLine(getAddToPanelText(fieldLabelId));
            bodyBuilder.appendFormalLine("");

            // Field value
            final String converterName = fieldValueId + "Converter";
            final String htmlOutputTextStr = "HtmlOutputText " + fieldValueId
                    + " = " + getComponentCreation("HtmlOutputText");
            final String inputTextStr = "InputText " + fieldValueId + " = "
                    + getComponentCreation("InputText");
            final String componentIdStr = fieldValueId + ".setId(\""
                    + fieldValueId + "\");";
            final String requiredStr = fieldValueId + ".setRequired("
                    + required + ");";

            if (field.getAnnotation(ROO_UPLOADED_FILE) != null) {
                final AnnotationMetadata annotation = field
                        .getAnnotation(ROO_UPLOADED_FILE);
                final String contentType = (String) annotation.getAttribute(
                        "contentType").getValue();
                final String allowedType = UploadedFileContentType
                        .getFileExtension(contentType).name();
                if (action == Action.VIEW) {
                    builder.getImportRegistrationResolver().addImports(
                            UI_COMPONENT,
                            PRIMEFACES_FILE_DOWNLOAD_ACTION_LISTENER,
                            PRIMEFACES_COMMAND_BUTTON,
                            PRIMEFACES_STREAMED_CONTENT);

                    // bodyBuilder.appendFormalLine("CommandButton " +
                    // fieldValueId + " = " +
                    // getComponentCreation("CommandButton"));
                    // bodyBuilder.appendFormalLine(fieldValueId +
                    // ".addActionListener(new FileDownloadActionListener(expressionFactory.createValueExpression(elContext, \"#{"
                    // + beanName + "." +
                    // fieldName +
                    // "StreamedContent}\", StreamedContent.class), null));");
                    // bodyBuilder.appendFormalLine(fieldValueId +
                    // ".setValue(\"Download\");");
                    // bodyBuilder.appendFormalLine(fieldValueId +
                    // ".setAjax(false);");

                    // TODO Make following code work as currently the view panel
                    // is not refreshed and the download field is always seen as
                    // null
                    bodyBuilder.appendFormalLine("UIComponent " + fieldValueId
                            + ";");
                    bodyBuilder.appendFormalLine("if (" + entityName
                            + " != null && " + entityName + ".get"
                            + StringUtils.capitalize(fieldName)
                            + "() != null && " + entityName + ".get"
                            + StringUtils.capitalize(fieldName)
                            + "().length > 0) {");
                    bodyBuilder.indent();
                    bodyBuilder.appendFormalLine(fieldValueId + " = "
                            + getComponentCreation("CommandButton"));
                    bodyBuilder
                            .appendFormalLine("((CommandButton) "
                                    + fieldValueId
                                    + ").addActionListener(new FileDownloadActionListener(expressionFactory.createValueExpression(elContext, \"#{"
                                    + beanName
                                    + "."
                                    + fieldName
                                    + "StreamedContent}\", StreamedContent.class), null));");
                    bodyBuilder.appendFormalLine("((CommandButton) "
                            + fieldValueId + ").setValue(\"Download\");");
                    bodyBuilder.appendFormalLine("((CommandButton) "
                            + fieldValueId + ").setAjax(false);");
                    bodyBuilder.indentRemove();
                    bodyBuilder.appendFormalLine("} else {");
                    bodyBuilder.indent();
                    bodyBuilder.appendFormalLine(fieldValueId + " = "
                            + getComponentCreation("HtmlOutputText"));
                    bodyBuilder.appendFormalLine("((HtmlOutputText) "
                            + fieldValueId + ").setValue(\"\");");
                    bodyBuilder.indentRemove();
                    bodyBuilder.appendFormalLine("}");
                }
                else {
                    builder.getImportRegistrationResolver().addImports(
                            PRIMEFACES_FILE_UPLOAD,
                            PRIMEFACES_FILE_UPLOAD_EVENT);

                    bodyBuilder.appendFormalLine("FileUpload " + fieldValueId
                            + " = " + getComponentCreation("FileUpload"));
                    bodyBuilder.appendFormalLine(componentIdStr);
                    bodyBuilder
                            .appendFormalLine(fieldValueId
                                    + ".setFileUploadListener(expressionFactory.createMethodExpression(elContext, \"#{"
                                    + beanName
                                    + "."
                                    + getFileUploadMethodName(fieldName)
                                    + "}\", void.class, new Class[] { FileUploadEvent.class }));");
                    bodyBuilder.appendFormalLine(fieldValueId
                            + ".setMode(\"advanced\");");
                    bodyBuilder.appendFormalLine(fieldValueId
                            + ".setAllowTypes(\"/(\\\\.|\\\\/)("
                            + getAllowTypeRegex(allowedType) + ")$/\");");
                    bodyBuilder.appendFormalLine(fieldValueId
                            + ".setUpdate(\":growlForm:growl\");");

                    final AnnotationAttributeValue<?> autoUploadAttr = annotation
                            .getAttribute("autoUpload");
                    if (autoUploadAttr != null
                            && (Boolean) autoUploadAttr.getValue()) {
                        bodyBuilder.appendFormalLine(fieldValueId
                                + ".setAuto(true);");
                    }
                    bodyBuilder.appendFormalLine(requiredStr);
                }
            }
            else if (fieldType.equals(BOOLEAN_OBJECT)
                    || fieldType.equals(BOOLEAN_PRIMITIVE)) {
                if (action == Action.VIEW) {
                    bodyBuilder.appendFormalLine(htmlOutputTextStr);
                    bodyBuilder.appendFormalLine(getSetValueExpression(
                            fieldValueId, fieldName));
                }
                else {
                    builder.getImportRegistrationResolver().addImport(
                            PRIMEFACES_SELECT_BOOLEAN_CHECKBOX);
                    bodyBuilder.appendFormalLine("SelectBooleanCheckbox "
                            + fieldValueId + " = "
                            + getComponentCreation("SelectBooleanCheckbox"));
                    bodyBuilder.appendFormalLine(componentIdStr);
                    bodyBuilder.appendFormalLine(getSetValueExpression(
                            fieldValueId, fieldName, simpleTypeName));
                    bodyBuilder.appendFormalLine(requiredStr);
                }
            }
            else if (customData.keySet().contains(ENUMERATED_KEY)) {
                if (action == Action.VIEW) {
                    bodyBuilder.appendFormalLine(htmlOutputTextStr);
                    bodyBuilder.appendFormalLine(getSetValueExpression(
                            fieldValueId, fieldName));
                }
                else {
                    builder.getImportRegistrationResolver().addImports(
                            PRIMEFACES_AUTO_COMPLETE, fieldType);

                    bodyBuilder.appendFormalLine("AutoComplete " + fieldValueId
                            + " = " + getComponentCreation("AutoComplete"));
                    bodyBuilder.appendFormalLine(componentIdStr);
                    bodyBuilder.appendFormalLine(getSetValueExpression(
                            fieldValueId, fieldName, simpleTypeName));
                    bodyBuilder.appendFormalLine(getSetCompleteMethod(
                            fieldValueId, fieldName));
                    bodyBuilder.appendFormalLine(fieldValueId
                            + ".setDropdown(true);");
                    bodyBuilder.appendFormalLine(requiredStr);
                }
            }
            else if (JdkJavaType.isDateField(fieldType)) {
                if (action == Action.VIEW) {
                    builder.getImportRegistrationResolver().addImport(
                            DATE_TIME_CONVERTER);

                    bodyBuilder.appendFormalLine(htmlOutputTextStr);
                    bodyBuilder.appendFormalLine(getSetValueExpression(
                            fieldValueId, fieldName, simpleTypeName));
                    bodyBuilder
                            .appendFormalLine("DateTimeConverter "
                                    + converterName
                                    + " = (DateTimeConverter) application.createConverter(DateTimeConverter.CONVERTER_ID);");
                    // TODO Get working:
                    // bodyBuilder.appendFormalLine(converterName +
                    // ".setPattern(((SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT)).toPattern());");
                    bodyBuilder.appendFormalLine(converterName
                            + ".setPattern(\"dd/MM/yyyy\");");
                    bodyBuilder.appendFormalLine(fieldValueId
                            + ".setConverter(" + converterName + ");");
                }
                else {
                    builder.getImportRegistrationResolver().addImports(
                            PRIMEFACES_CALENDAR, DATE);
                    // builder.getImportRegistrationResolver().addImports(DATE_FORMAT,
                    // SIMPLE_DATE_FORMAT);

                    bodyBuilder.appendFormalLine("Calendar " + fieldValueId
                            + " = " + getComponentCreation("Calendar"));
                    bodyBuilder.appendFormalLine(componentIdStr);
                    bodyBuilder.appendFormalLine(getSetValueExpression(
                            fieldValueId, fieldName, "Date"));
                    bodyBuilder.appendFormalLine(fieldValueId
                            + ".setNavigator(true);");
                    bodyBuilder.appendFormalLine(fieldValueId
                            + ".setEffect(\"slideDown\");");
                    // TODO Get working:
                    // bodyBuilder.appendFormalLine(fieldValueId +
                    // ".setPattern(((SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT)).toPattern());");
                    bodyBuilder.appendFormalLine(fieldValueId
                            + ".setPattern(\"dd/MM/yyyy\");");
                    bodyBuilder.appendFormalLine(requiredStr);
                    if (MemberFindingUtils.getAnnotationOfType(
                            field.getAnnotations(), PAST) != null) {
                        bodyBuilder.appendFormalLine(fieldValueId
                                + ".setMaxdate(new Date());");
                    }
                    if (MemberFindingUtils.getAnnotationOfType(
                            field.getAnnotations(), FUTURE) != null) {
                        bodyBuilder.appendFormalLine(fieldValueId
                                + ".setMindate(new Date());");
                    }
                }
            }
            else if (JdkJavaType.isIntegerType(fieldType)) {
                if (action == Action.VIEW) {
                    bodyBuilder.appendFormalLine(htmlOutputTextStr);
                    bodyBuilder.appendFormalLine(getSetValueExpression(
                            fieldValueId, fieldName));
                }
                else {
                    builder.getImportRegistrationResolver().addImports(
                            PRIMEFACES_INPUT_TEXT, PRIMEFACES_SPINNER);
                    if (fieldType.equals(JdkJavaType.BIG_INTEGER)) {
                        builder.getImportRegistrationResolver().addImport(
                                fieldType);
                    }

                    bodyBuilder.appendFormalLine("Spinner " + fieldValueId
                            + " = " + getComponentCreation("Spinner"));
                    bodyBuilder.appendFormalLine(componentIdStr);
                    bodyBuilder.appendFormalLine(getSetValueExpression(
                            fieldValueId, fieldName, simpleTypeName));
                    bodyBuilder.appendFormalLine(requiredStr);
                    if (minValue != null || maxValue != null) {
                        if (minValue != null) {
                            bodyBuilder.appendFormalLine(fieldValueId
                                    + ".setMin(" + minValue.doubleValue()
                                    + ");");
                        }
                        if (maxValue != null) {
                            bodyBuilder.appendFormalLine(fieldValueId
                                    + ".setMax(" + maxValue.doubleValue()
                                    + ");");
                        }
                        bodyBuilder.append(getLongRangeValdatorString(
                                fieldValueId, minValue, maxValue));
                    }
                    bodyBuilder.appendFormalLine("");
                }
            }
            else if (JdkJavaType.isDecimalType(fieldType)) {
                if (action == Action.VIEW) {
                    bodyBuilder.appendFormalLine(htmlOutputTextStr);
                    bodyBuilder.appendFormalLine(getSetValueExpression(
                            fieldValueId, fieldName));
                }
                else {
                    builder.getImportRegistrationResolver().addImport(
                            PRIMEFACES_INPUT_TEXT);
                    if (fieldType.equals(JdkJavaType.BIG_DECIMAL)) {
                        builder.getImportRegistrationResolver().addImport(
                                fieldType);
                    }

                    bodyBuilder.appendFormalLine(inputTextStr);
                    bodyBuilder.appendFormalLine(componentIdStr);
                    bodyBuilder.appendFormalLine(getSetValueExpression(
                            fieldValueId, fieldName, simpleTypeName));
                    bodyBuilder.appendFormalLine(requiredStr);
                    if (minValue != null || maxValue != null) {
                        bodyBuilder.append(getDoubleRangeValdatorString(
                                fieldValueId, minValue, maxValue));
                    }
                }
            }
            else if (fieldType.equals(STRING)) {
                if (isTextarea) {
                    builder.getImportRegistrationResolver().addImport(
                            PRIMEFACES_INPUT_TEXTAREA);
                    bodyBuilder.appendFormalLine("InputTextarea "
                            + fieldValueId + " = "
                            + getComponentCreation("InputTextarea"));
                }
                else {
                    if (action == Action.VIEW) {
                        bodyBuilder.appendFormalLine(htmlOutputTextStr);
                    }
                    else {
                        builder.getImportRegistrationResolver().addImport(
                                PRIMEFACES_INPUT_TEXT);
                        bodyBuilder.appendFormalLine(inputTextStr);
                    }
                }

                bodyBuilder.appendFormalLine(componentIdStr);
                bodyBuilder.appendFormalLine(getSetValueExpression(
                        fieldValueId, fieldName));
                if (action == Action.VIEW) {
                    if (isTextarea) {
                        bodyBuilder.appendFormalLine(fieldValueId
                                + ".setReadonly(true);");
                        bodyBuilder.appendFormalLine(fieldValueId
                                + ".setDisabled(true);");
                    }
                }
                else {
                    if (sizeMinValue != null || sizeMaxValue != null) {
                        bodyBuilder.append(getLengthValdatorString(
                                fieldValueId, sizeMinValue, sizeMaxValue));
                    }
                    setRegexPatternValidationString(field, fieldValueId,
                            bodyBuilder);
                    bodyBuilder.appendFormalLine(requiredStr);
                }
            }
            else if (customData.keySet().contains(PARAMETER_TYPE_KEY)) {
                final JavaType parameterType = (JavaType) customData
                        .get(PARAMETER_TYPE_KEY);
                final String parameterTypeSimpleTypeName = parameterType
                        .getSimpleTypeName();
                final String parameterTypeFieldName = StringUtils
                        .uncapitalize(parameterTypeSimpleTypeName);
                final String parameterTypeManagedBeanName = (String) customData
                        .get(PARAMETER_TYPE_MANAGED_BEAN_NAME_KEY);
                final String parameterTypePlural = (String) customData
                        .get(PARAMETER_TYPE_PLURAL_KEY);

                if (StringUtils.isNotBlank(parameterTypeManagedBeanName)) {
                    if (customData.keySet().contains(ONE_TO_MANY_FIELD)
                            || customData.keySet().contains(MANY_TO_MANY_FIELD)
                            && isInverseSideOfRelationship(field, ONE_TO_MANY,
                                    MANY_TO_MANY)) {
                        bodyBuilder.appendFormalLine(htmlOutputTextStr);
                        bodyBuilder.appendFormalLine(componentIdStr);
                        bodyBuilder
                                .appendFormalLine(fieldValueId
                                        + ".setValue(\"This relationship is managed from the "
                                        + parameterTypeSimpleTypeName
                                        + " side\");");
                    }
                    else {
                        final JavaType converterType = new JavaType(destination
                                .getPackage().getFullyQualifiedPackageName()
                                + ".converter."
                                + parameterTypeSimpleTypeName
                                + "Converter");
                        builder.getImportRegistrationResolver().addImports(
                                PRIMEFACES_SELECT_MANY_MENU, UI_SELECT_ITEMS,
                                fieldType, converterType);

                        bodyBuilder.appendFormalLine("SelectManyMenu "
                                + fieldValueId + " = "
                                + getComponentCreation("SelectManyMenu"));
                        bodyBuilder.appendFormalLine(componentIdStr);
                        bodyBuilder.appendFormalLine(fieldValueId
                                + ".setConverter(new "
                                + converterType.getSimpleTypeName() + "());");
                        bodyBuilder
                                .appendFormalLine(fieldValueId
                                        + ".setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{"
                                        + beanName + "."
                                        + getSelectedFieldName(fieldName)
                                        + "}\", List.class));");
                        bodyBuilder
                                .appendFormalLine("UISelectItems "
                                        + fieldValueId
                                        + "Items = (UISelectItems) application.createComponent(UISelectItems.COMPONENT_TYPE);");
                        if (action == Action.VIEW) {
                            bodyBuilder.appendFormalLine(fieldValueId
                                    + ".setReadonly(true);");
                            bodyBuilder.appendFormalLine(fieldValueId
                                    + ".setDisabled(true);");
                            bodyBuilder
                                    .appendFormalLine(fieldValueId
                                            + "Items.setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{"
                                            + beanName + "."
                                            + entityName.getSymbolName() + "."
                                            + fieldName + "}\", "
                                            + simpleTypeName + ".class));");
                        }
                        else {
                            bodyBuilder
                                    .appendFormalLine(fieldValueId
                                            + "Items.setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{"
                                            + parameterTypeManagedBeanName
                                            + ".all"
                                            + StringUtils
                                                    .capitalize(parameterTypePlural)
                                            + "}\", List.class));");
                            bodyBuilder.appendFormalLine(requiredStr);
                        }
                        bodyBuilder
                                .appendFormalLine(fieldValueId
                                        + "Items.setValueExpression(\"var\", expressionFactory.createValueExpression(elContext, \""
                                        + parameterTypeFieldName
                                        + "\", String.class));");
                        bodyBuilder
                                .appendFormalLine(fieldValueId
                                        + "Items.setValueExpression(\"itemLabel\", expressionFactory.createValueExpression(elContext, \"#{"
                                        + parameterTypeFieldName
                                        + "}\", String.class));");
                        bodyBuilder
                                .appendFormalLine(fieldValueId
                                        + "Items.setValueExpression(\"itemValue\", expressionFactory.createValueExpression(elContext, \"#{"
                                        + parameterTypeFieldName + "}\", "
                                        + parameterTypeSimpleTypeName
                                        + ".class));");
                        bodyBuilder.appendFormalLine(getAddChildToComponent(
                                fieldValueId, fieldValueId + "Items"));
                    }
                }
                else {
                    // Parameter type is an enum
                    bodyBuilder.appendFormalLine("SelectManyMenu "
                            + fieldValueId + " = "
                            + getComponentCreation("SelectManyMenu"));
                    bodyBuilder.appendFormalLine(componentIdStr);
                    if (action == Action.VIEW) {
                        bodyBuilder.appendFormalLine(fieldValueId
                                + ".setReadonly(true);");
                        bodyBuilder.appendFormalLine(fieldValueId
                                + ".setDisabled(true);");
                        bodyBuilder
                                .appendFormalLine(fieldValueId
                                        + ".setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{"
                                        + beanName + "."
                                        + getSelectedFieldName(fieldName)
                                        + "}\", List.class));");
                        bodyBuilder
                                .appendFormalLine("UISelectItems "
                                        + fieldValueId
                                        + "Items = (UISelectItems) application.createComponent(UISelectItems.COMPONENT_TYPE);");
                        bodyBuilder
                                .appendFormalLine(fieldValueId
                                        + "Items.setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{"
                                        + beanName + "."
                                        + entityName.getSymbolName() + "."
                                        + fieldName + "}\", " + simpleTypeName
                                        + ".class));");
                        bodyBuilder
                                .appendFormalLine(fieldValueId
                                        + "Items.setValueExpression(\"var\", expressionFactory.createValueExpression(elContext, \""
                                        + parameterTypeFieldName
                                        + "\", String.class));");
                        bodyBuilder
                                .appendFormalLine(fieldValueId
                                        + "Items.setValueExpression(\"itemLabel\", expressionFactory.createValueExpression(elContext, \"#{"
                                        + parameterTypeFieldName
                                        + "}\", String.class));");
                        bodyBuilder
                                .appendFormalLine(fieldValueId
                                        + "Items.setValueExpression(\"itemValue\", expressionFactory.createValueExpression(elContext, \"#{"
                                        + parameterTypeFieldName + "}\", "
                                        + parameterTypeSimpleTypeName
                                        + ".class));");
                        bodyBuilder.appendFormalLine(getAddChildToComponent(
                                fieldValueId, fieldValueId + "Items"));
                    }
                    else {
                        builder.getImportRegistrationResolver().addImports(
                                UI_SELECT_ITEM, ENUM_CONVERTER);

                        bodyBuilder
                                .appendFormalLine(fieldValueId
                                        + ".setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{"
                                        + beanName + "."
                                        + getSelectedFieldName(fieldName)
                                        + "}\", List.class));");
                        bodyBuilder.appendFormalLine(fieldValueId
                                + ".setConverter(new EnumConverter("
                                + parameterTypeSimpleTypeName + ".class));");
                        bodyBuilder.appendFormalLine(requiredStr);
                        bodyBuilder.appendFormalLine("UISelectItem "
                                + fieldValueId + "Item;");
                        bodyBuilder
                                .appendFormalLine("for ("
                                        + parameterTypeSimpleTypeName
                                        + " "
                                        + StringUtils
                                                .uncapitalize(parameterTypeSimpleTypeName)
                                        + " : " + parameterTypeSimpleTypeName
                                        + ".values()) {");
                        bodyBuilder.indent();
                        bodyBuilder
                                .appendFormalLine(fieldValueId
                                        + "Item = (UISelectItem) application.createComponent(UISelectItem.COMPONENT_TYPE);");
                        bodyBuilder
                                .appendFormalLine(fieldValueId
                                        + "Item.setItemLabel("
                                        + StringUtils
                                                .uncapitalize(parameterTypeSimpleTypeName)
                                        + ".name());");
                        bodyBuilder
                                .appendFormalLine(fieldValueId
                                        + "Item.setItemValue("
                                        + StringUtils
                                                .uncapitalize(parameterTypeSimpleTypeName)
                                        + ");");
                        bodyBuilder.appendFormalLine(getAddChildToComponent(
                                fieldValueId, fieldValueId + "Item"));
                        bodyBuilder.indentRemove();
                        bodyBuilder.appendFormalLine("}");
                    }
                }
            }
            else if (customData.keySet().contains(APPLICATION_TYPE_KEY)) {
                if (customData.keySet().contains(ONE_TO_ONE_FIELD)
                        && isInverseSideOfRelationship(field, ONE_TO_ONE)) {
                    bodyBuilder.appendFormalLine(htmlOutputTextStr);
                    bodyBuilder.appendFormalLine(componentIdStr);
                    bodyBuilder
                            .appendFormalLine(fieldValueId
                                    + ".setValue(\"This relationship is managed from the "
                                    + simpleTypeName + " side\");");
                }
                else {
                    final JavaType converterType = new JavaType(destination
                            .getPackage().getFullyQualifiedPackageName()
                            + ".converter." + simpleTypeName + "Converter");
                    builder.getImportRegistrationResolver().addImport(
                            converterType);
                    if (action == Action.VIEW) {
                        bodyBuilder.appendFormalLine(htmlOutputTextStr);
                        bodyBuilder.appendFormalLine(getSetValueExpression(
                                fieldValueId, fieldName, simpleTypeName));
                        bodyBuilder.appendFormalLine(fieldValueId
                                + ".setConverter(new "
                                + converterType.getSimpleTypeName() + "());");
                    }
                    else {
                        builder.getImportRegistrationResolver().addImports(
                                PRIMEFACES_AUTO_COMPLETE, fieldType);

                        bodyBuilder.appendFormalLine("AutoComplete "
                                + fieldValueId + " = "
                                + getComponentCreation("AutoComplete"));
                        bodyBuilder.appendFormalLine(componentIdStr);
                        bodyBuilder.appendFormalLine(getSetValueExpression(
                                fieldValueId, fieldName, simpleTypeName));
                        bodyBuilder.appendFormalLine(getSetCompleteMethod(
                                fieldValueId, fieldName));
                        bodyBuilder.appendFormalLine(fieldValueId
                                + ".setDropdown(true);");
                        bodyBuilder
                                .appendFormalLine(fieldValueId
                                        + ".setValueExpression(\"var\", expressionFactory.createValueExpression(elContext, \""
                                        + fieldName + "\", String.class));");
                        bodyBuilder
                                .appendFormalLine(fieldValueId
                                        + ".setValueExpression(\"itemLabel\", expressionFactory.createValueExpression(elContext, \""
                                        + getAutoCcompleteItemLabelValue(field,
                                                fieldName)
                                        + "\", String.class));");
                        bodyBuilder
                                .appendFormalLine(fieldValueId
                                        + ".setValueExpression(\"itemValue\", expressionFactory.createValueExpression(elContext, \"#{"
                                        + fieldName + "}\", " + simpleTypeName
                                        + ".class));");
                        bodyBuilder.appendFormalLine(fieldValueId
                                + ".setConverter(new "
                                + converterType.getSimpleTypeName() + "());");
                        bodyBuilder.appendFormalLine(requiredStr);
                    }
                }
            }
            else {
                if (action == Action.VIEW) {
                    bodyBuilder.appendFormalLine(htmlOutputTextStr);
                    bodyBuilder.appendFormalLine(getSetValueExpression(
                            fieldValueId, fieldName));
                }
                else {
                    builder.getImportRegistrationResolver().addImport(
                            PRIMEFACES_INPUT_TEXT);

                    bodyBuilder.appendFormalLine(inputTextStr);
                    bodyBuilder.appendFormalLine(componentIdStr);
                    bodyBuilder.appendFormalLine(getSetValueExpression(
                            fieldValueId, fieldName, simpleTypeName));
                    bodyBuilder.appendFormalLine(requiredStr);
                }
            }

            if (action != Action.VIEW) {
                bodyBuilder.appendFormalLine(getAddToPanelText(fieldValueId));
                // Add message for input field
                builder.getImportRegistrationResolver().addImport(
                        PRIMEFACES_MESSAGE);

                bodyBuilder.appendFormalLine("");
                bodyBuilder.appendFormalLine("Message " + fieldValueId
                        + "Message = " + getComponentCreation("Message"));
                bodyBuilder.appendFormalLine(fieldValueId + "Message.setId(\""
                        + fieldValueId + "Message\");");
                bodyBuilder.appendFormalLine(fieldValueId + "Message.setFor(\""
                        + fieldValueId + "\");");
                bodyBuilder.appendFormalLine(fieldValueId
                        + "Message.setDisplay(\"icon\");");
                bodyBuilder.appendFormalLine(getAddToPanelText(fieldValueId
                        + "Message"));
            }
            else {
                bodyBuilder.appendFormalLine(getAddToPanelText(fieldValueId));
            }

            bodyBuilder.appendFormalLine("");
        }
        bodyBuilder.appendFormalLine("return " + HTML_PANEL_GRID_ID + ";");

        return new MethodMetadataBuilder(getId(), PUBLIC, methodName,
                HTML_PANEL_GRID, new ArrayList<AnnotatedJavaType>(),
                new ArrayList<JavaSymbolName>(), bodyBuilder);
    }

    private MethodMetadataBuilder getResetMethod() {
        final JavaSymbolName methodName = new JavaSymbolName("reset");
        if (governorHasMethod(methodName)) {
            return null;
        }

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine(entityName.getSymbolName() + " = null;");
        for (final FieldMetadata field : locatedFields) {
            final CustomData customData = field.getCustomData();
            if (!customData.keySet().contains(PARAMETER_TYPE_KEY)) {
                continue;
            }

            bodyBuilder.appendFormalLine(getSelectedFieldName(field
                    .getFieldName().getSymbolName()) + " = null;");
        }
        bodyBuilder.appendFormalLine(CREATE_DIALOG_VISIBLE + " = false;");
        return getMethod(PUBLIC, methodName, VOID_PRIMITIVE, null, null,
                bodyBuilder);
    }

    private AnnotationMetadata getScopeAnnotation() {
        if (hasScopeAnnotation()) {
            return null;
        }
        final AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(
                SESSION_SCOPED);
        return annotationBuilder.build();
    }

    private String getSelectedFieldName(final String fieldName) {
        return "selected" + StringUtils.capitalize(fieldName);
    }

    private String getSetCompleteMethod(final String fieldValueId,
            final String fieldName) {
        return fieldValueId
                + ".setCompleteMethod(expressionFactory.createMethodExpression(elContext, \"#{"
                + beanName + ".complete" + StringUtils.capitalize(fieldName)
                + "}\", List.class, new Class[] { String.class }));";
    }

    private String getSetValueExpression(final String fieldValueId,
            final String fieldName) {
        return getSetValueExpression(fieldValueId, fieldName, "String");
    }

    private String getSetValueExpression(final String inputFieldVar,
            final String fieldName, final String className) {
        return inputFieldVar
                + ".setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{"
                + beanName + "." + entityName.getSymbolName() + "." + fieldName
                + "}\", " + className + ".class));";
    }

    private Integer getSizeMinOrMax(final FieldMetadata field,
            final String attrName) {
        final AnnotationMetadata annotation = MemberFindingUtils
                .getAnnotationOfType(field.getAnnotations(), SIZE);
        if (annotation != null
                && annotation.getAttribute(new JavaSymbolName(attrName)) != null) {
            return (Integer) annotation.getAttribute(
                    new JavaSymbolName(attrName)).getValue();
        }
        return null;
    }

    private boolean hasScopeAnnotation() {
        return governorTypeDetails.getAnnotation(SESSION_SCOPED) != null
                || governorTypeDetails.getAnnotation(VIEW_SCOPED) != null
                || governorTypeDetails.getAnnotation(REQUEST_SCOPED) != null
                || governorTypeDetails.getAnnotation(APPLICATION_SCOPED) != null;
    }

    private boolean isInverseSideOfRelationship(final FieldMetadata field,
            final JavaType... annotationTypes) {
        for (final JavaType annotationType : annotationTypes) {
            final AnnotationMetadata annotation = MemberFindingUtils
                    .getAnnotationOfType(field.getAnnotations(), annotationType);
            if (annotation != null
                    && annotation.getAttribute(new JavaSymbolName("mappedBy")) != null) {
                return true;
            }
        }
        return false;
    }

    private boolean isNullable(final FieldMetadata field) {
        return MemberFindingUtils.getAnnotationOfType(field.getAnnotations(),
                NOT_NULL) == null;
    }

    private void setRegexPatternValidationString(final FieldMetadata field,
            final String fieldValueId,
            final InvocableMemberBodyBuilder bodyBuilder) {
        final AnnotationMetadata patternAnnotation = MemberFindingUtils
                .getAnnotationOfType(field.getAnnotations(), PATTERN);
        if (patternAnnotation != null) {
            builder.getImportRegistrationResolver().addImport(REGEX_VALIDATOR);

            final AnnotationAttributeValue<?> regexpAttr = patternAnnotation
                    .getAttribute(new JavaSymbolName("regexp"));
            bodyBuilder.appendFormalLine("RegexValidator " + fieldValueId
                    + "RegexValidator = new RegexValidator();");
            bodyBuilder.appendFormalLine(fieldValueId
                    + "RegexValidator.setPattern(\"" + regexpAttr.getValue()
                    + "\");");
            bodyBuilder.appendFormalLine(fieldValueId + ".addValidator("
                    + fieldValueId + "RegexValidator);");
        }
    }

    private boolean isUIComponent(FieldMetadata field, JavaType fieldType,
            CustomData customData) {

        if (field.getAnnotation(ROO_UPLOADED_FILE) != null
                || fieldType.equals(BOOLEAN_OBJECT)
                || fieldType.equals(BOOLEAN_PRIMITIVE)
                || customData.keySet().contains(ENUMERATED_KEY)
                || JdkJavaType.isDateField(fieldType)
                || JdkJavaType.isIntegerType(fieldType)
                || JdkJavaType.isDecimalType(fieldType)
                || fieldType.equals(STRING)) {

            return true;

        }
        else if (customData.keySet().contains(PARAMETER_TYPE_KEY)) {
            if (StringUtils.isNotBlank((String) customData
                    .get(PARAMETER_TYPE_MANAGED_BEAN_NAME_KEY))) {
                if (customData.keySet().contains(ONE_TO_MANY_FIELD)
                        || customData.keySet().contains(MANY_TO_MANY_FIELD)
                        && isInverseSideOfRelationship(field, ONE_TO_MANY,
                                MANY_TO_MANY)) {
                    return false;
                }
            }

            return true;

        }
        else if (customData.keySet().contains(APPLICATION_TYPE_KEY)) {
            if (customData.keySet().contains(ONE_TO_ONE_FIELD)
                    && isInverseSideOfRelationship(field, ONE_TO_ONE)) {
                return false;
            }
            return true;
        }
        else {
            return true;
        }
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("identifier", getId());
        builder.append("valid", valid);
        builder.append("aspectName", aspectName);
        builder.append("destinationType", destination);
        builder.append("governor", governorPhysicalTypeMetadata.getId());
        builder.append("itdTypeDetails", itdTypeDetails);
        return builder.toString();
    }
}
