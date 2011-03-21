package org.springframework.roo.addon.dbre.model;

import java.io.OutputStream;
import java.util.Set;

/**
 * Retrieves database metadata from an XML file or a JDBC connection.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public interface DbreModelService {
	
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
	Set<Schema> getSchemas(boolean displayAddOns);
	
	/**
	 * Reads the database metadata information from DBRE XML if possible.
	 * 
	 * <p>
	 * NOTE: the XML file can only store one database.
	 * 
	 * @return the database metadata if it could be parsed, otherwise null if unavailable for any reason
	 */
	Database getDatabase();

	/**
	 * Retrieves the database metadata from a cache if possible.
	 *  
	 * @return the database metadata, otherwise null if unavailable for any reason
	 */
	Database getDatabaseFromCache();

	/**
	 * Returns the identifier for the DBRE XML file.
	 * 
	 * @return a String representing the path of the DBRE XML file
	 */
	String getDbreXmlPath();

	/**
	 * Retrieves the database metadata from a JDBC connection.
	 * 
	 * @param schema the schema to query (required)
	 * @param view true if database views are to be retrieved, otherwise false
	 * @param includeTables a set of table names to include
	 * @param excludeTables a set of table names to exlude
	 * @return the database metadata if available (null if cannot connect to the database or the schema is not found)
	 */
	Database refreshDatabase(Schema schema, boolean view, Set<String> includeTables, Set<String> excludeTables);
	
	/**
	 * Writes the database metadata to an output stream.
	 * 
	 * <p>
	 * It is the responsibility of the caller to close the output stream when finished.
	 * 
	 * @param database the database metadata (required)
	 * @param outputStream the output stream to write to (required)
	 * @param displayOnly whether the serialization operation is for instrospection purposes only or for the Roo-managed DBRE XML file
	 */
	void serializeDatabase(Database database, OutputStream outputStream, boolean displayOnly);
}
