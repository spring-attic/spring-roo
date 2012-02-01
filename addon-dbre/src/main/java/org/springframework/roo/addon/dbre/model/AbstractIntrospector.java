package org.springframework.roo.addon.dbre.model;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.apache.commons.lang3.Validate;

/**
 * Abstract base class for obtaining {@link DatabaseMetaData}.
 * 
 * @author Alan Stewart
 * @since 1.1.2
 */
public abstract class AbstractIntrospector {
    protected DatabaseMetaData databaseMetaData;

    AbstractIntrospector(final Connection connection) throws SQLException {
        Validate.notNull(connection, "Connection required");
        databaseMetaData = connection.getMetaData();
        Validate.notNull(databaseMetaData, "Database metadata is null");
    }
}