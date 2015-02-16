package org.springframework.roo.file.monitor.event;

/**
 * Represents the type of operations possible on a file or directory.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public enum FileOperation {
    /**
     * Represents a file or directory creation event.
     */
    CREATED,

    /**
     * Represents a file or directory deletion event. Guaranteed to be fired
     * before any {@link #MONITORING_FINISH}.
     */
    DELETED,

    /**
     * Represents a file that will no longer be monitored. This usually follows
     * a removal request, or a deletion. Once fired, a {@link #MONITORING_START}
     * will be fired before any other {@link FileOperation} status codes for
     * that same file (for example, if the file is re-monitored or re-created).
     */
    MONITORING_FINISH,

    /**
     * Represents a file that has been initially detected on the file system and
     * will be monitored. Guaranteed to be fired before any other
     * {@link FileOperation} status code.
     */
    MONITORING_START,

    /**
     * Represents a file or directory rename event; note this may not be
     * available on certain implementations (in which case a DELETED and CREATED
     * event would be issued instead).
     */
    RENAMED,

    /**
     * Represents a file or directory modification event.
     */
    UPDATED,
}
