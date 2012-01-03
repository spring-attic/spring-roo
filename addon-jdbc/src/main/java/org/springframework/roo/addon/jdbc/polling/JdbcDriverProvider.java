package org.springframework.roo.addon.jdbc.polling;

import java.sql.Driver;

import org.springframework.roo.addon.jdbc.JdbcDriverManager;
import org.springframework.roo.addon.jdbc.polling.internal.PollingJdbcDriverManager;

/**
 * Represents an implementation capable of participating in a
 * {@link PollingJdbcDriverManager}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public interface JdbcDriverProvider {

    /**
     * See {@link JdbcDriverManager#loadDriver(String, boolean)} for
     * description.
     */
    Driver loadDriver(String driverClassName) throws RuntimeException;
}
