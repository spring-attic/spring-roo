package org.springframework.roo.file.undo;

/**
 * Indicates an implementation of receiving {@link UndoEvent}s.
 * 
 * <p>
 * Register via {@link UndoManager#addUndoListener(UndoListener)}.
 * 
 * @author Ben Alex
 * @since 1.1.1
 *
 */
public interface UndoListener {
	
	/**
	 * @param event the new event which took place (required)
	 */
	void onUndoEvent(UndoEvent event);
}