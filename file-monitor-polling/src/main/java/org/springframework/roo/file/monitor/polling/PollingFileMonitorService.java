package org.springframework.roo.file.monitor.polling;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

import org.springframework.roo.file.monitor.DirectoryMonitoringRequest;
import org.springframework.roo.file.monitor.FileMonitorService;
import org.springframework.roo.file.monitor.MonitoringRequest;
import org.springframework.roo.file.monitor.NotifiableFileMonitorService;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.file.monitor.event.FileEvent;
import org.springframework.roo.file.monitor.event.FileEventListener;
import org.springframework.roo.file.monitor.event.FileOperation;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;

/**
 * A simple polling-based {@link FileMonitorService}.
 * 
 * <p>
 * This implementation iterates over each of the {@link MonitoringRequest} instances,
 * building an active file index at the time of execution. It then compares this active file
 * index with the last time it was executed for that particular {@link MonitoringRequest}.
 * Events are then fired, and only when the event firing process has completed is the next
 * {@link MonitoringRequest} examined.
 * 
 * <p>
 * This implementation does not recognize {@link FileOperation#RENAMED} events. 
 * 
 * <p>
 * In the case of {@link FileOperation#DELETED} events, this implementation will present in the
 * {@link FileEvent#getLastModified()} times equal to the last time a deleted file was
 * modified. The time does NOT represent the deletion time nor the time the deletion was first
 * detected.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public class PollingFileMonitorService implements NotifiableFileMonitorService {

	private static final Logger logger = Logger.getLogger(PollingFileMonitorService.class.getName());
	
	protected Set<FileEventListener> fileEventListeners = new CopyOnWriteArraySet<FileEventListener>();
	private Set<MonitoringRequest> requests = Collections.synchronizedSet(new CopyOnWriteArraySet<MonitoringRequest>());
	private Map<MonitoringRequest,Map<File,Long>> priorExecution = Collections.synchronizedMap(new WeakHashMap<MonitoringRequest,Map<File,Long>>());
	private Set<String> notifyChanged = Collections.synchronizedSet(new HashSet<String>());
	
	public List<FileDetails> getMonitored() {
		List<FileDetails> monitored = new ArrayList<FileDetails>();
		if (requests.size() == 0) {
			return monitored;
		}

		synchronized (requests) {
			for (MonitoringRequest request : requests) {
				synchronized (priorExecution) {
					if (priorExecution.containsKey(request)) {
						Map<File,Long> priorFiles = priorExecution.get(request);
						for (File priorFile : priorFiles.keySet()) {
							monitored.add(new FileDetails(priorFile, priorFiles.get(priorFile)));
						}
					}
				}
			}
		}
		
		return monitored;
	}

	public boolean isDirty() {
		return notifyChanged.size() > 0;
	}
	
	public int scanAll() {
		if (requests.size() == 0) {
			return 0;
		}

		int changes = 0;
		synchronized (notifyChanged) {
			
			synchronized (requests) {
				for (MonitoringRequest request : requests) {

					boolean includeSubtree = false;
					if (request instanceof DirectoryMonitoringRequest) {
						includeSubtree = ((DirectoryMonitoringRequest)request).isWatchSubtree();
					}
					
					if (!request.getFile().exists()) {
						logger.warning("Cannot monitor non-existent path '" + request.getFile() + "'");
						continue;
					}
					
					// Build contents of the monitored location
					Map<File,Long> currentExecution = new HashMap<File,Long>();
					computeEntries(currentExecution, request.getFile(), includeSubtree);
					
					List<FileEvent> eventsToPublish = new ArrayList<FileEvent>();

					synchronized (priorExecution) {
						if (priorExecution.containsKey(request)) {
							// Need to perform a comparison, as we have data from a previous execution
							Map<File,Long> priorFiles = priorExecution.get(request);
							
							// Locate created and modified files
							for (File thisFile : currentExecution.keySet()) {
								
								if (!priorFiles.containsKey(thisFile)) {
									// This file did not exist last execution, so it must be new
									eventsToPublish.add(new FileEvent(new FileDetails(thisFile, currentExecution.get(thisFile)), FileOperation.CREATED, null));
									continue;
								}
								
								Long currentTimestamp = currentExecution.get(thisFile);
								Long previousTimestamp = priorFiles.get(thisFile);
								if (!currentTimestamp.equals(previousTimestamp)) {
									// Modified
									eventsToPublish.add(new FileEvent(new FileDetails(thisFile, currentExecution.get(thisFile)), FileOperation.UPDATED, null));
									try {
										// If this file was already going to be notified, there is no need to do it twice
										notifyChanged.remove(thisFile.getCanonicalPath());
									} catch (IOException ignored) {}
									continue;
								}
							}
							
							// Now locate deleted files
							priorFiles.keySet().removeAll(currentExecution.keySet());
							for (File deletedFile : priorFiles.keySet()) {
								eventsToPublish.add(new FileEvent(new FileDetails(deletedFile, priorFiles.get(deletedFile)), FileOperation.DELETED, null));
								continue;
							}
						} else {
							// No data from previous execution, so it's a newly-monitored location
							for (File thisFile : currentExecution.keySet()) {
								eventsToPublish.add(new FileEvent(new FileDetails(thisFile, currentExecution.get(thisFile)), FileOperation.MONITORING_START, null));
							}
						}
						
						// Record the monitored location's contents, ready for next execution
						priorExecution.put(request, currentExecution);
					}
					
					for (String canonicalPath : notifyChanged) {
						File file = new File(canonicalPath);
						eventsToPublish.add(new FileEvent(new FileDetails(file, file.lastModified()), FileOperation.UPDATED, null));
					}
					notifyChanged.clear();
					
					publish(eventsToPublish);
					
					changes += eventsToPublish.size();
				}
			}
		}
		
		return changes;
	}

	/**
	 * Publish the events, if needed
	 * 
	 * @param eventsToPublish to publish (not null, but can be empty)
	 */
	private void publish(List<FileEvent> eventsToPublish) {
		if (eventsToPublish.size() > 0) {
			for (FileEvent event : eventsToPublish) {
				for (FileEventListener listener : fileEventListeners) {
					listener.onFileEvent(event);
				}
			}
		}
	}
	
	/**
	 * Adds one or more entries into the Map. The key of the Map is the File object, and the value
	 * is the {@link File#lastModified()} time.
	 * 
	 * <p>
	 * Specifically:
	 * 
	 * <ul>
	 * <li>If invoked with a File that is actually a File, only the file is added.</li>
	 * <li>If invoked with a File that is actually a Directory, all files and directories are added.</li>
	 * <li>If invoked with a File that is actually a Directory, subdirectories will be added only if 
	 * "includeSubtree" is true.</li>
	 * </ul>
	 */
	private void computeEntries(Map<File, Long> map, File currentFile, boolean includeSubtree) {
		Assert.notNull(map, "Map required");
		Assert.notNull(currentFile, "Current file is required");

		if (currentFile.exists() == false) {
			return;
		}
		
		map.put(currentFile, currentFile.lastModified());

		if (currentFile.isDirectory()) {
			File[] files = currentFile.listFiles();
			if (files == null || files.length == 0) return;
			for (File file : files) {
				if (file.isFile() || includeSubtree) {
					computeEntries(map, file, includeSubtree);
				}
			}
		}
	}

	public boolean add(MonitoringRequest request) {
		Assert.notNull(request, "MonitoringRequest required");
		boolean result;
		synchronized (requests) {
			result = requests.add(request);
		}
		
		if (result) {
			scanAll();
		}
		
		return result;
	}

	public boolean remove(MonitoringRequest request) {
		Assert.notNull(request, "MonitoringRequest required");
		
		synchronized(priorExecution) {
			// Advise of the cessation to monitoring
			if (priorExecution.containsKey(request)) {
				List<FileEvent> eventsToPublish = new ArrayList<FileEvent>();

				Map<File,Long> priorFiles = priorExecution.get(request);
				for (File thisFile : priorFiles.keySet()) {
					eventsToPublish.add(new FileEvent(new FileDetails(thisFile, priorFiles.get(thisFile)), FileOperation.MONITORING_FINISH, null));
				}

				publish(eventsToPublish);
			}

			priorExecution.remove(request);
		}
		
		boolean result;
		synchronized (requests) {
			result = requests.remove(request);
		}
		
		if (result) {
			scanAll();
		}
		
		return result;
		
	}

	public void addFileEventListener(FileEventListener fileEventListener) {
		Assert.notNull(fileEventListener, "File event listener required");
		fileEventListeners.add(fileEventListener);
	}

	public void removeFileEventListener(FileEventListener fileEventListener) {
		Assert.notNull(fileEventListener, "File event listener required");
		fileEventListeners.remove(fileEventListener);
	}

	public SortedSet<FileDetails> findMatchingAntPath(String antPath) {
		Assert.hasText(antPath, "Ant path required");

		SortedSet<FileDetails> result = new TreeSet<FileDetails>();
		
		if (requests.size() == 0) {
			return result;
		}

		synchronized (requests) {
			for (MonitoringRequest request : requests) {
				synchronized (priorExecution) {
					if (priorExecution.containsKey(request)) {
						Map<File,Long> priorFiles = priorExecution.get(request);
						for (File priorFile : priorFiles.keySet()) {
							FileDetails fd = new FileDetails(priorFile, priorFiles.get(priorFile));
							if (fd.matchesAntPath(antPath)) {
								result.add(fd);
							}
						}
					}
				}
			}
		}
		
		return result;
	}

	public void notifyChanged(String fileCanoncialPath) {
		synchronized (notifyChanged) {
			notifyChanged.add(fileCanoncialPath);
		}
	}
}
