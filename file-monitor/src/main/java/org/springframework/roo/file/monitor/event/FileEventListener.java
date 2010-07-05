package org.springframework.roo.file.monitor.event;

/**
 * Implemented by classes that wish to be notified of file system changes.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface FileEventListener {
	
	/**
	 * Invoked by a {@link FileEventPublisher} to report a new status.
	 * 
	 * @param fileEvent the file event (never null)
	 */
	void onFileEvent(FileEvent fileEvent);
}
