package org.springframework.roo.classpath.details.annotations;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.support.util.Assert;

/**
 * Represents a {@link String} annotation attribute value.
 * 
 * <p>
 * Source code parsers should treat any quoted string as a {@link StringAttributeValue}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class StringAttributeValue extends AbstractAnnotationAttributeValue<String> {
	private String value;
	
	public StringAttributeValue(JavaSymbolName name, String value) {
		super(name);
		Assert.notNull(value, "Value required");
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public String toString() {
		return getName() + " -> " + value;
	}
}
