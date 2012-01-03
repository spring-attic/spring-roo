package org.springframework.roo.classpath.details.annotations;

import org.springframework.roo.model.JavaSymbolName;

/**
 * Represent an annotation attribute value.
 * <p>
 * Implementations must correctly meet the contractual requirements of
 * {@link #equals(Object)} and {@link #hashCode()}.
 * 
 * @author Ben Alex
 * @since 1.0
 * @param <T> the type of value this attribute contains
 */
public interface AnnotationAttributeValue<T> {

    /**
     * @return the name of the attribute (never null; often the name will be
     *         "value")
     */
    JavaSymbolName getName();

    /**
     * @return the value (never null)
     */
    T getValue();
}
