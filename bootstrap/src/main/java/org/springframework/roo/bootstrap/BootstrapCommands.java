package org.springframework.roo.bootstrap;

import org.springframework.roo.process.manager.internal.DefaultProcessManager;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.lifecycle.ScopeDevelopmentShell;
import org.springframework.roo.support.util.Assert;

/**
 * Basic commands for overall system operation.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopmentShell
public class BootstrapCommands implements CommandMarker {
	private DefaultProcessManager processManager;

	public BootstrapCommands(DefaultProcessManager processManager) {
		Assert.notNull(processManager, "Process manager required");
		this.processManager = processManager;
	}
	
	@CliCommand(value="development mode", help="Switches the system into development mode (greater diagnostic information)")
	public String developmentMode(
			@CliOption(key={"","enabled"}, mandatory=false, specifiedDefaultValue="true", unspecifiedDefaultValue="true") boolean enabled) {
		processManager.setDevelopmentMode(enabled);
		return "Development mode set to " + enabled;
	}
}
