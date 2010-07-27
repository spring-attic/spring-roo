package org.springframework.roo.classpath.details.annotations;

import org.springframework.roo.model.JavaSymbolName;

/**
 * Represent an annotation attribute value.
 * 
 * <p>
 * Implementations must correctly meet the contractual requirements of
 * {@link Object#equals(Object)} and {@link Object#hashCode()}.
 *
 * @author Ben Alex
 * @since 1.0
 */
public interface AnnotationAttributeValue<T extends Object> {

	/**
	 * @return the name of the attribute (never null; often the name will be "value")
	 */
	JavaSymbolName getName();
	
	/**
	 * @return the value (never null)
	 */
	T getValue();
}
