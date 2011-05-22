package org.springframework.roo.process.manager.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.springframework.roo.file.monitor.NotifiableFileMonitorService;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Default implementation of {@link MutableFile}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class DefaultMutableFile implements MutableFile {
	private File file;
	private NotifiableFileMonitorService fileMonitorService;
	private ManagedMessageRenderer managedMessageRenderer;
	
	public void setDescriptionOfChange(String message) {
		this.managedMessageRenderer.setDescriptionOfChange(message);
	}

	public DefaultMutableFile(File file, NotifiableFileMonitorService fileMonitorService, ManagedMessageRenderer managedMessageRenderer) {
		Assert.notNull(file, "File required");
		Assert.notNull(managedMessageRenderer, "Message renderer required");
		Assert.isTrue(file.isFile(), "A mutable file must actually be a file (not a directory)");
		Assert.isTrue(file.exists(), "A mutable file must actually exist");
		this.file = file;
		this.managedMessageRenderer = managedMessageRenderer;
		// null is permitted
		this.fileMonitorService = fileMonitorService;
	}

	public String getCanonicalPath() {
		return FileDetails.getCanonicalPath(file);
	}

	public InputStream getInputStream() {
		// Do more checks, in case the file has changed since this instance was constructed
		Assert.isTrue(file.isFile(), "A mutable file must actually be a file (not a directory)");
		Assert.isTrue(file.exists(), "A mutable file must actually exist");
		try {
			return new BufferedInputStream(new FileInputStream(file));
		} catch (IOException ioe) {
			throw new IllegalStateException("Unable to acquire input stream for file '" + getCanonicalPath() + "'", ioe);
		}
	}

	public OutputStream getOutputStream() {
		// Do more checks, in case the file has changed since this instance was constructed
		Assert.isTrue(file.isFile(), "A mutable file must actually be a file (not a directory)");
		Assert.isTrue(file.exists(), "A mutable file must actually exist");

		try {
			return new MonitoredOutputStream(file, managedMessageRenderer, fileMonitorService);
		} catch (IOException ioe) {
			throw new IllegalStateException("Unable to acquire output stream for file '" + getCanonicalPath() + "'", ioe);
		}
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("file", getCanonicalPath());
		return tsc.toString();
	}
	
	
}
