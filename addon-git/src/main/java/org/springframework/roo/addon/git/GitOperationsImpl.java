package org.springframework.roo.addon.git;

import java.io.File;
import java.io.IOException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.Commit;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectWriter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.lib.WorkDirCheckout;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.Assert;
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

	private Repository repository;

	private PersonIdent person;
	
	@Reference private PathResolver pathResolver;
	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;
	
	public boolean isGitCommandAvailable() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) != null && fileManager.exists(pathResolver.getIdentifier(Path.ROOT, ".git"));
	}
	
	public boolean isSetupCommandAvailable() {
		return !fileManager.exists(pathResolver.getIdentifier(Path.ROOT, ".git"));
	}

	public GitCommandResult commitAllChanges(String message) {
		try {
			Commit commit = getCommitInstance(Constants.HEAD);
			commit.setMessage(message);
			commit.setTree(getDir());
			commit.commit();

			ObjectId id = commit.getCommitId();
			RefUpdate ru = repository.updateRef(Constants.HEAD);
			ru.setNewObjectId(id);
			ru.setRefLogIdent(person);
			ru.setRefLogMessage(message, true);
			Result result = ru.update();

			return new GitCommandResult(id.name(), result.toString());
		} catch (Exception e) {
			throw new IllegalStateException("Error committing to git repository", e);
		}
	}

	public void push() {
//		Transport transport = Transport.open(repository, repository.getConfig().getString("remote \"origin\"", null, "url"));
//		final org.eclipse.jgit.transport.PushResult pr = transport.push(null, );

	}

	public GitCommandResult revertCommit(int noOfCommitsToRevert, String message) {
		return revertCommitByRef(Constants.HEAD + "~" + noOfCommitsToRevert, message);
	}
	
	private GitCommandResult revertCommitByRef(String revstr, String message) {
		try {
			final ObjectId commitObjectId = repository.resolve(revstr);
			final Commit commit = repository.mapCommit(commitObjectId);
			final RefUpdate ru = repository.updateRef(Constants.HEAD);
			ru.setNewObjectId(commit.getCommitId());
			ru.setRefLogMessage(message, false);
			
			createBranch(repository.resolve(Constants.HEAD), "refs/heads/master_backup_" + commit.getCommitId().abbreviate(repository).name());
			
			WorkDirCheckout workDirCheckout = new WorkDirCheckout(repository, repository.getWorkDir(), repository.getIndex(), commit.getTree());
			workDirCheckout.setFailOnConflict(false);
			workDirCheckout.checkout();
		} catch (IOException e) {
			throw new IllegalStateException("Revert of commit " + revstr + " did not succeed.", e);
		}
		return null;
	}

	public void setConfig(String category, String key, String value) {
		try {
			repository.getConfig().setString(category, null, key, value);
			repository.getConfig().save();
		} catch (IOException ex) {
			throw new IllegalStateException("Could not initialize Git repository", ex);
		}
	}

	public void setup() {
		final File gitDir = new File(".", Constants.DOT_GIT);
		if (person == null) {
			person = new PersonIdent("Roo Git Add-On", "roo@bogus.com");
		}
		if (repository == null) {

			try {
				repository = new Repository(gitDir);
				repository.create();
			} catch (IOException e) {
				throw new IllegalStateException("Could not initialize Git repository", e);
			}
			setConfig("user", "name", person.getName());
			setConfig("user", "email", person.getEmailAddress());
			
			setConfig("remote \"origin\"", "fetch", "+refs/heads/*:refs/remotes/origin/*");
			setConfig("branch \"master\"", "remote", "origin");
			setConfig("branch \"master\"", "merge", "refs/heads/master");
		}
		
		String gitIgnore = pathResolver.getIdentifier(Path.ROOT, Constants.GITIGNORE_FILENAME);
		if (!fileManager.exists(gitIgnore)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "gitignore-template"), fileManager.createFile(gitIgnore).getOutputStream());
			} catch (IOException e) {
				throw new IllegalStateException("Could not install " + Constants.GITIGNORE_FILENAME + " file in project", e);
			}
		}
	}

	private Tree getDir() throws IOException {
		DirCache cache = DirCache.lock(repository);
		DirCacheBuilder builder = cache.builder();
		addToCache(repository.getWorkDir(), builder);
		builder.commit();
		return repository.mapTree(cache.writeTree(new ObjectWriter(repository)));
	}

	private void addToCache(File file, DirCacheBuilder builder) {
		Assert.notNull(file, "no file defined");
		if (file.getName().equals(Constants.DOT_GIT)) {
			return;
		}
		if (file.isDirectory()) {
			for (File _file : file.listFiles()) {
				addToCache(_file, builder);
			}
		} else {
			ObjectWriter writer = new ObjectWriter(repository);
			DirCacheEntry entry = new DirCacheEntry(file.getPath());
			entry.setFileMode(FileMode.REGULAR_FILE);
			try {
				entry.setObjectId(writer.writeBlob(file));
			} catch (Exception e) {
				throw new IllegalStateException("Error committing to git repository", e);
			}
			builder.add(entry);
		}
	}
	
	private Commit getCommitInstance(String rev) {
		try {
			ObjectId currentHead = repository.resolve(rev);

			ObjectId[] parentIds;
			if (currentHead != null) {
				parentIds = new ObjectId[] { currentHead };
			} else {
				parentIds = new ObjectId[0];
			}
			Commit commit = new Commit(repository, parentIds);
			commit.setAuthor(person);
			commit.setCommitter(person);
			return commit;
		} catch (IOException e) {
			throw new IllegalStateException("Could not resolve HEAD", e);
		}
	}
	
	private void createBranch(ObjectId objectId, String branchName) throws IOException {
		RefUpdate updateRef = repository.updateRef(branchName);
		updateRef.setNewObjectId(objectId);
		updateRef.update();
	}
	
//	private void checkoutBranch(String branchName) throws Exception  {
//		File workDir = repository.getWorkDir();
//		if (workDir != null) {
//			WorkDirCheckout workDirCheckout = new WorkDirCheckout(repository,
//					workDir, repository.mapCommit(Constants.HEAD).getTree(),
//					repository.getIndex(), repository.mapCommit(branchName).getTree());
//			workDirCheckout.setFailOnConflict(false);
//			try {
//				workDirCheckout.checkout();
//			} catch (CheckoutConflictException e) {
//				throw new IllegalStateException("Could not checkout " + branchName + " branch", e);
//			}
//		}
//
//		// update the HEAD
//		RefUpdate refUpdate = repository.updateRef(Constants.HEAD);
//		refUpdate.link(branchName);
//	}
}
