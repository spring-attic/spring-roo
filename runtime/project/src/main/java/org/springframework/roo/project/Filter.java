package org.springframework.roo.project;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.w3c.dom.Element;

/**
 * Simplified immutable representation of a filter.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Filter implements Comparable<Filter> {

    private final String value;

    /**
     * Convenience constructor for creating a filter instance from a XML Element
     * 
     * @param element containing the property definition (required)
     */
    public Filter(final Element element) {
        Validate.notNull(element, "Element required");
        value = element.getTextContent();
    }

    /**
     * Convenience constructor creating a filter instance
     * 
     * @param value the property value (required)
     */
    public Filter(final String value) {
        Validate.notBlank(value, "Value required");
        this.value = value;
    }

    public int compareTo(final Filter o) {
        if (o == null) {
            throw new NullPointerException();
        }
        return value.compareTo(o.value);
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof Filter && compareTo((Filter) obj) == 0;
    }

    /**
     * The value of a filter
     * 
     * @return the value
     */
    public String getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (value == null ? 0 : value.hashCode());
        return result;
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("value", value);
        return builder.toString();
    }
}
