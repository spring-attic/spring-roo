package org.springframework.roo.classpath.operations.jsr303;

import static org.springframework.roo.model.JpaJavaType.EMBEDDED;

import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * This field is intended for use with JSR 220 and will create a @Embedded
 * annotation.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class EmbeddedField extends FieldDetails {

    public EmbeddedField(final String physicalTypeIdentifier,
            final JavaType fieldType, final JavaSymbolName fieldName) {
        super(physicalTypeIdentifier, fieldType, fieldName);
    }

    @Override
    public void decorateAnnotationsList(
            final List<AnnotationMetadataBuilder> annotations) {
        super.decorateAnnotationsList(annotations);
        annotations.add(new AnnotationMetadataBuilder(EMBEDDED));
    }
}
