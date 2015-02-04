package org.springframework.roo.file.undo;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * {@link UndoableOperation} to update a file.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class UpdateFile implements UndoableOperation {

    private static final Logger LOGGER = HandlerUtils
            .getLogger(UpdateFile.class);

    private final File actual;
    private final File backup;
    private final FilenameResolver filenameResolver;

    /**
     * Constructor
     * 
     * @param undoManager cannot be <code>null</code>
     * @param filenameResolver cannot be <code>null</code>
     * @param actual the file to be updated; must be an existing file (not a
     *            directory)
     */
    public UpdateFile(final UndoManager undoManager,
            final FilenameResolver filenameResolver, final File actual) {
        Validate.notNull(undoManager, "Undo manager required");
        Validate.notNull(actual, "File required");
        Validate.isTrue(actual.exists(), "File '%s' must exist", actual);
        Validate.isTrue(actual.isFile(),
                "Path '%s' must be a file (not a directory)", actual);
        Validate.notNull(filenameResolver, "Filename resolver required");
        this.filenameResolver = filenameResolver;
        try {
            backup = File.createTempFile("UpdateFile", "tmp");
            FileUtils.copyFile(actual, backup);
        }
        catch (final IOException ioe) {
            throw new IllegalStateException("Unable to make a backup of file '"
                    + actual + "'", ioe);
        }
        this.actual = actual;
        undoManager.add(this);
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
            LOGGER.fine("Undo manage "
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
