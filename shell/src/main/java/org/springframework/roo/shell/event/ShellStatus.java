package org.springframework.roo.shell.event;

import org.springframework.roo.shell.ParseResult;

/**
 * Represents the different states that a shell can legally be in.
 * <p>
 * There is no "shut down" state because the shell would have been terminated by
 * that stage and potentially garbage collected. There is no guarantee that a
 * shell implementation will necessarily publish every state.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @since 1.0
 */
public class ShellStatus {

    public enum Status {
        STARTING, STARTED, USER_INPUT, PARSING, EXECUTING, EXECUTION_RESULT_PROCESSING, EXECUTION_SUCCESS, EXECUTION_FAILED, SHUTTING_DOWN
    }

    private final Status status;
    private String message = "";

    private ParseResult parseResult;

    ShellStatus(final Status status) {
        this.status = status;
    }

    ShellStatus(final Status status, final String msg,
            final ParseResult parseResult) {
        this.status = status;
        message = msg;
        this.parseResult = parseResult;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ShellStatus other = (ShellStatus) obj;
        if (message == null) {
            if (other.message != null) {
                return false;
            }
        }
        else if (!message.equals(other.message)) {
            return false;
        }
        if (parseResult == null) {
            if (other.parseResult != null) {
                return false;
            }
        }
        else if (!parseResult.equals(other.parseResult)) {
            return false;
        }
        if (status == null) {
            if (other.status != null) {
                return false;
            }
        }
        else if (!status.equals(other.status)) {
            return false;
        }
        return true;
    }

    public String getMessage() {
        return message;
    }

    public final ParseResult getParseResult() {
        return parseResult;
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (message == null ? 0 : message.hashCode());
        result = prime * result
                + (parseResult == null ? 0 : parseResult.hashCode());
        result = prime * result + (status == null ? 0 : status.hashCode());
        return result;
    }
}
