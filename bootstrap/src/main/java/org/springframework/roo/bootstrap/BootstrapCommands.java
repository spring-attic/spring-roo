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
			@CliOption(key={"","enabled"}, mandatory=false, specifiedDefaultValue="true", unspecifiedDefaultValue="true", help="Activates development mode") boolean enabled) {
		processManager.setDevelopmentMode(enabled);
		shell.setDevelopmentMode(enabled);
		return "Development mode set to " + enabled;
	}
	
	
	@CliCommand(value="poll status", help="Display file system polling information")
	public String pollingInfo() {
		StringBuilder sb = new StringBuilder("File system polling ");
		long duration = processManager.getLastPollDuration();
		if (duration == 0) {
			sb.append("never executed; ");
		} else {
			sb.append("last took ").append(duration).append(" ms; ");
		}
		long minimum = processManager.getMinimumDelayBetweenPoll();
		if (minimum == 0) {
			sb.append("automatic polling is disabled");
		} else if (minimum < 0) {
			sb.append("auto-scaled polling is enabled");
		} else {
			sb.append("polling frequency has a minimum interval of ").append(minimum).append(" ms");
		}
		return sb.toString();
	}
	
	@CliCommand(value="poll speed", help="Changes the file system polling speed")
	public String pollingSpeed(@CliOption(key={"","ms"}, mandatory=true, help="The number of milliseconds between each poll") long minimumDelayBetweenPoll) {
		processManager.setMinimumDelayBetweenPoll(minimumDelayBetweenPoll);
		return pollingInfo();
	}
	
	@CliCommand(value="poll now", help="Perform a manual file system poll")
	public String poll() {
		long originalSetting = processManager.getMinimumDelayBetweenPoll();
		try {
			processManager.setMinimumDelayBetweenPoll(1);
			processManager.run();
		} finally {
			// switch on manual polling again
			processManager.setMinimumDelayBetweenPoll(originalSetting);
		}
		return "Manual poll completed";
	}
	
}
