package org.springframework.roo.bootstrap;

import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.lifecycle.ScopeDevelopmentShell;
import org.springframework.roo.support.util.Assert;

/**
 * Commands related to add-on maintenance.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopmentShell
public class AddOnCommands implements CommandMarker {

	private AddOnOperations addOnOperations;
	
	public AddOnCommands(AddOnOperations addOnOperations) {
		Assert.notNull(addOnOperations, "Add on operations required");
		this.addOnOperations = addOnOperations;
	}

	@CliCommand(value="addon cleanup", help="Cleans the $ROO_HOME/work directory so it only contains correct JARs as per $ROO_HOME/add-ons")
	public void cleanUpCmd() {
		addOnOperations.cleanUp();
	}
	
	@CliCommand(value="addon install", help="Installs a new add-on to the $ROO_HOME/add-ons directory")
	public void installCmd(
			@CliOption(key={"","url"}, mandatory=true, help="The URL to obtain the add-on ZIP file from") String url) {
		addOnOperations.install(url);
	}

	@CliCommand(value="addon uninstall", help="Removes an existing add-on from the $ROO_HOME/add-ons directory")
	public void uninstallCmd(
			@CliOption(key={"","pattern"}, mandatory=true, help="The filename pattern to remove") String pattern) {
		addOnOperations.uninstall(pattern);
	}
	
	@CliCommand(value="addon list", help="Lists add-ons installed in the $ROO_HOME/add-ons directory")
	public void listCmd() {
		addOnOperations.list();
	}


}
