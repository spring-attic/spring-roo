package org.springframework.roo.classpath.details.annotations;

import org.springframework.roo.model.JavaSymbolName;


/**
 * Represents a long annotation attribute value.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class LongAttributeValue extends AbstractAnnotationAttributeValue<Long> {
	private long value;
	
	public LongAttributeValue(JavaSymbolName name, long value) {
		super(name);
		this.value = value;
	}

	public Long getValue() {
		return value;
	}
	
	public String toString() {
		return getName() + " -> " + new Long(value).toString();
	}
}
