package org.springframework.roo.file.undo;

import java.util.Stack;

/**
 * Provides the ability to undo changes to a file system.
 * 
 * <p>
 * Note that the complexities of file system I/O are significant and Java does not provide full
 * access to a file lock API that would simplify coding of implementations. Therefore undoing
 * changes to the file system is a best-effort basis only, and designs should not rely on
 * robust, guaranteed, fail-safe undo semantics. 
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface UndoManager {

	/**
	 * Registers an undoable operation in the {@link Stack}.
	 * 
	 * @param undoableOperation to register in a {@link Stack} (required)
	 */
	void add(UndoableOperation undoableOperation);
	
	/**
	 * Replays the undo {@link Stack}, and guarantees to clear the {@link Stack}.
	 * 
	 * <p>
	 * The first operation that returns false to {@link UndoableOperation#undo()} will cause the remainder
	 * of the {@link Stack} to stop undoing.
	 * 
	 * <p>
	 * Executing this command guarantees the {@link Stack} will be empty upon return, with every element either
	 * undone or reset.
	 * 
	 * @return true if all {@link UndoableOperation}s in the {@link Stack} were successfully undone
	 */
	boolean undo();
	
	/**
	 * Resets the undo {@link Stack}, and guarantees to clear the {@link Stack}.
	 * 
	 * <p>
	 * Executing this command guarantees the {@link Stack} will be empty upon return, with every element reset.
	 */
	void reset();
	
	/**
	 * Indicates a caller wishes the {@link UndoManager} or "flush" its contents. The exact meaning of a flush is
	 * implementation dependent. It is guaranteed to not change the undo stack, but simply notify {@link UndoListener}s.
	 */
	void flush();
	
	/**
	 * @param undoListener registers a new undo listener (required)
	 */
	void addUndoListener(UndoListener undoListener);
	
	/**
	 * @param undoListener removes a previously-registered undo listener (required)
	 */
	void removeUndoListener(UndoListener undoListener);
	
	/**
	 * @param undoEnabled enables or disables the undo feature, which is useful for debugging (defaults to true)
	 */
	void setUndoEnabled(boolean undoEnabled);
}