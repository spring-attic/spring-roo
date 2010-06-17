package org.springframework.roo.addon.dbre.db;

import org.springframework.roo.support.util.StringUtils;

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
	private TableType tableType;

	public IdentifiableTable(String catalog, String schema, String table, TableType tableType) {
	//	this.catalog = catalog;
		this.catalog = null;
	//	this.schema = schema;
		this.schema = null;
		this.table = table;
		this.tableType = tableType;
	}

	public IdentifiableTable(String catalog, String schema, String table) {
		this(catalog, schema, table, TableType.TABLE);
	}

	public IdentifiableTable(String table) {
		this(null, null, table);
	}

	public IdentifiableTable() {
		this(null);
	}

	public String getId() {
		StringBuilder builder = new StringBuilder();
		builder.append(tableType == null ? TableType.TABLE.name() : tableType.name());
		builder.append(".");
		if (catalog != null) {
			builder.append(catalog);
			builder.append(".");
		}
		if (schema != null) {
			builder.append(schema);
			builder.append(".");
		}
		builder.append(table);
		return builder.toString();
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

	public TableType getTableType() {
		return tableType;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((table == null) ? 0 : table.toUpperCase().hashCode());
		result = prime * result + ((catalog == null) ? 0 : catalog.toUpperCase().hashCode());
		result = prime * result + ((schema == null) ? 0 : schema.toUpperCase().hashCode());
		result = prime * result + ((tableType == null) ? 0 : tableType.hashCode());
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
		} else if (!StringUtils.toUpperCase(table).equals(StringUtils.toUpperCase(other.table))) {
			return false;
		}
		if (catalog == null) {
			if (other.catalog != null) {
				return false;
			}
		} else if (!StringUtils.toUpperCase(catalog).equals(StringUtils.toUpperCase(other.catalog))) {
			return false;
		}
		if (schema == null) {
			if (other.schema != null) {
				return false;
			}
		} else if (!StringUtils.toUpperCase(schema).equals(StringUtils.toUpperCase(other.schema))) {
			return false;
		}
		if (tableType == null) {
			if (other.tableType != null) {
				return false;
			}
		} else if (tableType != other.tableType) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return String.format("IdentifiableTable (catalog=%s, schema=%s, table=%s, tableType=%s)", catalog, schema, table, tableType);
	}
}