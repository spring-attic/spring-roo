package org.springframework.roo.project;

import static org.springframework.roo.file.monitor.event.FileOperation.CREATED;
import static org.springframework.roo.file.monitor.event.FileOperation.DELETED;
import static org.springframework.roo.file.monitor.event.FileOperation.MONITORING_FINISH;
import static org.springframework.roo.file.monitor.event.FileOperation.MONITORING_START;
import static org.springframework.roo.file.monitor.event.FileOperation.RENAMED;
import static org.springframework.roo.file.monitor.event.FileOperation.UPDATED;

import java.io.File;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.file.monitor.DirectoryMonitoringRequest;
import org.springframework.roo.file.monitor.MonitoringRequest;
import org.springframework.roo.file.monitor.NotifiableFileMonitorService;
import org.springframework.roo.file.monitor.event.FileOperation;
import org.springframework.roo.file.undo.CreateDirectory;
import org.springframework.roo.file.undo.FilenameResolver;
import org.springframework.roo.file.undo.UndoManager;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataNotificationListener;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

@Component(immediate = true)
@Service
public class ProjectPathMonitoringInitializer implements MetadataNotificationListener {
	
	// Constants
	private static final FileOperation[] MONITORED_OPERATIONS = { MONITORING_START, MONITORING_FINISH, CREATED, RENAMED, UPDATED, DELETED };

	// Fields
	@Reference private FilenameResolver filenameResolver;
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference private NotifiableFileMonitorService fileMonitorService;
	@Reference private PathResolver pathResolver;
	@Reference private UndoManager undoManager;
	private boolean pathsRegistered;

	protected void activate(final ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
	}

	protected void deactivate(final ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
	}

	public void notify(final String upstreamDependency, final String downstreamDependency) {
		if (pathsRegistered) {
			return;
		}
		// We assume the project's root directory is already monitored by ProcessManager
		monitorNonRootPaths();
		pathsRegistered = true;
	}
	
	private void monitorNonRootPaths() {
		for (final LogicalPath logicalPath : pathResolver.getPaths()) {
			if (!logicalPath.isProjectRoot()) {
				final String canonicalPath = pathResolver.getRoot(logicalPath);
				// The path can be blank if a sub-folder contains a POM that doesn't belong to a module
				if (StringUtils.hasText(canonicalPath)) {
					final File directory = ensureDirectoryExists(canonicalPath);
					final MonitoringRequest request = new DirectoryMonitoringRequest(directory, true, MONITORED_OPERATIONS);
					new UndoableMonitoringRequest(undoManager, fileMonitorService, request, true);
				}
			}
		}
	}

	private File ensureDirectoryExists(final String directoryPath) {
		final File file = new File(directoryPath);
		if (file.exists()) {
			Assert.isTrue(file.isDirectory(), "Path '" + directoryPath + "' exists but is not a directory");
		} else {
			// Create the directory, but no notifications, as that will happen once the caller starts monitoring it
			new CreateDirectory(undoManager, filenameResolver, file);
		}
		return file;
	}
}
