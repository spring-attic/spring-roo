package org.springframework.roo.file.undo;

import java.util.logging.Logger;

/**
 * An operation than can be undone by {@link UndoManager}.
 * 
 * <p>
 * An {@link UndoableOperation} is NOT permitted to throw any exception at any time. It should log
 * any error conditions to the {@link Logger} only.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface UndoableOperation {

	/**
	 * Attempt to undo the changes, and release any resources consumed.
	 * 
	 * <p>
	 * No exceptions may be thrown.
	 * 
	 * @return whether the undo was successful or not
	 */
	boolean undo();
	
	/**
	 * Release any temporary resources consumed by the {@link UndoableOperation}.
	 * 
	 * <p>
	 * No exceptions may be thrown.
	 */
	void reset();

}
