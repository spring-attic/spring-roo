package org.springframework.roo.addon.dbre.db;

/**
 * Foreign key metadata.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class ForeignKey {
	private final String name;
	private final String fkTable;

	ForeignKey(String name, String fkTable) {
		this.name = name;
		this.fkTable = fkTable;
	}

	public String getId() {
		return name + "." + fkTable;
	}

	public String getName() {
		return name;
	}

	public String getFkTable() {
		return fkTable;
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

	public String toString() {
		return "    FK_NAME " + name + ", FKTABLE_NAME " + fkTable;
	}
}
