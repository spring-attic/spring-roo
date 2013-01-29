package org.springframework.roo.file.monitor;

import java.io.File;
import java.util.Collection;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.file.monitor.event.FileOperation;

/**
 * A request to monitor a particular file.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class FileMonitoringRequest extends MonitoringRequest {

    public FileMonitoringRequest(final File file,
            final Collection<FileOperation> notifyOn) {
        super(file, notifyOn);
        Validate.isTrue(file.isFile(), "File '%s' must be a file", file);
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("resource", getFile());
        builder.append("notifyOn", getNotifyOn());
        return builder.toString();
    }
}
