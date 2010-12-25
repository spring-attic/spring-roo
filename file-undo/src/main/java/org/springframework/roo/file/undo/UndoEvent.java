package org.springframework.roo.file.undo;

/**
 * An event delivered to an {@link UndoListener}.
 * 
 * @author Ben Alex
 * @since 1.1.1
 *
 */
public class UndoEvent {
	private boolean undoing;
	
	public UndoEvent(boolean undoing) {
		this.undoing = undoing;
	}
	
	/**
	 * @return true if undoing, false if committing
	 */
	public boolean isUndoing() {
		return undoing;
	}
}