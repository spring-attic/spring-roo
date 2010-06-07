package org.springframework.roo.addon.dbre.db.connection;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import org.apache.derby.jdbc.EmbeddedDriver;
import org.hsqldb.jdbcDriver;

public class ConnectionProviderImpl implements ConnectionProvider {
	private Properties props;

	public ConnectionProviderImpl(Properties props) {
		configure(props);
	}

	public ConnectionProviderImpl(Map<String, String> map) {
		Properties props = new Properties();
		props.putAll(map);
		configure(props);
	}
	
	public void configure(Properties props) {
		props.put("user", props.get("database.username"));
		props.put("password", props.get("database.password"));
		this.props = props;
	}

	public Connection getConnection() {
		try {
			return getDriver().connect(props.getProperty("database.url"), props);
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to get connection", e);
		}
	}

	public void closeConnection(Connection connection) {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				// Ignore
			}
		}
	}

	private Driver getDriver() {
		String driverClassName = props.getProperty("database.driverClassName");
		Driver driver;
		try {
			if (driverClassName.startsWith("org.hsqldb")) {
				driver = new jdbcDriver();
			} else if (driverClassName.startsWith("com.mysql")) {
				driver = new com.mysql.jdbc.Driver();
			} else if (driverClassName.startsWith("org.apache.derby")) {
				driver = new EmbeddedDriver();
			} else if (driverClassName.startsWith("org.h2")) {
				driver = new org.h2.Driver();
				// } else if (driverClassName.startsWith("oracle")) {
				// driver = new OracleDriver();
			} else {
				throw new IllegalStateException("Failed to get jdbc driver for " + driverClassName);
			}
			return driver;
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to get jdbc driver", e);
		}
	}
}
