package org.springframework.roo.process.manager.internal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.file.monitor.NotifiableFileMonitorService;

/**
 * Ensures the {@link NotifiableFileMonitorService#notifyChanged(String)} method
 * is invoked when {@link #close()} is called.
 * <p>
 * This is useful for ensuring the file monitoring system is notified of all
 * changed files, even those which are changed very rapidly on disk and would
 * not normally be detected using the file system's "last updated" timestamps.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class MonitoredOutputStream extends ByteArrayOutputStream {

    private final File file;
    private final NotifiableFileMonitorService fileMonitorService;

    private final ManagedMessageRenderer managedMessageRenderer;

    /**
     * Constructs a {@link MonitoredOutputStream}.
     * 
     * @param file the file to output to (required)
     * @param managedMessageRenderer a rendered for outputting a message once
     *            the output stream is closed (required)
     * @param fileMonitorService an optional monitoring service (null is
     *            acceptable)
     * @throws FileNotFoundException if the file cannot be found
     */
    public MonitoredOutputStream(final File file,
            final ManagedMessageRenderer managedMessageRenderer,
            final NotifiableFileMonitorService fileMonitorService)
            throws FileNotFoundException {
        Validate.notNull(file, "File required");
        Validate.notNull(managedMessageRenderer, "Message renderer required");
        this.file = file;
        this.fileMonitorService = fileMonitorService;
        this.managedMessageRenderer = managedMessageRenderer;
    }

    @Override
    public void close() throws IOException {
        // Obtain the bytes the user is writing out
        final byte[] bytes = toByteArray();

        // Try to calculate the SHA hash code
        managedMessageRenderer.setHashCode(DigestUtils.shaHex(bytes));

        // Log that we're writing the file
        managedMessageRenderer.logManagedMessage();

        // Write the actual file out to disk
        FileUtils.writeByteArrayToFile(file, bytes);

        // Tell the FileMonitorService what happened
        String fileCanonicalPath;
        try {
            fileCanonicalPath = file.getCanonicalPath();
        }
        catch (final IOException ioe) {
            throw new IllegalStateException(ioe);
        }
        if (fileMonitorService != null) {
            fileMonitorService.notifyChanged(fileCanonicalPath);
        }
    }
}
