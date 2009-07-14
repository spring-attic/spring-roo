package org.springframework.roo.process.manager.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.roo.file.monitor.FileMonitorService;
import org.springframework.roo.file.undo.UndoManager;
import org.springframework.roo.process.manager.ActiveProcessManager;
import org.springframework.roo.process.manager.CommandCallback;
import org.springframework.roo.process.manager.ProcessManager;
import org.springframework.roo.process.manager.event.AbstractProcessManagerStatusPublisher;
import org.springframework.roo.process.manager.event.ProcessManagerStatus;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.ExceptionUtils;

/**
 * Default implementation of {@link ProcessManager} interface.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public class DefaultProcessManager extends AbstractProcessManagerStatusPublisher implements ProcessManager, Runnable {

	private static final Logger logger = Logger.getLogger(DefaultProcessManager.class.getName());
	
	private boolean developmentMode = false;
	private UndoManager undoManager;
	private FileMonitorService fileMonitorService;
	private InitialMonitoringRequest initialMonitoringRequest;
	
	public DefaultProcessManager(UndoManager undoManager, FileMonitorService fileMonitorService, InitialMonitoringRequest initialMonitoringRequest) {
		Assert.notNull(undoManager, "Undo manager is required");
		Assert.notNull(fileMonitorService, "File monitor service is required");
		Assert.notNull(initialMonitoringRequest, "Initial monitoring request is required");
		this.undoManager = undoManager;
		this.fileMonitorService = fileMonitorService;
		this.initialMonitoringRequest = initialMonitoringRequest;
		
		if (!"false".equals(System.getProperty("developmentMode", "false").toLowerCase())) {
			developmentMode = true;
		}
		
	}
	
	public void completeStartup() {
		// Quick sanity check that we're being called at the correct time; we don't need to get a synchronization lock if the method shouldn't even run
		Assert.isTrue(getProcessManagerStatus() == ProcessManagerStatus.STARTING, "Process manager must have a status of STARTING to complete startup");
		synchronized (processManagerStatus) {
			try {
				// Register the initial monitoring request
				doTransactionally(new MonitoringRequestCommand(fileMonitorService, initialMonitoringRequest.getMonitoringRequest(), true));
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
				fileMonitorService.scanAll();
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

	public void run() {
		try {
			backgroundPoll();
		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		}
	}

	public boolean isDevelopmentMode() {
		return developmentMode;
	}

	public void setDevelopmentMode(boolean developmentMode) {
		this.developmentMode = developmentMode;
	}

}
