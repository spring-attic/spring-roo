package org.springframework.roo.file.undo;

import java.io.File;
import java.util.Date;
import java.util.logging.Logger;

import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileUtils;

/**
 * {@link UndoableOperation} to delete a directory.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class DeleteDirectory implements UndoableOperation {
	private static final Logger logger = HandlerUtils.getLogger(DeleteDirectory.class);

	private FilenameResolver filenameResolver;
	private File actual;
	private File backup;
	
	public DeleteDirectory(UndoManager undoManager, FilenameResolver filenameResolver, File actual) {
		Assert.notNull(undoManager, "Undo manager required");
		Assert.notNull(actual, "Actual file required");
		Assert.notNull(filenameResolver, "Filename resolver required");
		Assert.isTrue(actual.exists(), "File '" + actual + "' must exist");
		Assert.isTrue(actual.isDirectory(), "Path '" + actual + "' must be a directory (not a file)");
		this.filenameResolver = filenameResolver;
		File tmpDir = new File(System.getProperty("java.io.tmpdir"));
		Assert.isTrue(tmpDir.isDirectory(), "Temporary directory '" + tmpDir + "' was not a directory");
		backup = new File(tmpDir, "tmp_" + new Date().getTime() + "_dir");
		this.actual = actual;
		if (!FileUtils.copyRecursively(actual, backup, true)) {
			throw new IllegalStateException("Unable to create a complete backup of directory '" + actual + "'");
		}
		if (!FileUtils.deleteRecursively(actual)) {
			throw new IllegalStateException("Unable to completely delete directory '" + actual + "'");
		}
		undoManager.add(this);
		logger.fine("Deleted " + filenameResolver.getMeaningfulName(actual));
	}
	
	public void reset() {
		// fix for ROO-1555
		try {
			if (FileUtils.deleteRecursively(backup)) {
				logger.finest("Reset manage " + filenameResolver.getMeaningfulName(backup));
			}
			else {
				backup.deleteOnExit();
				logger.fine("Reset failed " + filenameResolver.getMeaningfulName(backup));
			}
		}
		catch (Throwable ignore) {
			backup.deleteOnExit();
			logger.fine("Reset failed " + filenameResolver.getMeaningfulName(backup));
		}
	}

	public boolean undo() {
		boolean success = FileUtils.copyRecursively(backup, actual, false);
		if (success) {
			logger.fine("Undo delete " + filenameResolver.getMeaningfulName(actual));
		} else {
			logger.fine("Undo failed " + filenameResolver.getMeaningfulName(actual));
		}
		return success;
	}
	
}
