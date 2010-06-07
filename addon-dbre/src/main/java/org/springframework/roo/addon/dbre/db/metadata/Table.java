package org.springframework.roo.addon.dbre.db.metadata;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * JDBC table metadata.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Table {
	private final String catalog;
	private final String schema;
	private final String table;
	private final String tableType;
	private final DatabaseMetaData databaseMetaData;

	Table(ResultSet rs, DatabaseMetaData databaseMetaData) throws SQLException {
		Assert.notNull(rs, "ResultSet must not be null");
		Assert.notNull(databaseMetaData, "Database metadata must not be null");
		this.catalog = rs.getString("TABLE_CAT");
		this.schema = rs.getString("TABLE_SCHEM");
		this.table = rs.getString("TABLE_NAME");
		this.tableType = rs.getString("TABLE_TYPE");
		this.databaseMetaData = databaseMetaData;
	}
	
	public String getId() {
		StringBuilder builder = new StringBuilder();
		builder.append(tableType);
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

	public String getTableType() {
		return tableType;
	}

	public Column getColumn(String column) {
		try {
			ResultSet rs = null;
			try {
				rs = getColumnsRs(column);
				while (rs.next()) {
					if (column != null && column.toLowerCase().equals(rs.getString("COLUMN_NAME").toLowerCase())) {
						return new Column(rs);
					}
				}
			} finally {
				if (rs != null) {
					rs.close();
				}
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to get column from database metadata", e);
		}
		return null;
	}

	public Set<Column> getColumns() {
		Set<Column> columns = new HashSet<Column>();
		try {
			ResultSet rs = null;
			try {
				rs = getColumnsRs(null);
				while (rs.next()) {
					columns.add(new Column(rs));
				}
			} finally {
				if (rs != null) {
					rs.close();
				}
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to get columns from database metadata", e);
		}
		return columns;
	}

	public PrimaryKey getPrimaryKey(String primaryKey) {
		try {
			ResultSet rs = null;
			try {
				rs = getPrimaryKeysRs();
				while (rs.next()) {
					if (primaryKey != null && primaryKey.toLowerCase().equals(rs.getString("PK_NAME").toLowerCase())) {
						return new PrimaryKey(rs);
					}
				}
			} finally {
				if (rs != null) {
					rs.close();
				}
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to get primary key from database metadata", e);
		}
		return null;
	}

	public Set<PrimaryKey> getPrimaryKeys() {
		Set<PrimaryKey> primaryKeys = new HashSet<PrimaryKey>();
		try {
			ResultSet rs = null;
			try {
				rs = getPrimaryKeysRs();
				while (rs.next()) {
					primaryKeys.add(new PrimaryKey(rs));
				}
			} finally {
				if (rs != null) {
					rs.close();
				}
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to get primary keys from database metadata", e);
		}
		return primaryKeys;
	}

	public ForeignKey getForeignKey(String foreignKey) {
		try {
			ResultSet rs = null;
			try {
				rs = getForeignKeysRs();
				while (rs.next()) {
					if (foreignKey != null && foreignKey.toLowerCase().equals(rs.getString("FK_NAME").toLowerCase())) {
						return new ForeignKey(rs);
					}
				}
			} finally {
				if (rs != null) {
					rs.close();
				}
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to get foreign key from database metadata", e);
		}
		return null;
	}

	public Set<ForeignKey> getForeignKeys() {
		Set<ForeignKey> foreignKeys = new HashSet<ForeignKey>();
		try {
			ResultSet rs = null;
			try {
				rs = getForeignKeysRs();
				while (rs.next()) {
					foreignKeys.add(new ForeignKey(rs));
				}
			} finally {
				if (rs != null) {
					rs.close();
				}
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to get foreign keys from database metadata", e);
		}
		return foreignKeys;
	}

	public Index getIndex(String index) {
		try {
			ResultSet rs = null;
			try {
				rs = getIndexesRs();
				while (rs.next()) {
					if (index != null && index.toLowerCase().equals(rs.getString("INDEX_NAME").toLowerCase())) {
						return new Index(rs);
					}
				}
			} finally {
				if (rs != null) {
					rs.close();
				}
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to get the index from database metadata", e);
		}
		return null;
	}

	public Set<Index> getIndexes() {
		Set<Index> indexes = new HashSet<Index>();
		try {
			ResultSet rs = null;
			try {
				rs = getIndexesRs();
				while (rs.next()) {
					if (rs.getShort("TYPE") == DatabaseMetaData.tableIndexStatistic) {
						continue;
					}
					indexes.add(new Index(rs));
				}
			} finally {
				if (rs != null) {
					rs.close();
				}
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to get indexes from database metadata", e);
		}
		return indexes;
	}

	private ResultSet getColumnsRs(String column) throws SQLException {
		ResultSet rs = null;
		if (databaseMetaData.storesUpperCaseIdentifiers()) {
			rs = databaseMetaData.getColumns(StringUtils.toUpperCase(catalog), StringUtils.toUpperCase(schema), StringUtils.toUpperCase(table), StringUtils.toUpperCase(column));
		} else if (databaseMetaData.storesLowerCaseIdentifiers()) {
			rs = databaseMetaData.getColumns(StringUtils.toLowerCase(catalog), StringUtils.toLowerCase(schema), StringUtils.toLowerCase(table), StringUtils.toLowerCase(column));
		} else {
			rs = databaseMetaData.getColumns(catalog, schema, table, column);
		}
		return rs;
	}

	private ResultSet getPrimaryKeysRs() throws SQLException {
		ResultSet rs = null;
		if (databaseMetaData.storesUpperCaseIdentifiers()) {
			rs = databaseMetaData.getPrimaryKeys(StringUtils.toUpperCase(catalog), StringUtils.toUpperCase(schema), StringUtils.toUpperCase(table));
		} else if (databaseMetaData.storesLowerCaseIdentifiers()) {
			rs = databaseMetaData.getPrimaryKeys(StringUtils.toLowerCase(catalog), StringUtils.toLowerCase(schema), StringUtils.toLowerCase(table));
		} else {
			rs = databaseMetaData.getPrimaryKeys(catalog, schema, table);
		}
		return rs;
	}

	private ResultSet getForeignKeysRs() throws SQLException {
		ResultSet rs = null;
		if (databaseMetaData.storesUpperCaseIdentifiers()) {
			rs = databaseMetaData.getImportedKeys(StringUtils.toUpperCase(catalog), StringUtils.toUpperCase(schema), StringUtils.toUpperCase(table));
		} else if (databaseMetaData.storesLowerCaseIdentifiers()) {
			rs = databaseMetaData.getImportedKeys(StringUtils.toLowerCase(catalog), StringUtils.toLowerCase(schema), StringUtils.toLowerCase(table));
		} else {
			rs = databaseMetaData.getImportedKeys(catalog, schema, table);
		}
		return rs;
	}

	private ResultSet getIndexesRs() throws SQLException {
		ResultSet rs = null;
		if (databaseMetaData.storesUpperCaseIdentifiers()) {
			rs = databaseMetaData.getIndexInfo(StringUtils.toUpperCase(catalog), StringUtils.toUpperCase(schema), StringUtils.toUpperCase(table), false, true);
		} else if (databaseMetaData.storesLowerCaseIdentifiers()) {
			rs = databaseMetaData.getIndexInfo(StringUtils.toLowerCase(catalog), StringUtils.toLowerCase(schema), StringUtils.toLowerCase(table), false, true);
		} else {
			rs = databaseMetaData.getIndexInfo(catalog, schema, table, false, true);
		}
		return rs;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((catalog == null) ? 0 : catalog.hashCode());
		result = prime * result + ((schema == null) ? 0 : schema.hashCode());
		result = prime * result + ((table == null) ? 0 : table.hashCode());
		result = prime * result + ((tableType == null) ? 0 : tableType.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Table other = (Table) obj;
		if (catalog == null) {
			if (other.catalog != null)
				return false;
		} else if (!catalog.equals(other.catalog))
			return false;
		if (schema == null) {
			if (other.schema != null)
				return false;
		} else if (!schema.equals(other.schema))
			return false;
		if (table == null) {
			if (other.table != null)
				return false;
		} else if (!table.equals(other.table))
			return false;
		if (tableType == null) {
			if (other.tableType != null)
				return false;
		} else if (!tableType.equals(other.tableType))
			return false;
		return true;
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		final String lineSeparator = System.getProperty("line.separator");

		builder.append(tableType + " " + table);
		builder.append(lineSeparator);

		Set<Column> columns = getColumns();
		if (!columns.isEmpty()) {
			builder.append("  COLUMNS ");
			builder.append(lineSeparator);
			for (Column column : columns) {
				builder.append(column.toString());
				builder.append(lineSeparator);
			}
		}

		Set<PrimaryKey> primaryKeys = getPrimaryKeys();
		if (!primaryKeys.isEmpty()) {
			builder.append("  PRIMARY KEYS ");
			builder.append(lineSeparator);
			for (PrimaryKey primaryKey : getPrimaryKeys()) {
				builder.append(primaryKey.toString());
				builder.append(lineSeparator);
			}
		}

		Set<ForeignKey> foreignKeys = getForeignKeys();
		if (!foreignKeys.isEmpty()) {
			builder.append("  FOREIGN KEYS ");
			builder.append(lineSeparator);
			for (ForeignKey foreignKey : getForeignKeys()) {
				builder.append(foreignKey.toString());
				builder.append(lineSeparator);
			}
		}

		Set<Index> indexes = getIndexes();
		if (!indexes.isEmpty()) {
			builder.append("  INDEXES ");
			builder.append(lineSeparator);
			for (Index index : getIndexes()) {
				builder.append(index.toString());
				builder.append(lineSeparator);
			}
		}

		return builder.toString();
	}
}
