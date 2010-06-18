package org.springframework.roo.addon.dbre.db;

/**
 * Primary key metadata.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class PrimaryKey {
	private final String name;
	private final String columnName;
	private final Short keySeq;

	PrimaryKey(String columnName, String name, Short keySeq) {
		this.columnName = columnName;
		this.name = name;
		this.keySeq = keySeq;
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

	public Short getKeySeq() {
		return keySeq;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((columnName == null) ? 0 : columnName.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof PrimaryKey)) {
			return false;
		}
		PrimaryKey other = (PrimaryKey) obj;
		if (columnName == null) {
			if (other.columnName != null) {
				return false;
			}
		} else if (!columnName.equals(other.columnName)) {
			return false;
		}
		return true;
	}

	public String toString() {
		return "    COLUMN_NAME " + columnName + ", PK_NAME " + name + ", KEY_SEQ " + keySeq.toString();
	}
}
