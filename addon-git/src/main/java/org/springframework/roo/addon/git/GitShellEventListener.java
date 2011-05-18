package org.springframework.roo.addon.git;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.eclipse.jgit.lib.Constants;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
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
@Component(immediate = true)
@Service
public class GitShellEventListener implements ShellStatusListener {
	@Reference private GitOperations gitOperations;
	@Reference private Shell shell;
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;

	protected void activate(ComponentContext context) {
		shell.addShellStatusListener(this);
	}
	
	protected void deactivate(ComponentContext context) {
		shell.removeShellStatusListener(this);
	}

	public void onShellStatusChange(ShellStatus oldStatus, ShellStatus newStatus) {
		if (newStatus.getStatus().equals(Status.EXECUTION_SUCCESS) && isGitEnabled() && gitOperations.isAutomaticCommit()) {
			gitOperations.commitAllChanges(newStatus.getMessage());
		}
	}
	
	private boolean isGitEnabled() {
		return fileManager.exists(pathResolver.getIdentifier(Path.ROOT, Constants.DOT_GIT));
	}
}
