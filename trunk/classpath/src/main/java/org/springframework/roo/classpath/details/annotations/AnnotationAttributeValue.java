package org.springframework.roo.classpath.details.annotations;

import org.springframework.roo.model.JavaSymbolName;

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
