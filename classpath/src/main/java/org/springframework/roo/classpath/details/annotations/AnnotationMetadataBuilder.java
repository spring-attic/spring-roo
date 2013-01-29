package org.springframework.roo.classpath.details.annotations;

import static org.springframework.roo.model.JpaJavaType.COLUMN;
import static org.springframework.roo.model.JpaJavaType.EMBEDDED;
import static org.springframework.roo.model.JpaJavaType.EMBEDDED_ID;
import static org.springframework.roo.model.JpaJavaType.ENUMERATED;
import static org.springframework.roo.model.JpaJavaType.ID;
import static org.springframework.roo.model.JpaJavaType.LOB;
import static org.springframework.roo.model.JpaJavaType.MANY_TO_MANY;
import static org.springframework.roo.model.JpaJavaType.MANY_TO_ONE;
import static org.springframework.roo.model.JpaJavaType.ONE_TO_MANY;
import static org.springframework.roo.model.JpaJavaType.ONE_TO_ONE;
import static org.springframework.roo.model.JpaJavaType.TRANSIENT;
import static org.springframework.roo.model.JpaJavaType.VERSION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.details.comments.CommentStructure;
import org.springframework.roo.model.Builder;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Builder for {@link AnnotationMetadata}.
 * <p>
 * The "add" method will replace any existing annotation attribute with the same
 * name, taking care to preserve its location.
 * 
 * @author Ben Alex
 * @author Andrew Swan
 * @since 1.1
 */
public class AnnotationMetadataBuilder implements Builder<AnnotationMetadata> {

    public static final AnnotationMetadata JPA_COLUMN_ANNOTATION = getInstance(COLUMN);
    public static final AnnotationMetadata JPA_EMBEDDED_ANNOTATION = getInstance(EMBEDDED);
    public static final AnnotationMetadata JPA_EMBEDDED_ID_ANNOTATION = getInstance(EMBEDDED_ID);
    public static final AnnotationMetadata JPA_ENUMERATED_ANNOTATION = getInstance(ENUMERATED);
    public static final AnnotationMetadata JPA_ID_ANNOTATION = getInstance(ID);
    public static final AnnotationMetadata JPA_LOB_ANNOTATION = getInstance(LOB);
    public static final AnnotationMetadata JPA_MANY_TO_MANY_ANNOTATION = getInstance(MANY_TO_MANY);
    public static final AnnotationMetadata JPA_MANY_TO_ONE_ANNOTATION = getInstance(MANY_TO_ONE);
    public static final AnnotationMetadata JPA_ONE_TO_MANY_ANNOTATION = getInstance(ONE_TO_MANY);
    public static final AnnotationMetadata JPA_ONE_TO_ONE_ANNOTATION = getInstance(ONE_TO_ONE);
    public static final AnnotationMetadata JPA_TRANSIENT_ANNOTATION = getInstance(TRANSIENT);
    public static final AnnotationMetadata JPA_VERSION_ANNOTATION = getInstance(VERSION);

    /**
     * Returns the metadata for the existing annotation, with no attribute
     * values
     * 
     * @param annotationType the annotation type (required)
     * @return a non-<code>null</code> instance
     * @since 1.2.0
     */
    public static AnnotationMetadata getInstance(final Class<?> annotationType) {
        return new AnnotationMetadataBuilder(annotationType).build();
    }

    public static AnnotationMetadata getInstance(final JavaType annotationType) {
        return new AnnotationMetadataBuilder(annotationType).build();
    }

    public static AnnotationMetadataBuilder getInstance(
            final JavaType annotationType,
            final Collection<AnnotationAttributeValue<?>> attributeValues) {
        return new AnnotationMetadataBuilder(annotationType, attributeValues);
    }

    /**
     * Returns the metadata for the existing annotation, with no attribute
     * values
     * 
     * @param annotationType the fully-qualified name of the annotation type
     *            (required)
     * @return a non-<code>null</code> instance
     * @since 1.2.0
     */
    public static AnnotationMetadata getInstance(final String annotationType) {
        return new AnnotationMetadataBuilder(annotationType).build();
    }

    private JavaType annotationType;
    private final Map<String, AnnotationAttributeValue<?>> attributeValues = new LinkedHashMap<String, AnnotationAttributeValue<?>>();
    private CommentStructure commentStructure;

    /**
     * Constructor. The caller must set the annotation type via
     * {@link #setAnnotationType(JavaType)} before calling {@link #build()}
     */
    public AnnotationMetadataBuilder() {
    }

    /**
     * Constructor for using an existing {@link AnnotationMetadata} as a
     * baseline for building a new instance.
     * 
     * @param existing required
     */
    public AnnotationMetadataBuilder(final AnnotationMetadata existing) {
        Validate.notNull(existing);
        annotationType = existing.getAnnotationType();
        for (final JavaSymbolName attributeName : existing.getAttributeNames()) {
            attributeValues.put(attributeName.getSymbolName(),
                    existing.getAttribute(attributeName));
        }
        this.setCommentStructure(existing.getCommentStructure());
    }

    /**
     * Constructor for no initial attribute values
     * 
     * @param annotationType the annotation class (required)
     * @since 1.2.0
     */
    public AnnotationMetadataBuilder(final Class<?> annotationType) {
        this(new JavaType(annotationType));
    }

    /**
     * Constructor for no initial attribute values
     * 
     * @param annotationType
     */
    public AnnotationMetadataBuilder(final JavaType annotationType) {
        this.annotationType = annotationType;
    }

    /**
     * Constructor that accepts an optional list of values
     * 
     * @param annotationType
     * @param attributeValues can be <code>null</code>
     */
    public AnnotationMetadataBuilder(final JavaType annotationType,
            final Collection<AnnotationAttributeValue<?>> attributeValues) {
        this.annotationType = annotationType;
        setAttributes(attributeValues);
    }

    /**
     * Constructor for no initial attribute values
     * 
     * @param annotationType the fully-qualified name of the annotation type
     *            (required)
     */
    public AnnotationMetadataBuilder(final String annotationType) {
        this(new JavaType(annotationType));
    }

    public void addAttribute(final AnnotationAttributeValue<?> value) {
        // Locate existing attribute with this key and replace it
        attributeValues.put(value.getName().getSymbolName(), value);
    }

    public void addBooleanAttribute(final String key, final boolean value) {
        addAttribute(new BooleanAttributeValue(new JavaSymbolName(key), value));
    }

    public void addCharAttribute(final String key, final char value) {
        addAttribute(new CharAttributeValue(new JavaSymbolName(key), value));
    }

    /**
     * Adds an attribute with the given {@link JavaType} as its value
     * 
     * @param key the attribute name (required)
     * @param javaType the value (required)
     */
    public void addClassAttribute(final String key, final JavaType javaType) {
        addAttribute(new ClassAttributeValue(new JavaSymbolName(key), javaType));
    }

    public void addClassAttribute(final String key,
            final String fullyQualifiedTypeName) {
        addAttribute(new ClassAttributeValue(new JavaSymbolName(key),
                new JavaType(fullyQualifiedTypeName)));
    }

    public void addDoubleAttribute(final String key, final double value,
            final boolean floatingPrecisionOnly) {
        addAttribute(new DoubleAttributeValue(new JavaSymbolName(key), value,
                floatingPrecisionOnly));
    }

    public void addEnumAttribute(final String key, final EnumDetails details) {
        addAttribute(new EnumAttributeValue(new JavaSymbolName(key), details));
    }

    public void addEnumAttribute(final String key, final JavaType javaType,
            final JavaSymbolName enumConstant) {
        final EnumDetails details = new EnumDetails(javaType, enumConstant);
        addAttribute(new EnumAttributeValue(new JavaSymbolName(key), details));
    }

    public void addEnumAttribute(final String key, final JavaType javaType,
            final String enumConstant) {
        final EnumDetails details = new EnumDetails(javaType,
                new JavaSymbolName(enumConstant));
        addAttribute(new EnumAttributeValue(new JavaSymbolName(key), details));
    }

    public void addEnumAttribute(final String key,
            final String fullyQualifiedTypeName, final String enumConstant) {
        final EnumDetails details = new EnumDetails(new JavaType(
                fullyQualifiedTypeName), new JavaSymbolName(enumConstant));
        addAttribute(new EnumAttributeValue(new JavaSymbolName(key), details));
    }

    public void addIntegerAttribute(final String key, final int value) {
        addAttribute(new IntegerAttributeValue(new JavaSymbolName(key), value));
    }

    public void addLongAttribute(final String key, final long value) {
        addAttribute(new LongAttributeValue(new JavaSymbolName(key), value));
    }

    public void addStringAttribute(final String key, final String value) {
        addAttribute(new StringAttributeValue(new JavaSymbolName(key), value));
    }

    public AnnotationMetadata build() {

        DefaultAnnotationMetadata annotationMetadata = new DefaultAnnotationMetadata(getAnnotationType(),
                new ArrayList<AnnotationAttributeValue<?>>(getAttributes()
                        .values()));

        annotationMetadata.setCommentStructure(commentStructure);

        return annotationMetadata;
    }

    public JavaType getAnnotationType() {
        return annotationType;
    }

    public Map<String, AnnotationAttributeValue<?>> getAttributes() {
        return attributeValues;
    }

    public void removeAttribute(final String key) {
        // Locate existing attribute with this key and replace it
        attributeValues.remove(key);
    }

    public void setAnnotationType(final JavaType annotationType) {
        this.annotationType = annotationType;
    }

    /**
     * Sets the attribute values
     * 
     * @param attributeValues the values to set; can be <code>null</code> for
     *            none
     */
    public void setAttributes(
            final Collection<AnnotationAttributeValue<?>> attributeValues) {
        this.attributeValues.clear();
        if (attributeValues != null) {
            for (final AnnotationAttributeValue<?> attributeValue : attributeValues) {
                addAttribute(attributeValue);
            }
        }
    }

    public CommentStructure getCommentStructure() {
        return commentStructure;
    }

    public void setCommentStructure(CommentStructure commentStructure) {
        this.commentStructure = commentStructure;
    }
}
