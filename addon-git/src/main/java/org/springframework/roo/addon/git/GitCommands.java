package org.springframework.roo.addon.git;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Commands for addon-git.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component 
@Service 
public class GitCommands implements CommandMarker {
	@Reference private GitOperations revisionControl;

	@CliAvailabilityIndicator({ "git config", "git commit all", "git revert last", "git revert commit", "git log" }) 
	public boolean isCommandAvailable() {
		return revisionControl.isGitCommandAvailable();
	}

	@CliAvailabilityIndicator("git setup") 
	public boolean isSetupCommandAvailable() {
		return revisionControl.isSetupCommandAvailable();
	}

	@CliCommand(value = "git setup", help = "Setup Git revision control") 
	public void config() {
		revisionControl.setup();
	}

	@CliCommand(value = "git config", help = "Git revision control configuration (.git/config)") 
	public void config(
		@CliOption(key = { "userName" }, mandatory = false, help = "The user name") String userName, 
		@CliOption(key = { "email" }, mandatory = false, help = "The user email") String email, 
		@CliOption(key = { "repoUrl" }, mandatory = false, help = "The URL of the remote repository") String repoUrl, 
		@CliOption(key = { "colorCoding" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Enable color coding of commands in OS shell") boolean color) {

		if (userName != null && userName.length() > 0) {
			revisionControl.setConfig("user", "name", userName);
		}
		if (email != null && email.length() > 0) {
			revisionControl.setConfig("user", "email", email);
		}
		if (repoUrl != null && repoUrl.length() > 0) {
			revisionControl.setConfig("remote \"origin\"", "url", repoUrl);
		}
		if (color) {
			revisionControl.setConfig("color", "diff", "auto");
			revisionControl.setConfig("color", "branch", "auto");
			revisionControl.setConfig("color", "status", "auto");
		}
	}

	@CliCommand(value = "git commit all", help = "Trigger a commit manually for the project") 
	public void config(
		@CliOption(key = { "message" }, mandatory = true, help = "The commit message") String message) {
		revisionControl.commitAllChanges(message);
	}

	@CliCommand(value = "git revert last", help = "Revert (last x) commit(s)") 
	public void revertLast(
		@CliOption(key = { "commitCount" }, mandatory = false, help = "Number of commits to revert") Integer history, 
		@CliOption(key = { "message" }, mandatory = true, help = "The commit message") String message) {
		revisionControl.revertCommit(history == null ? 1 : history, message);
	}

	@CliCommand(value = "git revert commit", help = "Roll project back to a specific commit") 
	public void revertCommit(
		@CliOption(key = { "revString" }, mandatory = true, help = "Commit id") String revstr, 
		@CliOption(key = { "message" }, mandatory = true, help = "The commit message") String message) {
		revisionControl.revertCommit(revstr, message);
	}

	@CliCommand(value = "git log", help = "Commit log") 
	public void log(@CliOption(key = { "maxMessages" }, mandatory = false, help = "Number of commit messages to display") Integer count) {
		revisionControl.log(count == null ? Integer.MAX_VALUE : count);
	}
}
