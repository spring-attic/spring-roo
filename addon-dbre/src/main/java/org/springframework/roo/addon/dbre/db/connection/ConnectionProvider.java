package org.springframework.roo.addon.dbre.db.connection;

import java.sql.Connection;
import java.util.Properties;

public interface ConnectionProvider {

	void configure(Properties props);

	Connection getConnection();

	void closeConnection(Connection connection);
}
