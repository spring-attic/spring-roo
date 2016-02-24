package org.springframework.roo.file.monitor;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.file.monitor.event.FileOperation;

/**
 * A request to monitor a particular directory.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class DirectoryMonitoringRequest extends MonitoringRequest {

    private final boolean watchSubtree;

    /**
     * Constructor that accepts a Collection of operations
     * 
     * @param directory the directory to monitor; must be an existing directory
     * @param watchSubtree whether to also monitor the sub-directories of the
     *            given directory
     * @param notifyOn the operations to notify upon (can't be empty)
     */
    public DirectoryMonitoringRequest(final File directory,
            final boolean watchSubtree, final Collection<FileOperation> notifyOn) {
        super(directory, notifyOn);
        Validate.isTrue(directory.isDirectory(),
                "File '%s' must be a directory", directory);
        this.watchSubtree = watchSubtree;
    }

    /**
     * Constructor that accepts an array of operations
     * 
     * @param directory the directory to monitor; must be an existing directory
     * @param watchSubtree whether to also monitor the sub-directories of the
     *            given directory
     * @param notifyOn the operations to notify upon (can't be empty)
     */
    public DirectoryMonitoringRequest(final File file,
            final boolean watchSubtree, final FileOperation... notifyOn) {
        this(file, watchSubtree, Arrays.asList(notifyOn));
    }

    /**
     * @return whether all files and folders under this directory should also be
     *         monitored (to an unlimited depth).
     */
    public boolean isWatchSubtree() {
        return watchSubtree;
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("directory", getFile());
        builder.append("watchSubtree", watchSubtree);
        builder.append("notifyOn", getNotifyOn());
        return builder.toString();
    }
}
