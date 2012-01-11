package org.springframework.roo.addon.jdbc.polling.internal;

import java.sql.Driver;
import java.sql.DriverManager;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.jdbc.polling.JdbcDriverProvider;
import org.springframework.roo.support.osgi.BundleFindingUtils;

/**
 * Basic implementation of {@link JdbcDriverProvider} that provides common JDBC
 * drivers. To be returned by this provider, it is necessary the JDBC driver has
 * been declared as an optional import within the JDBC add-on's OSGi bundle
 * manifest.
 * 
 * @author Alan Stewart
 * @author Ben Alex
 * @since 1.1
 */
@Component
@Service
public class CommonJdbcDriverProvider implements JdbcDriverProvider {

    private BundleContext bundleContext;

    protected void activate(final ComponentContext context) {
        bundleContext = context.getBundleContext();
    }

    protected void deactivate(final ComponentContext context) {
        bundleContext = null;
    }

    public Driver loadDriver(final String driverClassName)
            throws RuntimeException {
        // Try a search
        final Class<?> clazz = BundleFindingUtils.findFirstBundleWithType(
                bundleContext, driverClassName);

        if (clazz == null) {
            // Let's give up given it doesn't seem to be loadable
            return null;
        }

        if (!Driver.class.isAssignableFrom(clazz)) {
            // That's weird, it doesn't seem to be a driver
            return null;
        }

        // Time to create it and register etc
        try {
            final Driver result = (Driver) clazz.newInstance();
            DriverManager.registerDriver(result);
            return result;
        }
        catch (final Exception e) {
            throw new IllegalStateException("Unable to load JDBC driver '"
                    + driverClassName + "'", e);
        }
    }
}
