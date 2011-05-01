package org.springframework.roo.file.undo;

import org.springframework.roo.support.util.Assert;

/**
 * An event delivered to an {@link UndoListener}.
 * 
 * @author Ben Alex
 * @since 1.1.1
 *
 */
public class UndoEvent {
	private UndoOperation operation;
	
	public UndoEvent(UndoOperation operation) {
		Assert.notNull(operation, "Operation required");
		this.operation = operation;
	}
	
	/**
	 * @return true if undoing, false if committing
	 */
	public boolean isUndoing() {
		return operation == UndoOperation.UNDO;
	}
	
	public boolean isResetting() {
		return operation == UndoOperation.RESET;
	}

	public boolean isFlushing() {
		return operation == UndoOperation.FLUSH;
	}
	
	public UndoOperation getOperation() {
		return operation;
	}
	
	public enum UndoOperation {
		UNDO,
		RESET,
		FLUSH
	}
}