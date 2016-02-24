package org.springframework.roo.file.undo;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.file.undo.UndoEvent.UndoOperation;

/**
 * Default implementation of the {@link UndoManager} interface.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
public class DefaultUndoManager implements UndoManager {

    private final Set<UndoListener> listeners = new HashSet<UndoListener>();
    private final Stack<UndoableOperation> stack = new Stack<UndoableOperation>();
    private boolean undoEnabled = true;

    protected void activate(final ComponentContext context) {
    }

    public void add(final UndoableOperation undoableOperation) {
        Validate.notNull(undoableOperation, "Undoable operation required");
        stack.push(undoableOperation);
    }

    public void addUndoListener(final UndoListener undoListener) {
        listeners.add(undoListener);
    }

    public void flush() {
        notifyListeners(UndoOperation.FLUSH);
    }

    private void notifyListeners(final UndoOperation operation) {
        for (final UndoListener listener : listeners) {
            listener.onUndoEvent(new UndoEvent(operation));
        }
    }

    public void removeUndoListener(final UndoListener undoListener) {
        listeners.remove(undoListener);
    }

    public void reset() {
        while (!stack.empty()) {
            final UndoableOperation op = stack.pop();
            try {
                op.reset();
            }
            catch (final Throwable t) {
                throw new IllegalStateException(
                        "UndoableOperation '"
                                + op
                                + "' threw an exception, in violation of the interface contract");
            }
        }
        notifyListeners(UndoOperation.RESET);
    }

    public void setUndoEnabled(final boolean undoEnabled) {
        this.undoEnabled = undoEnabled;
    }

    public boolean undo() {
        boolean undoMode = true;
        if (!undoEnabled) {
            // Force the undo stack to simply reset (but not perform any undos)
            undoMode = false;
        }
        while (!stack.empty()) {
            final UndoableOperation op = stack.pop();
            try {
                if (undoMode) {
                    if (!op.undo()) {
                        // Undo failed, so switch to reset mode going forward
                        undoMode = false;
                    }
                }
                else {
                    // In reset mode
                    op.reset();
                }
            }
            catch (final Throwable t) {
                throw new IllegalStateException(
                        "UndoableOperation '"
                                + op
                                + "' threw an exception, in violation of the interface contract");
            }
        }
        notifyListeners(UndoOperation.UNDO);
        return undoMode;
    }
}