package org.springframework.roo.addon.dbre;

import java.io.File;
import java.util.Set;

import org.springframework.roo.addon.dbre.model.Database;
import org.springframework.roo.addon.dbre.model.Schema;
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
	 * @param schema the {@link Schema schema) object representing the database schema.
	 * @param javaPackage the package where entities are placed.
	 * @return the database metadata as an XML string.
	 */
	String getDatabaseMetadata(Schema schema, JavaPackage javaPackage);

	/**
	 * Writes the database metadata in DOM format to an XML file.
	 * 
	 * @param schema the {@link Schema schema) object representing the database schema.
	 * @param javaPackage the package where entities are placed.
	 * @param file the {@link File file} to write the database metadata to.
	 */
	void serializeDatabaseMetadata(Schema schema, JavaPackage javaPackage, File file);

	/**
	 * Reads and converts the database XML file into a {@link Database} object.
	 * 
	 * @return the database model. May return null if the XML file does not exist or is empty.
	 */
	Database deserializeDatabaseMetadata();
}
