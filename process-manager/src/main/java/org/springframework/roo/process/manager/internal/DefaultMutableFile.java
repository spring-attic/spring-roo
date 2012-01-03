package org.springframework.roo.process.manager.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.springframework.roo.file.monitor.NotifiableFileMonitorService;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileUtils;

/**
 * Default implementation of {@link MutableFile}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class DefaultMutableFile implements MutableFile {

    // Fields
    private final File file;
    private final NotifiableFileMonitorService fileMonitorService;
    private final ManagedMessageRenderer managedMessageRenderer;

    public void setDescriptionOfChange(final String message) {
        this.managedMessageRenderer.setDescriptionOfChange(message);
    }

    public DefaultMutableFile(final File file,
            final NotifiableFileMonitorService fileMonitorService,
            final ManagedMessageRenderer managedMessageRenderer) {
        Assert.notNull(file, "File required");
        Assert.notNull(managedMessageRenderer, "Message renderer required");
        Assert.isTrue(file.isFile(),
                "A mutable file must actually be a file (not a directory)");
        Assert.isTrue(file.exists(), "A mutable file must actually exist");
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
        Assert.isTrue(file.isFile(),
                "A mutable file must actually be a file (not a directory)");
        Assert.isTrue(file.exists(), "A mutable file must actually exist");
        try {
            return new BufferedInputStream(new FileInputStream(file));
        }
        catch (IOException ioe) {
            throw new IllegalStateException(
                    "Unable to acquire input stream for file '"
                            + getCanonicalPath() + "'", ioe);
        }
    }

    public OutputStream getOutputStream() {
        // Do more checks, in case the file has changed since this instance was
        // constructed
        Assert.isTrue(file.isFile(),
                "A mutable file must actually be a file (not a directory)");
        Assert.isTrue(file.exists(), "A mutable file must actually exist");

        try {
            return new MonitoredOutputStream(file, managedMessageRenderer,
                    fileMonitorService);
        }
        catch (IOException ioe) {
            throw new IllegalStateException(
                    "Unable to acquire output stream for file '"
                            + getCanonicalPath() + "'", ioe);
        }
    }

    @Override
    public String toString() {
        ToStringCreator tsc = new ToStringCreator(this);
        tsc.append("file", getCanonicalPath());
        return tsc.toString();
    }
}
