package org.springframework.roo.process.manager.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

import org.springframework.roo.file.monitor.DirectoryMonitoringRequest;
import org.springframework.roo.file.monitor.FileMonitorService;
import org.springframework.roo.file.monitor.MonitoringRequest;
import org.springframework.roo.file.monitor.NotifiableFileMonitorService;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.file.monitor.event.FileOperation;
import org.springframework.roo.file.undo.FilenameResolver;
import org.springframework.roo.file.undo.UndoManager;
import org.springframework.roo.file.undo.internal.CreateDirectory;
import org.springframework.roo.file.undo.internal.CreateFile;
import org.springframework.roo.file.undo.internal.DefaultFilenameResolver;
import org.springframework.roo.file.undo.internal.DeleteDirectory;
import org.springframework.roo.file.undo.internal.DeleteFile;
import org.springframework.roo.file.undo.internal.UpdateFile;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataNotificationListener;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;

/**
 * Default implementation of {@link FileManager}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public class DefaultFileManager implements FileManager, MetadataNotificationListener {

	private MetadataService metadataService;
	private UndoManager undoManager;
	private FileMonitorService fileMonitorService;
	private NotifiableFileMonitorService notifiableFileMonitorService = null;
	private FilenameResolver filenameResolver = new DefaultFilenameResolver();
	private boolean pathsRegistered = false;
	
	public DefaultFileManager(MetadataService metadataService, UndoManager undoManager, MetadataDependencyRegistry metadataDependencyRegistry, FileMonitorService fileMonitorService) {
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(undoManager, "Undo manager required");
		Assert.notNull(metadataDependencyRegistry, "Metadata depedency registry required");
		Assert.notNull(fileMonitorService, "File monitor service required");
		this.metadataService = metadataService;
		this.undoManager = undoManager;
		this.fileMonitorService = fileMonitorService;
		if (fileMonitorService instanceof NotifiableFileMonitorService) {
			this.notifiableFileMonitorService = (NotifiableFileMonitorService) fileMonitorService;
		}
		metadataDependencyRegistry.addNotificationListener(this);
	}
	
	public boolean exists(String fileIdentifier) {
		return new File(fileIdentifier).exists();
	}

	public InputStream getInputStream(String fileIdentifier) {
		File file = new File(fileIdentifier);
		Assert.isTrue(file.exists(), "File '" + fileIdentifier + "' does not exist");
		Assert.isTrue(file.isFile(), "Path '" + fileIdentifier + "' is not a file");
		try {
			return new FileInputStream(new File(fileIdentifier));
		} catch (IOException ioe) {
			throw new IllegalStateException("Could not obtain input stream to file '" + fileIdentifier + "'", ioe);
		}
	}

	public FileDetails createDirectory(String fileIdentifier) {
		Assert.notNull(fileIdentifier, "File identifier required");
		File actual = new File(fileIdentifier);
		Assert.isTrue(!actual.exists(), "File '" + fileIdentifier + "' already exists");
		try {
			this.notifiableFileMonitorService.notifyCreated(actual.getCanonicalPath());
		} catch (IOException ignored) {}
		new CreateDirectory(undoManager, filenameResolver, actual);
		FileDetails fileDetails = new FileDetails(actual, actual.lastModified());
		return fileDetails;
	}

	public MutableFile createFile(String fileIdentifier) {
		Assert.notNull(fileIdentifier, "File identifier required");
		File actual = new File(fileIdentifier);
		Assert.isTrue(!actual.exists(), "File '" + fileIdentifier + "' already exists");
		try {
			this.notifiableFileMonitorService.notifyCreated(actual.getCanonicalPath());
		} catch (IOException ignored) {}
		File parentDirectory = new File(actual.getParent());
		if (!parentDirectory.exists()) {
			createDirectory(FileDetails.getCanonicalPath(parentDirectory));
		}
		new CreateFile(undoManager, filenameResolver, actual);
		return new DefaultMutableFile(actual, null);
	}

	public void delete(String fileIdentifier) {
		Assert.notNull(fileIdentifier, "File identifier required");
		File actual = new File(fileIdentifier);
		Assert.isTrue(actual.exists(), "File '" + fileIdentifier + "' does not exist");
		try {
			this.notifiableFileMonitorService.notifyDeleted(actual.getCanonicalPath());
		} catch (IOException ignored) {}
		if (actual.isDirectory()) {
			new DeleteDirectory(undoManager, filenameResolver, actual);
		} else {
			new DeleteFile(undoManager, filenameResolver, actual);
		}
	}

	public MutableFile updateFile(String fileIdentifier) {
		Assert.notNull(fileIdentifier, "File identifier required");
		File actual = new File(fileIdentifier);
		Assert.isTrue(actual.exists(), "File '" + fileIdentifier + "' does not exist");
		new UpdateFile(undoManager, filenameResolver, actual);
		return new DefaultMutableFile(actual, notifiableFileMonitorService);
	}

	public void notify(String upstreamDependency, String downstreamDependency) {
		if (pathsRegistered) {
			return;
		}
		
		Assert.isTrue(MetadataIdentificationUtils.isValid(upstreamDependency), "Upstream dependency is an invalid metadata identification string ('" + upstreamDependency + "')");
		
		if (upstreamDependency.equals(ProjectMetadata.getProjectIdentifier())) {
			// Acquire the Project Metadata, if available
			ProjectMetadata md = (ProjectMetadata) metadataService.get(upstreamDependency);
			if (md == null) {
				return;
			}
			
			PathResolver pathResolver = md.getPathResolver();
			Assert.notNull(pathResolver, "Path resolver could not be acquired from changed metadata '" + md + "'");
			this.filenameResolver = new PathResolvingAwareFilenameResolver(pathResolver);
			
			Set<FileOperation> notifyOn = new HashSet<FileOperation>();
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
					if (md.isValid()) {
						new UndoableMonitoringRequest(undoManager, fileMonitorService, request, true);
					} else {
						new UndoableMonitoringRequest(undoManager, fileMonitorService, request, false);
					}
				}
			}
			
			// Explicitly perform a scan now that we've added all the directories we wish to monitor
			fileMonitorService.scanAll();
			
			// Avoid doing this operation again unless the validity changes
			pathsRegistered = md.isValid();
		}
	}

	public SortedSet<FileDetails> findMatchingAntPath(String antPath) {
		return fileMonitorService.findMatchingAntPath(antPath);
	}
	
	public int scan() {
		if (fileMonitorService instanceof NotifiableFileMonitorService) {
			return ((NotifiableFileMonitorService)fileMonitorService).scanNotified();
		} else {
			return fileMonitorService.scanAll();
		}
	}
	
}
