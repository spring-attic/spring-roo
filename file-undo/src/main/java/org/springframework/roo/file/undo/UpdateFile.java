package org.springframework.roo.file.undo;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;

/**
 * {@link UndoableOperation} to update a file.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class UpdateFile implements UndoableOperation {
	private static final Logger logger = HandlerUtils.getLogger(UpdateFile.class);

	private FilenameResolver filenameResolver;
	private File actual;
	private File backup;
	
	public UpdateFile(UndoManager undoManager, FilenameResolver filenameResolver, File actual) {
		Assert.notNull(undoManager, "Undo manager required");
		Assert.notNull(actual, "File required");
		Assert.isTrue(actual.exists(), "File '" + actual + "' must exist");
		Assert.isTrue(actual.isFile(), "Path '" + actual + "' must be a file (not a directory)");
		Assert.notNull(filenameResolver, "Filename resolver required");
		this.filenameResolver = filenameResolver;
		try {
			backup = File.createTempFile("UpdateFile", "tmp");
			FileCopyUtils.copy(actual, backup);
		} catch (IOException ioe) {
			throw new IllegalStateException("Unable to make a backup of file '" + this.actual + "'", ioe);
		}
		this.actual = actual;
		undoManager.add(this);
	}
	
	public void reset() {
		// fix for ROO-1555
		try {
			if (backup.delete()) {
				logger.finest("Reset manage " + filenameResolver.getMeaningfulName(backup));
			} else {
				backup.deleteOnExit();
				logger.fine("Reset failed " + filenameResolver.getMeaningfulName(backup));
			}
		} catch (Throwable e) {
			backup.deleteOnExit();
			logger.fine("Reset failed " + filenameResolver.getMeaningfulName(backup));
		}
	}

	public boolean undo() {
		try {
			FileCopyUtils.copy(backup, actual);
			logger.fine("Undo manage " + filenameResolver.getMeaningfulName(actual));
			return true;
		} catch (IOException ioe) {
			logger.fine("Undo failed " + filenameResolver.getMeaningfulName(actual));
			return false;
		}
	}
	
}
