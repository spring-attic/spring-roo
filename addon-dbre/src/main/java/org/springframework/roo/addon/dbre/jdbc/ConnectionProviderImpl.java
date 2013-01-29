package org.springframework.roo.addon.dbre.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.jdbc.JdbcDriverManager;
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

    private static final String PASSWORD = "password";
    private static final String USER = "user";

    @Reference private JdbcDriverManager jdbcDriverManager;

    public void closeConnection(final Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            }
            catch (final SQLException ignored) {
            }
        }
    }

    public Connection getConnection(final Map<String, String> map,
            final boolean displayAddOns) throws RuntimeException {
        return getConnection(getProps(map), displayAddOns);
    }

    public Connection getConnection(final Properties props,
            final boolean displayAddOns) throws RuntimeException {
        Validate.notEmpty(props,
                "Connection properties must not be null or empty");

        // The properties "user" and "password" are required to make a
        // connection
        if (props.getProperty(USER) == null) {
            props.put(USER, props.getProperty("database.username"));
        }
        if (props.getProperty(PASSWORD) == null) {
            props.put(PASSWORD, props.getProperty("database.password"));
        }

        final String driverClassName = props
                .getProperty("database.driverClassName");
        final Driver driver = jdbcDriverManager.loadDriver(driverClassName,
                displayAddOns);
        Validate.notNull(driver, "JDBC driver not available for '%s'",
                driverClassName);
        try {
            return driver.connect(props.getProperty("database.url"), props);
        }
        catch (final SQLException e) {
            throw new IllegalStateException(
                    "Unable to get connection from driver: " + e.getMessage(),
                    e);
        }
    }

    public Connection getConnectionViaJndiDataSource(
            final String jndiDataSource, final Map<String, String> map,
            final boolean displayAddOns) throws RuntimeException {
        try {
            final InitialContext context = new InitialContext(getProps(map));
            final DataSource dataSource = (DataSource) context
                    .lookup(jndiDataSource);
            return dataSource.getConnection();
        }
        catch (final Exception e) {
            throw new IllegalStateException(
                    "Unable to get connection from driver: " + e.getMessage(),
                    e);
        }
    }

    private Properties getProps(final Map<String, String> map) {
        Validate.isTrue(!CollectionUtils.isEmpty(map),
                "Connection properties map must not be null or empty");
        final Properties props = new Properties();
        props.putAll(map);
        return props;
    }
}
