package org.springframework.roo.classpath.details;

import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.support.util.Assert;

/**
 * Convenience class to hold annotation details which should be introduced
 * to a method via an AspectJ ITD
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public final class DeclaredMethodAnnotationDetails {
	
	private MethodMetadata methodMetadata;
	
	private AnnotationMetadata methodAnnotation;

	/**
	 * Contructor must contain {@link MethodMetadata} of existing method (may already contain method annotations) and a list
	 * of new Annotations which should be introduced by an AspectJ ITD. The added annotations can not already be present 
	 * in {@link MethodMetadata}.
	 * 
	 * @param methodMetadata MethodMetadata of existing method (may not be null)
	 * @param methodAnnotation Annotation to be added to field via an ITD (may not be null)
	 */
	public DeclaredMethodAnnotationDetails(MethodMetadata methodMetadata, AnnotationMetadata methodAnnotation) {
		Assert.notNull(methodMetadata, "Method metadata required");
		Assert.notNull(methodAnnotation, "Method annotation required");
		this.methodMetadata = methodMetadata;
		this.methodAnnotation = methodAnnotation;
	}

	public MethodMetadata getMethodMetadata() {
		return methodMetadata;
	}

	public AnnotationMetadata getMethodAnnotation() {
		return methodAnnotation;
	}
}
