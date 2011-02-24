package org.springframework.roo.addon.git;

import java.io.File;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.eclipse.jgit.lib.Constants;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.file.monitor.event.FileEvent;
import org.springframework.roo.file.monitor.event.FileEventListener;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
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
public class GitShellEventListener implements ShellStatusListener, FileEventListener {
	@Reference private GitOperations gitOperations;
	@Reference private Shell shell;
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	@Reference private ProjectOperations projectOperations;
	private boolean isDirty = false;

	protected void activate(ComponentContext context) {
		shell.addShellStatusListener(this);
	}
	
	protected void deactivate(ComponentContext context) {
		shell.removeShellStatusListener(this);
	}

	public void onShellStatusChange(ShellStatus oldStatus, ShellStatus newStatus) {
		if (isDirty && isGitEnabled() && newStatus.getStatus().equals(Status.EXECUTION_SUCCESS)) {
			gitOperations.commitAllChanges(newStatus.getMessage());
			isDirty = false;
		}
	}

	public void onFileEvent(FileEvent fileEvent) {
		if (projectOperations.isProjectAvailable()) {
			if (!matchesIgnore(fileEvent.getFileDetails())) {
				isDirty = true;
			}
		}
	}
	
	private boolean matchesIgnore(FileDetails details) {
		String projectRoot = pathResolver.getIdentifier(Path.ROOT, ".");
		for (String exclusion: gitOperations.getExclusions()) {
			if (details.matchesAntPath(projectRoot) || details.matchesAntPath(projectRoot + File.separator + exclusion)) { 
				return true;
			}
		}
		return false;
	}
	
	private boolean isGitEnabled() {
		return fileManager.exists(pathResolver.getIdentifier(Path.ROOT, Constants.DOT_GIT));
	}
}
