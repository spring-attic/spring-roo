package org.springframework.roo.addon.dbre.model;

import java.io.Serializable;

/**
 * Represents a schema in the database model.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Schema implements Serializable {
	private static final long serialVersionUID = -6755809630751422192L;
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
