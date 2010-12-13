package org.springframework.roo.addon.dbre.jdbc;

import java.sql.Connection;
import java.util.Map;
import java.util.Properties;

/**
 * Provides JDBC (@link Connection}s.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public interface ConnectionProvider {

	/**
	 * Returns a JDBC {@link Connection} configured with the specified connection properties.
	 * 
	 * <p>
	 * The properties "user" and "password" are required for the driver to make a connection.
	 * If these properties are not supplied, the implementing method will need to provide them.
	 * 
	 * @param props the database connection properties (required)
	 * @param displayAddOns displays add-on availability if the JDBC driver isn't available (required)
	 * @return a new connection
	 * @throws RuntimeException if there is a problem acquiring a connection
	 */
	Connection getConnection(Properties props, boolean displayAddOns) throws RuntimeException;

	/**
	 * Returns a JDBC {@link Connection} configured with the specified connection properties map.
	 * 
	 * <p>
	 * The properties "user" and "password" are required for the driver to make a connection.
	 * If the map does not contain these properties, the implementing method will need to provide them.
	 * 
	 * @param map the database connection properties contained in a map (required)
	 * @param displayAddOns displays add-on availability if the JDBC driver isn't available (required)
	 * @return a new connection
	 * @throws RuntimeException if there is a problem acquiring a connection
	 */
	Connection getConnection(Map<String, String> map, boolean displayAddOns) throws RuntimeException;

	/**
	 * Closes the given {@link Connection}. 
	 * 
	 * <p>
	 * An exception will NOT be thrown if the connection cannot be closed.
	 * 
	 * @param connection the connection to close (may be null).
	 */
	void closeConnection(Connection connection);
}
