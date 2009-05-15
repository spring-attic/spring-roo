package org.springframework.roo.process.manager.event;

import org.springframework.roo.process.manager.ProcessManager;

/**
 * Implemented by {@link ProcessManager}s that support the publication of shell status changes.
 * 
 * <p>
 * Implementations are not required to provide any guarantees with respect to the order
 * in which notifications are delivered to listeners.
 * 
 * <p>
 * Implementations must permit modification of the listener list, even while delivering
 * event notifications to listeners. However, listeners do not receive any guarantee that
 * their addition or removal from the listener list will be effective or not for any event
 * notification that is currently proceeding.
 * 
 * <p>
 * Implementations must ensure that status notifications are only delivered when an actual
 * change has taken place.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface ProcessManagerStatusProvider {
	
	/**
	 * Registers a new status listener.
	 * 
	 * @param processManagerStatusListener to register (cannot be null)
	 */
	void addProcessManagerStatusListener(ProcessManagerStatusListener processManagerStatusListener);
	
	/**
	 * Removes an existing status listener.
	 * 
	 * <p>
	 * If the presented status listener is not found, the method returns without exception.
	 * 
	 * @param processManagerStatusListener to remove (cannot be null)
	 */
	void removeProcessManagerStatusListener(ProcessManagerStatusListener processManagerStatusListener);
	
	/**
	 * Returns the current {@link ProcessManager}.
	 * 
	 * @return the current status (never null)
	 */
	ProcessManagerStatus getProcessManagerStatus();
}
