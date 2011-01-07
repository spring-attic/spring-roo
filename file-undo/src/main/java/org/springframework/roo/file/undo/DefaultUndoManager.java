package org.springframework.roo.file.undo;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.file.undo.UndoEvent.UndoOperation;
import org.springframework.roo.support.util.Assert;

/**
 * Default implementation of the {@link UndoManager} interface.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@Component
@Service
public class DefaultUndoManager implements UndoManager {

	private Stack<UndoableOperation> stack = new Stack<UndoableOperation>();
	private Set<UndoListener> listeners = new HashSet<UndoListener>();
	private boolean undoEnabled = true;
	
	protected void activate(ComponentContext context) {
	}

	public void add(UndoableOperation undoableOperation) {
		Assert.notNull(undoableOperation, "Undoable operation required");
		this.stack.push(undoableOperation);
	}

	public void flush() {
		notifyListeners(UndoOperation.FLUSH);
	}
	
	public void reset() {
		while (!this.stack.empty()) {
			UndoableOperation op = this.stack.pop();
			try {
				op.reset();
			} catch (Throwable t) {
				throw new IllegalStateException("UndoableOperation '" + op + "' threw an exception, in violation of the interface contract");
			}
		}
		notifyListeners(UndoOperation.RESET);
	}

	public boolean undo() {
		boolean undoMode = true;
		if (!undoEnabled) {
			// Force the undo stack to simply reset (but not perform any undos)
			undoMode = false;
		}
		while (!this.stack.empty()) {
			UndoableOperation op = this.stack.pop();
			try {
				if (undoMode) {
					if (!op.undo()) {
						// undo failed, so switch to reset mode going forward
						undoMode = false;
					}
				} else {
					// in reset mode
					op.reset();
				}
			} catch (Throwable t) {
				throw new IllegalStateException("UndoableOperation '" + op + "' threw an exception, in violation of the interface contract");
			}
		}
		notifyListeners(UndoOperation.UNDO);
		return undoMode;
	}
	
	public void setUndoEnabled(boolean undoEnabled) {
		this.undoEnabled = undoEnabled;
	}

	private void notifyListeners(UndoOperation operation) {
		for (UndoListener listener : listeners) {
			listener.onUndoEvent(new UndoEvent(operation));
		}
	}

	public void addUndoListener(UndoListener undoListener) {
		listeners.add(undoListener);
	}

	public void removeUndoListener(UndoListener undoListener) {
		listeners.remove(undoListener);
	}

}