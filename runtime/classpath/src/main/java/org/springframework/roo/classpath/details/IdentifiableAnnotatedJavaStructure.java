package org.springframework.roo.classpath.details;

import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaType;

/**
 * Indicates an {@link IdentifiableJavaStructure} which can also be annotated.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public interface IdentifiableAnnotatedJavaStructure extends
        IdentifiableJavaStructure {

    /**
     * Locates the specified annotation on this structure.
     * 
     * @param type to locate (required)
     * @return the annotation, or <code>null</code> if not found
     * @since 1.2.0
     */
    AnnotationMetadata getAnnotation(final JavaType type);

    /**
     * @return annotations on this structure (never null, but may be empty)
     */
    List<AnnotationMetadata> getAnnotations();

    /**
     * Locates an annotation on this class and its superclasses.
     * 
     * @param annotationType annotation to locate (required)
     * @return the annotation, or <code>null</code> if not found
     * @since 1.2.0
     */
    AnnotationMetadata getTypeAnnotation(final JavaType annotationType);
}
