package org.springframework.roo.shell.event;

/**
 * Represents the different states that a shell can legally be in.
 * 
 * <p>
 * There is no "shut down" state because the shell would have been terminated by
 * that stage and potentially garbage collected. There is no guarantee that a
 * shell implementation will necessarily publish every state.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public enum ShellStatus {
	STARTING,
	STARTED,
	USER_INPUT,
	PARSING,
	EXECUTING,
	EXECUTION_RESULT_PROCESSING,
	EXECUTION_COMPLETE,
	SHUTTING_DOWN
}
