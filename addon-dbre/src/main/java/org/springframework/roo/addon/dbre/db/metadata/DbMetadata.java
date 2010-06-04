package org.springframework.roo.addon.dbre.db.metadata;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * JDBC database metadata.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class DbMetadata {
	private static final String[] TYPES = { "TABLE", "VIEW" };
	private DatabaseMetaData databaseMetaData;
	private Connection connection;

	public DbMetadata(Connection connection) {
		Assert.notNull(connection, "Connection must not be null");
		this.connection = connection;
		try {
			this.databaseMetaData = this.connection.getMetaData();
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to get database metadata", e);
		}
	}

	public Table getTable(String catalog, String schema, String table) {
		try {
			ResultSet rs = null;
			try {
				rs = getTablesRs(catalog, schema, table);
				while (rs.next()) {
					if (table != null && table.toLowerCase().equals(rs.getString("TABLE_NAME").toLowerCase())) {
						return new Table(rs, databaseMetaData);
					}
				}
			} finally {
				if (rs != null) {
					rs.close();
				}
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to get table from database metadata", e);
		}
		return null;
	}

	public List<Table> getTables(String catalog, String schema) {
		List<Table> tables = new LinkedList<Table>();
		try {
			ResultSet rs = null;
			try {
				rs = getTablesRs(catalog, schema, null);
				while (rs.next()) {
					tables.add(new Table(rs, databaseMetaData));
				}
			} finally {
				if (rs != null) {
					rs.close();
				}
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to get tables from database metadata", e);
		}
		return tables;
	}

	private ResultSet getTablesRs(String catalog, String schema, String table) throws SQLException {
		ResultSet rs;
		if (databaseMetaData.storesUpperCaseIdentifiers()) {
			rs = databaseMetaData.getTables(StringUtils.toUpperCase(catalog), StringUtils.toUpperCase(schema), StringUtils.toUpperCase(table), TYPES);
		} else if (databaseMetaData.storesLowerCaseIdentifiers()) {
			rs = databaseMetaData.getTables(StringUtils.toLowerCase(catalog), StringUtils.toLowerCase(schema), StringUtils.toLowerCase(table), TYPES);
		} else {
			rs = databaseMetaData.getTables(catalog, schema, table, TYPES);
		}
		return rs;
	}

	public Set<String> getSequences(Dialect dialect) {
		Set<String> sequences = new HashSet<String>();
		if (dialect.supportsSequences()) {
			String sql = dialect.getQuerySequencesString();
			if (sql != null) {
				try {
					Statement statement = null;
					ResultSet rs = null;
					try {
						statement = connection.createStatement();
						rs = statement.executeQuery(sql);
						while (rs.next()) {
							sequences.add(rs.getString(1).toLowerCase().trim());
						}
					} finally {
						if (rs != null) {
							rs.close();
						}
						if (statement != null) {
							statement.close();
						}
					}
				} catch (SQLException e) {
					throw new IllegalStateException("Failed to get sequences", e);
				}
			}
		}

		return sequences;
	}

	public String toString() {
		List<Table> tables = getTables(null, null);
		StringBuilder builder = new StringBuilder();
		if (!tables.isEmpty()) {
			for (Table table : tables) {
				builder.append(table.toString());
				builder.append(System.getProperty("line.separator"));
			}
		}
		return builder.toString();
	}
}
