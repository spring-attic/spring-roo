package org.springframework.roo.addon.creator;

import java.io.File;
import java.util.Locale;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Commands for the 'addon create' add-on to be used by the ROO shell.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component
@Service
public class CreatorCommands implements CommandMarker {
	@Reference private CreatorOperations creatorOperations;
		
	@CliAvailabilityIndicator({ "addon create i18n", "addon create simple", "addon create advanced" })
	public boolean isCreateAddonAvailable() {
		return creatorOperations.isCommandAvailable();
	}
	
	@CliCommand(value = "addon create i18n", help = "Create a new Internationalization add-on for Spring Roo")
	public void i18n(
		@CliOption(key = "topLevelPackage", mandatory = true, optionContext = "update", help = "The top level package of the new addon") JavaPackage tlp, 
		@CliOption(key = "locale", mandatory = true, help = "The locale abbreviation (ie: en, or more specific like en_AU, or de_DE)") Locale locale, 
		@CliOption(key = "messageBundle", mandatory = true, help = "Fully qualified path to the messages_xx.properties file") File messageBundle, 
		@CliOption(key = "language", mandatory = false, help = "The full name of the language (used as a label for the UI)") String language, 
		@CliOption(key = "flagGraphic", mandatory = false, help = "Fully qualified path to flag xx.png file") File flagGraphic, 
		@CliOption(key = "description", mandatory = false, help = "Description of your addon (surround text with double quotes)") String description,
		@CliOption(key = "projectName", mandatory = false, help = "Provide a custom project name (if not provided the top level package name will be used instead)") String projectName) {
		
		if (locale == null) {
			throw new IllegalStateException("Could not read provided locale. Please use correct format (ie: en, or more specific like en_AU, or de_DE)");
		}
		creatorOperations.createI18nAddon(tlp, language, locale, messageBundle, flagGraphic, description, projectName);
	}
	
	@CliCommand(value = "addon create simple", help = "Create a new simple add-on for Spring Roo (commands + operations)")
	public void simple(
		@CliOption(key = "topLevelPackage", mandatory = true, optionContext = "update", help = "The top level package of the new addon") JavaPackage tlp, 
		@CliOption(key = "description", mandatory = false, help = "Description of your addon (surround text with double quotes)") String description,
		@CliOption(key = "projectName", mandatory = false, help = "Provide a custom project name (if not provided the top level package name will be used instead)") String projectName) {
		
		creatorOperations.createSimpleAddon(tlp, description, projectName);
	}
	
	@CliCommand(value = "addon create advanced", help = "Create a new advanced add-on for Spring Roo (commands + operations + metadata + trigger annotation + dependencies)")
	public void advanced(
		@CliOption(key = "topLevelPackage", mandatory = true, optionContext = "update", help = "The top level package of the new addon") JavaPackage tlp, 
		@CliOption(key = "description", mandatory = false, help = "Description of your addon (surround text with double quotes)") String description,
		@CliOption(key = "projectName", mandatory = false, help = "Provide a custom project name (if not provided the top level package name will be used instead)") String projectName) {
		
		creatorOperations.createAdvancedAddon(tlp, description, projectName);
	}
	
	@CliCommand(value = "addon create wrapper", help = "Create a new add-on for Spring Roo which wraps a maven artifact to create a OSGi compliant bundle")
	public void wrapper(
		@CliOption(key = "topLevelPackage", mandatory = true, optionContext = "update", help = "The top level package of the new wrapper bundle") JavaPackage tlp, 
		@CliOption(key = "groupId", mandatory = true, help = "Dependency group id") String groupId,
		@CliOption(key = "artifactId", mandatory = true, help = "Dependency artifact id)") String artifactId,
		@CliOption(key = "version", mandatory = true, help = "Dependency version") String version,
		@CliOption(key = "vendorName", mandatory = true, help = "Dependency vendor name)") String vendorName,
		@CliOption(key = "licenseUrl", mandatory = true, help = "Dependency license URL") String lincenseUrl,
		@CliOption(key = "docUrl", mandatory = false, help = "Dependency documentation URL") String docUrl,
		@CliOption(key = "description", mandatory = false, help = "Description of the bundle (use keywords with #-tags for better search integration)") String description,
		@CliOption(key = "projectName", mandatory = false, help = "Provide a custom project name (if not provided the top level package name will be used instead)") String projectName,
		@CliOption(key = "osgiImports", mandatory = false, help = "Contents of Import-Package in OSGi manifest") String osgiImports) {
		
		creatorOperations.createWrapperAddon(tlp, groupId, artifactId, version, vendorName, lincenseUrl, docUrl, osgiImports, description, projectName);
	}
}