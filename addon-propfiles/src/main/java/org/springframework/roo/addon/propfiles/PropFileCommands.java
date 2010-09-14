package org.springframework.roo.addon.propfiles;

import java.util.SortedSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.project.Path;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Commands for the 'propfile' add-on to be used by the ROO shell.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component 
@Service
public class PropFileCommands implements CommandMarker {
	@Reference private PropFileOperations propFileOperations;

	@CliAvailabilityIndicator({ "properties list", "properties set", "properties remove" }) 
	public boolean isInstallWebFlowAvailable() {
		return propFileOperations.isPropertiesCommandAvailable();
	}

	@CliCommand(value = "properties list", help = "Shows the details of a particular properties file") 
	public SortedSet<String> propertyFileKeys(
		@CliOption(key = "name", mandatory = true, help = "Property file name (including .properties suffix)") String name, 
		@CliOption(key = "path", mandatory = true, help = "Source path to property file") Path path) {
		
		return propFileOperations.getPropertyKeys(path, name, true);
	}

	@CliCommand(value = "properties set", help = "Changes a particular properties file property") 
	public void databaseSet(
		@CliOption(key = "name", mandatory = true, help = "Property file name (including .properties suffix)") String name, 
		@CliOption(key = "path", mandatory = true, help = "Source path to property file") Path path, 
		@CliOption(key = "key", mandatory = true, help = "The property key that should be changed") String key, 
		@CliOption(key = "value", mandatory = true, help = "The new vale for this property key") String value) {
		
		propFileOperations.changeProperty(path, name, key, value);
	}

	@CliCommand(value = "properties remove", help = "Removes a particular properties file property") 
	public void databaseRemove(
		@CliOption(key = "name", mandatory = true, help = "Property file name (including .properties suffix)") String name,
		@CliOption(key = "path", mandatory = true, help = "Source path to property file") Path path, 
		@CliOption(key = { "", "key" }, mandatory = true, help = "The property key that should be removed") String key) {
		
		propFileOperations.removeProperty(path, name, key);
	}

}