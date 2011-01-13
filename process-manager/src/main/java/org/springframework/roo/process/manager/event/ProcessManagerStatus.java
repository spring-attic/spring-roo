package org.springframework.roo.process.manager.event;

import org.springframework.roo.process.manager.ProcessManager;

/**
 * Represents the different states that a {@link ProcessManager} can legally be in.
 * 
 * <p>
 * There is no "shut down" state because the process manager would have been terminated by
 * that stage and potentially garbage collected. There is no guarantee that a
 * process manager implementation will necessarily publish every state.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public enum ProcessManagerStatus {
	STARTING,
	COMPLETING_STARTUP,
	AVAILABLE,
	BUSY_POLLING,
	BUSY_EXECUTING,
	UNDOING,
	RESETTING_UNDOS,
	TERMINATED
}
