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

    @Reference private GitOperations gitOperations;

    @CliCommand(value = "git setup", help = "Setup Git revision control")
    public void config() {
        gitOperations.setup();
    }

    @CliCommand(value = "git commit all", help = "Trigger a commit manually for the project")
    public void config(
            @CliOption(key = { "message" }, mandatory = true, help = "The commit message") final String message) {

        gitOperations.commitAllChanges(message);
    }

    @CliCommand(value = "git config", help = "Git revision control configuration (.git/config)")
    public void config(
            @CliOption(key = { "userName" }, mandatory = false, help = "The user name") final String userName,
            @CliOption(key = { "email" }, mandatory = false, help = "The user email") final String email,
            @CliOption(key = { "repoUrl" }, mandatory = false, help = "The URL of the remote repository") final String repoUrl,
            @CliOption(key = { "colorCoding" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Enable color coding of commands in OS shell") final boolean color,
            @CliOption(key = { "automaticCommit" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "true", help = "Enable automatic commit after successful execution of Roo shell command") final Boolean automaticCommit) {

        if (userName != null && userName.length() > 0) {
            gitOperations.setConfig("user", "name", userName);
        }
        if (email != null && email.length() > 0) {
            gitOperations.setConfig("user", "email", email);
        }
        if (repoUrl != null && repoUrl.length() > 0) {
            gitOperations.setConfig("remote \"origin\"", "url", repoUrl);
        }
        if (color) {
            gitOperations.setConfig("color", "diff", "auto");
            gitOperations.setConfig("color", "branch", "auto");
            gitOperations.setConfig("color", "status", "auto");
        }
        gitOperations.setConfig("roo", "automaticCommit",
                automaticCommit.toString());
    }

    @CliAvailabilityIndicator({ "git config", "git commit all",
            "git revert last", "git revert commit", "git log", "git push",
            "git reset" })
    public boolean isCommandAvailable() {
        return gitOperations.isGitCommandAvailable();
    }

    @CliAvailabilityIndicator("git setup")
    public boolean isGitSetupAvailable() {
        return gitOperations.isGitInstallationPossible();
    }

    @CliCommand(value = "git log", help = "Commit log")
    public void log(
            @CliOption(key = { "maxMessages" }, mandatory = false, help = "Number of commit messages to display") final Integer count) {

        gitOperations.log(count == null ? Integer.MAX_VALUE : count);
    }

    @CliCommand(value = "git push", help = "Roll project back to a specific commit")
    public void push() {
        gitOperations.push();
    }

    @CliCommand(value = "git reset", help = "Reset (hard) last (x) commit(s)")
    public void resetLast(
            @CliOption(key = { "commitCount" }, mandatory = false, help = "Number of commits to reset") final Integer history,
            @CliOption(key = { "message" }, mandatory = true, help = "The commit message") final String message) {

        gitOperations.reset(history == null ? 0 : history, message);
    }

    @CliCommand(value = "git revert commit", help = "Roll project back to a specific commit")
    public void revertCommit(
            @CliOption(key = { "revString" }, mandatory = true, help = "Commit id") final String revstr,
            @CliOption(key = { "message" }, mandatory = true, help = "The commit message") final String message) {

        gitOperations.revertCommit(revstr, message);
    }

    @CliCommand(value = "git revert last", help = "Revert last commit")
    public void revertLast(
            @CliOption(key = { "message" }, mandatory = true, help = "The commit message") final String message) {
        gitOperations.revertLastCommit(message);
    }
}
