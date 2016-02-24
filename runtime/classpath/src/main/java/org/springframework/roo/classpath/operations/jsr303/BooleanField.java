package org.springframework.roo.classpath.operations.jsr303;

import static org.springframework.roo.model.Jsr303JavaType.ASSERT_FALSE;
import static org.springframework.roo.model.Jsr303JavaType.ASSERT_TRUE;

import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

public class BooleanField extends FieldDetails {

    /** Whether the JSR 303 @AssertFalse annotation will be added */
    private boolean assertFalse;

    /** Whether the JSR 303 @AssertTrue annotation will be added */
    private boolean assertTrue;

    public BooleanField(final String physicalTypeIdentifier,
            final JavaType fieldType, final JavaSymbolName fieldName) {
        super(physicalTypeIdentifier, fieldType, fieldName);
    }

    @Override
    public void decorateAnnotationsList(
            final List<AnnotationMetadataBuilder> annotations) {
        super.decorateAnnotationsList(annotations);
        if (assertTrue) {
            annotations.add(new AnnotationMetadataBuilder(ASSERT_TRUE));
        }
        if (assertFalse) {
            annotations.add(new AnnotationMetadataBuilder(ASSERT_FALSE));
        }
    }

    public boolean isAssertFalse() {
        return assertFalse;
    }

    public boolean isAssertTrue() {
        return assertTrue;
    }

    public void setAssertFalse(final boolean assertFalse) {
        this.assertFalse = assertFalse;
    }

    public void setAssertTrue(final boolean assertTrue) {
        this.assertTrue = assertTrue;
    }
}
