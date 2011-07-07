package org.springframework.roo.addon.dbre.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.jdbc.JdbcDriverManager;
import org.springframework.roo.support.util.Assert;

/**
 * Implementation of {@link ConnectionProvider}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class ConnectionProviderImpl implements ConnectionProvider {
	private static final String USER = "user";
	private static final String PASSWORD = "password";
	@Reference private JdbcDriverManager jdbcDriverManager;

	public Connection getConnection(Properties props, boolean displayAddOns) throws RuntimeException {
		Assert.notEmpty(props, "Connection properties must not be null or empty");

		// The properties "user" and "password" are required to make a connection
		if (props.getProperty(USER) == null) {
			props.put(USER, props.getProperty("database.username"));
		}
		if (props.getProperty(PASSWORD) == null) {
			props.put(PASSWORD, props.getProperty("database.password"));
		}

		String driverClassName = props.getProperty("database.driverClassName");
		Driver driver = jdbcDriverManager.loadDriver(driverClassName, displayAddOns);
		Assert.notNull(driver, "JDBC driver not available for '" + driverClassName + "'");
		try {
			return driver.connect(props.getProperty("database.url"), props);
		} catch (SQLException e) {
			throw new IllegalStateException("Unable to get connection from driver: " + e.getMessage(), e);
		}
	}

	public Connection getConnection(Map<String, String> map, boolean displayAddOns) throws RuntimeException {
		Assert.isTrue(map != null && !map.isEmpty(), "Connection properties map must not be null or empty");
		Properties props = new Properties();
		props.putAll(map);
		return getConnection(props, displayAddOns);
	}

	public void closeConnection(Connection connection) {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException ignored) {}
		}
	}
}
