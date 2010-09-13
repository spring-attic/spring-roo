package org.springframework.roo.classpath.details;

import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;

/**
 * Indicates an {@link IdentifiableJavaStructure} which can also be annotated.
 * 
 * @author Ben Alex
 * @since 1.1
 *
 */
public interface IdentifiableAnnotatedJavaStructure extends IdentifiableJavaStructure {

	/**
	 * @return annotations on this structure (never null, but may be empty)
	 */
	List<AnnotationMetadata> getAnnotations();

}
