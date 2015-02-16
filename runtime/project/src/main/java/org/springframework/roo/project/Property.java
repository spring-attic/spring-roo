package org.springframework.roo.project;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.w3c.dom.Element;

/**
 * Simplified immutable representation of a property.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Property implements Comparable<Property> {

    private final String name;
    private final String value;

    /**
     * Convenience constructor for creating a property instance from a XML
     * Element
     * 
     * @param element containing the property definition (required)
     */
    public Property(final Element element) {
        Validate.notNull(element, "Element required");
        name = element.getNodeName();
        value = element.getTextContent();
    }

    /**
     * Convenience constructor creating a property instance
     * 
     * @param name the property name (required)
     */
    public Property(final String name) {
        this.name = name;
        value = "";
    }

    /**
     * Convenience constructor creating a property instance
     * 
     * @param name the property name (required)
     * @param value the property value (required)
     */
    public Property(final String name, final String value) {
        Validate.notBlank(name, "Name required");
        Validate.notNull(value, "Value required");
        this.name = name;
        this.value = value;
    }

    public int compareTo(final Property o) {
        if (o == null) {
            throw new NullPointerException();
        }
        int result = name.compareTo(o.name);
        if (result == 0) {
            result = value.compareTo(o.value);
        }
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Property other = (Property) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        }
        else if (!name.equals(other.name)) {
            return false;
        }
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        }
        else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }

    /**
     * The name of a property
     * 
     * @return the name of the property (never null)
     */
    public String getName() {
        return name;
    }

    /**
     * The value of a property
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
        result = prime * result + (name == null ? 0 : name.hashCode());
        result = prime * result + (value == null ? 0 : value.hashCode());
        return result;
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("name", name);
        builder.append("value", value);
        return builder.toString();
    }
}
