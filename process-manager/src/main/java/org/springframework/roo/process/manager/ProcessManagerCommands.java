package org.springframework.roo.process.manager;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.Shell;

/**
 * Commands related to file system monitoring and process management.
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Component
@Service
public class ProcessManagerCommands implements CommandMarker {

    @Reference private ProcessManager processManager;
    @Reference private Shell shell;

    protected void activate(final ComponentContext context) {
        if (!"false".equals(System.getProperty("developmentMode", "false")
                .toLowerCase())) {
            developmentMode(true);
        }
    }

    @CliCommand(value = "development mode", help = "Switches the system into development mode (greater diagnostic information)")
    public String developmentMode(
            @CliOption(key = { "", "enabled" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "true", help = "Activates development mode") final boolean enabled) {
        processManager.setDevelopmentMode(enabled);
        shell.setDevelopmentMode(enabled);
        return "Development mode set to " + enabled;
    }

    @CliCommand(value = "poll now", help = "Perform a manual file system poll")
    public String poll() {
        final long originalSetting = processManager
                .getMinimumDelayBetweenPoll();
        try {
            processManager.setMinimumDelayBetweenPoll(1);
            processManager.timerBasedPoll();
        }
        finally {
            // Switch on manual polling again
            processManager.setMinimumDelayBetweenPoll(originalSetting);
        }
        return "Manual poll completed";
    }

    @CliCommand(value = "poll status", help = "Display file system polling information")
    public String pollingInfo() {
        final StringBuilder sb = new StringBuilder("File system polling ");
        final long duration = processManager.getLastPollDuration();
        if (duration == 0) {
            sb.append("never executed; ");
        }
        else {
            sb.append("last took ").append(duration).append(" ms; ");
        }
        final long minimum = processManager.getMinimumDelayBetweenPoll();
        if (minimum == 0) {
            sb.append("automatic polling is disabled");
        }
        else if (minimum < 0) {
            sb.append("auto-scaled polling is enabled");
        }
        else {
            sb.append("polling frequency has a minimum interval of ")
                    .append(minimum).append(" ms");
        }
        return sb.toString();
    }

    @CliCommand(value = "poll speed", help = "Changes the file system polling speed")
    public String pollingSpeed(
            @CliOption(key = { "", "ms" }, mandatory = true, help = "The number of milliseconds between each poll") final long minimumDelayBetweenPoll) {
        processManager.setMinimumDelayBetweenPoll(minimumDelayBetweenPoll);
        return pollingInfo();
    }
}
