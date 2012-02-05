package org.springframework.roo.file.undo;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * {@link UndoableOperation} to delete a file.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class DeleteFile implements UndoableOperation {

    private static final Logger LOGGER = HandlerUtils
            .getLogger(DeleteFile.class);

    private final File actual;
    private final File backup;
    private final FilenameResolver filenameResolver;

    /**
     * Constructor that doesn't allow a reason to be given
     * 
     * @param undoManager cannot be <code>null</code>
     * @param filenameResolver cannot be <code>null</code>
     * @param actual the file to delete; must be an existing file (not a
     *            directory)
     * @deprecated use the constructor that allows a reason to be given
     */
    @Deprecated
    public DeleteFile(final UndoManager undoManager,
            final FilenameResolver filenameResolver, final File actual) {
        this(undoManager, filenameResolver, actual, null);
    }

    /**
     * Constructor that allows a reason to be given
     * 
     * @param undoManager cannot be <code>null</code>
     * @param filenameResolver cannot be <code>null</code>
     * @param actual the file to delete; must be an existing file (not a
     *            directory)
     * @param reason the reason for the file's deletion (can be blank)
     * @since 1.2.0
     */
    public DeleteFile(final UndoManager undoManager,
            final FilenameResolver filenameResolver, final File actual,
            final String reason) {
        Validate.notNull(undoManager, "Undo manager required");
        Validate.notNull(actual, "File required");
        Validate.notNull(filenameResolver, "Filename resolver required");
        Validate.isTrue(actual.exists(), "File '" + actual + "' must exist");
        Validate.isTrue(actual.isFile(), "Path '" + actual
                + "' must be a file (not a directory)");

        try {
            backup = File.createTempFile("DeleteFile", "tmp");
            FileUtils.copyFile(actual, backup);
        }
        catch (final IOException ioe) {
            throw new IllegalStateException("Unable to make a backup of file '"
                    + actual + "'", ioe);
        }
        this.actual = actual;
        this.actual.delete();
        this.filenameResolver = filenameResolver;
        undoManager.add(this);
        String deletionMessage = "Deleted "
                + filenameResolver.getMeaningfulName(actual);
        if (StringUtils.isNotBlank(reason)) {
            deletionMessage += " - " + reason.trim();
        }
        LOGGER.fine(deletionMessage);
    }

    public void reset() {
        // Fix for ROO-1555
        try {
            if (backup.delete()) {
                LOGGER.finest("Reset manage "
                        + filenameResolver.getMeaningfulName(backup));
            }
            else {
                backup.deleteOnExit();
                LOGGER.fine("Reset failed "
                        + filenameResolver.getMeaningfulName(backup));
            }
        }
        catch (final Throwable e) {
            backup.deleteOnExit();
            LOGGER.fine("Reset failed "
                    + filenameResolver.getMeaningfulName(backup));
        }
    }

    public boolean undo() {
        try {
            FileUtils.copyFile(backup, actual);
            LOGGER.fine("Undo delete "
                    + filenameResolver.getMeaningfulName(actual));
            return true;
        }
        catch (final IOException ioe) {
            LOGGER.fine("Undo failed "
                    + filenameResolver.getMeaningfulName(actual));
            return false;
        }
    }

}
