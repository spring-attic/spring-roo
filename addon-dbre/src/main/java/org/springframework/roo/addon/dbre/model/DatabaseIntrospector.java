package org.springframework.roo.addon.dbre.model;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.roo.addon.dbre.model.dialect.Dialect;
import org.springframework.roo.support.util.StringUtils;

/**
 * Creates a {@link Database database} model from a live database using JDBC.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class DatabaseIntrospector extends AbstractIntrospector {
	private String catalogName;
	private Schema schema;
	private boolean view;
	private Set<String> includeTables = null;
	private Set<String> excludeTables = null;
	private String tableName;
	private String columnName;

	public DatabaseIntrospector(Connection connection, Schema schema, boolean view, Set<String> includeTables, Set<String> excludeTables) throws SQLException {
		super(connection);
		this.schema = schema;
		this.view = view;
		this.includeTables = includeTables;
		this.excludeTables = excludeTables;
	}

	public String getCatalogName() {
		return catalogName;
	}

	public Schema getSchema() {
		return schema;
	}

	public String getSchemaName() {
		return schema != null ? schema.getName() : null;
	}

	public String getTableName() {
		return tableName;
	}

	public String getColumnName() {
		return columnName;
	}

	public Set<Schema> getSchemas() throws SQLException {
		Set<Schema> schemas = new LinkedHashSet<Schema>();

		ResultSet rs = databaseMetaData.getSchemas();
		try {
			while (rs.next()) {
				schemas.add(new Schema(rs.getString("TABLE_SCHEM")));
			}
		} finally {
			rs.close();
		}

		return schemas;
	}

	public Database createDatabase() throws SQLException {
		String name = schema != null && StringUtils.hasText(schema.getName()) ? schema.getName() : catalogName;
		return new Database(name, getTables());
	}

	private Set<Table> getTables() throws SQLException {
		Set<Table> tables = new LinkedHashSet<Table>();

		String[] types = view ? new String[] { TableType.TABLE.name(), TableType.VIEW.name() } : new String[] { TableType.TABLE.name() };
		ResultSet rs = databaseMetaData.getTables(getCatalog(), getSchemaPattern(), getTableNamePattern(), types);
		try {
			while (rs.next()) {
				tableName = rs.getString("TABLE_NAME");
				catalogName = rs.getString("TABLE_CAT");
				schema = new Schema(rs.getString("TABLE_SCHEM"));

				// Check for certain tables such as Oracle recycle bin tables, and ignore
				if (ignoreTables()) {
					continue;
				}

				if (hasIncludedTable(tableName) && !hasExcludedTable(tableName)) {
					Table table = new Table();
					table.setName(tableName);
					table.setCatalog(catalogName);
					table.setSchema(schema);
					table.setDescription(rs.getString("REMARKS"));

					readColumns(table);
					readForeignKeys(table, false);
					readForeignKeys(table, true);
					readIndices(table);

					for (String columnName : readPrimaryKeyNames()) {
						Column column = table.findColumn(columnName);
						if (column != null) {
							column.setPrimaryKey(true);
						}
					}

					tables.add(table);
				}
			}
		} finally {
			rs.close();
		}

		return tables;
	}

	private boolean ignoreTables() {
		boolean ignore = false;
		try {
			if ("Oracle".equalsIgnoreCase(databaseMetaData.getDatabaseProductName()) && tableName.startsWith("BIN$")) {
				ignore = true;
			}
		} catch (SQLException ignored) {}
		return ignore;
	}

	private void readColumns(Table table) throws SQLException {
		ResultSet rs = databaseMetaData.getColumns(catalogName, getSchemaName(), tableName, getColumnNamePattern());
		try {
			while (rs.next()) {
				Column column = new Column(rs.getString("COLUMN_NAME"), rs.getInt("DATA_TYPE"), rs.getString("TYPE_NAME"), rs.getInt("COLUMN_SIZE"), rs.getInt("DECIMAL_DIGITS"));
				column.setDescription(rs.getString("REMARKS"));
				column.setDefaultValue(rs.getString("COLUMN_DEF"));
				column.setRequired("NO".equalsIgnoreCase(rs.getString("IS_NULLABLE")));

				table.addColumn(column);
			}
		} finally {
			rs.close();
		}
	}

	private void readForeignKeys(Table table, boolean exported) throws SQLException {
		Map<String, ForeignKey> foreignKeys = new LinkedHashMap<String, ForeignKey>();

		ResultSet rs;
		if (exported) {
			rs = databaseMetaData.getExportedKeys(catalogName, getSchemaName(), tableName);
		} else {
			rs = databaseMetaData.getImportedKeys(catalogName, getSchemaName(), tableName);
		}

		try {
			while (rs.next()) {
				String name = rs.getString("FK_NAME");
				String foreignTableName = rs.getString(exported ? "FKTABLE_NAME" : "PKTABLE_NAME");
				String key = name + "_" + foreignTableName;

				if (!hasExcludedTable(foreignTableName)) {
					ForeignKey foreignKey = new ForeignKey(name, foreignTableName);
					foreignKey.setOnUpdate(getCascadeAction(rs.getShort("UPDATE_RULE")));
					foreignKey.setOnDelete(getCascadeAction(rs.getShort("DELETE_RULE")));
					foreignKey.setExported(exported);

					String localColumnName = rs.getString(exported ? "PKCOLUMN_NAME" : "FKCOLUMN_NAME");
					String foreignColumnName = rs.getString(exported ? "FKCOLUMN_NAME" : "PKCOLUMN_NAME");
					Reference reference = new Reference(localColumnName, foreignColumnName);

					if (foreignKeys.containsKey(key)) {
						foreignKeys.get(key).addReference(reference);
					} else {
						foreignKey.addReference(reference);
						foreignKeys.put(key, foreignKey);
					}
				}
			}
		} finally {
			rs.close();
		}

		for (ForeignKey foreignKey : foreignKeys.values()) {
			if (exported) {
				table.addExportedKey(foreignKey);
			} else {
				table.addImportedKey(foreignKey);
			}
		}
	}

	private CascadeAction getCascadeAction(Short actionValue) {
		CascadeAction cascadeAction;
		switch (actionValue.intValue()) {
			case DatabaseMetaData.importedKeyCascade:
				cascadeAction = CascadeAction.CASCADE;
				break;
			case DatabaseMetaData.importedKeySetNull:
				cascadeAction = CascadeAction.SET_NULL;
				break;
			case DatabaseMetaData.importedKeySetDefault:
				cascadeAction = CascadeAction.SET_DEFAULT;
				break;
			case DatabaseMetaData.importedKeyRestrict:
				cascadeAction = CascadeAction.RESTRICT;
				break;
			case DatabaseMetaData.importedKeyNoAction:
				cascadeAction = CascadeAction.NONE;
				break;
			default:
				cascadeAction = CascadeAction.NONE;
		}
		return cascadeAction;
	}

	private boolean hasIncludedTable(String tableName) {
		if (includeTables == null || includeTables.isEmpty()) {
			return true;
		}
		return hasTable(includeTables, tableName);
	}

	private boolean hasExcludedTable(String tableName) {
		if (excludeTables == null || excludeTables.isEmpty()) {
			return false;
		}
		return hasTable(excludeTables, tableName);
	}

	private boolean hasTable(Set<String> tables, String tableName) {
		for (String table : tables) {
			String regex = table.replaceAll("\\*", ".*").replaceAll("\\?", ".?");
			Pattern pattern = Pattern.compile(regex);
			if (pattern.matcher(tableName).matches()) {
				return true;
			}
		}
		return false;
	}

	private void readIndices(Table table) throws SQLException {
		Set<Index> indices = new LinkedHashSet<Index>();

		ResultSet rs;
		try {
			// Catching SQLException here due to Oracle throwing exception when attempting to retrieve indices for deleted tables that exist in Oracle's recycle bin
			rs = databaseMetaData.getIndexInfo(catalogName, getSchemaName(), tableName, false, false);
		} catch (SQLException e) {
			return;
		}

		if (rs != null) {
			try {
				while (rs.next()) {
					Short type = rs.getShort("TYPE");
					if (type == DatabaseMetaData.tableIndexStatistic) {
						continue;
					}

					String indexName = rs.getString("INDEX_NAME");
					Index index = findIndex(indexName, indices);
					if (index == null) {
						index = new Index(indexName);
					} else {
						indices.remove(index);
					}
					index.setUnique(!rs.getBoolean("NON_UNIQUE"));

					IndexColumn indexColumn = new IndexColumn(rs.getString("COLUMN_NAME"));
					index.addColumn(indexColumn);

					indices.add(index);
				}
			} finally {
				rs.close();
			}
		}

		for (Index index : indices) {
			table.addIndex(index);
		}
	}

	private Index findIndex(String name, Set<Index> indices) {
		for (Index index : indices) {
			if (index.getName().equalsIgnoreCase(name)) {
				return index;
			}
		}
		return null;
	}

	private Set<String> readPrimaryKeyNames() throws SQLException {
		Set<String> columnNames = new LinkedHashSet<String>();

		ResultSet rs = databaseMetaData.getPrimaryKeys(catalogName, getSchemaName(), tableName);
		try {
			while (rs.next()) {
				columnNames.add(rs.getString("COLUMN_NAME"));
			}
		} finally {
			rs.close();
		}

		return columnNames;
	}

	private String getCatalog() throws SQLException {
		if (databaseMetaData.storesLowerCaseIdentifiers()) {
			return StringUtils.toLowerCase(catalogName);
		} else if (databaseMetaData.storesUpperCaseIdentifiers()) {
			return StringUtils.toUpperCase(catalogName);
		} else {
			return catalogName;
		}
	}

	private String getSchemaPattern() throws SQLException {
		if (databaseMetaData.storesLowerCaseIdentifiers()) {
			return StringUtils.toLowerCase(getSchemaName());
		} else if (databaseMetaData.storesUpperCaseIdentifiers()) {
			return StringUtils.toUpperCase(getSchemaName());
		} else {
			return getSchemaName();
		}
	}

	private String getTableNamePattern() throws SQLException {
		if (databaseMetaData.storesLowerCaseIdentifiers()) {
			return StringUtils.toLowerCase(tableName);
		} else if (databaseMetaData.storesUpperCaseIdentifiers()) {
			return StringUtils.toUpperCase(tableName);
		} else {
			return tableName;
		}
	}

	private String getColumnNamePattern() throws SQLException {
		if (databaseMetaData.storesLowerCaseIdentifiers()) {
			return StringUtils.toLowerCase(columnName);
		} else if (databaseMetaData.storesUpperCaseIdentifiers()) {
			return StringUtils.toUpperCase(columnName);
		} else {
			return columnName;
		}
	}

	@SuppressWarnings("unused") 
	private Dialect getDialect() {
		try {
			String productName = databaseMetaData.getDatabaseProductName();
			return (Dialect) Class.forName("org.springframework.roo.addon.dbre.model.dialect." + productName + "Dialect").newInstance();
		} catch (Exception e) {
			return null;
		}
	}
}
