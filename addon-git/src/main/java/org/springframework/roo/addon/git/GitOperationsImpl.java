package org.springframework.roo.addon.git;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

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
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;

/**
 * Operations for Git addon.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component 
@Service 
public class GitOperationsImpl implements GitOperations {
	private static final Logger logger = Logger.getLogger(GitOperationsImpl.class.getName());
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	private PersonIdent person;

	public boolean isGitCommandAvailable() {
		return hasDotGit();
	}

	public boolean isSetupCommandAvailable() {
		return !hasDotGit();
	}
	
	public boolean isAutomaticCommit() {
		return getRepository().getConfig().getBoolean("roo", "automaticCommit", true);
	}

	public void commitAllChanges(String message) {
		Repository repository = getRepository();
		try {
			Git git = new Git(repository);
			git.add().addFilepattern(".").call();
			Status status = git.status().call();
			if (status.getChanged().size() > 0 || status.getAdded().size() > 0 || status.getModified().size() > 0 || status.getRemoved().size() > 0) {
				RevCommit rev = git.commit().setAll(true).setCommitter(person).setAuthor(person).setMessage(message).call();
				logger.info("Git commit " + rev.getTree().getId().name() + " [" + message + "]");
			}
		} catch (Exception e) {
			throw new IllegalStateException("Could not commit changes to local Git repository", e);
		}
	}

	public void push() {
		Git git = new Git(getRepository());
		try {
			for (PushResult result : git.push().setPushAll().call()) {
				logger.info(result.getMessages());
			}
		} catch (Exception e) {
			throw new IllegalStateException("Unable to perform push operation ", e);
		}
	}

	public void log(int maxHistory) {
		Repository repository = getRepository();
		Git git = new Git(repository);
		try {
			int counter = 0;
			logger.warning("---------- Start Git log ----------");
			for (RevCommit commit : git.log().call()) {
				logger.info("commit id: " + commit.getName());
				logger.info("message:   " + commit.getFullMessage());
				logger.info("");
				if (++counter >= maxHistory)
					break;
			}
			logger.warning("---------- End Git log ----------");
		} catch (Exception e) {
			throw new IllegalStateException("Could not parse git log", e);
		}
	}

	public void reset(int noOfCommitsToRevert, String message) {
		Repository repository = getRepository();
		RevCommit commit = findCommit(Constants.HEAD + "~" + noOfCommitsToRevert, repository);
		if (commit == null) {
			return;
		}
		
		try {
			Git git = new Git(repository);
			git.reset().setRef(commit.getName()).setMode(ResetType.HARD).call();
			// Commit changes
			commitAllChanges(message);
			logger.info("Reset of last " + (noOfCommitsToRevert + 1) + " successful.");
		} catch (Exception e) {
			throw new IllegalStateException("Reset did not succeed.", e);
		}
	}
	
	public void revertLastCommit(String message) {
		revertCommit(Constants.HEAD + "~0", message);
	}

	public void revertCommit(String revstr, String message) {
		Repository repository = getRepository();
		RevCommit commit = findCommit(revstr, repository);
		if (commit == null) {
			return;
		}
		
		try {
			Git git = new Git(repository);
			git.revert().include(commit).call();
			// Commit changes
			commitAllChanges(message);
			logger.info("Revert of commit " + revstr + " successful.");
		} catch (Exception e) {
			throw new IllegalStateException("Revert of commit " + revstr + " did not succeed.", e);
		}
	}
	
	private RevCommit findCommit(String revstr, Repository repository) {
		RevWalk walk = new RevWalk(repository);
		RevCommit commit = null;
		try {
			commit = walk.parseCommit(repository.resolve(revstr));
		} catch (MissingObjectException e1) {
			logger.warning("Could not find commit with id: " + revstr);
		} catch (IncorrectObjectTypeException e1) {
			logger.warning("The provided rev is not a commit: " + revstr);
		} catch (Exception ignore) {} finally {
			walk.release();
		}
		return commit;
	}

	public void setConfig(String category, String key, String value) {
		Repository repository = getRepository();
		try {
			repository.getConfig().setString(category, null, key, value);
			repository.getConfig().save();
		} catch (IOException ex) {
			throw new IllegalStateException("Could not initialize Git repository", ex);
		}
	}

	public void setup() {
		if (hasDotGit()) {
			logger.info("Git is already configured");
			return;
		}
		if (person == null) {
			person = new PersonIdent("Roo Git Add-On", "s2-roo@vmware.com");
		}
		try {
			Repository repository = new FileRepositoryBuilder().readEnvironment().setGitDir(new File(".", Constants.DOT_GIT)).build();
			repository.create();
		} catch (Exception e) {
			throw new IllegalStateException("Could not initialize Git repository", e);
		}
		setConfig("user", "name", person.getName());
		setConfig("user", "email", person.getEmailAddress());

		setConfig("remote \"origin\"", "fetch", "+refs/heads/*:refs/remotes/origin/*");
		setConfig("branch \"master\"", "remote", "origin");
		setConfig("branch \"master\"", "merge", "refs/heads/master");
	
		String gitIgnore = pathResolver.getIdentifier(Path.ROOT, Constants.GITIGNORE_FILENAME);
		if (!fileManager.exists(gitIgnore)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "gitignore-template"), fileManager.createFile(gitIgnore).getOutputStream());
			} catch (IOException e) {
				throw new IllegalStateException("Could not install " + Constants.GITIGNORE_FILENAME + " file in project", e);
			}
		}
	}

	private Repository getRepository() {
		if (hasDotGit()) {
			try {
				return new FileRepositoryBuilder().readEnvironment().findGitDir().build();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
		throw new IllegalStateException("Git support not available");
	}

	private boolean hasDotGit() {
		return fileManager.exists(pathResolver.getIdentifier(Path.ROOT, Constants.DOT_GIT));
	}
}
