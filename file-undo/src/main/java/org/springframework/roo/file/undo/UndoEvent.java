package org.springframework.roo.file.undo;

import org.apache.commons.lang3.Validate;

/**
 * An event delivered to an {@link UndoListener}.
 * 
 * @author Ben Alex
 * @since 1.1.1
 */
public class UndoEvent {

    public enum UndoOperation {
        FLUSH, RESET, UNDO
    }

    private final UndoOperation operation;

    public UndoEvent(final UndoOperation operation) {
        Validate.notNull(operation, "Operation required");
        this.operation = operation;
    }

    public UndoOperation getOperation() {
        return operation;
    }

    public boolean isFlushing() {
        return operation == UndoOperation.FLUSH;
    }

    public boolean isResetting() {
        return operation == UndoOperation.RESET;
    }

    /**
     * @return true if undoing, false if committing
     */
    public boolean isUndoing() {
        return operation == UndoOperation.UNDO;
    }
}