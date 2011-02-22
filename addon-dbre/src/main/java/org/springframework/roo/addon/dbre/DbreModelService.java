package org.springframework.roo.addon.dbre;

import java.util.Set;

import org.springframework.roo.addon.dbre.model.Database;
import org.springframework.roo.addon.dbre.model.Schema;
import org.springframework.roo.model.JavaPackage;

/**
 * Specifies methods to retrieve database metadata.
 * 
 * <p>
 * An implementation must also guarantee to discover all {@link DatabaseListener} instances 
 * registered in the OSGi container and automatically notify them when a database is refreshed
 * or loaded for the first time.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public interface DbreModelService {
	
	/**
	 * The name of the DBRE XML file.
	 */
	String DBRE_FILE = ".roo-dbre";
	
	/**
	 * Determines if the database uses schemas.
	 * 
	 * <p>
	 * Examples of databases that do not use schemas are MySQL and Firebird.
	 * 
	 * @param displayAddOns display available add-ons if possible (required)
	 * @return true if the database supports schema, otherwise false;
	 * @throws RuntimeException if there is a problem acquiring a connection
	 */
	boolean supportsSchema(boolean displayAddOns) throws RuntimeException;

	/**
	 * Returns a Set of available database {@link Schema schemas}. 
	 * 
	 * @param displayAddOns display available add-ons if possible (required)
	 * @return a Set of schemas.
	 */
	Set<Schema> getDatabaseSchemas(boolean displayAddOns);
	
	/**
	 * Returns the last known schema.
	 * 
	 * @return the last schema introspected, or null if not introspected.
	 */
	Schema getLastSchema();

	/**
	 * Gets the latest representation of the database, potentially from a cache. Will connect to the
	 * database to obtain this information if it has not been cached.
	 * 
	 * @param schema to load (if not provided, the implementation will use the last known schema)
	 * @return the database if available (null if cannot connect to the database or the schema is not found)
	 */
	Database getDatabase(Schema schema);
	
	/**
	 * Forces the cache to be refreshed for the indicated schema. Useful if you know a database connection is
	 * available.
	 * 
	 * @param schema the schema to refresh (required)
	 * @return the database if available (null if cannot connect to the database or the schema is not found)
	 */
	Database refreshDatabase(Schema schema);
	
	/**
	 * Like {@link #refreshDatabase(Schema)}, except it will not change the last known schema and nor will it
	 * notify any listeners of the update nor change any caches (on-disk or otherwise). This method is ideal
	 * if you just want to view a database schema but not impact any existing DBRE status.
	 * 
	 * @param schema to refresh (required)
	 * @return the database if available (null if cannot connect to the database or the schema is not found)
	 */
	Database refreshDatabaseSafely(Schema schema);
		
	/**
	 * Specifies the {@link JavaPackage} to create entities in.
	 * 
	 * @param destinationPackage the JavaPackage
	 */
	void setDestinationPackage(JavaPackage destinationPackage);
	
	/**
	 * Sets the boolean flag to bring back database view information.
	 * 
	 * @param view true if database views are retrieved, otherwise false
	 */
	void setView(boolean view);
	
	/**
	 * Specifies the table names to include. 
	 *
	 * <p>
	 * If includeTables is not empty, only tables in this set will be reverse engineered.
	 *  
	 * @param includeTables a set of table names
	 */
	void setIncludeTables(Set<String> includeTables);

	/**
	 * Specifies the table names to exclude from reverse engineering.
	 *  
	 * @param excludeTables a set of table names
	 */
	void setExcludeTables(Set<String> excludeTables);
}
