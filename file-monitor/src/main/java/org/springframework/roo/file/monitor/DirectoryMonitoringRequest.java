package org.springframework.roo.file.monitor;

import java.io.File;
import java.util.Set;

import org.springframework.roo.file.monitor.event.FileOperation;
import org.springframework.roo.support.style.ToStringCreator;

/**
 * A request to monitor a particular directory.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class DirectoryMonitoringRequest extends MonitoringRequest {
	private boolean watchSubtree;
	
	public DirectoryMonitoringRequest(File file, boolean watchSubtree, Set<FileOperation> notifyOn) {
		super(file, notifyOn);
		// Assert.isTrue(file.isDirectory(), "File '" + file + "' must be a directory");
		this.watchSubtree = watchSubtree;
	}

	/**
	 * @return whether all files and folders under this directory should also be monitored (to an unlimited depth).
	 */
	public boolean isWatchSubtree() {
		return watchSubtree;
	}

	@Override
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("directory", getFile());
		tsc.append("watchSubtree", watchSubtree);
		tsc.append("notifyOn", getNotifyOn());
		return tsc.toString();
	}
}
