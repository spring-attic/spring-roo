package org.springframework.roo.process.manager.event;

import org.springframework.roo.process.manager.ProcessManager;

/**
 * Implemented by classes that wish to be notified of {@link ProcessManager} status changes.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface ProcessManagerStatusListener {
	
	/**
	 * Invoked by the {@link ProcessManager} to report a new status.
	 * 
	 * @param oldStatus the old status
	 * @param newStatus the new status
	 */
	void onProcessManagerStatusChange(ProcessManagerStatus oldStatus, ProcessManagerStatus newStatus);
}
