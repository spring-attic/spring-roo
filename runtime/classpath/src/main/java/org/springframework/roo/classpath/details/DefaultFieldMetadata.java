package org.springframework.roo.classpath.details;

import java.lang.reflect.Modifier;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.comments.CommentStructure;
import org.springframework.roo.model.CustomData;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Default implementation of {@link FieldMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class DefaultFieldMetadata extends
        AbstractIdentifiableAnnotatedJavaStructureProvider implements
        FieldMetadata {

    private final String fieldInitializer;
    private final JavaSymbolName fieldName;
    private final JavaType fieldType;
    private CommentStructure commentStructure;

    // Package protected to mandate the use of FieldMetadataBuilder
    DefaultFieldMetadata(final CustomData customData,
            final String declaredByMetadataId, final int modifier,
            final List<AnnotationMetadata> annotations,
            final JavaSymbolName fieldName, final JavaType fieldType,
            final String fieldInitializer) {
        super(customData, declaredByMetadataId, modifier, annotations);
        Validate.notBlank(declaredByMetadataId,
                "Declared by metadata ID required");
        Validate.notNull(fieldName, "Field name required");
        Validate.notNull(fieldType, "Field type required");
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.fieldInitializer = fieldInitializer;
    }

    @Override
    public CommentStructure getCommentStructure() {
        return commentStructure;
    }

    @Override
    public void setCommentStructure(CommentStructure commentStructure) {
        this.commentStructure = commentStructure;
    }

    public String getFieldInitializer() {
        return fieldInitializer;
    }

    public JavaSymbolName getFieldName() {
        return fieldName;
    }

    public JavaType getFieldType() {
        return fieldType;
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("declaredByMetadataId", getDeclaredByMetadataId());
        builder.append("modifier", Modifier.toString(getModifier()));
        builder.append("fieldType", fieldType);
        builder.append("fieldName", fieldName);
        builder.append("fieldInitializer", fieldInitializer);
        builder.append("annotations", getAnnotations());
        builder.append("customData", getCustomData());
        return builder.toString();
    }
}
