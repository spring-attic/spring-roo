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
	
	/**
	 * Factory method for monitoring the following operations upon the given
	 * file or directory:
	 * <ul>
	 * <li>{@link FileOperation#CREATED}</li>
	 * <li>{@link FileOperation#RENAMED}</li>
	 * <li>{@link FileOperation#UPDATED}</li>
	 * <li>{@link FileOperation#DELETED}</li>
	 * </ul>
	 * 
	 * @param resource the resource to monitor; <code>null</code> means the current directory
	 * @return a non-<code>null</code> monitoring request
	 */
	public static MonitoringRequest getInitialMonitoringRequest(String resource) {
		if (resource == null) {
			resource = ".";
		}
		final MonitoringRequestEditor mre = new MonitoringRequestEditor();
		mre.setAsText(resource + ",CRUD");
		return mre.getValue();
	}
	
	/**
	 * Factory method for monitoring the following operations upon the given
	 * directory and its sub-tree:
	 * <ul>
	 * <li>{@link FileOperation#CREATED}</li>
	 * <li>{@link FileOperation#RENAMED}</li>
	 * <li>{@link FileOperation#UPDATED}</li>
	 * <li>{@link FileOperation#DELETED}</li>
	 * </ul>
	 * 
	 * @param directory the directory to monitor; <code>null</code> means the current directory
	 * @return a non-<code>null</code> monitoring request
	 */
	public static MonitoringRequest getInitialSubTreeMonitoringRequest(String directory) {
		if (directory == null) {
			directory = ".";
		}
		final MonitoringRequestEditor mre = new MonitoringRequestEditor();
		mre.setAsText(directory + ",CRUD,**");
		return mre.getValue();
	}

	// Fields
	private final File resource;
	private final Set<FileOperation> notifyOn;

	/**
	 * Constructor
	 *
	 * @param resource the file to monitor (required)
	 * @param notifyOn the file operations to notify upon (can't be empty)
	 */
	protected MonitoringRequest(final File resource, final Set<FileOperation> notifyOn) {
		Assert.notNull(resource, "Resource to monitor is required");
		Assert.notEmpty(notifyOn, "At least one FileOperation to monitor must be specified");
		this.notifyOn = notifyOn;
		this.resource = resource;
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
}
