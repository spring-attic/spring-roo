package org.springframework.roo.addon.git;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.PushResult;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.support.util.FileUtils;

/**
 * Operations for Git addon.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component
@Service
public class GitOperationsImpl implements GitOperations {

    private static final Logger LOGGER = Logger
            .getLogger(GitOperationsImpl.class.getName());
    private static final String REVISION_STRING_DELIMITER = "~";

    @Reference private FileManager fileManager;
    @Reference private PathResolver pathResolver;

    private PersonIdent person;

    public void commitAllChanges(final String message) {
        final Repository repository = getRepository();
        try {
            final Git git = new Git(repository);
            git.add().addFilepattern(".").call();
            final Status status = git.status().call();
            if (status.getChanged().size() > 0 || status.getAdded().size() > 0
                    || status.getModified().size() > 0
                    || status.getRemoved().size() > 0) {
                final RevCommit rev = git.commit().setAll(true)
                        .setCommitter(person).setAuthor(person)
                        .setMessage(message).call();
                LOGGER.info("Git commit " + rev.getName() + " [" + message
                        + "]");
            }
        }
        catch (final Exception e) {
            throw new IllegalStateException(
                    "Could not commit changes to local Git repository", e);
        }
    }

    private RevCommit findCommit(final String revstr,
            final Repository repository) {
        final RevWalk walk = new RevWalk(repository);
        RevCommit commit = null;
        try {
            commit = walk.parseCommit(repository.resolve(revstr));
        }
        catch (final MissingObjectException e1) {
            LOGGER.warning("Could not find commit with id: " + revstr);
        }
        catch (final IncorrectObjectTypeException e1) {
            LOGGER.warning("The provided rev is not a commit: " + revstr);
        }
        catch (final Exception ignore) {
        }
        finally {
            walk.release();
        }
        return commit;
    }

    private Repository getRepository() {
        if (hasDotGit()) {
            try {
                return new FileRepositoryBuilder().readEnvironment()
                        .findGitDir().build();
            }
            catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalStateException("Git support not available");
    }

    private boolean hasDotGit() {
        return fileManager.exists(pathResolver.getFocusedIdentifier(Path.ROOT,
                Constants.DOT_GIT));
    }

    public boolean isAutomaticCommit() {
        return getRepository().getConfig().getBoolean("roo", "automaticCommit",
                true);
    }

    public boolean isGitCommandAvailable() {
        return hasDotGit();
    }

    public boolean isGitInstallationPossible() {
        return !hasDotGit();
    }

    public void log(final int maxHistory) {
        final Repository repository = getRepository();
        final Git git = new Git(repository);
        try {
            int counter = 0;
            LOGGER.warning("---------- Start Git log ----------");
            for (final RevCommit commit : git.log().call()) {
                LOGGER.info("commit id: " + commit.getName());
                LOGGER.info("message:   " + commit.getFullMessage());
                LOGGER.info("");
                if (++counter >= maxHistory) {
                    break;
                }
            }
            LOGGER.warning("---------- End Git log ----------");
        }
        catch (final Exception e) {
            throw new IllegalStateException("Could not parse git log", e);
        }
    }

    public void push() {
        final Git git = new Git(getRepository());
        try {
            for (final PushResult result : git.push().setPushAll().call()) {
                LOGGER.info(result.getMessages());
            }
        }
        catch (final Exception e) {
            throw new IllegalStateException(
                    "Unable to perform push operation ", e);
        }
    }

    public void reset(final int noOfCommitsToRevert, final String message) {
        final Repository repository = getRepository();
        final RevCommit commit = findCommit(Constants.HEAD
                + REVISION_STRING_DELIMITER + noOfCommitsToRevert, repository);
        if (commit == null) {
            return;
        }

        try {
            final Git git = new Git(repository);
            git.reset().setRef(commit.getName()).setMode(ResetType.HARD).call();
            // Commit changes
            commitAllChanges(message);
            LOGGER.info("Reset of last " + (noOfCommitsToRevert + 1)
                    + " successful.");
        }
        catch (final Exception e) {
            throw new IllegalStateException("Reset did not succeed.", e);
        }
    }

    public void revertCommit(final String revstr, final String message) {
        final Repository repository = getRepository();
        final RevCommit commit = findCommit(revstr, repository);
        if (commit == null) {
            return;
        }

        try {
            final Git git = new Git(repository);
            git.revert().include(commit).call();
            // Commit changes
            commitAllChanges(message);
            LOGGER.info("Revert of commit " + revstr + " successful.");
        }
        catch (final Exception e) {
            throw new IllegalStateException("Revert of commit " + revstr
                    + " did not succeed.", e);
        }
    }

    public void revertLastCommit(final String message) {
        revertCommit(Constants.HEAD + REVISION_STRING_DELIMITER + "0", message);
    }

    public void setConfig(final String category, final String key,
            final String value) {
        final Repository repository = getRepository();
        try {
            repository.getConfig().setString(category, null, key, value);
            repository.getConfig().save();
        }
        catch (final IOException ex) {
            throw new IllegalStateException(
                    "Could not initialize Git repository", ex);
        }
    }

    public void setup() {
        if (hasDotGit()) {
            LOGGER.info("Git is already configured");
            return;
        }
        if (person == null) {
            person = new PersonIdent("Roo Git Add-On", "s2-roo@vmware.com");
        }
        try {
            final Repository repository = new FileRepositoryBuilder()
                    .readEnvironment()
                    .setGitDir(new File(".", Constants.DOT_GIT)).build();
            repository.create();
        }
        catch (final Exception e) {
            throw new IllegalStateException(
                    "Could not initialize Git repository", e);
        }
        setConfig("user", "name", person.getName());
        setConfig("user", "email", person.getEmailAddress());

        setConfig("remote \"origin\"", "fetch",
                "+refs/heads/*:refs/remotes/origin/*");
        setConfig("branch \"master\"", "remote", "origin");
        setConfig("branch \"master\"", "merge", "refs/heads/master");

        final String gitIgnore = pathResolver.getFocusedIdentifier(Path.ROOT,
                Constants.GITIGNORE_FILENAME);

        if (!fileManager.exists(gitIgnore)) {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                inputStream = FileUtils.getInputStream(getClass(),
                        "gitignore-template");
                outputStream = fileManager.createFile(gitIgnore)
                        .getOutputStream();
                IOUtils.copy(inputStream, outputStream);
            }
            catch (final IOException e) {
                throw new IllegalStateException("Could not install "
                        + Constants.GITIGNORE_FILENAME + " file in project", e);
            }
            finally {
                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(outputStream);
            }
        }
    }
}
