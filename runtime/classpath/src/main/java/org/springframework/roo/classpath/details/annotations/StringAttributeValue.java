package org.springframework.roo.classpath.details.annotations;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.model.JavaSymbolName;

/**
 * Represents a {@link String} annotation attribute value.
 * <p>
 * Source code parsers should treat any quoted string as a
 * {@link StringAttributeValue}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class StringAttributeValue extends
        AbstractAnnotationAttributeValue<String> {
    private final String value;

    public StringAttributeValue(final JavaSymbolName name, final String value) {
        super(name);
        Validate.notNull(value, "Value required");
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getName() + " -> " + value;
    }
}
