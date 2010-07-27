package org.springframework.roo.addon.jdbc.polling.internal;

import java.sql.Driver;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.jdbc.polling.JdbcDriverProvider;
import org.springframework.roo.support.util.ClassUtils;

/**
 * Basic implementation of {@link JdbcDriverProvider} that provides common JDBC drivers.
 * To be returned by this provider, it is necessary the JDBC driver has been declared
 * as an optional import within the JDBC add-on's OSGi bundle manifest.
 *
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class CommonJdbcDriverProvider implements JdbcDriverProvider {

	public Driver loadDriver(String driverClassName) throws RuntimeException {
		if (!ClassUtils.isPresent(driverClassName, CommonJdbcDriverProvider.class.getClassLoader())) {
			// Not present - return null as per interface contract so another provider can be searched
			return null;
		}
		try {
			return (Driver) ClassUtils.forName(driverClassName, CommonJdbcDriverProvider.class.getClassLoader()).newInstance();
		} catch (Exception e) {
			throw new IllegalStateException("Unable to load JDBC driver '" + driverClassName + "'", e);
		}
	}
}
