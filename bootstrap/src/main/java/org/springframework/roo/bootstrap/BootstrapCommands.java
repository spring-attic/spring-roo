package org.springframework.roo.bootstrap;

import org.springframework.roo.process.manager.internal.DefaultProcessManager;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.Shell;
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
	private Shell shell;

	public BootstrapCommands(DefaultProcessManager processManager, Shell shell) {
		Assert.notNull(processManager, "Process manager required");
		Assert.notNull(shell, "Shell required");
		this.processManager = processManager;
		this.shell = shell;
		
		if (!"false".equals(System.getProperty("developmentMode", "false").toLowerCase())) {
			developmentMode(true);
		}
	}
	
	@CliCommand(value="development mode", help="Switches the system into development mode (greater diagnostic information)")
	public String developmentMode(
			@CliOption(key={"","enabled"}, mandatory=false, specifiedDefaultValue="true", unspecifiedDefaultValue="true") boolean enabled) {
		processManager.setDevelopmentMode(enabled);
		shell.setDevelopmentMode(enabled);
		return "Development mode set to " + enabled;
	}
}
