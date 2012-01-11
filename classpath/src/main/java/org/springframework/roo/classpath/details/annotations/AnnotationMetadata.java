package org.springframework.roo.classpath.details.annotations;

import java.util.List;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Metadata concerning a particular annotation appearing on a member.
 * 
 * @author Ben Alex
 * @author Andrew Swan
 * @since 1.0
 */
public interface AnnotationMetadata {

    /**
     * @return the annotation type (never null)
     */
    JavaType getAnnotationType();

    /**
     * Acquires an attribute value for the requested name.
     * 
     * @param attributeName
     * @return the requested attribute (or null if not found)
     */
    AnnotationAttributeValue<?> getAttribute(JavaSymbolName attributeName);

    /**
     * Returns the value of the given attribute
     * 
     * @param attributeName
     * @return the requested attribute (or null if not found)
     * @since 1.2.0
     */
    <T> AnnotationAttributeValue<T> getAttribute(String attributeName);

    /**
     * @return the attribute names, preferably in the order they are declared in
     *         the annotation (never null, but may be empty)
     */
    List<JavaSymbolName> getAttributeNames();
}
