package org.springframework.roo.classpath.operations.jsr303;

import static org.springframework.roo.model.JpaJavaType.COLUMN;
import static org.springframework.roo.model.Jsr303JavaType.NOT_NULL;
import static org.springframework.roo.model.Jsr303JavaType.NULL;
import static org.springframework.roo.model.SpringJavaType.VALUE;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.BooleanAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Base class containing common JSR 303 and JSR 220 properties that can be
 * auto-generated.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class FieldDetails {

    /** The JPA @Column value */
    private String column;

    /** Any JavaDoc comments (reserved for future expansion) */
    protected String comment = "";

    /** The name of the field to be added */
    private final JavaSymbolName fieldName;

    /** The type of field to be added */
    private final JavaType fieldType;

    /** Whether the JSR 303 @NotNull annotation will be added */
    private boolean notNull;

    /** Whether the JSR 303 @Null annotation will be added */
    private boolean nullRequired;

    /** The type that will receive the field */
    private final String physicalTypeIdentifier;

    /** Whether unique = true is added to the @Column annotation */
    private boolean unique;

    /** The Spring @Value value **/
    private String value;

    /**
     * Constructor
     * 
     * @param physicalTypeIdentifier
     * @param fieldType
     * @param fieldName
     */
    public FieldDetails(final String physicalTypeIdentifier,
            final JavaType fieldType, final JavaSymbolName fieldName) {
        Validate.isTrue(PhysicalTypeIdentifier.isValid(physicalTypeIdentifier),
                "Destination physical type identifier is invalid");
        Validate.notNull(fieldType, "Field type required");
        Validate.notNull(fieldName, "Field name required");
        this.physicalTypeIdentifier = physicalTypeIdentifier;
        this.fieldType = fieldType;
        this.fieldName = fieldName;
    }

    public void decorateAnnotationsList(
            final List<AnnotationMetadataBuilder> annotations) {
        Validate.notNull(annotations);

        if (notNull) {
            annotations.add(new AnnotationMetadataBuilder(NOT_NULL));
        }

        if (nullRequired) {
            annotations.add(new AnnotationMetadataBuilder(NULL));
        }

        AnnotationMetadataBuilder columnBuilder = null;
        if (column != null) {
            final List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
            attrs.add(new StringAttributeValue(new JavaSymbolName("name"),
                    column));
            columnBuilder = new AnnotationMetadataBuilder(COLUMN, attrs);
        }
        if (unique) {
            if (columnBuilder != null) {
                columnBuilder.addBooleanAttribute("unique", true);
            }
            else {
                final List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
                attrs.add(new BooleanAttributeValue(
                        new JavaSymbolName("unique"), true));
                columnBuilder = new AnnotationMetadataBuilder(COLUMN, attrs);
            }
        }
        if (value != null) {
            final List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
            attrs.add(new StringAttributeValue(new JavaSymbolName("value"),
                    value));
            annotations.add(new AnnotationMetadataBuilder(VALUE, attrs));
        }
        if (fieldName.getSymbolName().equals("created")) {
            if (columnBuilder == null) {
                columnBuilder = new AnnotationMetadataBuilder(COLUMN);
            }
            columnBuilder.addBooleanAttribute("updatable", false);
        }

        if (columnBuilder != null) {
            annotations.add(columnBuilder);
        }
    }

    public String getColumn() {
        return column;
    }

    public String getComment() {
        return comment;
    }

    public JavaSymbolName getFieldName() {
        return fieldName;
    }

    public JavaType getFieldType() {
        return fieldType;
    }

    public String getPhysicalTypeIdentifier() {
        return physicalTypeIdentifier;
    }

    public String getValue() {
        return value;
    }

    public boolean isNotNull() {
        return notNull;
    }

    public boolean isNullRequired() {
        return nullRequired;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setColumn(final String column) {
        this.column = column;
    }

    public void setComment(final String comment) {
        if (StringUtils.isNotBlank(comment)) {
            this.comment = comment;
        }
    }

    public void setNotNull(final boolean notNull) {
        this.notNull = notNull;
    }

    public void setNullRequired(final boolean nullRequired) {
        this.nullRequired = nullRequired;
    }

    public void setUnique(final boolean unique) {
        this.unique = unique;
    }

    public void setValue(final String value) {
        this.value = value;
    }
}
