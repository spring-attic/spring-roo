package org.springframework.roo.addon.dbre.db;

/**
 * Foreign key metadata.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class ForeignKey /*implements Comparable<ForeignKey>*/{
	private final String name;
	private final String foreignKeyTable;
	private final String foreignKeyColumn;
	private final String primaryKeyTable;
	private final String primaryKeyColumn;
	private final Short keySequence;

	ForeignKey(String name, String foreignKeyTable, String foreignKeyColumn, String primaryKeyTable, String primaryKeyColumn, Short keySequence) {
		this.name = name;
		this.foreignKeyTable = foreignKeyTable;
		this.foreignKeyColumn = foreignKeyColumn;
		this.primaryKeyTable = primaryKeyTable;
		this.primaryKeyColumn = primaryKeyColumn;
		this.keySequence = keySequence;
	}

	public String getId() {
		return name + "." + foreignKeyTable;
	}

	public String getName() {
		return name;
	}

	public String getForeignKeyTable() {
		return foreignKeyTable;
	}

	public String getForeignKeyColumn() {
		return foreignKeyColumn;
	}

	protected String getPrimaryKeyTable() {
		return primaryKeyTable;
	}

	protected String getPrimaryKeyColumn() {
		return primaryKeyColumn;
	}

	protected Short getKeySequence() {
		return keySequence;
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
		if (!(obj instanceof ForeignKey)) {
			return false;
		}
		ForeignKey other = (ForeignKey) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}

	public int compareTo(ForeignKey o) {
		if (keySequence == null && o.getKeySequence() == null) return 1;
		if (keySequence != null && o.getKeySequence() == null) return -1;
		if (keySequence == null && o.getKeySequence() != null) return 1;
		return keySequence.compareTo(o.getKeySequence());
	}

	public String toString() {
		return "    FK_NAME " + name + ", FKTABLE_NAME " + foreignKeyTable + ", FKCOLUMN_NAME " + foreignKeyColumn + ", PKTABLE_NAME " + primaryKeyTable + ", PKCOLUMN_NAME " + primaryKeyColumn + ", KEY_SEQ " + keySequence.toString();
	}
}
