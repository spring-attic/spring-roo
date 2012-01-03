package org.springframework.roo.classpath.details.annotations;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.support.util.Assert;

/**
 * Represents a nested annotation attribute value.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class NestedAnnotationAttributeValue extends
        AbstractAnnotationAttributeValue<AnnotationMetadata> {
    private final AnnotationMetadata value;

    public NestedAnnotationAttributeValue(final JavaSymbolName name,
            final AnnotationMetadata value) {
        super(name);
        Assert.notNull(value, "Value required");
        this.value = value;
    }

    public AnnotationMetadata getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getName() + " -> " + value.toString();
    }
}
