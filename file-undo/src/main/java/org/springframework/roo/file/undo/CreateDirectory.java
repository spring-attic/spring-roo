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
 */
public class CreateDirectory implements UndoableOperation {

	// Constants
	private static final Logger LOGGER = HandlerUtils.getLogger(CreateDirectory.class);

	// Fields
	private final FilenameResolver filenameResolver;
	private final File actual;
	private File deleteFrom;

	public CreateDirectory(final UndoManager undoManager, final FilenameResolver filenameResolver, final File actual) {
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
		LOGGER.fine("Created " + filenameResolver.getMeaningfulName(actual));
	}

	public void reset() {}

	public boolean undo() {
		boolean success = FileUtils.deleteRecursively(deleteFrom);
		if (success) {
			LOGGER.fine("Undo create " + filenameResolver.getMeaningfulName(actual));
		} else {
			LOGGER.fine("Undo failed " + filenameResolver.getMeaningfulName(actual));
		}
		return success;
	}
}
