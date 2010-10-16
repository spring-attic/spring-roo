package org.springframework.roo.addon.dbre.model;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.roo.addon.dbre.model.dialect.Dialect;
import org.springframework.roo.support.util.Assert;

/**
 * Creates a {@link Database database} model from a live database using JDBC.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class SchemaIntrospector {
	private static final String[] TYPES = { TableType.TABLE.name() };
	private Connection connection;
	private DatabaseMetaData databaseMetaData;
	private String catalog;
	private Schema schema;
	private String tableNamePattern;
	private String columnNamePattern;
	private String[] types = TYPES;

	public SchemaIntrospector(Connection connection, Schema schema) throws SQLException {
		Assert.notNull(connection, "Connection must not be null");
		this.connection = connection;
		catalog = this.connection.getCatalog();
		databaseMetaData = this.connection.getMetaData();
		this.schema = schema;
	}

	public SchemaIntrospector(Connection connection) throws SQLException {
		this(connection, null);
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

	public String getSchemaPattern() {
		return schema != null ? schema.getName() : null;
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

	public Database getDatabase() throws SQLException {
		Database database = new Database(catalog, schema, readTables());
		database.setSequences(readSequences());
		return database;
	}

	private Set<Table> readTables() throws SQLException {
		readSequences();
		Set<Table> tables = new LinkedHashSet<Table>();

		ResultSet rs = databaseMetaData.getTables(catalog, getSchemaPattern(), tableNamePattern, types);
		try {
			while (rs.next()) {
				tableNamePattern = rs.getString("TABLE_NAME");
				catalog = rs.getString("TABLE_CAT");
				schema = new Schema(rs.getString("TABLE_SCHEM"));

				// Check for certain tables such as Oracle recycle bin tables, and ignore
				if (ignoreTables()) {
					continue;
				}

				Table table = new Table();
				table.setName(tableNamePattern);
				table.setCatalog(catalog);
				table.setSchema(schema);
				table.setDescription(rs.getString("REMARKS"));

				table.addColumns(readColumns());
				table.addForeignKeys(readForeignKeys());
				table.addExportedKeys(readExportedKeys());
				table.addIndices(readIndices());

				for (String columnName : readPrimaryKeyNames()) {
					Column column = table.findColumn(columnName);
					if (column != null) {
						column.setPrimaryKey(true);
					}
				}

				tables.add(table);
			}
		} finally {
			rs.close();
		}

		return tables;
	}

	private boolean ignoreTables() {
		boolean ignore = false;
		try {
			if ("Oracle".equalsIgnoreCase(databaseMetaData.getDatabaseProductName()) && tableNamePattern.startsWith("BIN$")) {
				ignore = true;
			}
		} catch (SQLException ignored) {
		}
		return ignore;
	}

	private Set<Column> readColumns() throws SQLException {
		Set<Column> columns = new LinkedHashSet<Column>();

		ResultSet rs = databaseMetaData.getColumns(catalog, getSchemaPattern(), tableNamePattern, columnNamePattern);
		try {
			while (rs.next()) {
				Column column = new Column(rs.getString("COLUMN_NAME"));
				column.setDescription(rs.getString("REMARKS"));
				column.setDefaultValue(rs.getString("COLUMN_DEF"));
				column.setTypeCode(rs.getInt("DATA_TYPE"));
				column.setType(ColumnType.getColumnType(column.getTypeCode())); // "TYPE_NAME"
				
				int columnSize = rs.getInt("COLUMN_SIZE");
				switch (column.getType()) {
					case DECIMAL:
					case DOUBLE:
					case NUMERIC:
						column.setPrecision(columnSize);
						column.setScale(rs.getInt("DECIMAL_DIGITS"));
						column.setLength(0);
						break;
					case CHAR:
						if (columnSize > 1) {
							column.setType(ColumnType.VARCHAR);
							column.setLength(columnSize);
						}
						break;
					default:
						column.setLength(columnSize);
						break;
				}
				
				column.setRequired("NO".equalsIgnoreCase(rs.getString("IS_NULLABLE")));
				column.setOrdinalPosition(rs.getInt("ORDINAL_POSITION"));

				columns.add(column);
			}
		} finally {
			rs.close();
		}

		return columns;
	}

	private Set<ForeignKey> readForeignKeys() throws SQLException {
		Map<String, ForeignKey> foreignKeys = new LinkedHashMap<String, ForeignKey>();

		ResultSet rs = databaseMetaData.getImportedKeys(catalog, getSchemaPattern(), tableNamePattern);
		try {
			while (rs.next()) {
				String name = rs.getString("FK_NAME");
				String foreignTableName = rs.getString("PKTABLE_NAME");
				String key = name + "_" + foreignTableName;

				ForeignKey foreignKey = new ForeignKey(name, foreignTableName);
				foreignKey.setOnUpdate(getCascadeAction(rs.getShort("UPDATE_RULE")));
				foreignKey.setOnDelete(getCascadeAction(rs.getShort("DELETE_RULE")));

				Reference reference = new Reference();
				reference.setSequenceNumber(rs.getShort("KEY_SEQ"));
				reference.setLocalColumnName(rs.getString("FKCOLUMN_NAME"));
				reference.setForeignColumnName(rs.getString("PKCOLUMN_NAME"));

				if (foreignKeys.containsKey(key)) {
					foreignKeys.get(key).addReference(reference);
				} else {
					foreignKey.addReference(reference);
					foreignKeys.put(key, foreignKey);
				}
			}
		} finally {
			rs.close();
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

	private Set<ForeignKey> readExportedKeys() throws SQLException {
		Map<String, ForeignKey> exportedKeys = new LinkedHashMap<String, ForeignKey>();

		ResultSet rs = databaseMetaData.getExportedKeys(catalog, getSchemaPattern(), tableNamePattern);
		try {
			while (rs.next()) {
				String name = rs.getString("FK_NAME");
				String foreignTableName = rs.getString("FKTABLE_NAME");
				String key = name + "_" + foreignTableName;

				ForeignKey foreignKey = new ForeignKey(name, foreignTableName);
				foreignKey.setOnUpdate(getCascadeAction(rs.getShort("UPDATE_RULE")));
				foreignKey.setOnDelete(getCascadeAction(rs.getShort("DELETE_RULE")));

				Reference reference = new Reference();
				reference.setSequenceNumber(rs.getShort("KEY_SEQ"));
				reference.setLocalColumnName(rs.getString("PKCOLUMN_NAME"));
				reference.setForeignColumnName(rs.getString("FKCOLUMN_NAME"));

				if (exportedKeys.containsKey(key)) {
					exportedKeys.get(key).addReference(reference);
				} else {
					foreignKey.addReference(reference);
					exportedKeys.put(key, foreignKey);
				}
			}
		} finally {
			rs.close();
		}

		return new LinkedHashSet<ForeignKey>(exportedKeys.values());
	}

	private Set<Index> readIndices() throws SQLException {
		Set<Index> indices = new LinkedHashSet<Index>();

		ResultSet rs;
		try {
			// Catching SQLException here due to Oracle throwing exception when attempting to retrieve indices for deleted tables that exist in Oracle's recycle bin
			rs = databaseMetaData.getIndexInfo(catalog, getSchemaPattern(), tableNamePattern, false, false);
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

					IndexColumn indexColumn = new IndexColumn(rs.getString("COLUMN_NAME"));
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

	private Set<String> readPrimaryKeyNames() throws SQLException {
		Set<String> columnNames = new LinkedHashSet<String>();

		ResultSet rs = databaseMetaData.getPrimaryKeys(catalog, getSchemaPattern(), tableNamePattern);
		try {
			while (rs.next()) {
				columnNames.add(rs.getString("COLUMN_NAME"));
			}
		} finally {
			rs.close();
		}

		return columnNames;
	}

	private Set<Sequence> readSequences() {
		Set<Sequence> sequences = new LinkedHashSet<Sequence>();
		Dialect dialect = getDialect();
		if (dialect != null && dialect.supportsSequences()) {
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try {
				pstmt = connection.prepareStatement(dialect.getQuerySequencesString(schema));
				rs = pstmt.executeQuery();
				while (rs.next()) {
					sequences.add(new Sequence(rs.getString(1)));
				}
			} catch (SQLException ignored) {} 
			finally {
				if (rs != null) {
					try {
						rs.close();
					} catch (SQLException ignored) {}
				}
				if (pstmt != null) {
					try {
						pstmt.close();
					} catch (SQLException ignored) {}
				}
			}
		}
		return sequences;
	}
	
	private Dialect getDialect() {
		try {
			String productName = databaseMetaData.getDatabaseProductName();
			return (Dialect) Class.forName("org.springframework.roo.addon.dbre.model.dialect." + productName + "Dialect").newInstance();
		} catch (Exception e) {
			return null;
		}
	}
}
