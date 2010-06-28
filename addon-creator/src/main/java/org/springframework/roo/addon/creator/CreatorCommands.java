package org.springframework.roo.addon.creator;

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
 *
 */
@Component
@Service
public class CreatorCommands implements CommandMarker {
	
	@Reference private CreatorOperations generatorOperations;
		
	@CliAvailabilityIndicator({/*"addon create i18n",*/ "addon create simple", "addon create advanced"})
	public boolean isCreateAddonAvailable() {
		return generatorOperations.isCommandAvailable();
	}
	
//	@CliCommand(value="addon create i18n", help="Create a new Internationalization add-on for Spring Roo")
//	public void i18n(
//			@CliOption(key="topLevelPackage", mandatory=true, help="The top level package of the new addon") JavaPackage tlp,
//			@CliOption(key="language", mandatory=true, help="The full name of the language") String language, 
//			@CliOption(key="locale", mandatory=true, help="The locale abbreviation (ie: en_AU, or de_DE") String locale, 
//			@CliOption(key="messageBundle", mandatory=true, help="Source path to the messages_xx.properties file") File messageBundle,
//			@CliOption(key="flagGraphic", mandatory=true, help="Source path to flag xx.png file") File flagGraphic) {
//		generatorOperations.createI18nAddon(tlp, language, locale, messageBundle, flagGraphic);
//	}
	
	@CliCommand(value="addon create simple", help="Create a new simple add-on for Spring Roo (commands + operations)")
	public void simple(
			@CliOption(key="topLevelPackage", mandatory=true, help="The top level package of the new addon") JavaPackage tlp) {
		generatorOperations.createSimpleAddon(tlp);
	}
	
	@CliCommand(value="addon create advanced", help="Create a new advanced add-on for Spring Roo (commands + operations + metadata + trigger annotation + depedencies")
	public void advanced(
			@CliOption(key="topLevelPackage", mandatory=true, help="The top level package of the new addon") JavaPackage tlp) {
		generatorOperations.createAdvancedAddon(tlp);
	}
}