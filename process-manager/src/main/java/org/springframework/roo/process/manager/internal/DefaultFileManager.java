package org.springframework.roo.process.manager.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.file.monitor.DirectoryMonitoringRequest;
import org.springframework.roo.file.monitor.MonitoringRequest;
import org.springframework.roo.file.monitor.NotifiableFileMonitorService;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.file.monitor.event.FileOperation;
import org.springframework.roo.file.undo.CreateDirectory;
import org.springframework.roo.file.undo.CreateFile;
import org.springframework.roo.file.undo.DefaultFilenameResolver;
import org.springframework.roo.file.undo.DeleteDirectory;
import org.springframework.roo.file.undo.DeleteFile;
import org.springframework.roo.file.undo.FilenameResolver;
import org.springframework.roo.file.undo.UndoManager;
import org.springframework.roo.file.undo.UpdateFile;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataNotificationListener;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;

/**
 * Default implementation of {@link FileManager}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
public class DefaultFileManager implements FileManager, MetadataNotificationListener {
	@Reference private MetadataService metadataService;
	@Reference private UndoManager undoManager;
	@Reference private NotifiableFileMonitorService fileMonitorService;
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	private boolean pathsRegistered = false;
	private FilenameResolver filenameResolver = new DefaultFilenameResolver();

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
	}

	public boolean exists(String fileIdentifier) {
		Assert.hasText(fileIdentifier, "File identifier required");
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
			this.fileMonitorService.notifyCreated(actual.getCanonicalPath());
		} catch (IOException ignored) {}
		new CreateDirectory(undoManager, filenameResolver, actual);
		FileDetails fileDetails = new FileDetails(actual, actual.lastModified());
		return fileDetails;
	}

	public FileDetails readFile(String fileIdentifier) {
		Assert.notNull(fileIdentifier, "File identifier required");
		File f = new File(fileIdentifier);
		if (!f.exists()) {
			return null;
		}
		return new FileDetails(f, f.lastModified());
	}

	public MutableFile createFile(String fileIdentifier) {
		Assert.notNull(fileIdentifier, "File identifier required");
		File actual = new File(fileIdentifier);
		Assert.isTrue(!actual.exists(), "File '" + fileIdentifier + "' already exists");
		try {
			this.fileMonitorService.notifyCreated(actual.getCanonicalPath());
		} catch (IOException ignored) {}
		File parentDirectory = new File(actual.getParent());
		if (!parentDirectory.exists()) {
			createDirectory(FileDetails.getCanonicalPath(parentDirectory));
		}
		new CreateFile(undoManager, filenameResolver, actual);
		ManagedMessageRenderer renderer = new ManagedMessageRenderer(filenameResolver, actual, true);
		return new DefaultMutableFile(actual, null, renderer);
	}

	public void delete(String fileIdentifier) {
		Assert.notNull(fileIdentifier, "File identifier required");
		File actual = new File(fileIdentifier);
		Assert.isTrue(actual.exists(), "File '" + fileIdentifier + "' does not exist");
		try {
			this.fileMonitorService.notifyDeleted(actual.getCanonicalPath());
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
		ManagedMessageRenderer renderer = new ManagedMessageRenderer(filenameResolver, actual, false);
		return new DefaultMutableFile(actual, fileMonitorService, renderer);
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
					new UndoableMonitoringRequest(undoManager, fileMonitorService, request, md.isValid());
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
		return ((NotifiableFileMonitorService) fileMonitorService).scanNotified();
	}

	public void createOrUpdateTextFileIfRequired(String fileIdentifier, String newContents) {
		MutableFile mutableFile = null;
		if (exists(fileIdentifier)) {
			// First verify if the file has even changed
			File f = new File(fileIdentifier);
			String existing = null;
			try {
				existing = FileCopyUtils.copyToString(new FileReader(f));
			} catch (IOException ignoreAndJustOverwriteIt) {}

			if (!newContents.equals(existing)) {
				mutableFile = updateFile(fileIdentifier);
			}
		} else {
			mutableFile = createFile(fileIdentifier);
			Assert.notNull(mutableFile, "Could not create file '" + fileIdentifier + "'");
		}

		try {
			if (mutableFile != null) {
				FileCopyUtils.copy(newContents.getBytes(), mutableFile.getOutputStream());
			}
		} catch (IOException ioe) {
			throw new IllegalStateException("Could not output '" + mutableFile.getCanonicalPath() + "'", ioe);
		}
	}
}
