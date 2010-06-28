package org.springframework.roo.addon.creator;

import java.io.File;

import org.springframework.roo.model.JavaPackage;

/**
 * Provides an interface to {@link CreatorOperationsImpl}.
 * 
 * @author Stefan Schmidt
 */
public interface CreatorOperations {
	
	boolean isCommandAvailable();

	void createI18nAddon(JavaPackage topLevelPackage, String language, String locale, File messageBundle, File flagGraphic);
	
	void createSimpleAddon(JavaPackage topLevelPackage);
	
	void createAdvancedAddon(JavaPackage topLevelPackage);
}