package org.springframework.roo.addon.dbre.model;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.support.util.StringUtils;

/**
 * Creates a {@link Database database} model from a live database using JDBC.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class DatabaseSchemaIntrospector {
	private static final String[] TYPES = { TableType.TABLE.name() };
	private Connection connection;
	private String catalog;
	private Schema schema;
	private String tableNamePattern;
	private String columnNamePattern;
	private String[] types = TYPES;

	DatabaseSchemaIntrospector(Connection connection) {
		this.connection = connection;
	}

	public Connection getConnection() {
		return connection;
	}

	public String getCatalog() {
		return catalog;
	}

	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

	public Schema getSchema() {
		return schema;
	}

	public void setSchema(Schema schema) {
		this.schema = schema;
	}

	public String getTableNamePattern() {
		return tableNamePattern;
	}

	public void setTableNamePattern(String tableNamePattern) {
		this.tableNamePattern = tableNamePattern;
	}

	public String getColumnNamePattern() {
		return columnNamePattern;
	}

	public void setColumnNamePattern(String columnNamePattern) {
		this.columnNamePattern = columnNamePattern;
	}

	public String[] getTypes() {
		return types;
	}

	public void setTypes(String[] types) {
		this.types = types;
	}

	public Set<Schema> getSchemas() throws SQLException {
		Set<Schema> schemas = new LinkedHashSet<Schema>();

		ResultSet rs = connection.getMetaData().getSchemas();
		if (rs != null) {
			try {
				while (rs.next()) {
					schemas.add(new Schema(rs.getString("TABLE_SCHEM")));
				}
			} finally {
				rs.close();
			}
		}

		return schemas;
	}

	public Database getDatabase(JavaPackage javaPackage) throws SQLException {
		return new Database(catalog, javaPackage, readTables(connection.getMetaData()));
	}

	private Set<Table> readTables(DatabaseMetaData databaseMetaData) throws SQLException {
		Set<Table> tables = new LinkedHashSet<Table>();
		ResultSet rs = getTables(databaseMetaData);
		if (rs != null) {
			try {
				while (rs.next()) {
					setTableNamePattern(rs.getString("TABLE_NAME"));
					setCatalog(rs.getString("TABLE_CAT"));
					setSchema(new Schema(rs.getString("TABLE_SCHEM")));

					Table table = new Table();
					table.setName(tableNamePattern);
					table.setCatalog(catalog);
					table.setSchema(schema);
					table.setDescription(rs.getString("REMARKS"));

					try {
						// Catching SQLException here as getSuperTables() is not supported by every driver
						ResultSet superRs = getSuperTables(databaseMetaData);
						if (superRs != null) {
							try {
								while (superRs.next()) {
									table.setSuperTableName(superRs.getString("SUPERTABLE_NAME"));
									break;
								}
							} finally {
								superRs.close();
							}
						}
					} catch (SQLException ignorred) {
					}

					table.addColumns(readColumns(databaseMetaData));
					table.addForeignKeys(readForeignKeys(databaseMetaData));
					table.addExportedKeys(readExportedKeys(databaseMetaData));
					table.addIndices(readIndices(databaseMetaData));

					for (String columnName : readPrimaryKeyNames(databaseMetaData)) {
						table.findColumn(columnName).setPrimaryKey(true);
					}

					tables.add(table);
				}
			} finally {
				rs.close();
			}
		}

		return tables;
	}

	private Set<Column> readColumns(DatabaseMetaData databaseMetaData) throws SQLException {
		Set<Column> columns = new LinkedHashSet<Column>();

		ResultSet rs = getColumns(databaseMetaData);
		if (rs != null) {
			try {
				while (rs.next()) {
					Column column = new Column(rs.getString("COLUMN_NAME"));
					column.setDescription(rs.getString("REMARKS"));
					column.setDefaultValue(rs.getString("COLUMN_DEF"));
					column.setSize(rs.getInt("COLUMN_SIZE"));
					column.setScale(rs.getInt("DECIMAL_DIGITS"));
					column.setTypeCode(rs.getInt("DATA_TYPE"));
					column.setType(ColumnType.getColumnType(column.getTypeCode())); // "TYPE_NAME" ;
					column.setRequired("NO".equalsIgnoreCase(rs.getString("IS_NULLABLE")));

					columns.add(column);
				}
			} finally {
				rs.close();
			}
		}

		return columns;
	}

	private Set<ForeignKey> readForeignKeys(DatabaseMetaData databaseMetaData) throws SQLException {
		Map<String, ForeignKey> foreignKeys = new LinkedHashMap<String, ForeignKey>();

		ResultSet rs = getForeignKeys(databaseMetaData);
		if (rs != null) {
			try {
				while (rs.next()) {
					String foreignTableName = rs.getString("PKTABLE_NAME");
					ForeignKey foreignKey = new ForeignKey(rs.getString("FK_NAME"));
					foreignKey.setForeignTableName(foreignTableName);
					foreignKey.setOnUpdate(getCascadeAction(rs.getShort("UPDATE_RULE")));
					foreignKey.setOnDelete(getCascadeAction(rs.getShort("DELETE_RULE")));

					Reference reference = new Reference();
					reference.setSequenceValue(rs.getShort("KEY_SEQ"));
					reference.setLocalColumnName(rs.getString("FKCOLUMN_NAME"));
					reference.setForeignColumnName(rs.getString("PKCOLUMN_NAME"));
					
					if (foreignKeys.containsKey(foreignTableName)) {
						foreignKeys.get(foreignTableName).addReference(reference);
					} else {
						foreignKey.addReference(reference);
						foreignKeys.put(foreignTableName, foreignKey); 
					}
				}
			} finally {
				rs.close();
			}
		}

		return new LinkedHashSet<ForeignKey>(foreignKeys.values());
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

	private Set<ForeignKey> readExportedKeys(DatabaseMetaData databaseMetaData) throws SQLException {
		Map<String, ForeignKey> exportedKeys = new LinkedHashMap<String, ForeignKey>();

		ResultSet rs = getExportedKeys(databaseMetaData);
		if (rs != null) {
			try {
				while (rs.next()) {
					String foreignTableName = rs.getString("FKTABLE_NAME");
					ForeignKey foreignKey = new ForeignKey(rs.getString("FK_NAME"));
					foreignKey.setForeignTableName(foreignTableName);
					foreignKey.setOnUpdate(getCascadeAction(rs.getShort("UPDATE_RULE")));
					foreignKey.setOnDelete(getCascadeAction(rs.getShort("DELETE_RULE")));

					Reference reference = new Reference();
					reference.setSequenceValue(rs.getShort("KEY_SEQ"));
					reference.setLocalColumnName(rs.getString("PKCOLUMN_NAME"));
					reference.setForeignColumnName(rs.getString("FKCOLUMN_NAME"));

					if (exportedKeys.containsKey(foreignTableName)) {
						exportedKeys.get(foreignTableName).addReference(reference);
					} else {
						foreignKey.addReference(reference);
						exportedKeys.put(foreignTableName, foreignKey); 
					}
				}
			} finally {
				rs.close();
			}
		}

		return new LinkedHashSet<ForeignKey>(exportedKeys.values());
	}

	private Set<Index> readIndices(DatabaseMetaData databaseMetaData) throws SQLException {
		Set<Index> indices = new LinkedHashSet<Index>();

		ResultSet rs;
		try {
			// Catching SQLException here due to Oracle throwing exception when attempting to retrieve indices for deleted tables that exist in Oracle's recycle bin
			rs = getIndices(databaseMetaData);
		} catch (SQLException e) {
			return indices;
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

					IndexColumn indexColumn = new IndexColumn();
					indexColumn.setName(rs.getString("COLUMN_NAME"));
					indexColumn.setOrdinalPosition(rs.getShort("ORDINAL_POSITION"));

					index.addColumn(indexColumn);

					indices.add(index);
				}
			} finally {
				rs.close();
			}
		}

		return indices;
	}

	private Index findIndex(String name, Set<Index> indices) {
		for (Index index : indices) {
			if (index.getName().equalsIgnoreCase(name)) {
				return index;
			}
		}
		return null;
	}

	private Set<String> readPrimaryKeyNames(DatabaseMetaData databaseMetaData) throws SQLException {
		Set<String> columnNames = new LinkedHashSet<String>();

		ResultSet rs = getPrimaryKeys(databaseMetaData);
		if (rs != null) {
			try {
				while (rs.next()) {
					columnNames.add(rs.getString("COLUMN_NAME"));
				}
			} finally {
				rs.close();
			}
		}

		return columnNames;
	}

	private ResultSet getTables(DatabaseMetaData databaseMetaData) throws SQLException {
		String schemaPattern = schema.getName();
		ResultSet rs;
		if (databaseMetaData.storesUpperCaseIdentifiers()) {
			rs = databaseMetaData.getTables(StringUtils.toUpperCase(catalog), StringUtils.toUpperCase(schemaPattern), StringUtils.toUpperCase(tableNamePattern), types);
		} else if (databaseMetaData.storesLowerCaseIdentifiers()) {
			rs = databaseMetaData.getTables(StringUtils.toLowerCase(catalog), StringUtils.toLowerCase(schemaPattern), StringUtils.toLowerCase(tableNamePattern), types);
		} else {
			rs = databaseMetaData.getTables(catalog, schemaPattern, tableNamePattern, types);
		}
		return rs;
	}

	private ResultSet getSuperTables(DatabaseMetaData databaseMetaData) throws SQLException {
		String schemaPattern = schema.getName();
		ResultSet rs = null;
		if (databaseMetaData.storesUpperCaseIdentifiers()) {
			rs = databaseMetaData.getSuperTables(StringUtils.toUpperCase(catalog), StringUtils.toUpperCase(schemaPattern), StringUtils.toUpperCase(tableNamePattern));
		} else if (databaseMetaData.storesLowerCaseIdentifiers()) {
			rs = databaseMetaData.getSuperTables(StringUtils.toLowerCase(catalog), StringUtils.toLowerCase(schemaPattern), StringUtils.toLowerCase(tableNamePattern));
		} else {
			rs = databaseMetaData.getSuperTables(catalog, schemaPattern, tableNamePattern);
		}
		return rs;
	}

	private ResultSet getColumns(DatabaseMetaData databaseMetaData) throws SQLException {
		String schemaPattern = schema.getName();
		ResultSet rs;
		if (databaseMetaData.storesUpperCaseIdentifiers()) {
			rs = databaseMetaData.getColumns(StringUtils.toUpperCase(catalog), StringUtils.toUpperCase(schemaPattern), StringUtils.toUpperCase(tableNamePattern), StringUtils.toUpperCase(columnNamePattern));
		} else if (databaseMetaData.storesLowerCaseIdentifiers()) {
			rs = databaseMetaData.getColumns(StringUtils.toLowerCase(catalog), StringUtils.toLowerCase(schemaPattern), StringUtils.toLowerCase(tableNamePattern), StringUtils.toLowerCase(columnNamePattern));
		} else {
			rs = databaseMetaData.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern);
		}
		return rs;
	}

	private ResultSet getPrimaryKeys(DatabaseMetaData databaseMetaData) throws SQLException {
		String schemaPattern = schema.getName();
		ResultSet rs;
		if (databaseMetaData.storesUpperCaseIdentifiers()) {
			rs = databaseMetaData.getPrimaryKeys(StringUtils.toUpperCase(catalog), StringUtils.toUpperCase(schemaPattern), StringUtils.toUpperCase(tableNamePattern));
		} else if (databaseMetaData.storesLowerCaseIdentifiers()) {
			rs = databaseMetaData.getPrimaryKeys(StringUtils.toLowerCase(catalog), StringUtils.toLowerCase(schemaPattern), StringUtils.toLowerCase(tableNamePattern));
		} else {
			rs = databaseMetaData.getPrimaryKeys(catalog, schemaPattern, tableNamePattern);
		}
		return rs;
	}

	private ResultSet getForeignKeys(DatabaseMetaData databaseMetaData) throws SQLException {
		String schemaPattern = schema.getName();
		ResultSet rs;
		if (databaseMetaData.storesUpperCaseIdentifiers()) {
			rs = databaseMetaData.getImportedKeys(StringUtils.toUpperCase(catalog), StringUtils.toUpperCase(schemaPattern), StringUtils.toUpperCase(tableNamePattern));
		} else if (databaseMetaData.storesLowerCaseIdentifiers()) {
			rs = databaseMetaData.getImportedKeys(StringUtils.toLowerCase(catalog), StringUtils.toLowerCase(schemaPattern), StringUtils.toLowerCase(tableNamePattern));
		} else {
			rs = databaseMetaData.getImportedKeys(catalog, schemaPattern, tableNamePattern);
		}
		return rs;
	}

	private ResultSet getExportedKeys(DatabaseMetaData databaseMetaData) throws SQLException {
		String schemaPattern = schema.getName();
		ResultSet rs;
		if (databaseMetaData.storesUpperCaseIdentifiers()) {
			rs = databaseMetaData.getExportedKeys(StringUtils.toUpperCase(catalog), StringUtils.toUpperCase(schemaPattern), StringUtils.toUpperCase(tableNamePattern));
		} else if (databaseMetaData.storesLowerCaseIdentifiers()) {
			rs = databaseMetaData.getExportedKeys(StringUtils.toLowerCase(catalog), StringUtils.toLowerCase(schemaPattern), StringUtils.toLowerCase(tableNamePattern));
		} else {
			rs = databaseMetaData.getExportedKeys(catalog, schemaPattern, tableNamePattern);
		}
		return rs;
	}

	private ResultSet getIndices(DatabaseMetaData databaseMetaData) throws SQLException {
		String schemaPattern = schema.getName();
		ResultSet rs = null;
		if (databaseMetaData.storesUpperCaseIdentifiers()) {
			rs = databaseMetaData.getIndexInfo(StringUtils.toUpperCase(catalog), StringUtils.toUpperCase(schemaPattern), StringUtils.toUpperCase(tableNamePattern), false, false);
		} else if (databaseMetaData.storesLowerCaseIdentifiers()) {
			rs = databaseMetaData.getIndexInfo(StringUtils.toLowerCase(catalog), StringUtils.toLowerCase(schemaPattern), StringUtils.toLowerCase(tableNamePattern), false, false);
		} else {
			rs = databaseMetaData.getIndexInfo(catalog, schemaPattern, tableNamePattern, false, false);
		}
		return rs;
	}
}
