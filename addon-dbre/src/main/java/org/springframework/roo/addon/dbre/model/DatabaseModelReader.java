package org.springframework.roo.addon.dbre.model;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.support.util.StringUtils;

/**
 * Creates a {@link Database database} model from a live database using JDBC.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class DatabaseModelReader {
	private static final String[] TYPES = { TableType.TABLE.name() };
	private Connection connection;
	private String catalog;
	private Schema schema;
	private String tableNamePattern;
	private String columnNamePattern;
	private String[] types = TYPES;

	DatabaseModelReader(Connection connection) {
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
		Database database = new Database();
		database.addTables(readTables(connection.getMetaData()));
		database.setName(catalog);
		database.setJavaPackage(javaPackage);
		return database;
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

					table.addColumns(readColumns(databaseMetaData));
					table.addForeignKeys(readForeignKeys(databaseMetaData));
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
					column.setType(rs.getString("TYPE_NAME"));
					column.setTypeCode(rs.getInt("DATA_TYPE"));
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
		Set<ForeignKey> foreignKeys = new LinkedHashSet<ForeignKey>();

		ResultSet rs = getForeignKeys(databaseMetaData);
		if (rs != null) {
			try {
				while (rs.next()) {
					ForeignKey foreignKey = new ForeignKey(rs.getString("FK_NAME"));
					foreignKey.setForeignTableName(rs.getString("PKTABLE_NAME"));
					foreignKey.setOnUpdate(rs.getShort("UPDATE_RULE"));
					foreignKey.setOnDelete(rs.getShort("DELETE_RULE"));

					Reference reference = new Reference();
					reference.setSequenceValue(rs.getShort("KEY_SEQ"));
					reference.setLocalColumnName(rs.getString("FKCOLUMN_NAME"));
					reference.setForeignColumnName(rs.getString("PKCOLUMN_NAME"));
					foreignKey.addReference(reference);

					foreignKeys.add(foreignKey);
				}
			} finally {
				rs.close();
			}
		}

		return foreignKeys;
	}

	private Set<Index> readIndices(DatabaseMetaData databaseMetaData) throws SQLException {
		Set<Index> indices = new LinkedHashSet<Index>();

		ResultSet rs = getIndices(databaseMetaData);
		if (rs != null) {
			try {
				while (rs.next()) {
					Short type = rs.getShort("TYPE");
					if (type == DatabaseMetaData.tableIndexStatistic) {
						continue;
					}

					Index index = new Index();
					index.setName(rs.getString("INDEX_NAME"));
					index.setColumnName(rs.getString("COLUMN_NAME"));
					index.setUnique(rs.getBoolean("NON_UNIQUE"));
					index.setType(type);

					indices.add(index);
				}
			} finally {
				rs.close();
			}
		}

		return indices;
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

	private ResultSet getColumns(DatabaseMetaData databaseMetaData) throws SQLException {
		String schemaPattern = schema.getName();
		ResultSet rs;
		if (databaseMetaData.storesUpperCaseIdentifiers()) {
			rs = databaseMetaData.getColumns(StringUtils.toUpperCase(catalog), StringUtils.toUpperCase(schemaPattern), StringUtils.toUpperCase(tableNamePattern), StringUtils.toUpperCase(columnNamePattern));
		} else if (databaseMetaData.storesLowerCaseIdentifiers()) {
			rs = databaseMetaData.getColumns(StringUtils.toLowerCase(catalog), StringUtils.toLowerCase(schemaPattern), StringUtils.toLowerCase(tableNamePattern), StringUtils.toUpperCase(columnNamePattern));
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

	private ResultSet getIndices(DatabaseMetaData databaseMetaData) throws SQLException {
		String schemaPattern = schema.getName();
		ResultSet rs = null;
		if (databaseMetaData.storesUpperCaseIdentifiers()) {
			rs = databaseMetaData.getIndexInfo(StringUtils.toUpperCase(catalog), StringUtils.toUpperCase(schemaPattern), StringUtils.toUpperCase(tableNamePattern), false, true);
		} else if (databaseMetaData.storesLowerCaseIdentifiers()) {
			rs = databaseMetaData.getIndexInfo(StringUtils.toLowerCase(catalog), StringUtils.toLowerCase(schemaPattern), StringUtils.toLowerCase(tableNamePattern), false, true);
		} else {
			rs = databaseMetaData.getIndexInfo(catalog, schemaPattern, tableNamePattern, false, true);
		}
		return rs;
	}
}
