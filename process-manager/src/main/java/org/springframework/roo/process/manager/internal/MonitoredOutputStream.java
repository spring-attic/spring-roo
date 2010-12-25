package org.springframework.roo.process.manager.internal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.roo.file.monitor.NotifiableFileMonitorService;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.HexUtils;

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
public class MonitoredOutputStream extends ByteArrayOutputStream {
	private File file;
	private NotifiableFileMonitorService fileMonitorService;
	private ManagedMessageRenderer managedMessageRenderer;
	private static MessageDigest sha = null;
	
	static {
		try {
			sha = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException ignored) {}
	}
	
	/**
	 * Constructs a {@link MonitoredOutputStream}.
	 * 
	 * @param file the file to output to (required)
	 * @param managedMessageRenderer a rendered for outputting a message once the output stream is closed (required)
	 * @param fileMonitorService an optional monitoring service (null is acceptable)
	 * @throws FileNotFoundException if the file cannot be found
	 */
	public MonitoredOutputStream(File file, ManagedMessageRenderer managedMessageRenderer, NotifiableFileMonitorService fileMonitorService) throws FileNotFoundException {
		Assert.notNull(file, "File required");
		Assert.notNull(managedMessageRenderer, "Message renderer required");
		this.file = file;
		this.fileMonitorService = fileMonitorService;
		this.managedMessageRenderer = managedMessageRenderer;
	}

	@Override
	public void close() throws IOException {
		// Obtain the bytes the user is writing out
		byte[] bytes = toByteArray();
		
		// Try to calculate the SHA hash code
		if (sha != null) {
			byte[] digest = sha.digest(bytes);
			this.managedMessageRenderer.setHashCode(HexUtils.toHex(digest));
		}
		
		// Log that we're writing the file
		this.managedMessageRenderer.logManagedMessage();
		
		// Write the actual file out to disk
		FileCopyUtils.copy(bytes, file);
		
		// Tell the FileMonitorService what happened
		String fileCanonicalPath;
		try {
			fileCanonicalPath = file.getCanonicalPath();
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
		if (fileMonitorService != null) {
			fileMonitorService.notifyChanged(fileCanonicalPath);
		}
	}
	
}
