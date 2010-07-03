package org.springframework.roo.addon.dbre.model;

import java.io.File;
import java.util.Set;

import org.springframework.roo.model.JavaPackage;

/**
 * Specifies methods to retrieve database metadata.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public interface DatabaseModelService {

	/**
	 * Returns a Set of available database {@link Schema schemas). 
	 * 
	 * @return a Set of schemas.
	 */
	Set<Schema> getDatabaseSchemas();

	/**
	 * Connects to a live database and displays database metadata.
	 * 
	 * @param catalog the name of the database catalog (may be null).
	 * @param schema the {@link Schema schema) object representing the database schema.
	 */
	void displayDatabaseMetadata(String catalog, Schema schema);

	/**
	 * Writes the database metadata in DOM format to an XML file.
	 * 
	 * @param catalog the name of the database catalog (may be null).
	 * @param schema the {@link Schema schema) object representing the database schema.
	 * @param javaPackage the package where entities are placed.
	 */
	void serializeDatabaseMetadata(String catalog, Schema schema, JavaPackage javaPackage, File file);

	/**
	 * Reads and converts the database XML file into a {@link Database} object.
	 * 
	 * @return the database model.
	 */
	Database deserializeDatabaseMetadata();
}
