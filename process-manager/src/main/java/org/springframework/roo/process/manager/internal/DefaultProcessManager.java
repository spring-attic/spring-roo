package org.springframework.roo.process.manager.internal;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.ExceptionUtils;

/**
 * Default implementation of {@link ProcessManager} interface.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@Component(immediate=true)
@Service
public class DefaultProcessManager extends AbstractProcessManagerStatusPublisher implements ProcessManager {

	private static final Logger logger = HandlerUtils.getLogger(DefaultProcessManager.class);
	
	@Reference private UndoManager undoManager;
	@Reference private FileMonitorService fileMonitorService;
	@Reference private StartLevel startLevel;
	private Timer t = new Timer(true);
	private boolean developmentMode = false;
	private long minimumDelayBetweenPoll = -1; // how many ms must pass at minimum between each poll (negative denotes auto-scaling; 0 = never)
	private long lastPollTime = 0; // what time the last poll was completed
	private long lastPollDuration = 0; // how many ms the last poll actually took
	private String workingDir = null; // the working directory of the current roo project
	
	protected void activate(ComponentContext context) {
		// obtain the working directory from the framework properties
		// TODO CD move constant to proper location
		workingDir = context.getBundleContext().getProperty("roo.working.directory");
		context.getBundleContext().addFrameworkListener(new FrameworkListener() {
			public void frameworkEvent(FrameworkEvent event) {
				if (event.getType() == FrameworkEvent.STARTLEVEL_CHANGED) {
					if (startLevel.getStartLevel() >= 99) {
						if (getProcessManagerStatus().equals(ProcessManagerStatus.STARTING)) {
							completeStartup();
						} else {
						}
					}
				}
			}
		});
		
		// Now start a thread that will undertake a background poll every second
		t.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (getProcessManagerStatus() == ProcessManagerStatus.AVAILABLE) {
					timerBasedPoll();
				}
			}
		}, 0, 1000);
	}

	protected void deactivate(ComponentContext context) {
		// We have lost a required component (eg UndoManager; ROO-1037)
		terminate(); // safe to call even if we'd terminated earlier
	}
	
	public void terminate() {
		synchronized (processManagerStatus) {
			// To get this far this thread has a lock on process manager status, so we control process manager and can terminate it
			if (getProcessManagerStatus() != ProcessManagerStatus.TERMINATED) {
				t.cancel();
				setProcessManagerStatus(ProcessManagerStatus.TERMINATED);
			}
		}
	}

	public void completeStartup() {
		// Quick sanity check that we're being called at the correct time; we don't need to get a synchronization lock if the method shouldn't even run
		Assert.isTrue(getProcessManagerStatus() == ProcessManagerStatus.STARTING, "Process manager must have a status of STARTING to complete startup");
		synchronized (processManagerStatus) {
			try {
				// Register the initial monitoring request
				doTransactionally(new MonitoringRequestCommand(fileMonitorService, MonitoringRequest.getInitialMonitoringRequest(workingDir), true));
			} catch (Throwable t) {
				logException(t);
			} finally {
				setProcessManagerStatus(ProcessManagerStatus.AVAILABLE);
			}
		}
	}

	public boolean backgroundPoll() {
		// Quickly determine if another thread is running; we don't need to sit around and wait (we'll get called again in a few hundred milliseconds anyway)
		if (getProcessManagerStatus() != ProcessManagerStatus.AVAILABLE) {
			return false;
		}
		synchronized (processManagerStatus) {
			// Do the check again, now this thread has a lock on processManagerStatus
			if (getProcessManagerStatus() != ProcessManagerStatus.AVAILABLE) {
				throw new IllegalStateException("Process manager status " + getProcessManagerStatus() + " but background thread acquired synchronization lock");
			}
			
			setProcessManagerStatus(ProcessManagerStatus.BUSY_POLLING);
			
			try {
				doTransactionally(null);
			} catch (Throwable t) {
				// We don't want a poll failure to cause the background polling thread to die
				logException(t);
			} finally {
				setProcessManagerStatus(ProcessManagerStatus.AVAILABLE);
			}
		}
		return true;
	}

	public <T> T execute(CommandCallback<T> callback) {
		Assert.notNull(callback, "Callback required");
		synchronized (processManagerStatus) {
			// For us to acquire this lock means no other thread has hold of process manager status
			Assert.isTrue(getProcessManagerStatus() == ProcessManagerStatus.AVAILABLE || getProcessManagerStatus() == ProcessManagerStatus.BUSY_EXECUTING, "Unable to execute as another thread has set status to " + getProcessManagerStatus());
			setProcessManagerStatus(ProcessManagerStatus.BUSY_EXECUTING);
			try {
				return doTransactionally(callback);
			} catch (RuntimeException ex) {
				logException(ex);
				throw ex;
			} finally {
				setProcessManagerStatus(ProcessManagerStatus.AVAILABLE);
			}
		}
	}

	private void logException(Throwable ex) {
		Throwable root = ExceptionUtils.extractRootCause(ex);
		if (developmentMode) {
			logger.log(Level.FINE, root.getMessage(), root);
		} else {
			String message = root.getMessage();
			if (message == null || "".equals(message)) {
				StackTraceElement[] trace = root.getStackTrace();
				if (trace != null && trace.length > 0) {
					message = root.getClass().getSimpleName() + " at " + trace[0].toString();
				} else {
					message = root.getClass().getSimpleName();
				}
			}
			logger.log(Level.FINE, message);
		}
	}
	
	private <T> T doTransactionally(CommandCallback<T> callback) {
		T result = null;
		try {
			ActiveProcessManager.setActiveProcessManager(this);
			
			// run the requested operation
			if (callback == null) {
				fileMonitorService.scanAll();
			} else {
				result = callback.callback();
			}
			
			// guarantee scans repeat until there are no more changes detected
			while (fileMonitorService.isDirty()) {
				if (fileMonitorService instanceof NotifiableFileMonitorService) {
					((NotifiableFileMonitorService)fileMonitorService).scanNotified();
				} else {
					fileMonitorService.scanAll();
				}
			}
			
			// it all seems to have worked, so clear the undo history
			setProcessManagerStatus(ProcessManagerStatus.RESETTING_UNDOS);
			
			undoManager.reset();
			
		} catch (RuntimeException rt) {
			// Something went wrong, so attempt to undo
			try {
				setProcessManagerStatus(ProcessManagerStatus.UNDOING);
				throw rt;
			} finally {
				undoManager.undo();
			}
		} finally {
			// TODO: Review in consultation with Christian as STS is clearing active process manager itself
			//ActiveProcessManager.clearActiveProcessManager();
		}
		
		return result;
	}

	public void timerBasedPoll() {
		try {
			if (minimumDelayBetweenPoll == 0) {
				// Manual polling only, we never allow the timer to kick of a poll
				return;
			}
			
			long effectiveMinimumDelayBetweenPoll = minimumDelayBetweenPoll;
			if (effectiveMinimumDelayBetweenPoll < 0) {
				// A negative minimum delay between poll means auto-scaling is used
				if (lastPollDuration < 500) {
					// We've never done a poll, or they are very fast
					effectiveMinimumDelayBetweenPoll = 0;
				} else {
					// Use the last duration (we might make this sliding scale in the future)
					effectiveMinimumDelayBetweenPoll = lastPollDuration;
				}
			}
			long started = System.currentTimeMillis();
			if (started < lastPollTime + effectiveMinimumDelayBetweenPoll) {
				// Too soon to re-poll
				return;
			}
			backgroundPoll();
			// record the completion time so we can ensure we don't re-poll too soon
			lastPollTime = System.currentTimeMillis();
			// compute how many milliseconds it took to run
			lastPollDuration = lastPollTime - started;
			if (lastPollDuration == 0) {
				lastPollDuration = 1; // ensure it correctly reflects that it has ever run
			}
		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		}
	}

	public boolean isDevelopmentMode() {
		return developmentMode;
	}

	public void setDevelopmentMode(boolean developmentMode) {
		this.developmentMode = developmentMode;
		// To assist with debugging, development mode does not undertake undo operations
		this.undoManager.setUndoEnabled(!developmentMode);
	}

	/**
	 * @param minimumDelayBetweenPoll how many milliseconds must pass between each poll
	 */
	public void setMinimumDelayBetweenPoll(long minimumDelayBetweenPoll) {
		this.minimumDelayBetweenPoll = minimumDelayBetweenPoll;
	}

	/**
	 * @return how many milliseconds must pass between each poll (0 = manual only; <0 = auto-scaled; >0 = interval)
	 */
	public long getMinimumDelayBetweenPoll() {
		return minimumDelayBetweenPoll;
	}

	/**
	 * @return how many milliseconds the last poll execution took to complete (0 = never ran; >0 = last execution time)
	 */
	public long getLastPollDuration() {
		return lastPollDuration;
	}

}
