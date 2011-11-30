package org.springframework.roo.addon.dbre.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.jdbc.JdbcDriverManager;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.CollectionUtils;

/**
 * Implementation of {@link ConnectionProvider}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class ConnectionProviderImpl implements ConnectionProvider {

	// Constants
	private static final String USER = "user";
	private static final String PASSWORD = "password";

	// Fields
	@Reference private JdbcDriverManager jdbcDriverManager;

	public Connection getConnection(final Properties props, final boolean displayAddOns) throws RuntimeException {
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

	public Connection getConnection(final Map<String, String> map, final boolean displayAddOns) throws RuntimeException {
		return getConnection(getProps(map), displayAddOns);
	}

	public Connection getConnection(final String jndiDataSource, final Map<String, String> map, final boolean displayAddOns) throws RuntimeException {
		try {
			InitialContext context = new InitialContext(getProps(map));
			DataSource dataSource = (DataSource) context.lookup(jndiDataSource);
			return dataSource.getConnection();
		} catch (Exception e) {
			throw new IllegalStateException("Unable to get connection from driver: " + e.getMessage(), e);
		}
	}

	public void closeConnection(final Connection connection) {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException ignored) {}
		}
	}

	private Properties getProps(final Map<String, String> map) {
		Assert.isTrue(!CollectionUtils.isEmpty(map), "Connection properties map must not be null or empty");
		Properties props = new Properties();
		props.putAll(map);
		return props;
	}
}
