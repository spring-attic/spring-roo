package org.springframework.roo.addon.dbre.model;

/**
 * Represents an index definition for a table which may be either unique or non-unique.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Index {
	private String name;
	private String columnName;
	private boolean unique;
	private Short type;

	Index() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public boolean isUnique() {
		return unique;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public Short getType() {
		return type;
	}

	public void setType(Short type) {
		this.type = type;
	}

	public String toString() {
		return String.format("Index [name=%s, columnName=%s, unique=%s, type=%s]", name, columnName, unique, type);
	}
}
