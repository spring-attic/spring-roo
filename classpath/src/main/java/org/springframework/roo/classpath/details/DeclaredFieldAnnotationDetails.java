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
	private boolean removeAnnotation;

	/**
	 * Constructor must contain {@link FieldMetadata} of existing field (may already contain field annotations) and a list
	 * of new Annotations which should be introduced by an AspectJ ITD. The added annotations can not already be present 
	 * in {@link FieldMetadata}.
	 * 
	 * @param fieldMetadata FieldMetadata of existing field (may not be null)
	 * @param fieldAnnotation Annotation to be added to field via an ITD (may not be null)
	 * @param removeAnnotation if true, will cause the specified annotation to be REMOVED via AspectJ's "-" syntax (usually would be false)
	 */
	public DeclaredFieldAnnotationDetails(FieldMetadata fieldMetadata, AnnotationMetadata fieldAnnotation, boolean removeAnnotation) {
		Assert.notNull(fieldMetadata, "Field metadata required");
		Assert.notNull(fieldAnnotation, "Field annotation required");
		if (removeAnnotation) {
			Assert.isTrue(fieldAnnotation.getAttributeNames().isEmpty(), "Field annotation '@" + fieldAnnotation.getAnnotationType().getSimpleTypeName() + "' (on the target field '" + fieldMetadata.getFieldType().getFullyQualifiedTypeName() + "." + fieldMetadata.getFieldName().getSymbolName() + ") must not have any attributes when requesting its removal");
		}
		this.fieldMetadata = fieldMetadata;
		this.fieldAnnotation = fieldAnnotation;
		this.removeAnnotation = removeAnnotation;
	}

	/**
	 * Overloaded constructor which is used in the most typical case of ADDING an annotation to a field, not removing one.
	 * 
	 * @param fieldMetadata FieldMetadata of existing field (may not be null)
	 * @param fieldAnnotation Annotation to be added to field via an ITD (may not be null)
	 */
	public DeclaredFieldAnnotationDetails(FieldMetadata fieldMetadata, AnnotationMetadata fieldAnnotation) {
		this(fieldMetadata, fieldAnnotation, false);
	}

	public FieldMetadata getFieldMetadata() {
		return fieldMetadata;
	}

	public AnnotationMetadata getFieldAnnotation() {
		return fieldAnnotation;
	}

	public final boolean isRemoveAnnotation() {
		return removeAnnotation;
	}
}
