package org.springframework.roo.process.manager.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.springframework.roo.file.monitor.NotifiableFileMonitorService;

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
	
	/**
	 * Constructs a {@link MonitoredOutputStream}.
	 * 
	 * @param file the file to output to (required)
	 * @param fileMonitorService an optional monitoring service (null is acceptable)
	 * @throws FileNotFoundException if the file cannot be found
	 */
	public MonitoredOutputStream(File file, NotifiableFileMonitorService fileMonitorService) throws FileNotFoundException {
		super(file);
		try {
			this.fileCanonicalPath = file.getCanonicalPath();
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
		this.fileMonitorService = fileMonitorService;
	}
	
	@Override
	public void close() throws IOException {
		super.close();
		if (fileMonitorService != null) {
			fileMonitorService.notifyChanged(fileCanonicalPath);
		}
	}
	
}
