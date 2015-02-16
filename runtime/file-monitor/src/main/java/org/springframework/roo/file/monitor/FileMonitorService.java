package org.springframework.roo.file.monitor;

import java.util.Collection;
import java.util.List;
import java.util.SortedSet;

import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.file.monitor.event.FileEventListener;

/**
 * Provides a mechanism to monitor disk locations and publish events when those
 * disk locations change.
 * <p>
 * Implementations are required to monitor the locations expressed via the
 * methods on this interface. The mechanism used for monitoring is an
 * implementation choice. The order in which notifications must be published is
 * unspecified.
 * <p>
 * This API is provided as an interim measure until <a
 * href="http://jcp.org/en/jsr/detail?id=203">JSR 203</a> (to be included in
 * Java Standard Edition, version 7 AKA "Dolphin") is widely available. Several
 * useful prior works in the area of file monitoring includes <a
 * href="http://jnotify.sourceforge.net/">JNotify</a> and <a
 * href="http://mark.heily.com/pnotify/">PNotify</a>.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface FileMonitorService {

    /**
     * @param request a monitoring request
     * @return true if the monitor did not already contain the specified request
     */
    boolean add(MonitoringRequest request);

    /**
     * Locates all {@link FileDetails} which match the presented Ant path.
     * 
     * @param antPath the Ant path to evaluate, as per the canonical file path
     *            format (required)
     * @return all matching identifiers (may be empty, but never null)
     */
    public SortedSet<FileDetails> findMatchingAntPath(String antPath);

    /**
     * Provides a list of canonical paths which represent changes to the file
     * system since the requesting class last requested the change set. The
     * returned change set is relative the requesting class in order to provide
     * a list of changes to a number of callers, instead of a point of time
     * snapshot which would change depending on the order invocation. This
     * method should provide a more robust implementation of {@link #isDirty()}
     * as the change set isn't cleared when a scan is performed and greater
     * insight is given into what has changed instead of just indicating if the
     * filesystem is dirty.
     * 
     * @param requestingClass the invoking class (required)
     * @return file system changes that occurred since the last invocation by
     *         the requesting class (may be empty, but never null)
     */
    Collection<String> getDirtyFiles(String requestingClass);

    /**
     * Indicates the files currently being monitored, which is potentially
     * useful for newly-registered {@link FileEventListener} instances that may
     * have missed previous events.
     * 
     * @return every file currently being monitored (never null, but may be
     *         empty if there are no files being monitored)
     */
    List<FileDetails> getMonitored();

    /**
     * Indicates on a best-efforts basis whether there are known changes to the
     * disk which would be reported should {@link #scanAll()} be invoked. This
     * method is not required to return a guaranteed outcome of what will happen
     * should {@link #scanAll()} be invoked, but callers may rely on this method
     * to assist with optimisations where applicable.
     * 
     * @return true if there are known changes to be notified during the next
     *         {@link #scanAll}
     */
    boolean isDirty();

    /**
     * @param request a monitoring request
     * @return true if this set contained the specified element
     */
    boolean remove(MonitoringRequest request);

    /**
     * Execute a scan of all monitored locations.
     * 
     * @return the number of changes detected during this invocation (can be 0
     *         or above)
     */
    int scanAll();
}
