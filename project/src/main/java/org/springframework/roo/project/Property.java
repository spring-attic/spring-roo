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
	 * @return the url
	 */
	public String getValue() {
		return value;
	}
		
	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Property))
			return false;
		Property other = (Property) obj;
		return name.equals(other.name);
	}

	public int compareTo(Property o) {
		return name.compareTo(o.name);
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("name", name);
		tsc.append("value", value);
		return tsc.toString();
	}
}
