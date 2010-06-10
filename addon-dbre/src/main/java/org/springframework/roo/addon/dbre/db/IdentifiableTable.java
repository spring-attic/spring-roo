package org.springframework.roo.addon.dbre.db;

/**
 * Identifies a table using the database catalog, schema and table names
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.1
 */
public class IdentifiableTable {
	private String catalog;
	private String schema;
	private String table;

	public IdentifiableTable(String catalog, String schema, String table) {
		this.catalog = catalog;
		this.schema = schema;
		this.table = table;
	}

	public String getCatalog() {
		return catalog;
	}

	public String getSchema() {
		return schema;
	}

	public String getTable() {
		return table;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((table == null) ? 0 : table.toUpperCase().hashCode());
		result = prime * result + ((catalog == null) ? 0 : catalog.toUpperCase().hashCode());
		result = prime * result + ((schema == null) ? 0 : schema.toUpperCase().hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		IdentifiableTable other = (IdentifiableTable) obj;
		if (table == null) {
			if (other.table != null) {
				return false;
			}
		} else if (!table.toUpperCase().equals(other.table.toUpperCase())) {
			return false;
		}
		if (catalog == null) {
			if (other.catalog != null) {
				return false;
			}
		} else if (!catalog.toUpperCase().equals(other.catalog.toUpperCase())) {
			return false;
		}
		if (schema == null) {
			if (other.schema != null) {
				return false;
			}
		} else if (!schema.toUpperCase().equals(other.schema.toUpperCase())) {
			return false;
		}
		return true;
	}

	public String toString() {
		return String.format("Table (catalog=%s, schema=%s, name=%s)", catalog, schema, table);
	}
}