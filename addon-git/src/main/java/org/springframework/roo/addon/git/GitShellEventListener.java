package org.springframework.roo.addon.git;

import java.io.File;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.eclipse.jgit.lib.Constants;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.shell.event.ShellStatus;
import org.springframework.roo.shell.event.ShellStatus.Status;
import org.springframework.roo.shell.event.ShellStatusListener;

/**
 * Listener for Shell events to support automatic Git repository commits.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component
@Service
public class GitShellEventListener implements ShellStatusListener {

    @Reference private GitOperations gitOperations;
    @Reference private PathResolver pathResolver;
    @Reference private Shell shell;

    protected void activate(final ComponentContext context) {
        shell.addShellStatusListener(this);
    }

    protected void deactivate(final ComponentContext context) {
        shell.removeShellStatusListener(this);
    }

    private boolean isGitEnabled() {
        return new File(pathResolver.getRoot(), Constants.DOT_GIT)
                .isDirectory();
    }

    public void onShellStatusChange(final ShellStatus oldStatus,
            final ShellStatus newStatus) {
        if (newStatus.getStatus().equals(Status.EXECUTION_SUCCESS)
                && isGitEnabled() && gitOperations.isAutomaticCommit()) {
            gitOperations.commitAllChanges(newStatus.getMessage());
        }
    }
}
