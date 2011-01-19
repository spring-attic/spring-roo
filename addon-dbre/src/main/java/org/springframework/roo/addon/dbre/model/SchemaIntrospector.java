package org.springframework.roo.addon.dbre.model;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Returns database schemas from a live database connection using JDBC.
 * 
 * @author Alan Stewart
 * @since 1.1.2
 */
public class SchemaIntrospector extends AbstractIntrospector {
	
	public SchemaIntrospector(Connection connection) throws SQLException {
		super(connection);
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
}
