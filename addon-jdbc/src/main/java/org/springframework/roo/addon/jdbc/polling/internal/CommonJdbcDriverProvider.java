package org.springframework.roo.addon.jdbc.polling.internal;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

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
			registerDriverIfRequired(driverClassName);
			return (Driver) ClassUtils.forName(driverClassName, CommonJdbcDriverProvider.class.getClassLoader()).newInstance();
		} catch (Exception e) {
			throw new IllegalStateException("Unable to load JDBC driver '" + driverClassName + "'", e);
		}
	}

	private void registerDriverIfRequired(String driverClassName) throws SQLException {
		// DB2/400 driver must be registered with DriverManager (ROO-1479)
		if ("com.ibm.as400.access.AS400JDBCDriver".equals(driverClassName)) {
			DriverManager.registerDriver(new com.ibm.as400.access.AS400JDBCDriver());
		}
		
		// Firebird driver must be registered with DriverManager 
		if ("org.firebirdsql.jdbc.FBDriver".equals(driverClassName)) {
			DriverManager.registerDriver(new org.firebirdsql.jdbc.FBDriver());
		}
	}
}
