package org.springframework.roo.process.manager.event;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.roo.process.manager.ProcessManager;
import org.springframework.roo.support.util.Assert;

/**
 * Provides a convenience superclass for those {@link ProcessManager}s wishing to publish status messages.
 * 
 * @author Ben Alex
 * @since 1.0
 * 
 */
public abstract class AbstractProcessManagerStatusPublisher implements ProcessManagerStatusProvider {
	
	protected Set<ProcessManagerStatusListener> processManagerStatusListeners = new CopyOnWriteArraySet<ProcessManagerStatusListener>();
	protected StatusHolder processManagerStatus = new StatusHolder(ProcessManagerStatus.STARTING);
	
	public final void addProcessManagerStatusListener(ProcessManagerStatusListener processManagerStatusListener) {
		Assert.notNull(processManagerStatusListener, "Status listener required");
		processManagerStatusListeners.add(processManagerStatusListener);
	}

	public final void removeProcessManagerStatusListener(ProcessManagerStatusListener processManagerStatusListener) {
		Assert.notNull(processManagerStatusListener, "Status listener required");
		processManagerStatusListeners.remove(processManagerStatusListener);
	}

	/**
	 * Obtains the process manager status without synchronization.
	 */
	public final ProcessManagerStatus getProcessManagerStatus() {
		return processManagerStatus.status;
	}
	
	/**
	 * Set the process manager status without synchronization.
	 */
	protected void setProcessManagerStatus(ProcessManagerStatus processManagerStatus) {
		Assert.notNull(processManagerStatus, "Process manager status required");
		
		if (this.processManagerStatus.status == processManagerStatus) {
			// No need to make a change
			return;
		}
		
		this.processManagerStatus.status = processManagerStatus;

		for (ProcessManagerStatusListener listener : processManagerStatusListeners) {
			listener.onProcessManagerStatusChange(this.processManagerStatus.status, processManagerStatus);
		}
	}
	
	/**
	 * Used so a single object instance contains the changing {@link ProcessManagerStatus} enum. This
	 * is needed so there is a single object instance for synchronization purposes.
	 */
	private class StatusHolder {
		private ProcessManagerStatus status;
		
		private StatusHolder(ProcessManagerStatus initialStatus) {
			status = initialStatus;
		}
	}
	
}
