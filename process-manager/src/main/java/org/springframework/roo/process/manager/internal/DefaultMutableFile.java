package org.springframework.roo.process.manager.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.file.monitor.NotifiableFileMonitorService;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.support.util.FileUtils;

/**
 * Default implementation of {@link MutableFile}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class DefaultMutableFile implements MutableFile {

    private final File file;
    private final NotifiableFileMonitorService fileMonitorService;
    private final ManagedMessageRenderer managedMessageRenderer;

    public DefaultMutableFile(final File file,
            final NotifiableFileMonitorService fileMonitorService,
            final ManagedMessageRenderer managedMessageRenderer) {
        Validate.notNull(file, "File required");
        Validate.notNull(managedMessageRenderer, "Message renderer required");
        Validate.isTrue(file.isFile(),
                "A mutable file must actually be a file (not a directory)");
        Validate.isTrue(file.exists(), "A mutable file must actually exist");
        this.file = file;
        this.managedMessageRenderer = managedMessageRenderer;
        // null is permitted
        this.fileMonitorService = fileMonitorService;
    }

    public String getCanonicalPath() {
        return FileUtils.getCanonicalPath(file);
    }

    public InputStream getInputStream() {
        // Do more checks, in case the file has changed since this instance was
        // constructed
        Validate.isTrue(file.isFile(),
                "A mutable file must actually be a file (not a directory)");
        Validate.isTrue(file.exists(), "A mutable file must actually exist");
        try {
            return new BufferedInputStream(new FileInputStream(file));
        }
        catch (final IOException ioe) {
            throw new IllegalStateException(
                    "Unable to acquire input stream for file '"
                            + getCanonicalPath() + "'", ioe);
        }
    }

    public OutputStream getOutputStream() {
        // Do more checks, in case the file has changed since this instance was
        // constructed
        Validate.isTrue(file.isFile(),
                "A mutable file must actually be a file (not a directory)");
        Validate.isTrue(file.exists(), "A mutable file must actually exist");

        try {
            return new MonitoredOutputStream(file, managedMessageRenderer,
                    fileMonitorService);
        }
        catch (final IOException ioe) {
            throw new IllegalStateException(
                    "Unable to acquire output stream for file '"
                            + getCanonicalPath() + "'", ioe);
        }
    }

    public void setDescriptionOfChange(final String message) {
        managedMessageRenderer.setDescriptionOfChange(message);
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("file", getCanonicalPath());
        return builder.toString();
    }
}
