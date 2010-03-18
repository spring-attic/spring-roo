package org.springframework.roo.file.monitor;

import java.io.File;
import java.util.Set;

import org.springframework.roo.file.monitor.event.FileOperation;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * A request to monitor a particular file.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class FileMonitoringRequest extends MonitoringRequest {

	public FileMonitoringRequest(File file, Set<FileOperation> notifyOn) {
		super(file, notifyOn);
		Assert.isTrue(file.isFile(), "File '" + file + "' must be a file");
	}

	@Override
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("resource", getFile());
		tsc.append("notifyOn", getNotifyOn());
		return tsc.toString();
	}
	
}
