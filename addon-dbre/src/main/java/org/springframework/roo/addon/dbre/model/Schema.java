package org.springframework.roo.addon.dbre.model;

/**
 * Represents a schema in the database model.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Schema {
	private String name;

	public Schema(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String toString() {
		return name;
	}
}
