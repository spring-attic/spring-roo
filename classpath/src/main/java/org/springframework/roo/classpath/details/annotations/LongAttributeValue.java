package org.springframework.roo.classpath.details.annotations;

import org.springframework.roo.model.JavaSymbolName;

/**
 * Represents a long annotation attribute value.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class LongAttributeValue extends AbstractAnnotationAttributeValue<Long> {

    private final long value;

    /**
     * Constructor
     * 
     * @param name
     * @param value
     */
    public LongAttributeValue(final JavaSymbolName name, final long value) {
        super(name);
        this.value = value;
    }

    public Long getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getName() + " -> " + value;
    }
}
