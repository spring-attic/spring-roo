package org.springframework.roo.uaa;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.shell.event.ShellStatus;
import org.springframework.roo.shell.event.ShellStatus.Status;
import org.springframework.roo.shell.event.ShellStatusListener;
import org.springframework.roo.support.util.MessageDisplayUtils;
import org.springframework.uaa.client.UaaService;
import org.springframework.uaa.client.protobuf.UaaClient.Privacy.PrivacyLevel;

/**
 * Provides a startup-time reminder of the 'uaa status' command if the user
 * hasn't indicated a UAA Terms of Use acceptance or rejection.
 * <p>
 * This class is separate from the other {@link ShellStatusListener} in the UAA
 * module due to of lifecycle timing reasons. It needs minimal dependencies on
 * other SCR components.
 * 
 * @author Ben Alex
 * @since 1.1.1
 */
@Service
@Component(immediate = true)
public class UaaShellStatusListener implements ShellStatusListener {

    @Reference Shell shell;
    private boolean startupMessageConsidered = false;
    @Reference UaaService uaaService;

    protected void activate(final ComponentContext componentContext) {
        shell.addShellStatusListener(this);
        final String originalThreadName = Thread.currentThread().getName();
        try {
            // Preventing thread name appearing on JLine console
            Thread.currentThread().setName("");
            onShellStatusChange(null, shell.getShellStatus());
        }
        finally {
            Thread.currentThread().setName(originalThreadName);
        }
    }

    protected void deactivate(final ComponentContext componentContext) {
        shell.removeShellStatusListener(this);
    }

    public void onShellStatusChange(final ShellStatus oldStatus,
            final ShellStatus newStatus) {
        if (!startupMessageConsidered
                && newStatus.getStatus() == Status.USER_INPUT) {
            startupMessageConsidered = true;
            if (uaaService.getPrivacyLevel() == PrivacyLevel.UNDECIDED_TOU) {
                // NB: The first line of the text file must contain spaces to
                // overwrite the roo> prompt on the current line
                MessageDisplayUtils.displayFile("startup_undecided.txt",
                        ShellListeningUaaRegistrationFacility.class, true);
            }
        }
    }
}
