package org.springframework.roo.classpath.details.annotations;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.model.JavaSymbolName;

/**
 * Abstract base class for annotation attribute values.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public abstract class AbstractAnnotationAttributeValue<T extends Object>
        implements AnnotationAttributeValue<T> {

    private final JavaSymbolName name;

    /**
     * Constructor
     * 
     * @param name the attribute name (required)
     */
    protected AbstractAnnotationAttributeValue(final JavaSymbolName name) {
        Validate.notNull(name, "Annotation attribute name required");
        this.name = name;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AbstractAnnotationAttributeValue<?>)) {
            return false;
        }
        final AbstractAnnotationAttributeValue<?> other = (AbstractAnnotationAttributeValue<?>) obj;
        if (getValue() == null) {
            if (other.getValue() != null) {
                return false;
            }
        }
        else if (!getValue().equals(other.getValue())) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        }
        else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    public JavaSymbolName getName() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + (getValue() == null ? 0 : getValue().hashCode());
        result = prime * result + (name == null ? 0 : name.hashCode());
        return result;
    }
}
