package org.springframework.roo.classpath.details.annotations;

import org.springframework.roo.model.JavaSymbolName;

/**
 * Represents a double annotation attribute value.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class DoubleAttributeValue extends
        AbstractAnnotationAttributeValue<Double> {

    private boolean floatingPrecisionOnly = false;
    private final double value;

    public DoubleAttributeValue(final JavaSymbolName name, final double value,
            final boolean floatingPrecisionOnly) {
        super(name);
        this.value = value;
        this.floatingPrecisionOnly = floatingPrecisionOnly;
    }

    public Double getValue() {
        return value;
    }

    public boolean isFloatingPrecisionOnly() {
        return floatingPrecisionOnly;
    }

    @Override
    public String toString() {
        return getName() + " -> " + new Double(value).toString();
    }
}
