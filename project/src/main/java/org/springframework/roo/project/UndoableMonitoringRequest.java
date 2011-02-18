package org.springframework.roo.project;

import org.springframework.roo.file.monitor.FileMonitorService;
import org.springframework.roo.file.monitor.MonitoringRequest;
import org.springframework.roo.file.undo.UndoManager;
import org.springframework.roo.file.undo.UndoableOperation;
import org.springframework.roo.support.util.Assert;

/**
 * Allows {@link org.springframework.roo.file.monitor.MonitoringRequest}s to be applied as {@link org.springframework.roo.file.undo.UndoableOperation}s.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class UndoableMonitoringRequest implements UndoableOperation {
	private FileMonitorService fileMonitorService;
	private MonitoringRequest monitoringRequest;
	private boolean add;

	private boolean resetRequired;
	
	public UndoableMonitoringRequest(UndoManager undoManager, FileMonitorService fileMonitorService, MonitoringRequest monitoringRequest, boolean add) {
		Assert.notNull(undoManager, "Undo manager required");
		Assert.notNull(fileMonitorService, "File monitor service required");
		Assert.notNull(monitoringRequest, "Request required");
		this.fileMonitorService = fileMonitorService;
		this.monitoringRequest = monitoringRequest;
		this.add = add;
		
		if (add) {
			resetRequired = fileMonitorService.add(monitoringRequest);
		} else {
			resetRequired = fileMonitorService.remove(monitoringRequest);
		}
	
		undoManager.add(this);
	}
	
	public void reset() {}

	public boolean undo() {
		if (!resetRequired) {
			return true;
		}
		try {
			if (add) {
				fileMonitorService.remove(monitoringRequest);
			} else {
				fileMonitorService.add(monitoringRequest);
			}
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

}
