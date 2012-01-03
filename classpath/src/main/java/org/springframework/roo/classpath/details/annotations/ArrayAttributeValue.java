package org.springframework.roo.classpath.details.annotations;

import java.util.Collections;
import java.util.List;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Represents an array of annotation attribute values.
 * 
 * @author Ben Alex
 * @since 1.0
 * @param <Y> the type of each {@link AnnotationAttributeValue}
 */
public class ArrayAttributeValue<Y extends AnnotationAttributeValue<?>> extends
        AbstractAnnotationAttributeValue<List<Y>> {

    // Fields
    private final List<Y> value;

    /**
     * Constructor
     * 
     * @param name the attribute name (required)
     * @param value the attribute values (required)
     */
    public ArrayAttributeValue(final JavaSymbolName name, final List<Y> value) {
        super(name);
        Assert.notNull(value, "Value required");
        this.value = value;
    }

    /**
     * Returns an unmodifiable copy of the array values
     */
    public List<Y> getValue() {
        return Collections.unmodifiableList(value);
    }

    @Override
    public String toString() {
        return getName() + " -> {"
                + StringUtils.collectionToCommaDelimitedString(value) + "}";
    }
}
