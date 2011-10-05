package org.springframework.roo.file.undo;

import java.io.File;
import java.util.Date;
import java.util.logging.Logger;

import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.StringUtils;

/**
 * {@link UndoableOperation} to delete a directory.
 *
 * @author Ben Alex
 * @since 1.0
 */
public class DeleteDirectory implements UndoableOperation {

	// Constants
	private static final File TEMP_DIRECTORY = new File(System.getProperty("java.io.tmpdir"));
	private static final Logger LOGGER = HandlerUtils.getLogger(DeleteDirectory.class);

	// Fields
	private final File actual;
	private final File backup;
	private final FilenameResolver filenameResolver;

	/**
	 * Constructor that doesn't allow a reason to be given
	 *
	 * @param undoManager (required)
	 * @param filenameResolver (required)
	 * @param directory the directory to delete; must be an existing directory
	 * (not a file)
	 * @deprecated use the constructor that allows a reason to be provided
	 */
	@Deprecated
	public DeleteDirectory(final UndoManager undoManager, final FilenameResolver filenameResolver, final File directory) {
		this(undoManager, filenameResolver, directory, null);
	}

	/**
	 * Constructor that allows a reason to be given
	 *
	 * @param undoManager (required)
	 * @param filenameResolver (required)
	 * @param directory the directory to delete; must be an existing directory
	 * (not a file)
	 * @param reason the reason for the directory's deletion; can be blank
	 * @since 1.2.0
	 */
	public DeleteDirectory(final UndoManager undoManager, final FilenameResolver filenameResolver, final File directory, final String reason) {
		Assert.notNull(undoManager, "Undo manager required");
		Assert.notNull(directory, "Actual file required");
		Assert.notNull(filenameResolver, "Filename resolver required");
		Assert.isTrue(directory.exists(), "File '" + directory + "' must exist");
		Assert.isTrue(directory.isDirectory(), "Path '" + directory + "' must be a directory (not a file)");
		Assert.isTrue(TEMP_DIRECTORY.isDirectory(), "Temporary directory '" + TEMP_DIRECTORY + "' is not a directory");
		this.actual = directory;
		this.backup = new File(TEMP_DIRECTORY, "tmp_" + new Date().getTime() + "_dir");
		this.filenameResolver = filenameResolver;
		if (!FileUtils.copyRecursively(directory, backup, true)) {
			throw new IllegalStateException("Unable to create a complete backup of directory '" + directory + "'");
		}
		if (!FileUtils.deleteRecursively(directory)) {
			throw new IllegalStateException("Unable to completely delete directory '" + directory + "'");
		}
		undoManager.add(this);
		String deletionMessage = "Deleted " + filenameResolver.getMeaningfulName(directory);
		if (StringUtils.hasText(reason)) {
			deletionMessage += " - " + reason.trim();
		}
		LOGGER.fine(deletionMessage);
	}

	public void reset() {
		// Fix for ROO-1555
		try {
			if (FileUtils.deleteRecursively(backup)) {
				LOGGER.finest("Reset manage " + filenameResolver.getMeaningfulName(backup));
			}
			else {
				backup.deleteOnExit();
				LOGGER.fine("Reset failed " + filenameResolver.getMeaningfulName(backup));
			}
		}
		catch (final Throwable ignore) {
			backup.deleteOnExit();
			LOGGER.fine("Reset failed " + filenameResolver.getMeaningfulName(backup));
		}
	}

	public boolean undo() {
		final boolean success = FileUtils.copyRecursively(backup, actual, false);
		if (success) {
			LOGGER.fine("Undo delete " + filenameResolver.getMeaningfulName(actual));
		} else {
			LOGGER.fine("Undo failed " + filenameResolver.getMeaningfulName(actual));
		}
		return success;
	}
}
