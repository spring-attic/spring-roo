package org.springframework.roo.project;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
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
        Assert.notNull(element, "Element required");
        value = element.getTextContent();
    }

    /**
     * Convenience constructor creating a filter instance
     * 
     * @param value the property value (required)
     */
    public Filter(final String value) {
        Assert.hasText(value, "Value required");
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
        return (obj instanceof Filter) && (compareTo((Filter) obj) == 0);
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
        result = (prime * result) + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public String toString() {
        final ToStringCreator tsc = new ToStringCreator(this);
        tsc.append("value", value);
        return tsc.toString();
    }
}
