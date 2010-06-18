package org.springframework.roo.addon.dbre.db;

/**
 * Table index metadata.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Index {
	private final String name;
	private final String columnName;
	private boolean nonUnique;
	private Short type;

	Index(String name, String columnName, boolean nonUnique, Short type) {
		this.name = name;
		this.columnName = columnName;
		this.nonUnique = nonUnique;
		this.type = type;
	}

	public String getId() {
		return name + "." + columnName;
	}

	public String getName() {
		return name;
	}

	public String getColumnName() {
		return columnName;
	}

	public boolean isNonUnique() {
		return nonUnique;
	}

	public Short getType() {
		return type;
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
		if (!(obj instanceof Index)) {
			return false;
		}
		Index other = (Index) obj;
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
		return "    INDEX_NAME " + name + ", COLUMN_NAME " + columnName + ", NON_UNIQUE " + nonUnique + ", TYPE " + type;
	}
}
