package org.springframework.roo.addon.creator;

import java.io.File;
import java.util.Locale;

import org.springframework.roo.model.JavaPackage;

/**
 * Provides an interface to {@link CreatorOperationsImpl}.
 * 
 * @author Stefan Schmidt
 */
public interface CreatorOperations {
	
	boolean isCommandAvailable();

	void createI18nAddon(JavaPackage topLevelPackage, String language, Locale locale, File messageBundle, File flagGraphic, String description);
	
	void createSimpleAddon(JavaPackage topLevelPackage, String description);
	
	void createAdvancedAddon(JavaPackage topLevelPackage, String description);
}