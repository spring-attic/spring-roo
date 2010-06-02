package org.springframework.roo.addon.dbre;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.roo.addon.dbre.db.connection.ConnectionProvider;

public class JdbcMetadata {
	private ConnectionProvider provider;
	private Connection connection;
	private DatabaseMetaData metaData;

	public void configure(ConnectionProvider provider) {
		this.provider = provider;
	}

	public void close() {
		metaData = null;
		if (connection != null) {
		//	try {
				provider.closeConnection(connection);
		//	} catch (SQLException e) {
		//		throw new RuntimeException("Problem while closing connection", e);
		//	}
		}
		provider = null;
	}

	protected DatabaseMetaData getMetaData() {
		if (metaData == null) {
			try {
				metaData = getConnection().getMetaData();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		return metaData;
	}

	public Iterator getTables(final String catalog, final String schema, String table) {
		try {
			ResultSet tableRs = getMetaData().getTables(catalog, schema, table, new String[] { "TABLE", "VIEW" });

			return new ResultSetIterator(tableRs) {
				Map element = new HashMap();

				protected Object convertRow(ResultSet tableRs) throws SQLException {
					element.clear();
					putTablePart(element, tableRs);
					element.put("TABLE_TYPE", tableRs.getString("TABLE_TYPE"));
					element.put("REMARKS", tableRs.getString("REMARKS"));
					return element;
				}

				protected Throwable handleSQLException(SQLException e) {
					// schemaRs and catalogRs are only used for error reporting if we get an exception
					String databaseStructure = getDatabaseStructure(catalog, schema);
					throw new RuntimeException("Could not get list of tables from database. Probably a JDBC driver problem. " + databaseStructure, e);
				}
			};
		} catch (SQLException e) {
			// schemaRs and catalogRs are only used for error reporting if we get an exception
			String databaseStructure = getDatabaseStructure(catalog, schema);
			throw new RuntimeException("Could not get list of tables from database. Probably a JDBC driver problem. " + databaseStructure, e);
		}
	}

	private void dumpHeader(ResultSet columnRs) throws SQLException {
		ResultSetMetaData md2 = columnRs.getMetaData();

		int columnCount = md2.getColumnCount();
		for (int i = 1; i <= columnCount; i++) {
			System.out.print(md2.getColumnName(i) + "|");
		}
		System.out.println();
	}

	private void dumpRow(ResultSet columnRs) throws SQLException {
		ResultSetMetaData md2 = columnRs.getMetaData();

		int columnCount = md2.getColumnCount();
		for (int i = 1; i <= columnCount; i++) {
			System.out.print(columnRs.getObject(i) + "|");
		}
		System.out.println();
	}

	private String getDatabaseStructure(String catalog, String schema) {
		ResultSet schemaRs = null;
		ResultSet catalogRs = null;
		String nl = System.getProperty("line.separator");
		StringBuffer sb = new StringBuffer(nl);
		// Let's give the user some feedback. The exception
		// is probably related to incorrect schema configuration.
		sb.append("Configured schema:").append(schema).append(nl);
		sb.append("Configured catalog:").append(catalog).append(nl);

		try {
			schemaRs = getMetaData().getSchemas();
			sb.append("Available schemas:").append(nl);
			while (schemaRs.next()) {
				sb.append("  ").append(schemaRs.getString("TABLE_SCHEM")).append(nl);
			}
		} catch (SQLException e2) {
			// log.warn("Could not get schemas", e2);
			sb.append("  <SQLException while getting schemas>").append(nl);
		} finally {
			try {
				schemaRs.close();
			} catch (Exception ignore) {
			}
		}

		try {
			catalogRs = getMetaData().getCatalogs();
			sb.append("Available catalogs:").append(nl);
			while (catalogRs.next()) {
				sb.append("  ").append(catalogRs.getString("TABLE_CAT")).append(nl);
			}
		} catch (SQLException e2) {
			// log.warn("Could not get catalogs", e2);
			sb.append("  <SQLException while getting catalogs>").append(nl);
		} finally {
			try {
				catalogRs.close();
			} catch (Exception ignore) {
			}
		}
		return sb.toString();
	}

	public void close(Iterator iterator) {
		if (iterator instanceof ResultSetIterator) {
			((ResultSetIterator) iterator).close();
		}
	}

	public Iterator getIndexInfo(final String catalog, final String schema, final String table) {
		try {
			// log.debug("getIndexInfo(" + catalog + "." + schema + "." + table + ")");
			ResultSet tableRs = getMetaData().getIndexInfo(catalog, schema, table, false, true);
			return new ResultSetIterator(tableRs) {
				Map element = new HashMap();

				protected Object convertRow(ResultSet rs) throws SQLException {
					element.clear();
					putTablePart(element, rs);
					element.put("INDEX_NAME", rs.getString("INDEX_NAME"));
					element.put("COLUMN_NAME", rs.getString("COLUMN_NAME"));
					element.put("NON_UNIQUE", Boolean.valueOf(rs.getBoolean("NON_UNIQUE")));
					element.put("TYPE", new Short(rs.getShort("TYPE")));
					return element;
				}

				protected Throwable handleSQLException(SQLException e) {
					throw new RuntimeException("Exception while getting index info for ", e);
				}
			};
		} catch (SQLException e) {
			throw new RuntimeException("Exception while getting index info for ", e);
		}
	}

	private void putTablePart(Map element, ResultSet tableRs) throws SQLException {
		element.put("TABLE_NAME", tableRs.getString("TABLE_NAME"));
		element.put("TABLE_SCHEM", tableRs.getString("TABLE_SCHEM"));
		element.put("TABLE_CAT", tableRs.getString("TABLE_CAT"));
	}

	public Iterator getColumns(final String catalog, final String schema, final String table, String column) {
		try {
			// log.debug("getColumns(" + catalog + "." + schema + "." + table + "." + column + ")");
			ResultSet tableRs = getMetaData().getColumns(catalog, schema, table, column);

			return new ResultSetIterator(tableRs) {

				Map element = new HashMap();

				protected Object convertRow(ResultSet rs) throws SQLException {
					element.clear();
					putTablePart(element, rs);
					element.put("DATA_TYPE", new Integer(rs.getInt("DATA_TYPE")));
					element.put("TYPE_NAME", rs.getString("TYPE_NAME"));
					element.put("COLUMN_NAME", rs.getString("COLUMN_NAME"));
					element.put("NULLABLE", new Integer(rs.getInt("NULLABLE")));
					element.put("COLUMN_SIZE", new Integer(rs.getInt("COLUMN_SIZE")));
					element.put("DECIMAL_DIGITS", new Integer(rs.getInt("DECIMAL_DIGITS")));
					element.put("REMARKS", rs.getString("REMARKS"));
					return element;
				}

				protected Throwable handleSQLException(SQLException e) {
					throw new RuntimeException("Error while reading column meta data for ", e);
				}
			};
		} catch (SQLException e) {
			throw new RuntimeException("Error while reading column meta data for ", e);
		}
	}

	public Iterator getPrimaryKeys(final String catalog, final String schema, final String table) {
		try {
			// log.debug("getPrimaryKeys(" + catalog + "." + schema + "." + table + ")");
			ResultSet tableRs = getMetaData().getPrimaryKeys(catalog, schema, table);

			return new ResultSetIterator(tableRs) {
				Map element = new HashMap();

				protected Object convertRow(ResultSet rs) throws SQLException {
					element.clear();
					putTablePart(element, rs);
					element.put("COLUMN_NAME", rs.getString("COLUMN_NAME"));
					element.put("KEY_SEQ", new Short(rs.getShort("KEY_SEQ")));
					element.put("PK_NAME", rs.getString("PK_NAME"));
					return element;
				}

				protected Throwable handleSQLException(SQLException e) {
					throw new RuntimeException("Error while reading primary key meta data for ", e);
				}
			};
		} catch (SQLException e) {
			throw new RuntimeException("Error while reading primary key meta data for ", e);
		}
	}

	public Iterator getExportedKeys(final String catalog, final String schema, final String table) {
		try {
			// log.debug("getExportedKeys(" + catalog + "." + schema + "." + table + ")");
			ResultSet tableRs = getMetaData().getExportedKeys(catalog, schema, table);

			return new ResultSetIterator(tableRs) {

				Map element = new HashMap();

				protected Object convertRow(ResultSet rs) throws SQLException {
					element.clear();
					element.put("PKTABLE_NAME", rs.getString("PKTABLE_NAME"));
					element.put("PKTABLE_SCHEM", rs.getString("PKTABLE_SCHEM"));
					element.put("PKTABLE_CAT", rs.getString("PKTABLE_CAT"));
					element.put("FKTABLE_CAT", rs.getString("FKTABLE_CAT"));
					element.put("FKTABLE_SCHEM", rs.getString("FKTABLE_SCHEM"));
					element.put("FKTABLE_NAME", rs.getString("FKTABLE_NAME"));
					element.put("FKCOLUMN_NAME", rs.getString("FKCOLUMN_NAME"));
					element.put("PKCOLUMN_NAME", rs.getString("PKCOLUMN_NAME"));
					element.put("FK_NAME", rs.getString("FK_NAME"));
					element.put("KEY_SEQ", new Short(rs.getShort("KEY_SEQ")));
					return element;
				}

				protected Throwable handleSQLException(SQLException e) {
					throw new RuntimeException("Error while reading exported keys meta data for ", e);
				}
			};
		} catch (SQLException e) {
			throw new RuntimeException("Error while reading exported keys meta data for ", e);
		}
	}

	protected Connection getConnection() throws SQLException {
		if (connection == null) {
			connection = provider.getConnection();
		}
		return connection;
	}

	public boolean needQuote(String name) {
		// TODO: use jdbc metadata to decide on this. but for now we just handle the most typical cases.
		if (name.indexOf('-') > 0)
			return true;
		if (name.indexOf(' ') > 0)
			return true;
		return false;
	}
}
