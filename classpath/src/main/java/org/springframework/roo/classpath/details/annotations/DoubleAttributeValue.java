package org.springframework.roo.classpath.details.annotations;

import org.springframework.roo.model.JavaSymbolName;

/**
 * Represents a double annotation attribute value.
 *
 * @author Ben Alex
 * @since 1.0
 */
public class DoubleAttributeValue extends AbstractAnnotationAttributeValue<Double> {

	// Fields
	private final double value;
	private boolean floatingPrecisionOnly = false;

	public DoubleAttributeValue(final JavaSymbolName name, final double value, final boolean floatingPrecisionOnly) {
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

	@Override
	public String toString() {
		return getName() + " -> " + new Double(value).toString();
	}
}
