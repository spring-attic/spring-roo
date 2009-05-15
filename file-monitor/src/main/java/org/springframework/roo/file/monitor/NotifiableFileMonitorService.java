package org.springframework.roo.file.monitor;

import org.springframework.roo.file.monitor.event.FileEventListener;
import org.springframework.roo.file.monitor.event.FileOperation;

/**
 * A {@link FileMonitorService} that permits callers to explicitly indicate they have
 * changed a specific file. These files are guaranteed to be included in a change notification
 * when the next {@link FileMonitorService#scanAll()} is performed. The change notification will
 * always be an {@link FileOperation#UPDATED} notification, and the timestamp will be equal to the
 * actual timestamp on disk.
 * 
 * <p>
 * This interface works around the practical problem that many file systems only provide
 * precision to a whole second for file update operations. This precludes polling-based implementations
 * (which rely on last update time) from identifying changes. The downside is this interface must be
 * used by any type that can rapidly modify the file system (ie make more than one change per second).
 * Failure to do so will mean some files can be updated in the same whole second but not be detected as
 * updated.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface NotifiableFileMonitorService extends FileMonitorService {
	
	/**
	 * Indicates the canonical path specified should be treated as if it had changed.
	 * The last update time will become equal to actual disk timestamp.
	 * 
	 * <p>
	 * The implementation must only present the indicated file once in a given
	 * {@link FileMonitorService#scanAll()} invocation, even if this method has been repeatedly
	 * called and/or the file was detected as changed using normal last updated timestamps.
	 * 
	 * <p>
	 * No attempt is made to verify whether the presented path is subject to monitoring or not.
	 * It is expected that {@link FileEventListener}s will ignore files they are not interested in.
	 * 
	 * @param fileCanonicalPath required (not null)
	 */
	void notifyChanged(String fileCanonicalPath);
}
