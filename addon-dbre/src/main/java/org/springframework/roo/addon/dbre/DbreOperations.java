package org.springframework.roo.addon.dbre;

/**
 * Interface to commands available in {@link DbreOperationsImpl}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public interface DbreOperations {

	boolean isDbreAvailable();
	
	void displayDatabaseMetadata(String table);
}