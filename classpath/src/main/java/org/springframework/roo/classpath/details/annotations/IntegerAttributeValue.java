package org.springframework.roo.classpath.details.annotations;

import org.springframework.roo.model.JavaSymbolName;


/**
 * Represents an integer annotation attribute value.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class IntegerAttributeValue extends AbstractAnnotationAttributeValue<Integer> {
	
	// Fields
	private final int value;
	
	/**
	 * Constructor
	 *
	 * @param name
	 * @param value
	 */
	public IntegerAttributeValue(JavaSymbolName name, int value) {
		super(name);
		this.value = value;
	}

	public Integer getValue() {
		return value;
	}
	
	public String toString() {
		return getName() + " -> " + value;
	}
}
