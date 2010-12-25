package org.springframework.roo.classpath.details.annotations.populator;

import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

/**
 * Abstract class that provides a convenience parser and holder for annotation values. Useful if an add-on
 * needs to share annotation parsing outcomes between its provider and metadata instances.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public abstract class AbstractAnnotationValues {
	
	/** Indicates whether the class was able to be parsed at all (ie the metadata was properly formed) */
	protected boolean classParsed = false;
	
	protected AnnotationMetadata annotationMetadata = null;
	
	protected ClassOrInterfaceTypeDetails governorTypeDetails = null;
	
	/**
	 * Parses the {@link PhysicalTypeMetadata} for the requested annotation {@link JavaType}. If found, makes
	 * the annotation available via the {@link #annotationMetadata} field. Subclasses will then generally use
	 * {@link AutoPopulationUtils#populate(Object, AnnotationMetadata)} to complete the configuration of the
	 * subclass (we don't invoke {@link AutoPopulationUtils} from this constructor because the subclass is
	 * likely to have set default values for each field, and these will be overwritten when the control flow
	 * returns to the subclass constructor).
	 * 
	 * <p>
	 * If the {@link PhysicalTypeMetadata} cannot be parsed or does not internally contain a
	 * {@link ClassOrInterfaceTypeDetails}, no attempt will be made to populate the values. 
	 *  
	 * @param governorPhysicalTypeMetadata to parse (required)
	 * @param annotationType to locate and parse (required)
	 */
	public AbstractAnnotationValues(PhysicalTypeMetadata governorPhysicalTypeMetadata, JavaType annotationType) {
		Assert.notNull(governorPhysicalTypeMetadata, "Governor physical type metadata required");
		Assert.notNull(annotationType, "Annotation to locate is required");

		PhysicalTypeDetails physicalTypeDetails = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
		
		if (physicalTypeDetails != null && physicalTypeDetails instanceof ClassOrInterfaceTypeDetails) {
			classParsed = true;
			
			// We have reliable physical type details
			this.governorTypeDetails = (ClassOrInterfaceTypeDetails) physicalTypeDetails;
			
			// Process values from the annotation, if present
			this.annotationMetadata = MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, annotationType);
		}
	}
	
	/**
	 * @return the type which declared the annotation (ie the governor; never returns null) 
	 */
	public ClassOrInterfaceTypeDetails getGovernorTypeDetails() {
		return governorTypeDetails;
	}

	public boolean isClassParsed() {
		return classParsed;
	}

	public boolean isAnnotationFound() {
		return annotationMetadata != null;
	}
	
}
