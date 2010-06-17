package org.springframework.roo.addon.dbre.db;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;

import org.springframework.roo.support.util.StringUtils;

/**
 * Represents table metadata from JDBC.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class JdbcTableImpl extends AbstractTable implements Table {

	JdbcTableImpl(IdentifiableTable identifiableTable, DatabaseMetaData databaseMetaData) {
		super(identifiableTable);
		setColumns(databaseMetaData);
		setPrimaryKeys(databaseMetaData);
		setForeignKeys(databaseMetaData);
		setIndexes(databaseMetaData);
	}

	private void setColumns(DatabaseMetaData databaseMetaData) {
		columns.clear();

		try {
			ResultSet rs = null;
			try {
				rs = getColumnsRs(null, databaseMetaData);
				while (rs.next()) {
					String name = rs.getString("COLUMN_NAME");
					int dataType = rs.getInt("DATA_TYPE");
					int columnSize = rs.getInt("COLUMN_SIZE");
					int decimalDigits = rs.getInt("DECIMAL_DIGITS");
					boolean isNullable = rs.getBoolean("IS_NULLABLE");
					String remarks = rs.getString("REMARKS");
					String typeName = new StringTokenizer(rs.getString("TYPE_NAME"), "() ").nextToken();

					columns.add(new Column(name, dataType, columnSize, decimalDigits, isNullable, remarks, typeName));
				}
			} finally {
				if (rs != null) {
					rs.close();
				}
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to get columns from database metadata", e);
		}
	}

	private void setPrimaryKeys(DatabaseMetaData databaseMetaData) {
		primaryKeys.clear();

		try {
			ResultSet rs = null;
			try {
				rs = getPrimaryKeysRs(databaseMetaData);
				while (rs.next()) {
					primaryKeys.add(new PrimaryKey(rs.getString("PK_NAME"), rs.getString("COLUMN_NAME"), rs.getShort("KEY_SEQ")));
				}
			} finally {
				if (rs != null) {
					rs.close();
				}
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to get primary keys from database metadata", e);
		}
	}

	private void setForeignKeys(DatabaseMetaData databaseMetaData) {
		foreignKeys.clear();

		try {
			ResultSet rs = null;
			try {
				rs = getForeignKeysRs(databaseMetaData);
				while (rs.next()) {
					foreignKeys.add(new ForeignKey(rs.getString("FK_NAME"), rs.getString("FKTABLE_NAME")));
				}
			} finally {
				if (rs != null) {
					rs.close();
				}
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to get foreign keys from database metadata", e);
		}
	}

	private void setIndexes(DatabaseMetaData databaseMetaData) {
		indexes.clear();

		try {
			ResultSet rs = null;
			try {
				rs = getIndexesRs(databaseMetaData);
				while (rs.next()) {
					if (rs.getShort("TYPE") == DatabaseMetaData.tableIndexStatistic) {
						continue;
					}
					indexes.add(new Index(rs.getString("INDEX_NAME"), rs.getString("COLUMN_NAME"), rs.getBoolean("NON_UNIQUE"), rs.getShort("TYPE")));
				}
			} finally {
				if (rs != null) {
					rs.close();
				}
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to get indexes from database metadata", e);
		}
	}

	private ResultSet getColumnsRs(String column, DatabaseMetaData databaseMetaData) throws SQLException {
		ResultSet rs = null;
		if (databaseMetaData.storesUpperCaseIdentifiers()) {
			rs = databaseMetaData.getColumns(StringUtils.toUpperCase(identifiableTable.getCatalog()), StringUtils.toUpperCase(identifiableTable.getSchema()), StringUtils.toUpperCase(identifiableTable.getTable()), StringUtils.toUpperCase(column));
		} else if (databaseMetaData.storesLowerCaseIdentifiers()) {
			rs = databaseMetaData.getColumns(StringUtils.toLowerCase(identifiableTable.getCatalog()), StringUtils.toLowerCase(identifiableTable.getSchema()), StringUtils.toLowerCase(identifiableTable.getTable()), StringUtils.toLowerCase(column));
		} else {
			rs = databaseMetaData.getColumns(identifiableTable.getCatalog(), identifiableTable.getSchema(), identifiableTable.getTable(), column);
		}
		return rs;
	}

	private ResultSet getPrimaryKeysRs(DatabaseMetaData databaseMetaData) throws SQLException {
		ResultSet rs = null;
		if (databaseMetaData.storesUpperCaseIdentifiers()) {
			rs = databaseMetaData.getPrimaryKeys(StringUtils.toUpperCase(identifiableTable.getCatalog()), StringUtils.toUpperCase(identifiableTable.getSchema()), StringUtils.toUpperCase(identifiableTable.getTable()));
		} else if (databaseMetaData.storesLowerCaseIdentifiers()) {
			rs = databaseMetaData.getPrimaryKeys(StringUtils.toLowerCase(identifiableTable.getCatalog()), StringUtils.toLowerCase(identifiableTable.getSchema()), StringUtils.toLowerCase(identifiableTable.getTable()));
		} else {
			rs = databaseMetaData.getPrimaryKeys(identifiableTable.getCatalog(), identifiableTable.getSchema(), identifiableTable.getTable());
		}
		return rs;
	}

	private ResultSet getForeignKeysRs(DatabaseMetaData databaseMetaData) throws SQLException {
		ResultSet rs = null;
		if (databaseMetaData.storesUpperCaseIdentifiers()) {
			rs = databaseMetaData.getImportedKeys(StringUtils.toUpperCase(identifiableTable.getCatalog()), StringUtils.toUpperCase(identifiableTable.getSchema()), StringUtils.toUpperCase(identifiableTable.getTable()));
		} else if (databaseMetaData.storesLowerCaseIdentifiers()) {
			rs = databaseMetaData.getImportedKeys(StringUtils.toLowerCase(identifiableTable.getCatalog()), StringUtils.toLowerCase(identifiableTable.getSchema()), StringUtils.toLowerCase(identifiableTable.getTable()));
		} else {
			rs = databaseMetaData.getImportedKeys(identifiableTable.getCatalog(), identifiableTable.getSchema(), identifiableTable.getTable());
		}
		return rs;
	}

	private ResultSet getIndexesRs(DatabaseMetaData databaseMetaData) throws SQLException {
		ResultSet rs = null;
		if (databaseMetaData.storesUpperCaseIdentifiers()) {
			rs = databaseMetaData.getIndexInfo(StringUtils.toUpperCase(identifiableTable.getCatalog()), StringUtils.toUpperCase(identifiableTable.getSchema()), StringUtils.toUpperCase(identifiableTable.getTable()), false, true);
		} else if (databaseMetaData.storesLowerCaseIdentifiers()) {
			rs = databaseMetaData.getIndexInfo(StringUtils.toLowerCase(identifiableTable.getCatalog()), StringUtils.toLowerCase(identifiableTable.getSchema()), StringUtils.toLowerCase(identifiableTable.getTable()), false, true);
		} else {
			rs = databaseMetaData.getIndexInfo(identifiableTable.getCatalog(), identifiableTable.getSchema(), identifiableTable.getTable(), false, true);
		}
		return rs;
	}
}
