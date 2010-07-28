package org.springframework.roo.addon.dbre;

import java.util.Set;

import org.springframework.roo.addon.dbre.model.Database;
import org.springframework.roo.addon.dbre.model.Schema;

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
	 * Returns a Set of available database {@link Schema schemas). 
	 * 
	 * @return a Set of schemas.
	 */
	Set<Schema> getDatabaseSchemas();
	
	/**
	 * Returns the last known schema.
	 * 
	 * @return schema the last schema introspected, or null if not introspected.
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
	 * @param schema to refresh (required)
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
}
