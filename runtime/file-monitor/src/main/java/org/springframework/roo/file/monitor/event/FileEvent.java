package org.springframework.roo.file.monitor.event;

import java.io.File;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Represents a file notification message.
 * <p>
 * There are three types of file event notifications:
 * <ul>
 * <li>An event with {@link FileOperation#MONITORING_START} when a file is first
 * detected on the disk and will be monitored.</li>
 * <li>An event with {@link FileOperation#MONITORING_FINISH} when a file that
 * has been monitored is no longer going to be monitored.</li>
 * <li>An event with any other {@link FileOperation} code when the file is
 * created, updated, deleted or (if available) renamed.</li>
 * </ul>
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class FileEvent {
    private final FileDetails fileDetails;
    private final FileOperation operation;
    private final File previousName;

    public FileEvent(final FileDetails fileDetails,
            final FileOperation operation, final File previousName) {
        Validate.notNull(fileDetails, "File details required");
        Validate.notNull(operation, "File operation required");
        this.fileDetails = fileDetails;
        this.operation = operation;
        this.previousName = previousName;
    }

    /**
     * @return the file that is subject of this event (never null).
     */
    public FileDetails getFileDetails() {
        return fileDetails;
    }

    /**
     * @return the file operation being performed (never null).
     */
    public FileOperation getOperation() {
        return operation;
    }

    /**
     * If supported by the implementation, indicates the old name of the
     * resource. Implementations are not required to support rename
     * notifications.
     * 
     * @return the old name of the file being {@link FileOperation#RENAMED}
     *         (will be null if not a rename notification).
     */
    public File getPreviousName() {
        return previousName;
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("fileDetails", fileDetails);
        builder.append("operation", operation);
        builder.append("previousName", previousName);
        return builder.toString();
    }

}
