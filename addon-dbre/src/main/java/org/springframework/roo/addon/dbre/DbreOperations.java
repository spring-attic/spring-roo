package org.springframework.roo.addon.dbre;

import org.springframework.roo.model.JavaPackage;

/**
 * Interface to commands available in {@link DbreOperationsImpl}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public interface DbreOperations {

	boolean isDbreAvailable();
	
	void displayDbMetadata(String table, String file);
	
	void updateDbreXml(JavaPackage javaPackage);
}