package org.springframework.roo.classpath.details;

import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.support.util.Assert;

/**
 * Convenience class to hold annotation details which should be introduced
 * to a field via an AspectJ ITD
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public final class DeclaredFieldAnnotationDetails {
	
	private FieldMetadata fieldMetadata;
	
	private AnnotationMetadata fieldAnnotation;

	/**
	 * Contructor must contain {@link FieldMetadata} of existing field (may already contain field annotations) and a list
	 * of new Annotations which should be introduced by an AspectJ ITD. The added annotations can not already be present 
	 * in {@link FieldMetadata}.
	 * 
	 * @param fieldMetadata FieldMetadata of existing field (may not be null)
	 * @param fieldAnnotation Annotation to be added to field via an ITD (may not be null)
	 */
	public DeclaredFieldAnnotationDetails(FieldMetadata fieldMetadata, AnnotationMetadata fieldAnnotation) {
		Assert.notNull(fieldMetadata, "Field metadata required");
		Assert.notNull(fieldAnnotation, "Field annotation required");
		this.fieldMetadata = fieldMetadata;
		this.fieldAnnotation = fieldAnnotation;
	}

	public FieldMetadata getFieldMetadata() {
		return fieldMetadata;
	}

	public AnnotationMetadata getFieldAnnotation() {
		return fieldAnnotation;
	}
}
