package org.springframework.roo.addon.jdbc;

import java.sql.Driver;

/**
 * Locates a JDBC driver and returns an instantiated version thereof.
 * 
 * <p>
 * An implementation may inform the user where such a driver can be obtained if it is not
 * presently available.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public interface JdbcDriverManager {

	/**
	 * Attempts to locate and instantiate the specified JDBC driver.
	 * 
	 * <p>
	 * The JDBC driver must provide a public no-argument constructor.
	 * 
	 * @param driverClassName to load (required)
	 * @param displayAddOns display available add-ons if possible (required)
	 * @return the driver, or null if the driver could not be located
	 * @throws RuntimeException if the driver was located but could not be instantiated
	 */
	Driver loadDriver(String driverClassName, boolean displayAddOns) throws RuntimeException;

}
