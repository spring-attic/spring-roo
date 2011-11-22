package org.springframework.roo.addon.oscommands;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.StringUtils;

/**
 * Command type to allow execution of native OS commands from the Spring Roo shell.
 *
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Component
@Service
public class OsCommands implements CommandMarker {

	// Constants
	private static final Logger LOGGER = HandlerUtils.getLogger(OsCommands.class);

	// Fields
	@Reference private OsOperations osOperations;

	@CliAvailabilityIndicator("!")
	public boolean isCommandAvailable() {
		return true; // This command is always available!
	}

	@CliCommand(value = "!", help = "Allows execution of operating system (OS) commands.")
	public void command(
		@CliOption(key = { "", "command" }, mandatory = false, specifiedDefaultValue = "*", unspecifiedDefaultValue = "*", help = "The command to execute") final String command) {

		if (StringUtils.hasText(command)) {
			try {
				osOperations.executeCommand(command);
			} catch (IOException e) {
				LOGGER.severe("Unable to execute command " + command + " [" + e.getMessage() + "]");
			}
		}
	}
}