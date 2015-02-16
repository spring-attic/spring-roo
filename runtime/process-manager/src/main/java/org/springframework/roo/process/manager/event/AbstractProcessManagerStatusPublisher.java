package org.springframework.roo.process.manager.event;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.process.manager.ProcessManager;

/**
 * Provides a convenience superclass for those {@link ProcessManager}s wishing
 * to publish status messages.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public abstract class AbstractProcessManagerStatusPublisher implements
        ProcessManagerStatusProvider {

    /**
     * Used so a single object instance contains the changing
     * {@link ProcessManagerStatus} enum. This is needed so there is a single
     * object instance for synchronization purposes.
     */
    private static class StatusHolder {

        private ProcessManagerStatus status;

        /**
         * Constructor
         * 
         * @param initialStatus
         */
        private StatusHolder(final ProcessManagerStatus initialStatus) {
            status = initialStatus;
        }
    }

    protected StatusHolder processManagerStatus = new StatusHolder(
            ProcessManagerStatus.STARTING);

    protected Set<ProcessManagerStatusListener> processManagerStatusListeners = new CopyOnWriteArraySet<ProcessManagerStatusListener>();

    public final void addProcessManagerStatusListener(
            final ProcessManagerStatusListener processManagerStatusListener) {
        Validate.notNull(processManagerStatusListener,
                "Status listener required");
        processManagerStatusListeners.add(processManagerStatusListener);
    }

    /**
     * Obtains the process manager status without synchronization.
     */
    public final ProcessManagerStatus getProcessManagerStatus() {
        return processManagerStatus.status;
    }

    public final void removeProcessManagerStatusListener(
            final ProcessManagerStatusListener processManagerStatusListener) {
        Validate.notNull(processManagerStatusListener,
                "Status listener required");
        processManagerStatusListeners.remove(processManagerStatusListener);
    }

    /**
     * Set the process manager status without synchronization.
     */
    protected void setProcessManagerStatus(
            final ProcessManagerStatus processManagerStatus) {
        Validate.notNull(processManagerStatus,
                "Process manager status required");

        if (this.processManagerStatus.status == processManagerStatus) {
            // No need to make a change
            return;
        }

        this.processManagerStatus.status = processManagerStatus;

        for (final ProcessManagerStatusListener listener : processManagerStatusListeners) {
            listener.onProcessManagerStatusChange(
                    this.processManagerStatus.status, processManagerStatus);
        }
    }

}
