package org.springframework.roo.addon.git;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.dircache.DirCacheCheckout;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefRename;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
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
	private Set<String> exclusions = new HashSet<String>();

	public boolean isGitCommandAvailable() {
		return hasDotGit();
	}

	public boolean isSetupCommandAvailable() {
		return !hasDotGit();
	}

	public void commitAllChanges(String message) {
		Repository repository = getRepository();
		try {
			Git git = new Git(repository);
			git.add().addFilepattern(".").call();
			RevCommit rev = git.commit().setAll(true).setCommitter(person).setAuthor(person).setMessage(message).call();
			logger.info("Git commit " + rev.getTree().getId().name() + " [" + message + "]");
		} catch (Exception e) {
			throw new IllegalStateException("Could not commit changes to local Git repository", e);
		}
	}

	public void push() {
		// Transport transport = Transport.open(repository,
		// repository.getConfig().getString("remote \"origin\"", null, "url"));
		// final org.eclipse.jgit.transport.PushResult pr = transport.push(null,
		// );
	}

	public void log(int maxHistory) {
		Repository repository = getRepository();
		Git git = new Git(repository);
		try {
			int counter = 0;
			logger.info("---------- Start Git log ----------");
			for (RevCommit commit : git.log().call()) {
				logger.info("commit id: " + commit.getName());
				logger.info("message:   " + commit.getFullMessage());
				logger.info("");
				if (++counter >= maxHistory)
					break;
			}
			logger.info("---------- End Git log ----------");
		} catch (Exception e) {
			throw new IllegalStateException("Could not parse git log", e);
		}
	}

	public void revertCommit(int noOfCommitsToRevert, String message) {
		revertCommit(Constants.HEAD + "~" + noOfCommitsToRevert, message);
	}

	public void revertCommit(String revstr, String message) {
		Repository repository = getRepository();
		RevWalk walk = new RevWalk(repository);
		RevCommit commit = null;
		try {
			commit = walk.parseCommit(repository.resolve(revstr));
		} catch (MissingObjectException e1) {
			logger.warning("Could not find commit with id: " + revstr);
		} catch (IncorrectObjectTypeException e1) {
			logger.warning("The provided rev does is not a commit: " + revstr);
		} catch (Exception ignore) {} finally {
			walk.release();
		}

		if (commit == null) {
			return;
		}

		try {
			// Create a tmp branch with commits up to the rev
			createBranch(repository, commit, "refs/heads/tmp");

			// Rename master branch to backup-
			RefRename renameMaster = repository.renameRef("refs/heads/master", "refs/heads/backup-" + commit.getId().abbreviate(5).name());
			renameMaster.rename();

			// Rename tmp branch to master
			RefRename renameTmp = repository.renameRef("refs/heads/tmp", "refs/heads/master");
			renameTmp.rename();

			// Make sure we are on master
			checkoutBranch(repository, "refs/heads/master");
			System.out.println(repository.getFullBranch());

			// Commit changes
			commitAllChanges(message);
		} catch (Exception e) {
			throw new IllegalStateException("Revert of commit " + revstr + " did not succeed.", e);
		}
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

		loadGitIgnore();
	}

	public Set<String> getExclusions() {
		return exclusions;
	}

	private Repository getRepository() {
		if (hasDotGit()) {
			try {
				return new FileRepositoryBuilder().readEnvironment().findGitDir().build();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		} else {
			throw new IllegalStateException("Git support not available");
		}
	}

	private boolean hasDotGit() {
		return fileManager.exists(pathResolver.getIdentifier(Path.ROOT, Constants.DOT_GIT));
	}

	private void createBranch(Repository repository, ObjectId objectId, String branchName) throws IOException {
		RefUpdate updateRef = repository.updateRef(branchName);
		updateRef.setNewObjectId(objectId);
		updateRef.forceUpdate();
		updateRef.update();
	}

	private boolean checkoutBranch(Repository repository, String branchName) throws IllegalStateException, IOException {
		RevWalk walk = new RevWalk(repository);
		RevCommit head = walk.parseCommit(repository.resolve(Constants.HEAD));
		RevCommit branch = walk.parseCommit(repository.resolve(branchName));
		DirCacheCheckout dco = new DirCacheCheckout(repository, head.getTree().getId(), repository.lockDirCache(), branch.getTree().getId());
		dco.setFailOnConflict(true);
		boolean success = dco.checkout();
		walk.release();
		// Update the HEAD
		RefUpdate refUpdate = repository.updateRef(Constants.HEAD);
		refUpdate.link(branchName);
		return success;
	}

	private void loadGitIgnore() {
		exclusions.clear();
		String gitIgnore = pathResolver.getIdentifier(Path.ROOT, Constants.GITIGNORE_FILENAME);
		if (fileManager.exists(gitIgnore)) {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(gitIgnore));
				String line;
				while ((line = reader.readLine()) != null) {
					exclusions.add(line);
				}
			} catch (Exception ignored) {
			} finally {
				try {
					if (reader != null) reader.close();
				} catch (IOException ignored) {}
			}
		}
	}
}
