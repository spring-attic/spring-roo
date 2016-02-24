package org.springframework.roo.classpath.details.annotations;

import org.springframework.roo.model.JavaSymbolName;

/**
 * Represents a boolean annotation attribute value.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class BooleanAttributeValue extends
        AbstractAnnotationAttributeValue<Boolean> {

    private final boolean value;

    /**
     * Constructor
     * 
     * @param name
     * @param value
     */
    public BooleanAttributeValue(final JavaSymbolName name, final boolean value) {
        super(name);
        this.value = value;
    }

    public Boolean getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getName() + " -> " + value;
    }
}
