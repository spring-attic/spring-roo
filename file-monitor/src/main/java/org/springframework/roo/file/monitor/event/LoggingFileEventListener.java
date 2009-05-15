package org.springframework.roo.file.monitor.event;

import java.util.logging.Logger;

import org.springframework.roo.support.util.Assert;

/**
 * Logs {@link FileEvent} notifications to the JDK logger.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class LoggingFileEventListener implements FileEventListener {

	private static final Logger logger = Logger.getLogger(LoggingFileEventListener.class.getName());
	
	public LoggingFileEventListener(FileEventPublisher fileEventPublisher) {
		Assert.notNull(fileEventPublisher, "File event publisher required");
		fileEventPublisher.addFileEventListener(this);
	}
	
	public void onFileEvent(FileEvent fileEvent) {
		logger.fine(fileEvent.toString());
	}

}
