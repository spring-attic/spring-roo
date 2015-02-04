package org.springframework.roo.classpath.customdata.tagkeys;

import java.util.List;

import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * {@link FieldMetadata}-specific implementation of
 * {@link IdentifiableAnnotatedJavaStructureCustomDataKey}.
 * 
 * @author James Tyrrell
 * @since 1.1.3
 */
public class FieldMetadataCustomDataKey extends
        IdentifiableAnnotatedJavaStructureCustomDataKey<FieldMetadata> {
    private String fieldInitializer;
    private JavaSymbolName fieldName;
    private JavaType fieldType;
    private String name;

    public FieldMetadataCustomDataKey(final Integer modifier,
            final List<AnnotationMetadata> annotations) {
        super(modifier, annotations);
    }

    public FieldMetadataCustomDataKey(final Integer modifier,
            final List<AnnotationMetadata> annotations,
            final JavaType fieldType, final JavaSymbolName fieldName,
            final String fieldInitializer) {
        super(modifier, annotations);
        this.fieldType = fieldType;
        this.fieldName = fieldName;
        this.fieldInitializer = fieldInitializer;
    }

    public FieldMetadataCustomDataKey(final String name) {
        super(null, null);
        this.name = name;
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
    public boolean meets(final FieldMetadata field) {
        // TODO: Add in validation logic for fieldType, fieldName,
        // fieldInitializer
        return super.meets(field);
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
