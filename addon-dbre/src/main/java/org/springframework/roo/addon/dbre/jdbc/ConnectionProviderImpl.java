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
 * Implementation of {@link ConnectionProvider).
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class ConnectionProviderImpl implements ConnectionProvider {
	@Reference private JdbcDriverManager jdbcDriverManager;
	private Properties props;

	public void configure(Properties props) {
		Assert.notNull(props, "Connection properties must not be null");
		props.put("user", props.get("database.username"));
		props.put("password", props.get("database.password"));
		this.props = props;
	}

	public void configure(Map<String, String> map) {
		Assert.notNull(map, "Connection properties map must not be null");
		Properties props = new Properties();
		props.putAll(map);
		configure(props);
	}

	public Connection getConnection() throws SQLException {
		String driverClassName = props.getProperty("database.driverClassName");
		Driver driver = jdbcDriverManager.loadDriver(driverClassName);
		Assert.notNull(driver, "JDBC driver not available for '" + driverClassName + "'");
		return driver.connect(props.getProperty("database.url"), props);
	}

	public void closeConnection(Connection connection) {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException ignored) {}
		}
	}
}
