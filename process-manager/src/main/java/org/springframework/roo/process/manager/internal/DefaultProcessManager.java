package org.springframework.roo.process.manager.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.startlevel.StartLevel;
import org.springframework.roo.file.monitor.FileMonitorService;
import org.springframework.roo.file.monitor.MonitoringRequest;
import org.springframework.roo.file.monitor.NotifiableFileMonitorService;
import org.springframework.roo.file.undo.UndoManager;
import org.springframework.roo.process.manager.ActiveProcessManager;
import org.springframework.roo.process.manager.CommandCallback;
import org.springframework.roo.process.manager.ProcessManager;
import org.springframework.roo.process.manager.event.AbstractProcessManagerStatusPublisher;
import org.springframework.roo.process.manager.event.ProcessManagerStatus;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.OSGiUtils;

/**
 * Default implementation of {@link ProcessManager} interface.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true)
@Service
public class DefaultProcessManager extends
        AbstractProcessManagerStatusPublisher implements ProcessManager {

    private static final Logger LOGGER = HandlerUtils
            .getLogger(DefaultProcessManager.class);

    private boolean developmentMode = false;
    @Reference private FileMonitorService fileMonitorService;
    private long lastPollDuration = 0;
    private long lastPollTime = 0; // What time the last poll was completed
    private long minimumDelayBetweenPoll = -1; // How many ms must pass at
    @Reference private StartLevel startLevel;
    @Reference private UndoManager undoManager;
    private String workingDir;

    public <T> T execute(final CommandCallback<T> callback) {
        Validate.notNull(callback, "Callback required");
        synchronized (processManagerStatus) {
            // For us to acquire this lock means no other thread has hold of
            // process manager status
            Validate.isTrue(
                    getProcessManagerStatus() == ProcessManagerStatus.AVAILABLE
                            || getProcessManagerStatus() == ProcessManagerStatus.BUSY_EXECUTING,
                    "Unable to execute as another thread has set status to %s",
                    getProcessManagerStatus());
            setProcessManagerStatus(ProcessManagerStatus.BUSY_EXECUTING);
            try {
                return doTransactionally(callback);
            }
            catch (final RuntimeException e) {
                logException(e);
                throw e;
            }
            finally {
                setProcessManagerStatus(ProcessManagerStatus.AVAILABLE);
            }
        }
    }

    /**
     * @return how many milliseconds the last poll execution took to complete (0
     *         = never ran; >0 = last execution time)
     */
    public long getLastPollDuration() {
        return lastPollDuration;
    }

    /**
     * @return how many milliseconds must pass between each poll (0 = manual
     *         only; <0 = auto-scaled; >0 = interval)
     */
    public long getMinimumDelayBetweenPoll() {
        return minimumDelayBetweenPoll;
    }

    public boolean isDevelopmentMode() {
        return developmentMode;
    }

    public void setDevelopmentMode(final boolean developmentMode) {
        this.developmentMode = developmentMode;

        // To assist with debugging, development mode does not undertake undo
        // operations
        undoManager.setUndoEnabled(!developmentMode);
    }

    /**
     * @param minimumDelayBetweenPoll how many milliseconds must pass between
     *            each poll
     */
    public void setMinimumDelayBetweenPoll(final long minimumDelayBetweenPoll) {
        this.minimumDelayBetweenPoll = minimumDelayBetweenPoll;
    }

    public void terminate() {
        synchronized (processManagerStatus) {
            // To get this far this thread has a lock on process manager status,
            // so we control process manager and can terminate its background
            // timer thread
            if (getProcessManagerStatus() != ProcessManagerStatus.TERMINATED) {
                // The thread started above will terminate of its own accord,
                // given we are shutting down
                setProcessManagerStatus(ProcessManagerStatus.TERMINATED);
            }
        }
    }

    public void timerBasedPoll() {
        try {
            if (minimumDelayBetweenPoll == 0) {
                // Manual polling only, we never allow the timer to kick of a
                // poll
                return;
            }

            long effectiveMinimumDelayBetweenPoll = minimumDelayBetweenPoll;
            if (effectiveMinimumDelayBetweenPoll < 0) {
                // A negative minimum delay between poll means auto-scaling is
                // used
                if (lastPollDuration < 500) {
                    // We've never done a poll, or they are very fast
                    effectiveMinimumDelayBetweenPoll = 0;
                }
                else {
                    // Use the last duration (we might make this sliding scale
                    // in the future)
                    effectiveMinimumDelayBetweenPoll = lastPollDuration;
                }
            }
            final long started = System.currentTimeMillis();
            if (started < lastPollTime + effectiveMinimumDelayBetweenPoll) {
                // Too soon to re-poll
                return;
            }
            backgroundPoll();
            // Record the completion time so we can ensure we don't re-poll too
            // soon
            lastPollTime = System.currentTimeMillis();

            // Compute how many milliseconds it took to run
            lastPollDuration = lastPollTime - started;
            if (lastPollDuration == 0) {
                // Ensure it correctly reflects that it has ever run
                lastPollDuration = 1;
            }
        }
        catch (final Throwable t) {
            LOGGER.log(Level.SEVERE, t.getMessage(), t);
        }
    }

    protected void activate(final ComponentContext context) {
        workingDir = OSGiUtils.getRooWorkingDirectory(context);
        context.getBundleContext().addFrameworkListener(
                new FrameworkListener() {
                    public void frameworkEvent(final FrameworkEvent event) {
                        if (startLevel.getStartLevel() >= 99) {
                            // We check we haven't already started, as this
                            // event listener will be called several times at SL
                            // >= 99
                            if (getProcessManagerStatus() == ProcessManagerStatus.STARTING) {
                                // A proper synchronized process manager status
                                // check will take place in the
                                // completeStartup() method
                                completeStartup();
                            }
                        }
                    }
                });

        // Now start a thread that will undertake a background poll every second
        final Thread t = new Thread(new Runnable() {
            public void run() {
                // Unsynchronized lookup of terminated status to avoid anything
                // blocking the termination of the thread
                while (getProcessManagerStatus() != ProcessManagerStatus.TERMINATED) {
                    // We only bother doing a poll if we seem to be available (a
                    // proper synchronized check happens later)
                    if (getProcessManagerStatus() == ProcessManagerStatus.AVAILABLE) {
                        timerBasedPoll();
                    }
                    try {
                        Thread.sleep(1000);
                    }
                    catch (final InterruptedException ignoreAndContinue) {
                    }
                }
            }
        }, "Spring Roo Process Manager Background Polling Thread");
        t.start();
    }

    protected void deactivate(final ComponentContext context) {
        // We have lost a required component (eg UndoManager; ROO-1037)
        terminate(); // Safe to call even if we'd terminated earlier
    }

    private boolean backgroundPoll() {
        // Quickly determine if another thread is running; we don't need to sit
        // around and wait (we'll get called again in a few hundred milliseconds
        // anyway)
        if (getProcessManagerStatus() != ProcessManagerStatus.AVAILABLE) {
            return false;
        }
        synchronized (processManagerStatus) {
            // Do the check again, now this thread has a lock on
            // processManagerStatus
            if (getProcessManagerStatus() != ProcessManagerStatus.AVAILABLE) {
                throw new IllegalStateException(
                        "Process manager status "
                                + getProcessManagerStatus()
                                + " but background thread acquired synchronization lock");
            }

            setProcessManagerStatus(ProcessManagerStatus.BUSY_POLLING);

            try {
                doTransactionally(null);
            }
            catch (final Throwable t) {
                // We don't want a poll failure to cause the background polling
                // thread to die
                logException(t);
            }
            finally {
                setProcessManagerStatus(ProcessManagerStatus.AVAILABLE);
            }
        }
        return true;
    }

    private void completeStartup() {
        synchronized (processManagerStatus) {
            if (getProcessManagerStatus() != ProcessManagerStatus.STARTING) {
                throw new IllegalStateException("Process manager status "
                        + getProcessManagerStatus() + " but should be STARTING");
            }
            setProcessManagerStatus(ProcessManagerStatus.COMPLETING_STARTUP);
            try {
                // Register the initial monitoring request
                doTransactionally(new MonitoringRequestCommand(
                        fileMonitorService,
                        MonitoringRequest
                                .getInitialSubTreeMonitoringRequest(workingDir),
                        true));
            }
            catch (final Throwable t) {
                logException(t);
            }
            finally {
                setProcessManagerStatus(ProcessManagerStatus.AVAILABLE);
            }
        }
    }

    private <T> T doTransactionally(final CommandCallback<T> callback) {
        T result = null;
        try {
            ActiveProcessManager.setActiveProcessManager(this);

            // Run the requested operation
            if (callback == null) {
                fileMonitorService.scanAll();
            }
            else {
                result = callback.callback();
            }

            // Flush the undo manager so that any changes it has been holding
            // are written to disk and the file monitor service
            undoManager.flush();

            // Guarantee scans repeat until there are no more changes detected
            while (fileMonitorService.isDirty()) {
                if (fileMonitorService instanceof NotifiableFileMonitorService) {
                    ((NotifiableFileMonitorService) fileMonitorService)
                            .scanNotified();
                }
                else {
                    fileMonitorService.scanAll();
                }
                // In case something else happened as a result of event
                // notifications above
                undoManager.flush();
            }

            // It all seems to have worked, so clear the undo history
            setProcessManagerStatus(ProcessManagerStatus.RESETTING_UNDOS);

            undoManager.reset();

        }
        catch (final RuntimeException e) {
            // Something went wrong, so attempt to undo
            try {
                setProcessManagerStatus(ProcessManagerStatus.UNDOING);
                throw e;
            }
            finally {
                undoManager.undo();
            }
        }
        finally {
            // TODO: Review in consultation with Christian as STS is clearing
            // active process manager itself
            // ActiveProcessManager.clearActiveProcessManager();
        }

        return result;
    }

    private void logException(final Throwable t) {
        final Throwable root = ObjectUtils.defaultIfNull(
                ExceptionUtils.getRootCause(t), t);
        if (developmentMode) {
            LOGGER.log(Level.FINE, root.getMessage(), root);
        }
        else {
            String message = root.getMessage();
            if (StringUtils.isBlank(message)) {
                final StackTraceElement[] trace = root.getStackTrace();
                if (trace != null && trace.length > 0) {
                    message = root.getClass().getSimpleName() + " at "
                            + trace[0].toString();
                }
                else {
                    message = root.getClass().getSimpleName();
                }
            }
            LOGGER.log(Level.FINE, message);
        }
    }
}
