package org.springframework.roo.addon.dbre.model;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.springframework.roo.support.util.Assert;

/**
 * Abstract base class for obtaining {@link DatabaseMetaData}.
 * 
 * @author Alan Stewart
 * @since 1.1.2
 */
public abstract class AbstractIntrospector {
	protected DatabaseMetaData databaseMetaData;

	AbstractIntrospector(Connection connection) throws SQLException {
		Assert.notNull(connection, "Connection must not be null");
		databaseMetaData = connection.getMetaData();
		Assert.notNull(databaseMetaData, "Database metadata is null");
	}
}