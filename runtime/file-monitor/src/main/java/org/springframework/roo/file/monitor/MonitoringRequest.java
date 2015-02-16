package org.springframework.roo.file.monitor;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.file.monitor.event.FileOperation;

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
     * @param resource the resource to monitor; <code>null</code> means the
     *            current directory
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
     * @param directory the directory to monitor; <code>null</code> means the
     *            current directory
     * @return a non-<code>null</code> monitoring request
     */
    public static MonitoringRequest getInitialSubTreeMonitoringRequest(
            String directory) {
        if (directory == null) {
            directory = ".";
        }
        final MonitoringRequestEditor mre = new MonitoringRequestEditor();
        mre.setAsText(directory + ",CRUD,**");
        return mre.getValue();
    }

    private final Collection<FileOperation> notifyOn;
    private final File resource;

    /**
     * Constructor
     * 
     * @param resource the file to monitor (required)
     * @param notifyOn the file operations to notify upon (can't be empty)
     */
    protected MonitoringRequest(final File resource,
            final Collection<FileOperation> notifyOn) {
        Validate.notNull(resource, "Resource to monitor is required");
        Validate.notEmpty(notifyOn,
                "At least one FileOperation to monitor must be specified");
        this.notifyOn = new HashSet<FileOperation>(notifyOn);
        this.resource = resource;
    }

    /**
     * @return the file to be monitored (never null)
     */
    public File getFile() {
        return resource;
    }

    /**
     * Returns the operations to be monitored
     * 
     * @return an unmodifiable collection containing one or more elements
     */
    public Collection<FileOperation> getNotifyOn() {
        return Collections.unmodifiableCollection(notifyOn);
    }
}
