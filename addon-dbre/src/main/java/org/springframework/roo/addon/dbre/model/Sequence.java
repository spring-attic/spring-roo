package org.springframework.roo.addon.dbre.model;

import java.io.Serializable;

/**
 * Represents a sequence in the database model.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Sequence implements Serializable {
	private static final long serialVersionUID = 2929387068486746444L;
	private String name;

	public Sequence(String name) {
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
		if (!(obj instanceof Sequence)) {
			return false;
		}
		Sequence other = (Sequence) obj;
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
