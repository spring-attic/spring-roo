package org.springframework.roo.classpath.details.annotations;

import org.springframework.roo.model.JavaSymbolName;


/**
 * Represents a double annotation attribute value.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class DoubleAttributeValue extends AbstractAnnotationAttributeValue<Double> {
	private double value;
	private boolean floatingPrecisionOnly = false;
	
	public DoubleAttributeValue(JavaSymbolName name, double value, boolean floatingPrecisionOnly) {
		super(name);
		this.value = value;
		this.floatingPrecisionOnly = floatingPrecisionOnly;
	}

	public boolean isFloatingPrecisionOnly() {
		return floatingPrecisionOnly;
	}

	public Double getValue() {
		return value;
	}
	
	public String toString() {
		return getName() + " -> " + new Double(value).toString();
	}
}
