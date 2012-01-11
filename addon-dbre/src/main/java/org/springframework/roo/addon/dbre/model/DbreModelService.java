package org.springframework.roo.addon.dbre.model;

import java.util.Set;

/**
 * Retrieves database metadata from the DBRE XML file or a JDBC connection. Also
 * writes database metadata to the DBRE XML file.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public interface DbreModelService {

    /** The DBRE XML file name */
    String DBRE_XML = "dbre.xml";

    /**
     * The schema string for databases which do not support schemas, such as
     * MySQL.
     */
    String NO_SCHEMA_REQUIRED = "no-schema-required";

    /**
     * Reads the database metadata information from either a cache or from the
     * DBRE XML file if possible.
     * 
     * @param evictCache forces eviction of the database from the cache before
     *            attempting retrieval
     * @return the database metadata if it could be parsed, otherwise null if
     *         unavailable for any reason
     */
    Database getDatabase(boolean evictCache);

    /**
     * Returns a Set of available database {@link Schema schemas}.
     * 
     * @param displayAddOns display available add-ons if possible (required)
     * @return a Set of schemas.
     */
    Set<Schema> getSchemas(boolean displayAddOns);

    /**
     * Retrieves the database metadata from a JDBC connection.
     * 
     * @param schemas the schema(s) to query (required)
     * @param view true if database views are to be retrieved, otherwise false
     * @param includeTables a set of table names to include
     * @param excludeTables a set of table names to exlude
     * @return the database metadata if available (null if cannot connect to the
     *         database or the schema is not found)
     */
    Database refreshDatabase(Set<Schema> schemas, boolean view,
            Set<String> includeTables, Set<String> excludeTables);

    /**
     * Determines if the database uses schemas.
     * <p>
     * Examples of databases that do not use schemas are MySQL and Firebird.
     * 
     * @param displayAddOns display available add-ons if possible (required)
     * @return true if the database supports schema, otherwise false;
     * @throws RuntimeException if there is a problem acquiring a connection
     */
    boolean supportsSchema(boolean displayAddOns) throws RuntimeException;

    /**
     * Serializes the database to the DBRE XML file.
     * 
     * @param database the database to be written out to disk
     */
    void writeDatabase(Database database);
}
