package org.springframework.roo.file.undo;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * {@link UndoableOperation} to create a file.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class CreateFile implements UndoableOperation {

    private static final Logger LOGGER = HandlerUtils
            .getLogger(CreateFile.class);

    private final File actual;
    private final FilenameResolver filenameResolver;

    public CreateFile(final UndoManager undoManager,
            final FilenameResolver filenameResolver, final File actual) {
        Validate.notNull(undoManager, "Undo manager required");
        Validate.notNull(actual, "Actual file required");
        Validate.notNull(filenameResolver, "Filename resolver required");
        Validate.isTrue(!actual.exists(), "Actual file '" + actual
                + "' cannot exist");
        this.filenameResolver = filenameResolver;
        this.actual = actual;
        try {
            this.actual.createNewFile();
        }
        catch (final IOException ioe) {
            throw new IllegalStateException("Unable to create file '"
                    + this.actual + "'", ioe);
        }
        undoManager.add(this);
    }

    public void reset() {
    }

    public boolean undo() {
        final boolean success = actual.delete();
        LOGGER.fine((success ? "Undo create " : "Undo failed ")
                + filenameResolver.getMeaningfulName(actual));
        return success;
    }
}
