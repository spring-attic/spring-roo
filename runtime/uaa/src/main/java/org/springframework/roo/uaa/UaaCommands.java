package org.springframework.roo.uaa;

import static org.apache.commons.io.IOUtils.LINE_SEPARATOR;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.converters.StaticFieldConverter;
import org.springframework.roo.support.util.MessageDisplayUtils;
import org.springframework.uaa.client.UaaService;
import org.springframework.uaa.client.protobuf.UaaClient.Privacy.PrivacyLevel;

/**
 * Provides shell commands for end user interaction with the Spring User Agent
 * Analysis (UAA) system.
 * 
 * @author Ben Alex
 * @since 1.1.1
 */
@Service
@Component
public class UaaCommands implements CommandMarker {

    @Reference private StaticFieldConverter staticFieldConverter;
    @Reference private UaaRegistrationService uaaRegistrationService;
    @Reference private UaaService uaaService;

    @CliCommand(value = "download accept terms of use", help = "Accepts the Spring User Agent Analysis (UAA) Terms of Use")
    public void acceptTou() {
        uaaService.setPrivacyLevel(PrivacyLevel.ENABLE_UAA);
        uaaRegistrationService.flushIfPossible();
        MessageDisplayUtils.displayFile("accepted_tou.txt", UaaCommands.class);
    }

    protected void activate(final ComponentContext context) {
        staticFieldConverter.add(PrivacyLevel.class);
    }

    protected void deactivate(final ComponentContext context) {
        staticFieldConverter.remove(PrivacyLevel.class);
    }

    @CliCommand(value = "download privacy level", help = "Changes the Spring User Agent Analysis (UAA) privacy level")
    public String privacyLevel(
            @CliOption(key = "privacyLevel", mandatory = true, help = "The new UAA privacy level to use") final PrivacyLevel privacyLevel) {
        uaaService.setPrivacyLevel(privacyLevel);
        return "UAA privacy level updated "
                + uaaService.getPrivacyLevelLastChanged()
                + " (use 'download view' to view the new data)";
    }

    @CliCommand(value = "download reject terms of use", help = "Rejects the Spring User Agent Analysis (UAA) Terms of Use")
    public void rejectTou() {
        uaaService.setPrivacyLevel(PrivacyLevel.DECLINE_TOU);
        MessageDisplayUtils.displayFile("declined_tou.txt", UaaCommands.class);
    }

    @CliCommand(value = "download status", help = "Provides a summary of the Spring User Agent Analysis (UAA) status and commands")
    public void uaaStatus() {
        final PrivacyLevel privacyLevel = uaaService.getPrivacyLevel();
        if (privacyLevel == PrivacyLevel.DECLINE_TOU) {
            MessageDisplayUtils.displayFile("status_declined.txt",
                    UaaCommands.class);
        }
        else if (privacyLevel == PrivacyLevel.UNDECIDED_TOU) {
            MessageDisplayUtils.displayFile("status_undecided.txt",
                    UaaCommands.class);
        }
        else {
            MessageDisplayUtils.displayFile("status_accepted.txt",
                    UaaCommands.class);
        }
    }

    @CliCommand(value = "download view", help = "Displays the Spring User Agent Analysis (UAA) header content in plain text")
    public String view(
            @CliOption(key = "file", mandatory = false, help = "The file to save the UAA JSON content to") final File file) {
        final String readablePayload = uaaService.getReadablePayload();

        final StringBuilder sb = new StringBuilder();
        sb.append("Output for privacy level ")
                .append(uaaService.getPrivacyLevel()).append(" (last changed ")
                .append(uaaService.getPrivacyLevelLastChanged()).append(")")
                .append(LINE_SEPARATOR).append(LINE_SEPARATOR);

        sb.append(readablePayload);
        sb.append(LINE_SEPARATOR).append(LINE_SEPARATOR);

        if (file != null) {
            try {
                FileUtils.write(file, sb.toString());
            }
            catch (final IOException ioe) {
                throw new IllegalStateException(ioe);
            }
        }

        return sb.toString();
    }
}
