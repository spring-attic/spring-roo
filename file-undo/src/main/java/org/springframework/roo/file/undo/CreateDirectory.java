package org.springframework.roo.file.undo;

import java.io.File;
import java.util.logging.Logger;

import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileUtils;

/**
 * {@link UndoableOperation} to create a directory, including any parents.
 * 
 * <p>
 * Note that the created instance will internally track the uppermost directory it created,
 * and remove that directory during any undo operation.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class CreateDirectory implements UndoableOperation {
	private static final Logger logger = HandlerUtils.getLogger(CreateDirectory.class);

	private FilenameResolver filenameResolver;
	private File actual;
	private File deleteFrom;
	
	public CreateDirectory(UndoManager undoManager, FilenameResolver filenameResolver, File actual) {
		Assert.notNull(undoManager, "Undo manager required");
		Assert.notNull(actual, "Actual file required");
		Assert.notNull(filenameResolver, "Filename resolver required");
		Assert.isTrue(!actual.exists(), "Actual file '" + actual + "' cannot exist");
		this.filenameResolver = filenameResolver;
		this.actual = actual;
		
		// Figure out the first directory we should delete from
		deleteFrom = actual;
		while (true) {
			File parent = deleteFrom.getParentFile();
			if (!parent.exists()) {
				deleteFrom = parent;
			} else {
				break;
			}
		}
		
		Assert.state(this.actual.mkdirs(), "Could not create directory '" + actual + "'");
		undoManager.add(this);
		logger.fine("Created " + filenameResolver.getMeaningfulName(actual));
	}
	
	public void reset() {}

	public boolean undo() {
		boolean success = FileUtils.deleteRecursively(deleteFrom);
		if (success) {
			logger.fine("Undo create " + filenameResolver.getMeaningfulName(actual));
		} else {
			logger.fine("Undo failed " + filenameResolver.getMeaningfulName(actual));
		}
		return success;
	}
	
}
