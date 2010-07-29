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

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Schema)) {
			return false;
		}
		Schema other = (Schema) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}

	public String toString() {
		return name;
	}
}
