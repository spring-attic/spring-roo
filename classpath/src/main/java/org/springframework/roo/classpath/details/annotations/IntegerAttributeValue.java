package org.springframework.roo.classpath.details.annotations;

import org.springframework.roo.model.JavaSymbolName;

/**
 * Represents an integer annotation attribute value.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class IntegerAttributeValue extends
        AbstractAnnotationAttributeValue<Integer> {

    private final int value;

    /**
     * Constructor
     * 
     * @param name
     * @param value
     */
    public IntegerAttributeValue(final JavaSymbolName name, final int value) {
        super(name);
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getName() + " -> " + value;
    }
}
