package org.springframework.roo.addon.web.mvc.jsp;

import static org.springframework.roo.model.JavaType.BOOLEAN_OBJECT;
import static org.springframework.roo.model.JavaType.BOOLEAN_PRIMITIVE;
import static org.springframework.roo.model.JavaType.DOUBLE_OBJECT;
import static org.springframework.roo.model.JavaType.FLOAT_OBJECT;
import static org.springframework.roo.model.JavaType.INT_OBJECT;
import static org.springframework.roo.model.JavaType.LONG_OBJECT;
import static org.springframework.roo.model.JavaType.SHORT_OBJECT;
import static org.springframework.roo.model.JavaType.STRING;
import static org.springframework.roo.model.JdkJavaType.BIG_DECIMAL;
import static org.springframework.roo.model.JdkJavaType.BIG_INTEGER;
import static org.springframework.roo.model.JdkJavaType.CALENDAR;
import static org.springframework.roo.model.JdkJavaType.DATE;
import static org.springframework.roo.model.Jsr303JavaType.DECIMAL_MAX;
import static org.springframework.roo.model.Jsr303JavaType.DECIMAL_MIN;
import static org.springframework.roo.model.Jsr303JavaType.FUTURE;
import static org.springframework.roo.model.Jsr303JavaType.MAX;
import static org.springframework.roo.model.Jsr303JavaType.MIN;
import static org.springframework.roo.model.Jsr303JavaType.NOT_NULL;
import static org.springframework.roo.model.Jsr303JavaType.PAST;
import static org.springframework.roo.model.Jsr303JavaType.PATTERN;
import static org.springframework.roo.model.Jsr303JavaType.SIZE;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.addon.web.mvc.controller.details.FinderMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.details.JavaTypeMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.details.JavaTypePersistenceMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldAnnotationValues;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlRoundTripUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Helper class which generates the contents of the various jsp documents
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public class JspViewManager {

    private static final String CREATED = "created";
    private static final JavaSymbolName VALUE = new JavaSymbolName("value");
    private final String controllerPath;
    private final String entityName;
    private final List<FieldMetadata> fields;
    private final JavaType formBackingType;
    private final JavaTypeMetadataDetails formBackingTypeMetadata;
    private final JavaTypePersistenceMetadataDetails formBackingTypePersistenceMetadata;
    private final Map<JavaType, JavaTypeMetadataDetails> relatedDomainTypes;
    private final WebScaffoldAnnotationValues webScaffoldAnnotationValues;

    /**
     * Constructor
     * 
     * @param fields can't be <code>null</code>
     * @param webScaffoldAnnotationValues can't be <code>null</code>
     * @param relatedDomainTypes can't be <code>null</code>
     */
    public JspViewManager(final List<FieldMetadata> fields,
            final WebScaffoldAnnotationValues webScaffoldAnnotationValues,
            final Map<JavaType, JavaTypeMetadataDetails> relatedDomainTypes) {
        Validate.notNull(fields, "List of fields required");
        Validate.notNull(webScaffoldAnnotationValues,
                "Web scaffold annotation values required");
        Validate.notNull(relatedDomainTypes, "Related domain types required");
        this.fields = Collections.unmodifiableList(fields);
        this.webScaffoldAnnotationValues = webScaffoldAnnotationValues;
        formBackingType = webScaffoldAnnotationValues.getFormBackingObject();
        this.relatedDomainTypes = relatedDomainTypes;
        entityName = JavaSymbolName.getReservedWordSafeName(formBackingType)
                .getSymbolName();
        formBackingTypeMetadata = relatedDomainTypes.get(formBackingType);
        Validate.notNull(formBackingTypeMetadata,
                "Form backing type metadata required");
        formBackingTypePersistenceMetadata = formBackingTypeMetadata
                .getPersistenceDetails();
        Validate.notNull(formBackingTypePersistenceMetadata,
                "Persistence metadata required for form backing type");
        Validate.notNull(
                webScaffoldAnnotationValues.getPath(),
                "Path is not specified in the @RooWebScaffold annotation for '%s'",
                webScaffoldAnnotationValues.getGovernorTypeDetails().getName());

        if (webScaffoldAnnotationValues.getPath().startsWith("/")) {
            controllerPath = webScaffoldAnnotationValues.getPath();
        }
        else {
            controllerPath = "/" + webScaffoldAnnotationValues.getPath();
        }
    }

    private void addCommonAttributes(final FieldMetadata field,
            final Element fieldElement) {
        AnnotationMetadata annotationMetadata;
        if (field.getFieldType().equals(INT_OBJECT)
                || field.getFieldType().getFullyQualifiedTypeName()
                        .equals(int.class.getName())
                || field.getFieldType().equals(SHORT_OBJECT)
                || field.getFieldType().getFullyQualifiedTypeName()
                        .equals(short.class.getName())
                || field.getFieldType().equals(LONG_OBJECT)
                || field.getFieldType().getFullyQualifiedTypeName()
                        .equals(long.class.getName())
                || field.getFieldType().equals(BIG_INTEGER)) {
            fieldElement.setAttribute("validationMessageCode",
                    "field_invalid_integer");
        }
        else if (isEmailField(field)) {
            fieldElement.setAttribute("validationMessageCode",
                    "field_invalid_email");
        }
        else if (field.getFieldType().equals(DOUBLE_OBJECT)
                || field.getFieldType().getFullyQualifiedTypeName()
                        .equals(double.class.getName())
                || field.getFieldType().equals(FLOAT_OBJECT)
                || field.getFieldType().getFullyQualifiedTypeName()
                        .equals(float.class.getName())
                || field.getFieldType().equals(BIG_DECIMAL)) {
            fieldElement.setAttribute("validationMessageCode",
                    "field_invalid_number");
        }
        if ("field:input".equals(fieldElement.getTagName())
                && null != (annotationMetadata = MemberFindingUtils
                        .getAnnotationOfType(field.getAnnotations(), MIN))) {
            final AnnotationAttributeValue<?> min = annotationMetadata
                    .getAttribute(VALUE);
            if (min != null) {
                fieldElement.setAttribute("min", min.getValue().toString());
                fieldElement.setAttribute("required", "true");
            }
        }
        if ("field:input".equals(fieldElement.getTagName())
                && null != (annotationMetadata = MemberFindingUtils
                        .getAnnotationOfType(field.getAnnotations(), MAX))
                && !"field:textarea".equals(fieldElement.getTagName())) {
            final AnnotationAttributeValue<?> maxA = annotationMetadata
                    .getAttribute(VALUE);
            if (maxA != null) {
                fieldElement.setAttribute("max", maxA.getValue().toString());
            }
        }
        if ("field:input".equals(fieldElement.getTagName())
                && null != (annotationMetadata = MemberFindingUtils
                        .getAnnotationOfType(field.getAnnotations(),
                                DECIMAL_MIN))
                && !"field:textarea".equals(fieldElement.getTagName())) {
            final AnnotationAttributeValue<?> decimalMin = annotationMetadata
                    .getAttribute(VALUE);
            if (decimalMin != null) {
                fieldElement.setAttribute("decimalMin", decimalMin.getValue()
                        .toString());
                fieldElement.setAttribute("required", "true");
            }
        }
        if ("field:input".equals(fieldElement.getTagName())
                && null != (annotationMetadata = MemberFindingUtils
                        .getAnnotationOfType(field.getAnnotations(),
                                DECIMAL_MAX))) {
            final AnnotationAttributeValue<?> decimalMax = annotationMetadata
                    .getAttribute(VALUE);
            if (decimalMax != null) {
                fieldElement.setAttribute("decimalMax", decimalMax.getValue()
                        .toString());
            }
        }
        if (null != (annotationMetadata = MemberFindingUtils
                .getAnnotationOfType(field.getAnnotations(), PATTERN))) {
            final AnnotationAttributeValue<?> regexp = annotationMetadata
                    .getAttribute(new JavaSymbolName("regexp"));
            if (regexp != null) {
                fieldElement.setAttribute("validationRegex", regexp.getValue()
                        .toString());
            }
        }
        if ("field:input".equals(fieldElement.getTagName())
                && null != (annotationMetadata = MemberFindingUtils
                        .getAnnotationOfType(field.getAnnotations(), SIZE))) {
            final AnnotationAttributeValue<?> max = annotationMetadata
                    .getAttribute(new JavaSymbolName("max"));
            if (max != null) {
                fieldElement.setAttribute("max", max.getValue().toString());
            }
            final AnnotationAttributeValue<?> min = annotationMetadata
                    .getAttribute(new JavaSymbolName("min"));
            if (min != null) {
                fieldElement.setAttribute("min", min.getValue().toString());
                fieldElement.setAttribute("required", "true");
            }
        }
        if (null != (annotationMetadata = MemberFindingUtils
                .getAnnotationOfType(field.getAnnotations(), NOT_NULL))) {
            final String tagName = fieldElement.getTagName();
            if (tagName.endsWith("textarea") || tagName.endsWith("input")
                    || tagName.endsWith("datetime")
                    || tagName.endsWith("textarea")
                    || tagName.endsWith("select")
                    || tagName.endsWith("reference")) {
                fieldElement.setAttribute("required", "true");
            }
        }
        if (field.getCustomData().keySet()
                .contains(CustomDataKeys.COLUMN_FIELD)) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> values = (Map<String, Object>) field
                    .getCustomData().get(CustomDataKeys.COLUMN_FIELD);
            if (values.keySet().contains("nullable")
                    && (Boolean) values.get("nullable") == false) {
                fieldElement.setAttribute("required", "true");
            }
        }
        // Disable form binding for nested fields (mainly PKs)
        if (field.getFieldName().getSymbolName().contains(".")) {
            fieldElement.setAttribute("disableFormBinding", "true");
        }
    }

    private void createFieldsForCreateAndUpdate(
            final List<FieldMetadata> formFields, final Document document,
            final Element root, final boolean isCreate) {
        for (final FieldMetadata field : formFields) {
            final String fieldName = field.getFieldName().getSymbolName();
            JavaType fieldType = field.getFieldType();
            AnnotationMetadata annotationMetadata;

            // Ignoring java.util.Map field types (see ROO-194)
            if (fieldType.equals(new JavaType(Map.class.getName()))) {
                continue;
            }
            // Fields contained in the embedded Id type have been added
            // separately to the field list
            if (field.getCustomData().keySet()
                    .contains(CustomDataKeys.EMBEDDED_ID_FIELD)) {
                continue;
            }

            fieldType = getJavaTypeForField(field);

            final JavaTypeMetadataDetails typeMetadataHolder = relatedDomainTypes
                    .get(fieldType);
            JavaTypePersistenceMetadataDetails typePersistenceMetadataHolder = null;
            if (typeMetadataHolder != null) {
                typePersistenceMetadataHolder = typeMetadataHolder
                        .getPersistenceDetails();
            }

            Element fieldElement = null;

            if (fieldType.getFullyQualifiedTypeName().equals(
                    Boolean.class.getName())
                    || fieldType.getFullyQualifiedTypeName().equals(
                            boolean.class.getName())) {
                fieldElement = document.createElement("field:checkbox");
                // Handle enum fields
            }
            else if (typeMetadataHolder != null
                    && typeMetadataHolder.isEnumType()) {
                fieldElement = new XmlElementBuilder("field:select", document)
                        .addAttribute(
                                "items",
                                "${"
                                        + typeMetadataHolder.getPlural()
                                                .toLowerCase() + "}")
                        .addAttribute("path", getPathForType(fieldType))
                        .build();
            }
            else if (field.getCustomData().keySet()
                    .contains(CustomDataKeys.ONE_TO_MANY_FIELD)) {
                // OneToMany relationships are managed from the 'many' side of
                // the relationship, therefore we provide a link to the relevant
                // form the link URL is determined as a best effort attempt
                // following Roo REST conventions, this link might be wrong if
                // custom paths are used if custom paths are used the developer
                // can adjust the path attribute in the field:reference tag
                // accordingly
                if (typePersistenceMetadataHolder != null) {
                    fieldElement = new XmlElementBuilder("field:simple",
                            document)
                            .addAttribute("messageCode",
                                    "entity_reference_not_managed")
                            .addAttribute(
                                    "messageCodeAttribute",
                                    new JavaSymbolName(fieldType
                                            .getSimpleTypeName())
                                            .getReadableSymbolName()).build();
                }
                else {
                    continue;
                }
            }
            else if (field.getCustomData().keySet()
                    .contains(CustomDataKeys.MANY_TO_ONE_FIELD)
                    || field.getCustomData().keySet()
                            .contains(CustomDataKeys.MANY_TO_MANY_FIELD)
                    || field.getCustomData().keySet()
                            .contains(CustomDataKeys.ONE_TO_ONE_FIELD)) {
                final JavaType referenceType = getJavaTypeForField(field);
                final JavaTypeMetadataDetails referenceTypeMetadata = relatedDomainTypes
                        .get(referenceType);
                if (referenceType != null && referenceTypeMetadata != null
                        && referenceTypeMetadata.isApplicationType()
                        && typePersistenceMetadataHolder != null) {
                    fieldElement = new XmlElementBuilder("field:select",
                            document)
                            .addAttribute(
                                    "items",
                                    "${"
                                            + referenceTypeMetadata.getPlural()
                                                    .toLowerCase() + "}")
                            .addAttribute(
                                    "itemValue",
                                    typePersistenceMetadataHolder
                                            .getIdentifierField()
                                            .getFieldName().getSymbolName())
                            .addAttribute(
                                    "path",
                                    "/"
                                            + getPathForType(getJavaTypeForField(field)))
                            .build();
                    if (field.getCustomData().keySet()
                            .contains(CustomDataKeys.MANY_TO_MANY_FIELD)) {
                        fieldElement.setAttribute("multiple", "true");
                    }
                }
            }
            else if (fieldType.equals(DATE) || fieldType.equals(CALENDAR)) {
                if (fieldName.equals(CREATED)) {
                    continue;
                }
                // Only include the date picker for styles supported by Dojo
                // (SMALL & MEDIUM)
                fieldElement = new XmlElementBuilder("field:datetime", document)
                        .addAttribute(
                                "dateTimePattern",
                                "${" + entityName + "_"
                                        + fieldName.toLowerCase()
                                        + "_date_format}").build();
                if (null != MemberFindingUtils.getAnnotationOfType(
                        field.getAnnotations(), FUTURE)) {
                    fieldElement.setAttribute("future", "true");
                }
                else if (null != MemberFindingUtils.getAnnotationOfType(
                        field.getAnnotations(), PAST)) {
                    fieldElement.setAttribute("past", "true");
                }
            }
            else if (field.getCustomData().keySet()
                    .contains(CustomDataKeys.LOB_FIELD)) {
                fieldElement = new XmlElementBuilder("field:textarea", document)
                        .build();
            }
            if ((annotationMetadata = MemberFindingUtils.getAnnotationOfType(
                    field.getAnnotations(), SIZE)) != null) {
                final AnnotationAttributeValue<?> max = annotationMetadata
                        .getAttribute(new JavaSymbolName("max"));
                if (max != null) {
                    final int maxValue = (Integer) max.getValue();
                    if (fieldElement == null && maxValue > 30) {
                        fieldElement = new XmlElementBuilder("field:textarea",
                                document).build();
                    }
                }
            }
            // Use a default input field if no other criteria apply
            if (fieldElement == null) {
                fieldElement = document.createElement("field:input");
            }
            addCommonAttributes(field, fieldElement);
            fieldElement.setAttribute("field", fieldName);
            fieldElement.setAttribute(
                    "id",
                    XmlUtils.convertId("c:"
                            + formBackingType.getFullyQualifiedTypeName() + "."
                            + field.getFieldName().getSymbolName()));

            // If identifier manually assigned, then add 'required=true'
            if (formBackingTypePersistenceMetadata.getIdentifierField()
                    .getFieldName().equals(field.getFieldName())
                    && field.getAnnotation(JpaJavaType.GENERATED_VALUE) == null) {
                fieldElement.setAttribute("required", "true");
            }

            fieldElement.setAttribute("z",
                    XmlRoundTripUtils.calculateUniqueKeyFor(fieldElement));

            root.appendChild(fieldElement);
        }
    }

    public Document getCreateDocument() {
        final DocumentBuilder builder = XmlUtils.getDocumentBuilder();
        final Document document = builder.newDocument();

        // Add document namespaces
        final Element div = (Element) document
                .appendChild(new XmlElementBuilder("div", document)
                        .addAttribute("xmlns:form",
                                "urn:jsptagdir:/WEB-INF/tags/form")
                        .addAttribute("xmlns:field",
                                "urn:jsptagdir:/WEB-INF/tags/form/fields")
                        .addAttribute("xmlns:jsp",
                                "http://java.sun.com/JSP/Page")
                        .addAttribute("xmlns:c",
                                "http://java.sun.com/jsp/jstl/core")
                        .addAttribute("xmlns:spring",
                                "http://www.springframework.org/tags")
                        .addAttribute("version", "2.0")
                        .addChild(
                                new XmlElementBuilder("jsp:directive.page",
                                        document).addAttribute("contentType",
                                        "text/html;charset=UTF-8").build())
                        .addChild(
                                new XmlElementBuilder("jsp:output", document)
                                        .addAttribute("omit-xml-declaration",
                                                "yes").build()).build());

        // Add form create element
        final Element formCreate = new XmlElementBuilder("form:create",
                document)
                .addAttribute(
                        "id",
                        XmlUtils.convertId("fc:"
                                + formBackingType.getFullyQualifiedTypeName()))
                .addAttribute("modelAttribute", entityName)
                .addAttribute("path", controllerPath)
                .addAttribute("render", "${empty dependencies}").build();

        if (!controllerPath.equalsIgnoreCase(formBackingType
                .getSimpleTypeName())) {
            formCreate.setAttribute("path", controllerPath);
        }

        final List<FieldMetadata> formFields = new ArrayList<FieldMetadata>();
        final List<FieldMetadata> fieldCopy = new ArrayList<FieldMetadata>(
                fields);

        // Handle Roo identifiers
        if (!formBackingTypePersistenceMetadata.getRooIdentifierFields()
                .isEmpty()) {
            final String identifierFieldName = formBackingTypePersistenceMetadata
                    .getIdentifierField().getFieldName().getSymbolName();
            formCreate.setAttribute("compositePkField", identifierFieldName);
            for (final FieldMetadata embeddedField : formBackingTypePersistenceMetadata
                    .getRooIdentifierFields()) {
                final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(
                        embeddedField);
                fieldBuilder
                        .setFieldName(new JavaSymbolName(identifierFieldName
                                + "."
                                + embeddedField.getFieldName().getSymbolName()));
                for (int i = 0; i < fieldCopy.size(); i++) {
                    // Make sure form fields are not presented twice.
                    if (fieldCopy.get(i).getFieldName()
                            .equals(embeddedField.getFieldName())) {
                        fieldCopy.remove(i);
                        break;
                    }
                }
                formFields.add(fieldBuilder.build());
            }
        }
        formFields.addAll(fieldCopy);

        // If identifier manually assigned, show it in creation
        if (formBackingTypePersistenceMetadata.getIdentifierField()
                .getAnnotation(JpaJavaType.GENERATED_VALUE) == null) {

            formFields.add(formBackingTypePersistenceMetadata
                    .getIdentifierField());
        }

        createFieldsForCreateAndUpdate(formFields, document, formCreate, true);
        formCreate.setAttribute("z",
                XmlRoundTripUtils.calculateUniqueKeyFor(formCreate));

        final Element dependency = new XmlElementBuilder("form:dependency",
                document)
                .addAttribute(
                        "id",
                        XmlUtils.convertId("d:"
                                + formBackingType.getFullyQualifiedTypeName()))
                .addAttribute("render", "${not empty dependencies}")
                .addAttribute("dependencies", "${dependencies}").build();
        dependency.setAttribute("z",
                XmlRoundTripUtils.calculateUniqueKeyFor(dependency));

        div.appendChild(formCreate);
        div.appendChild(dependency);

        return document;
    }

    public Document getFinderDocument(
            final FinderMetadataDetails finderMetadataDetails) {
        final DocumentBuilder builder = XmlUtils.getDocumentBuilder();
        final Document document = builder.newDocument();

        // Add document namespaces
        final Element div = (Element) document
                .appendChild(new XmlElementBuilder("div", document)
                        .addAttribute("xmlns:form",
                                "urn:jsptagdir:/WEB-INF/tags/form")
                        .addAttribute("xmlns:field",
                                "urn:jsptagdir:/WEB-INF/tags/form/fields")
                        .addAttribute("xmlns:jsp",
                                "http://java.sun.com/JSP/Page")
                        .addAttribute("version", "2.0")
                        .addChild(
                                new XmlElementBuilder("jsp:directive.page",
                                        document).addAttribute("contentType",
                                        "text/html;charset=UTF-8").build())
                        .addChild(
                                new XmlElementBuilder("jsp:output", document)
                                        .addAttribute("omit-xml-declaration",
                                                "yes").build()).build());

        final Element formFind = new XmlElementBuilder("form:find", document)
                .addAttribute(
                        "id",
                        XmlUtils.convertId("ff:"
                                + formBackingType.getFullyQualifiedTypeName()))
                .addAttribute("path", controllerPath)
                .addAttribute(
                        "finderName",
                        finderMetadataDetails
                                .getFinderMethodMetadata()
                                .getMethodName()
                                .getSymbolName()
                                .replace(
                                        "find"
                                                + formBackingTypeMetadata
                                                        .getPlural(), ""))
                .build();
        formFind.setAttribute("z",
                XmlRoundTripUtils.calculateUniqueKeyFor(formFind));
        div.appendChild(formFind);

        for (final FieldMetadata field : finderMetadataDetails
                .getFinderMethodParamFields()) {
            final JavaType type = field.getFieldType();
            final JavaSymbolName paramName = field.getFieldName();

            // Ignoring java.util.Map field types (see ROO-194)
            if (type.equals(new JavaType(Map.class.getName()))) {
                continue;
            }
            Validate.notNull(paramName, "Could not find field '%s' in '%s'",
                    paramName, type.getFullyQualifiedTypeName());
            Element fieldElement = null;

            final JavaTypeMetadataDetails typeMetadataHolder = relatedDomainTypes
                    .get(getJavaTypeForField(field));

            if (type.isCommonCollectionType()
                    && relatedDomainTypes
                            .containsKey(getJavaTypeForField(field))) {
                final JavaTypeMetadataDetails collectionTypeMetadataHolder = relatedDomainTypes
                        .get(getJavaTypeForField(field));
                final JavaTypePersistenceMetadataDetails typePersistenceMetadataHolder = collectionTypeMetadataHolder
                        .getPersistenceDetails();
                if (typePersistenceMetadataHolder != null) {
                    fieldElement = new XmlElementBuilder("field:select",
                            document)
                            .addAttribute("required", "true")
                            .addAttribute(
                                    "items",
                                    "${"
                                            + collectionTypeMetadataHolder
                                                    .getPlural().toLowerCase()
                                            + "}")
                            .addAttribute(
                                    "itemValue",
                                    typePersistenceMetadataHolder
                                            .getIdentifierField()
                                            .getFieldName().getSymbolName())
                            .addAttribute(
                                    "path",
                                    "/"
                                            + getPathForType(getJavaTypeForField(field)))
                            .build();
                    if (field.getCustomData().keySet()
                            .contains(CustomDataKeys.MANY_TO_MANY_FIELD)) {
                        fieldElement.setAttribute("multiple", "true");
                    }
                }
            }
            else if (typeMetadataHolder != null
                    && typeMetadataHolder.isEnumType()
                    && field.getCustomData().keySet()
                            .contains(CustomDataKeys.ENUMERATED_FIELD)) {
                fieldElement = new XmlElementBuilder("field:select", document)
                        .addAttribute("required", "true")
                        .addAttribute(
                                "items",
                                "${"
                                        + typeMetadataHolder.getPlural()
                                                .toLowerCase() + "}")
                        .addAttribute("path", "/" + getPathForType(type))
                        .build();
            }
            else if (type.equals(BOOLEAN_OBJECT)
                    || type.equals(BOOLEAN_PRIMITIVE)) {
                fieldElement = document.createElement("field:checkbox");
            }
            else if (typeMetadataHolder != null
                    && typeMetadataHolder.isApplicationType()) {
                final JavaTypePersistenceMetadataDetails typePersistenceMetadataHolder = typeMetadataHolder
                        .getPersistenceDetails();
                if (typePersistenceMetadataHolder != null) {
                    fieldElement = new XmlElementBuilder("field:select",
                            document)
                            .addAttribute("required", "true")
                            .addAttribute(
                                    "items",
                                    "${"
                                            + typeMetadataHolder.getPlural()
                                                    .toLowerCase() + "}")
                            .addAttribute(
                                    "itemValue",
                                    typePersistenceMetadataHolder
                                            .getIdentifierField()
                                            .getFieldName().getSymbolName())
                            .addAttribute("path", "/" + getPathForType(type))
                            .build();
                }
            }
            else if (type.equals(DATE) || type.equals(CALENDAR)) {
                fieldElement = new XmlElementBuilder("field:datetime", document)
                        .addAttribute("required", "true")
                        .addAttribute(
                                "dateTimePattern",
                                "${"
                                        + entityName
                                        + "_"
                                        + paramName.getSymbolName()
                                                .toLowerCase()
                                        + "_date_format}").build();
            }
            if (fieldElement == null) {
                fieldElement = new XmlElementBuilder("field:input", document)
                        .addAttribute("required", "true").build();
            }
            addCommonAttributes(field, fieldElement);
            fieldElement.setAttribute("disableFormBinding", "true");
            fieldElement.setAttribute("field", paramName.getSymbolName());
            fieldElement.setAttribute(
                    "id",
                    XmlUtils.convertId("f:"
                            + formBackingType.getFullyQualifiedTypeName() + "."
                            + paramName));
            fieldElement.setAttribute("z",
                    XmlRoundTripUtils.calculateUniqueKeyFor(fieldElement));
            formFind.appendChild(fieldElement);
        }

        DomUtils.removeTextNodes(document);
        return document;
    }

    private JavaType getJavaTypeForField(final FieldMetadata field) {
        if (field.getFieldType().isCommonCollectionType()) {
            // Currently there is no scaffolding available for Maps (see
            // ROO-194)
            if (field.getFieldType().equals(new JavaType(Map.class.getName()))) {
                return null;
            }
            final List<JavaType> parameters = field.getFieldType()
                    .getParameters();
            if (parameters.isEmpty()) {
                throw new IllegalStateException(
                        "Unable to determine the parameter type for the "
                                + field.getFieldName().getSymbolName()
                                + " field in "
                                + formBackingType.getSimpleTypeName());
            }
            return parameters.get(0);
        }
        return field.getFieldType();
    }

    public Document getListDocument() {
        final DocumentBuilder builder = XmlUtils.getDocumentBuilder();
        final Document document = builder.newDocument();

        // Add document namespaces
        final Element div = new XmlElementBuilder("div", document)
                .addAttribute("xmlns:page", "urn:jsptagdir:/WEB-INF/tags/form")
                .addAttribute("xmlns:table",
                        "urn:jsptagdir:/WEB-INF/tags/form/fields")
                .addAttribute("xmlns:jsp", "http://java.sun.com/JSP/Page")
                .addAttribute("version", "2.0")
                .addChild(
                        new XmlElementBuilder("jsp:directive.page", document)
                                .addAttribute("contentType",
                                        "text/html;charset=UTF-8").build())
                .addChild(
                        new XmlElementBuilder("jsp:output", document)
                                .addAttribute("omit-xml-declaration", "yes")
                                .build()).build();
        document.appendChild(div);

        final Element fieldTable = new XmlElementBuilder("table:table",
                document)
                .addAttribute(
                        "id",
                        XmlUtils.convertId("l:"
                                + formBackingType.getFullyQualifiedTypeName()))
                .addAttribute(
                        "data",
                        "${"
                                + formBackingTypeMetadata.getPlural()
                                        .toLowerCase() + "}")
                .addAttribute("path", controllerPath).build();

        if (!webScaffoldAnnotationValues.isUpdate()) {
            fieldTable.setAttribute("update", "false");
        }
        if (!webScaffoldAnnotationValues.isDelete()) {
            fieldTable.setAttribute("delete", "false");
        }
        if (!formBackingTypePersistenceMetadata.getIdentifierField()
                .getFieldName().getSymbolName().equals("id")) {
            fieldTable.setAttribute("typeIdFieldName",
                    formBackingTypePersistenceMetadata.getIdentifierField()
                            .getFieldName().getSymbolName());
        }
        fieldTable.setAttribute("z",
                XmlRoundTripUtils.calculateUniqueKeyFor(fieldTable));

        int fieldCounter = 0;
        for (final FieldMetadata field : fields) {
            if (++fieldCounter < 7) {
                final Element columnElement = new XmlElementBuilder(
                        "table:column", document)
                        .addAttribute(
                                "id",
                                XmlUtils.convertId("c:"
                                        + formBackingType
                                                .getFullyQualifiedTypeName()
                                        + "."
                                        + field.getFieldName().getSymbolName()))
                        .addAttribute(
                                "property",
                                uncapitalize(field.getFieldName()
                                        .getSymbolName())).build();
                final String fieldName = uncapitalize(field.getFieldName()
                        .getSymbolName());
                if (field.getFieldType().equals(DATE)) {
                    columnElement.setAttribute("date", "true");
                    columnElement.setAttribute("dateTimePattern", "${"
                            + entityName + "_" + fieldName.toLowerCase()
                            + "_date_format}");
                }
                else if (field.getFieldType().equals(CALENDAR)) {
                    columnElement.setAttribute("calendar", "true");
                    columnElement.setAttribute("dateTimePattern", "${"
                            + entityName + "_" + fieldName.toLowerCase()
                            + "_date_format}");
                }
                else if (field.getFieldType().isCommonCollectionType()
                        && field.getCustomData().get(
                                CustomDataKeys.ONE_TO_MANY_FIELD) != null) {
                    continue;
                }
                columnElement.setAttribute("z",
                        XmlRoundTripUtils.calculateUniqueKeyFor(columnElement));
                fieldTable.appendChild(columnElement);
            }
        }

        // Create page:list element
        final Element pageList = new XmlElementBuilder("page:list", document)
                .addAttribute(
                        "id",
                        XmlUtils.convertId("pl:"
                                + formBackingType.getFullyQualifiedTypeName()))
                .addAttribute(
                        "items",
                        "${"
                                + formBackingTypeMetadata.getPlural()
                                        .toLowerCase() + "}")
                .addChild(fieldTable).build();
        pageList.setAttribute("z",
                XmlRoundTripUtils.calculateUniqueKeyFor(pageList));
        div.appendChild(pageList);

        return document;
    }

    private String getPathForType(final JavaType type) {
        final JavaTypeMetadataDetails javaTypeMetadataHolder = relatedDomainTypes
                .get(type);
        Validate.notNull(javaTypeMetadataHolder,
                "Unable to obtain metadata for type %s",
                type.getFullyQualifiedTypeName());
        return javaTypeMetadataHolder.getControllerPath();
    }

    public Document getShowDocument() {
        final DocumentBuilder builder = XmlUtils.getDocumentBuilder();
        final Document document = builder.newDocument();

        // Add document namespaces
        final Element div = (Element) document
                .appendChild(new XmlElementBuilder("div", document)
                        .addAttribute("xmlns:page",
                                "urn:jsptagdir:/WEB-INF/tags/form")
                        .addAttribute("xmlns:field",
                                "urn:jsptagdir:/WEB-INF/tags/form/fields")
                        .addAttribute("xmlns:jsp",
                                "http://java.sun.com/JSP/Page")
                        .addAttribute("version", "2.0")
                        .addChild(
                                new XmlElementBuilder("jsp:directive.page",
                                        document).addAttribute("contentType",
                                        "text/html;charset=UTF-8").build())
                        .addChild(
                                new XmlElementBuilder("jsp:output", document)
                                        .addAttribute("omit-xml-declaration",
                                                "yes").build()).build());

        final Element pageShow = new XmlElementBuilder("page:show", document)
                .addAttribute(
                        "id",
                        XmlUtils.convertId("ps:"
                                + formBackingType.getFullyQualifiedTypeName()))
                .addAttribute("object", "${" + entityName.toLowerCase() + "}")
                .addAttribute("path", controllerPath).build();
        if (!webScaffoldAnnotationValues.isCreate()) {
            pageShow.setAttribute("create", "false");
        }
        if (!webScaffoldAnnotationValues.isUpdate()) {
            pageShow.setAttribute("update", "false");
        }
        if (!webScaffoldAnnotationValues.isDelete()) {
            pageShow.setAttribute("delete", "false");
        }
        pageShow.setAttribute("z",
                XmlRoundTripUtils.calculateUniqueKeyFor(pageShow));

        // Add field:display elements for each field
        for (final FieldMetadata field : fields) {
            // Ignoring java.util.Map field types (see ROO-194)
            if (field.getFieldType().equals(new JavaType(Map.class.getName()))) {
                continue;
            }
            final String fieldName = uncapitalize(field.getFieldName()
                    .getSymbolName());
            final Element fieldDisplay = new XmlElementBuilder("field:display",
                    document)
                    .addAttribute(
                            "id",
                            XmlUtils.convertId("s:"
                                    + formBackingType
                                            .getFullyQualifiedTypeName() + "."
                                    + field.getFieldName().getSymbolName()))
                    .addAttribute("object",
                            "${" + entityName.toLowerCase() + "}")
                    .addAttribute("field", fieldName).build();
            if (field.getFieldType().equals(DATE)) {
                if (fieldName.equals(CREATED)) {
                    continue;
                }
                fieldDisplay.setAttribute("date", "true");
                fieldDisplay.setAttribute("dateTimePattern", "${" + entityName
                        + "_" + fieldName.toLowerCase() + "_date_format}");
            }
            else if (field.getFieldType().equals(CALENDAR)) {
                fieldDisplay.setAttribute("calendar", "true");
                fieldDisplay.setAttribute("dateTimePattern", "${" + entityName
                        + "_" + fieldName.toLowerCase() + "_date_format}");
            }
            else if (field.getFieldType().isCommonCollectionType()
                    && field.getCustomData().get(
                            CustomDataKeys.ONE_TO_MANY_FIELD) != null) {
                continue;
            }
            fieldDisplay.setAttribute("z",
                    XmlRoundTripUtils.calculateUniqueKeyFor(fieldDisplay));

            pageShow.appendChild(fieldDisplay);
        }
        div.appendChild(pageShow);

        return document;
    }

    public Document getUpdateDocument() {
        final DocumentBuilder builder = XmlUtils.getDocumentBuilder();
        final Document document = builder.newDocument();

        // Add document namespaces
        final Element div = (Element) document
                .appendChild(new XmlElementBuilder("div", document)
                        .addAttribute("xmlns:form",
                                "urn:jsptagdir:/WEB-INF/tags/form")
                        .addAttribute("xmlns:field",
                                "urn:jsptagdir:/WEB-INF/tags/form/fields")
                        .addAttribute("xmlns:jsp",
                                "http://java.sun.com/JSP/Page")
                        .addAttribute("version", "2.0")
                        .addChild(
                                new XmlElementBuilder("jsp:directive.page",
                                        document).addAttribute("contentType",
                                        "text/html;charset=UTF-8").build())
                        .addChild(
                                new XmlElementBuilder("jsp:output", document)
                                        .addAttribute("omit-xml-declaration",
                                                "yes").build()).build());

        // Add form update element
        final Element formUpdate = new XmlElementBuilder("form:update",
                document)
                .addAttribute(
                        "id",
                        XmlUtils.convertId("fu:"
                                + formBackingType.getFullyQualifiedTypeName()))
                .addAttribute("modelAttribute", entityName).build();

        if (!controllerPath.equalsIgnoreCase(formBackingType
                .getSimpleTypeName())) {
            formUpdate.setAttribute("path", controllerPath);
        }
        if (!"id".equals(formBackingTypePersistenceMetadata
                .getIdentifierField().getFieldName().getSymbolName())) {
            formUpdate.setAttribute("idField",
                    formBackingTypePersistenceMetadata.getIdentifierField()
                            .getFieldName().getSymbolName());
        }
        final MethodMetadata versionAccessorMethod = formBackingTypePersistenceMetadata
                .getVersionAccessorMethod();
        if (versionAccessorMethod == null) {
            formUpdate.setAttribute("versionField", "none");
        }
        else {
            final String methodName = versionAccessorMethod.getMethodName()
                    .getSymbolName();
            formUpdate.setAttribute("versionField",
                    methodName.substring("get".length()));
        }

        // Filter out embedded ID fields as they represent the composite PK
        // which is not to be updated.
        final List<FieldMetadata> fieldCopy = new ArrayList<FieldMetadata>(
                fields);
        for (final FieldMetadata embeddedField : formBackingTypePersistenceMetadata
                .getRooIdentifierFields()) {
            for (int i = 0; i < fieldCopy.size(); i++) {
                // Make sure form fields are not presented twice.
                if (fieldCopy.get(i).getFieldName()
                        .equals(embeddedField.getFieldName())) {
                    fieldCopy.remove(i);
                }
            }
        }

        createFieldsForCreateAndUpdate(fieldCopy, document, formUpdate, false);
        formUpdate.setAttribute("z",
                XmlRoundTripUtils.calculateUniqueKeyFor(formUpdate));
        div.appendChild(formUpdate);

        return document;
    }

    private boolean isEmailField(final FieldMetadata field) {
        return STRING.equals(field.getFieldType())
                && uncapitalize(field.getFieldName().getSymbolName()).contains(
                        "email");
    }

    private String uncapitalize(final String term) {
        // [ROO-1790] this is needed to adhere to the JavaBean naming
        // conventions (see JavaBean spec section 8.8)
        return Introspector.decapitalize(StringUtils.capitalize(term));
    }
}
