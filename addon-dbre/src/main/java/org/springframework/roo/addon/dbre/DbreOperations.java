package org.springframework.roo.addon.dbre;

import java.io.File;

import org.springframework.roo.addon.dbre.model.Schema;
import org.springframework.roo.model.JavaPackage;

/**
 * Interface to commands available in {@link DbreOperationsImpl}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public interface DbreOperations {

	boolean isDbreAvailable();

	/**
	 * Displays the metadata for the indicated schema on the screen, or writes it to
	 * the given file if a filename is specified.
	 * 
	 * @param schema to display (required)
	 * @param file to write to (can be null, in which case the output will appear on-screen)
	 */
	void displayDatabaseMetadata(Schema schema, File file);

	/**
	 * Introspects the schema and causes the related entities on disk to be created, updated
	 * and deleted.
	 * 
	 * @param schema to introspect (required)
	 * @param destinationPackage the package in which all entities will be stored (if not
	 * given, the package of any already-introspected entities will be used, or, failing
	 * that, the project's top level package)
	 */
	void reverseEngineerDatabase(Schema schema, JavaPackage destinationPackage);
}