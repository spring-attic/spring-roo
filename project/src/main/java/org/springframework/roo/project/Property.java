package org.springframework.roo.project;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.w3c.dom.Element;

/**
 * Simplified immutable representation of a property.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Property implements Comparable<Property> {
	private String name;
	private String value;
	
	/**
	 * Convenience constructor creating a property instance
	 * 
	 * @param name the property name (required)
	 * @param value the property value (required)
	 */
	public Property(String name, String value) {
		Assert.hasText(name, "Name required");
		Assert.notNull(value, "Value required");
		this.name = name;
		this.value = value;
	}
	
	/**
	 * Convenience constructor creating a property instance
	 * 
	 * @param name the property name (required)
	 */	
	public Property(String name) {
		this.name = name;
		this.value = "";
	}

	/**
	 * Convenience constructor for creating a property instance from a 
	 * XML Element
	 * 
	 * @param element containing the property definition (required)
	 */
	public Property(Element element) {
		Assert.notNull(element, "Element required");
		this.name = element.getNodeName();
		this.value = element.getTextContent();
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
		
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Property other = (Property) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	public int compareTo(Property o) {
		if (o == null) {
			throw new NullPointerException();
		}
		int result = this.name.compareTo(o.name);
		if (result == 0) {
			result = this.value.compareTo(o.value);
		}
		return result;
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("name", name);
		tsc.append("value", value);
		return tsc.toString();
	}
}
