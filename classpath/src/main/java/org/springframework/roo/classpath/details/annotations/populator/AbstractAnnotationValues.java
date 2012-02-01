package org.springframework.roo.classpath.details.annotations.populator;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.MemberHoldingTypeDetailsMetadataItem;
import org.springframework.roo.model.JavaType;

/**
 * Abstract class that provides a convenience parser and holder for annotation
 * values. Useful if an add-on needs to share annotation parsing outcomes
 * between its provider and metadata instances.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public abstract class AbstractAnnotationValues {

    protected AnnotationMetadata annotationMetadata;

    /**
     * Indicates whether the class was able to be parsed at all (ie the metadata
     * was properly formed)
     */
    protected boolean classParsed;

    protected ClassOrInterfaceTypeDetails governorTypeDetails;

    protected AbstractAnnotationValues(
            final MemberHoldingTypeDetails memberHoldingTypeDetails,
            final JavaType annotationType) {
        Validate.notNull(annotationType, "Annotation to locate is required");

        if (memberHoldingTypeDetails instanceof ClassOrInterfaceTypeDetails) {
            classParsed = true;

            // We have reliable physical type details
            governorTypeDetails = (ClassOrInterfaceTypeDetails) memberHoldingTypeDetails;

            // Process values from the annotation, if present
            annotationMetadata = governorTypeDetails
                    .getAnnotation(annotationType);
        }
    }

    /**
     * Convenience constructor that takes a {@link Class} for the annotation
     * type
     * 
     * @param governorMetadata to parse (can be <code>null</code>)
     * @param annotationType the annotation class (required)
     */
    protected AbstractAnnotationValues(
            final MemberHoldingTypeDetailsMetadataItem<?> governorMetadata,
            final Class<?> annotationType) {
        this(governorMetadata, new JavaType(annotationType));
    }

    /**
     * Parses the governor's metadata for the requested annotation
     * {@link JavaType}. If found, makes the annotation available via the
     * {@link #annotationMetadata} field. Subclasses will then generally use
     * {@link AutoPopulationUtils#populate(Object, AnnotationMetadata)} to
     * complete the configuration of the subclass (we don't invoke
     * {@link AutoPopulationUtils} from this constructor because the subclass is
     * likely to have set default values for each field, and these will be
     * overwritten when the control flow returns to the subclass constructor).
     * <p>
     * If the {@link PhysicalTypeMetadata} cannot be parsed or does not
     * internally contain a {@link ClassOrInterfaceTypeDetails}, no attempt will
     * be made to populate the values.
     * 
     * @param governorMetadata to parse (can be <code>null</code>)
     * @param annotationType to locate and parse (can be <code>null</code>)
     */
    protected AbstractAnnotationValues(
            final MemberHoldingTypeDetailsMetadataItem<?> governorMetadata,
            final JavaType annotationType) {
        Validate.notNull(annotationType, "Annotation to locate is required");

        if (governorMetadata != null) {
            final Object governorDetails = governorMetadata
                    .getMemberHoldingTypeDetails();

            if (governorDetails instanceof ClassOrInterfaceTypeDetails) {
                classParsed = true;

                // We have reliable physical type details
                governorTypeDetails = (ClassOrInterfaceTypeDetails) governorDetails;

                // Process values from the annotation, if present
                annotationMetadata = governorTypeDetails
                        .getAnnotation(annotationType);
            }
        }
    }

    /**
     * @return the type which declared the annotation (ie the governor; never
     *         returns null)
     */
    public ClassOrInterfaceTypeDetails getGovernorTypeDetails() {
        return governorTypeDetails;
    }

    public boolean isAnnotationFound() {
        return annotationMetadata != null;
    }

    public boolean isClassParsed() {
        return classParsed;
    }
}
