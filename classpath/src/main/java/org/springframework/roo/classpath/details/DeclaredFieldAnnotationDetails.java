package org.springframework.roo.classpath.details;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;

/**
 * Convenience class to hold annotation details which should be introduced to a
 * field via an AspectJ ITD
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public class DeclaredFieldAnnotationDetails {

    private final FieldMetadata field;
    private final AnnotationMetadata fieldAnnotation;
    private final boolean removeAnnotation;

    /**
     * Overloaded constructor which is used in the most typical case of ADDING
     * an annotation to a field, not removing one.
     * 
     * @param field FieldMetadata of existing field (may not be null)
     * @param fieldAnnotation Annotation to be added to field via an ITD (may
     *            not be null)
     */
    public DeclaredFieldAnnotationDetails(final FieldMetadata field,
            final AnnotationMetadata fieldAnnotation) {
        this(field, fieldAnnotation, false);
    }

    /**
     * Constructor must contain {@link FieldMetadata} of existing field (may
     * already contain field annotations) and a list of new Annotations which
     * should be introduced by an AspectJ ITD. The added annotations can not
     * already be present in {@link FieldMetadata}.
     * 
     * @param field FieldMetadata of existing field (may not be null)
     * @param fieldAnnotation Annotation to be added to field via an ITD (may
     *            not be null)
     * @param removeAnnotation if true, will cause the specified annotation to
     *            be REMOVED via AspectJ's "-" syntax (usually would be false)
     */
    public DeclaredFieldAnnotationDetails(final FieldMetadata field,
            final AnnotationMetadata fieldAnnotation,
            final boolean removeAnnotation) {
        Validate.notNull(field, "Field metadata required");
        Validate.notNull(fieldAnnotation, "Field annotation required");
        if (removeAnnotation) {
            Validate.isTrue(
                    fieldAnnotation.getAttributeNames().isEmpty(),
                    "Field annotation '@"
                            + fieldAnnotation.getAnnotationType()
                                    .getSimpleTypeName()
                            + "' (on the target field '"
                            + field.getFieldType().getFullyQualifiedTypeName()
                            + "."
                            + field.getFieldName().getSymbolName()
                            + ") must not have any attributes when requesting its removal");
        }
        this.field = field;
        this.fieldAnnotation = fieldAnnotation;
        this.removeAnnotation = removeAnnotation;
    }

    public FieldMetadata getField() {
        return field;
    }

    public AnnotationMetadata getFieldAnnotation() {
        return fieldAnnotation;
    }

    public final boolean isRemoveAnnotation() {
        return removeAnnotation;
    }
}
