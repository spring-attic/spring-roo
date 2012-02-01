package org.springframework.roo.addon.oscommands;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Command type to allow execution of native OS commands from the Spring Roo
 * shell.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Component
@Service
public class OsCommands implements CommandMarker {

    private static final Logger LOGGER = HandlerUtils
            .getLogger(OsCommands.class);

    @Reference private OsOperations osOperations;

    @CliCommand(value = "!", help = "Allows execution of operating system (OS) commands.")
    public void command(
            @CliOption(key = { "", "command" }, mandatory = false, specifiedDefaultValue = "", unspecifiedDefaultValue = "", help = "The command to execute") final String command) {

        if (StringUtils.isNotBlank(command)) {
            try {
                osOperations.executeCommand(command);
            }
            catch (final IOException e) {
                LOGGER.severe("Unable to execute command " + command + " ["
                        + e.getMessage() + "]");
            }
        }
    }

    @CliAvailabilityIndicator("!")
    public boolean isCommandAvailable() {
        return true; // This command is always available!
    }
}