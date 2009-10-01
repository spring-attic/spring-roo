package org.springframework.roo.addon.propfiles;

import java.util.SortedSet;

import org.springframework.roo.project.Path;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.converters.StaticFieldConverter;
import org.springframework.roo.support.lifecycle.ScopeDevelopmentShell;
import org.springframework.roo.support.util.Assert;

/**
 * Commands for the 'propfile' add-on to be used by the ROO shell.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopmentShell
public class PropFileCommands implements CommandMarker {
	
	private PropFileOperations propFileOperations;
	
	public PropFileCommands(StaticFieldConverter staticFieldConverter, PropFileOperations loggingOperations) {
		Assert.notNull(staticFieldConverter, "Static field converter required");
		Assert.notNull(loggingOperations, "Logging operations required");
		this.propFileOperations = loggingOperations;
	}
	
	@CliCommand(value="properties list", help="Shows the details of a particular properties file")
	public SortedSet<String> propertyFileKeys(
			@CliOption(key="name", mandatory=true) String name, 
			@CliOption(key="path", mandatory=true) Path path) {
		return propFileOperations.getPropertyKeys(path, name, true);
	}
	
	@CliCommand(value="properties set", help="Changes a particular properties file property")
	public void databaseSet(
			@CliOption(key="name", mandatory=true) String name, 
			@CliOption(key="path", mandatory=true) Path path,
			@CliOption(key="key", mandatory=true, help="The property key that should be changed") String key, @CliOption(key="value", mandatory=true, help="The new vale for this property key") String value) {
		propFileOperations.changeProperty(path, name, key, value);
	}

	@CliCommand(value="properties remove", help="Removes a particular properties file property")
	public void databaseRemove(
			@CliOption(key="name", mandatory=true) String name, 
			@CliOption(key="path", mandatory=true) Path path,
			@CliOption(key={"","key"}, mandatory=true, help="The property key that should be removed") String key) {
		propFileOperations.removeProperty(path, name, key);
	}

	
}