package org.springframework.roo.file.monitor;

import org.springframework.roo.file.monitor.event.FileEventListener;

/**
 * A {@link FileMonitorService} that permits callers to explicitly indicate they have
 * changed a specific file. These files are guaranteed to be included in a change notification
 * when the next {@link FileMonitorService#scanAll()} or {@link #scanNotified()} is called.
 * 
 * <p>
 * This interface works around the practical problem that many file systems only provide
 * precision to a whole second for file update operations. This precludes polling-based implementations
 * (which rely on last update time) from identifying changes. The downside is this interface must be
 * used by any type that can rapidly modify the file system (ie make more than one change per second).
 * Failure to do so will mean some files can be updated in the same whole second but not be detected as
 * updated.
 * 
 * <p>
 * This interface also exists so there are lightweight methods available for explicitly recording
 * disk changes and then publishing those changes without requiring a full scan.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface NotifiableFileMonitorService extends FileMonitorService {
	
	/**
	 * Similar to {@link #scanAll()} except will only notify those files explicitly advised
	 * via notification methods on {@link NotifiableFileMonitorService}. This is designed to allow
	 * faster operation where a full disk scan (as would be provided by {@link #scanAll()} is unnecessary.
	 * 
	 * <p>
	 * Note that executing this method will result in change notifications 
	 * 
	 * @return the number of changes detected during this invocation (can be 0 or above)
	 */
	int scanNotified();
	
	/**
	 * Indicates the canonical path specified should be treated as if it had changed.
	 * The last update time will become equal to actual disk timestamp.
	 * 
	 * <p>
	 * The implementation must only present the indicated file once in a given
	 * {@link FileMonitorService#scanAll()} or {@link #scanNotified()} invocation, even if this
	 * method has been repeatedly called and/or the file was detected as changed using normal
	 * last updated timestamps.
	 * 
	 * <p>
	 * No attempt is made to verify whether the presented path is subject to monitoring or not.
	 * It is expected that {@link FileEventListener}s will ignore files they are not interested in.
	 * 
	 * @param fileCanonicalPath required (not null)
	 */
	void notifyChanged(String fileCanonicalPath);
	
	void notifyDeleted(String fileCanonicalPath);
	
	void notifyCreated(String fileCanonicalPath);
	
}
