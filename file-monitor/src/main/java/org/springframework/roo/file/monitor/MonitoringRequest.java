package org.springframework.roo.file.monitor;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import org.springframework.roo.file.monitor.event.FileOperation;
import org.springframework.roo.support.util.Assert;

/**
 * Represents a request to monitor a particular file system location.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public abstract class MonitoringRequest {
	private File resource;
	private Set<FileOperation> notifyOn;

	public MonitoringRequest(File resource, Set<FileOperation> notifyOn) {
		Assert.notNull(resource, "File to monitor is required");
		//Assert.isTrue(resource.exists(), "File must exist");
		Assert.notEmpty(notifyOn, "At least one FileOperation to monitor must be specified");
		this.resource = resource;
		this.notifyOn = notifyOn;
	}

	/**
	 * @return the file to be monitored (never null)
	 */
	public File getFile() {
		return resource;
	}

	/**
	 * @return an unmodifiable set containing one or more operations to be monitored (never null or empty)
	 */
	public Set<FileOperation> getNotifyOn() {
		return Collections.unmodifiableSet(notifyOn);
	}

	public static MonitoringRequest getInitialMonitoringRequest(String workingDir) {
		if (workingDir == null) {
			workingDir = ".";
		}
		MonitoringRequestEditor mre = new MonitoringRequestEditor();
		mre.setAsText(workingDir + ",CRUD");
		return (MonitoringRequest) mre.getValue();
	}
}
