package org.springframework.roo.project;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

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

@Component(immediate = true)
@Service
public class ProjectPathMonitoringInitializer implements MetadataNotificationListener {
	
	// Fields
	@Reference private FilenameResolver filenameResolver;
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference private NotifiableFileMonitorService fileMonitorService;
	@Reference private UndoManager undoManager;
	@Reference private PathResolver pathResolver;
	private boolean pathsRegistered = false;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
	}

	public void notify(String upstreamDependency, String downstreamDependency) {
		if (pathsRegistered) {
			return;
		}

		Set<FileOperation> notifyOn = new LinkedHashSet<FileOperation>();
		notifyOn.add(FileOperation.MONITORING_START);
		notifyOn.add(FileOperation.MONITORING_FINISH);
		notifyOn.add(FileOperation.CREATED);
		notifyOn.add(FileOperation.RENAMED);
		notifyOn.add(FileOperation.UPDATED);
		notifyOn.add(FileOperation.DELETED);

		for (Path p : pathResolver.getPaths()) {
			// Verify path exists and ensure it's monitored, except root (which we assume is already monitored via ProcessManager)
			if (!Path.ROOT.equals(p)) {
				String fileIdentifier = pathResolver.getRoot(p);
				File file = new File(fileIdentifier);
				Assert.isTrue(!file.exists() || (file.exists() && file.isDirectory()), "Path '" + fileIdentifier + "' must either not exist or be a directory");
				if (!file.exists()) {
					// Create directory, but no notifications as that will happen once we start monitoring it below
					new CreateDirectory(undoManager, filenameResolver, file);
				}
				MonitoringRequest request = new DirectoryMonitoringRequest(file, true, notifyOn);
				new UndoableMonitoringRequest(undoManager, fileMonitorService, request, true);
			}
		}

		// Avoid doing this operation again unless the validity changes
		pathsRegistered = true;

		// Explicitly perform a scan now that we've added all the directories we wish to monitor
		fileMonitorService.scanAll();
	}
}
