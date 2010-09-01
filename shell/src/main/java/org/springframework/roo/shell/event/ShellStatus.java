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
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
public class ShellStatus {
	
	private Status status;
	private String message = "";
	
	public enum Status {
		STARTING,
		STARTED,
		USER_INPUT,
		PARSING,
		EXECUTING,
		EXECUTION_RESULT_PROCESSING,
		EXECUTION_SUCCESS,
		EXECUTION_FAILED,
		SHUTTING_DOWN;
	}
	
	ShellStatus(Status status) {
		this.status = status;
	}
	
	ShellStatus(Status status, String msg) {
		this.status = status;
		this.message = msg;
	}

	public String getMessage() {
		return message;
	}
	
	public Status getStatus() {
		return status;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ShellStatus other = (ShellStatus) obj;
		if (status != other.status)
			return false;
		return true;
	}
}
