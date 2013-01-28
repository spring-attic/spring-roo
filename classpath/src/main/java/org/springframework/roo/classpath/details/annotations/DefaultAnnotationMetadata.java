package org.springframework.roo.classpath.details.annotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.details.comments.CommentStructure;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Default implementation of {@link AnnotationMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class DefaultAnnotationMetadata implements AnnotationMetadata {

    private final JavaType annotationType;
    private final Map<JavaSymbolName, AnnotationAttributeValue<?>> attributeMap;
    private final List<AnnotationAttributeValue<?>> attributes;
    private CommentStructure commentStructure;

    /**
     * Constructor
     * 
     * @param annotationType the type of annotation for which these are the
     *            metadata (required)
     * @param attributeValues the given annotation's values; can be
     *            <code>null</code>
     */
    DefaultAnnotationMetadata(final JavaType annotationType,
            final List<AnnotationAttributeValue<?>> attributeValues) {
        Validate.notNull(annotationType, "Annotation type required");
        this.annotationType = annotationType;
        attributes = new ArrayList<AnnotationAttributeValue<?>>();
        attributeMap = new HashMap<JavaSymbolName, AnnotationAttributeValue<?>>();
        if (attributeValues != null) {
            attributes.addAll(attributeValues);
            for (final AnnotationAttributeValue<?> value : attributeValues) {
                attributeMap.put(value.getName(), value);
            }
        }
    }

    public JavaType getAnnotationType() {
        return annotationType;
    }

    public AnnotationAttributeValue<?> getAttribute(
            final JavaSymbolName attributeName) {
        Validate.notNull(attributeName, "Attribute name required");
        return attributeMap.get(attributeName);
    }

    @SuppressWarnings("unchecked")
    public AnnotationAttributeValue<?> getAttribute(final String attributeName) {
        return getAttribute(new JavaSymbolName(attributeName));
    }

    public List<JavaSymbolName> getAttributeNames() {
        final List<JavaSymbolName> result = new ArrayList<JavaSymbolName>();
        for (final AnnotationAttributeValue<?> value : attributes) {
            result.add(value.getName());
        }
        return result;
    }

    public CommentStructure getCommentStructure() {
        return commentStructure;
    }

    public void setCommentStructure(CommentStructure commentStructure) {
        this.commentStructure = commentStructure;
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("annotationType", annotationType);
        builder.append("attributes", attributes);
        return builder.toString();
    }
}
