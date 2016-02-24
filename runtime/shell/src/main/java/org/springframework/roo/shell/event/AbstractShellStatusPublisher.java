package org.springframework.roo.shell.event;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.shell.ParseResult;
import org.springframework.roo.shell.event.ShellStatus.Status;

/**
 * Provides a convenience superclass for those shells wishing to publish status
 * messages.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public abstract class AbstractShellStatusPublisher implements
        ShellStatusProvider {

    protected Set<ShellStatusListener> shellStatusListeners = new CopyOnWriteArraySet<ShellStatusListener>();
    protected ShellStatus shellStatus = new ShellStatus(Status.STARTING);

    public final void addShellStatusListener(
            final ShellStatusListener shellStatusListener) {
        Validate.notNull(shellStatusListener, "Status listener required");
        synchronized (shellStatus) {
            shellStatusListeners.add(shellStatusListener);
        }
    }

    public final ShellStatus getShellStatus() {
        synchronized (shellStatus) {
            return shellStatus;
        }
    }

    public final void removeShellStatusListener(
            final ShellStatusListener shellStatusListener) {
        Validate.notNull(shellStatusListener, "Status listener required");
        synchronized (shellStatus) {
            shellStatusListeners.remove(shellStatusListener);
        }
    }

    protected void setShellStatus(final Status shellStatus) {
        setShellStatus(shellStatus, null, null);
    }

    protected void setShellStatus(final Status shellStatus, final String msg,
            final ParseResult parseResult) {
        Validate.notNull(shellStatus, "Shell status required");

        synchronized (this.shellStatus) {
            ShellStatus st;
            if (msg == null || msg.length() == 0) {
                st = new ShellStatus(shellStatus);
            }
            else {
                st = new ShellStatus(shellStatus, msg, parseResult);
            }

            if (this.shellStatus.equals(st)) {
                return;
            }

            for (final ShellStatusListener listener : shellStatusListeners) {
                listener.onShellStatusChange(this.shellStatus, st);
            }
            this.shellStatus = st;
        }
    }
}
