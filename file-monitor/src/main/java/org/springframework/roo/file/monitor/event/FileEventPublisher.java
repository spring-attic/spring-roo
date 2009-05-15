package org.springframework.roo.file.monitor.event;



/**
 * Implemented by classes that support the publication of file events.
 * 
 * <p>
 * Implementations are not required to provide any guarantees with respect to the order
 * in which notifications are delivered to listeners.
 * 
 * <p>
 * Implementations must permit modification of the listener list, even while delivering
 * event notifications to listeners. However, listeners do not receive any guarantee that
 * their addition or removal from the listener list will be effective or not for any event
 * notification that is currently proceeding.
 * 
 * <p>
 * Implementations must ensure that events are only delivered when an actual change has 
 * taken place.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface FileEventPublisher {
	
	/**
	 * Registers a new file event listener.
	 * 
	 * @param fileEventListener to register (cannot be null).
	 */
	void addFileEventListener(FileEventListener fileEventListener);
	
	/**
	 * Removes an existing file event listener.
	 * 
	 * <p>
	 * If the presented status listener is not found, the method returns without exception.
	 * 
	 * @param fileEventListener to remove (cannot be null).
	 */
	void removeFileEventListener(FileEventListener fileEventListener);
}
