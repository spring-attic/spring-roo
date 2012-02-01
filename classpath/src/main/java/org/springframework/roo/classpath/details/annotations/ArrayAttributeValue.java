package org.springframework.roo.classpath.details.annotations;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.model.JavaSymbolName;

/**
 * Represents an array of annotation attribute values.
 * 
 * @author Ben Alex
 * @since 1.0
 * @param <Y> the type of each {@link AnnotationAttributeValue}
 */
public class ArrayAttributeValue<Y extends AnnotationAttributeValue<?>> extends
        AbstractAnnotationAttributeValue<List<Y>> {

    private final List<Y> value;

    /**
     * Constructor
     * 
     * @param name the attribute name (required)
     * @param value the attribute values (required)
     */
    public ArrayAttributeValue(final JavaSymbolName name, final List<Y> value) {
        super(name);
        Validate.notNull(value, "Value required");
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
        return getName() + " -> {" + StringUtils.join(value, ",") + "}";
    }
}
