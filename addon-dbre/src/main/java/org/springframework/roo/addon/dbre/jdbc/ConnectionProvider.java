package org.springframework.roo.addon.dbre.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

/**
 * Provides JDBC (@link Connection}s.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public interface ConnectionProvider {

	void configure(Properties props);

	void configure(Map<String, String> map);

	Connection getConnection() throws SQLException;

	void closeConnection(Connection connection);
}
