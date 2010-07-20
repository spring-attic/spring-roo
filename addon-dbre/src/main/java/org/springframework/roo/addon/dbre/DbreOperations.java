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

	void displayDatabaseMetadata(Schema schema, File file);

	void serializeDatabaseMetadata(Schema schema, JavaPackage javaPackage);
}