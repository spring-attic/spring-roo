package org.springframework.roo.addon.dbre.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public interface DbConnectionProvider {

	void configure(Properties props);

	Connection getConnection() throws SQLException;

	void closeConnection(Connection connection);
}
