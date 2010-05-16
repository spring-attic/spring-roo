package org.springframework.roo.process.manager.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.springframework.roo.file.monitor.NotifiableFileMonitorService;
import org.springframework.roo.support.util.Assert;

/**
 * Ensures the {@link NotifiableFileMonitorService#notifyChanged(String)} method is invoked when 
 * {@link #close()} is called.
 * 
 * <p>
 * This is useful for ensuring the file monitoring system is notified of all changed files, even
 * those which are changed very rapidly on disk and would not normally be detected using the
 * file system's "last updated" timestamps. 
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class MonitoredOutputStream extends FileOutputStream {

	private String fileCanonicalPath;
	private NotifiableFileMonitorService fileMonitorService;
	private ManagedMessageRenderer managedMessageRenderer;
	private boolean logged = false;
	
	/**
	 * Constructs a {@link MonitoredOutputStream}.
	 * 
	 * @param file the file to output to (required)
	 * @param managedMessageRenderer a rendered for outputting a message once the output stream is closed (required)
	 * @param fileMonitorService an optional monitoring service (null is acceptable)
	 * @throws FileNotFoundException if the file cannot be found
	 */
	public MonitoredOutputStream(File file, ManagedMessageRenderer managedMessageRenderer, NotifiableFileMonitorService fileMonitorService) throws FileNotFoundException {
		super(file);
		Assert.notNull(file, "File required");
		Assert.notNull(managedMessageRenderer, "Message renderer required");
		try {
			this.fileCanonicalPath = file.getCanonicalPath();
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
		this.fileMonitorService = fileMonitorService;
		this.managedMessageRenderer = managedMessageRenderer;
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (!logged) logNow();
		super.write(b, off, len);
	}

	@Override
	public void write(byte[] b) throws IOException {
		if (!logged) logNow();
		super.write(b);
	}

	@Override
	public void write(int b) throws IOException {
		if (!logged) logNow();
		super.write(b);
	}
	
	private void logNow() {
		logged = true;
		this.managedMessageRenderer.logManagedMessage();
	}

	@Override
	public void close() throws IOException {
		super.close();
		if (fileMonitorService != null) {
			fileMonitorService.notifyChanged(fileCanonicalPath);
		}
	}
	
}
