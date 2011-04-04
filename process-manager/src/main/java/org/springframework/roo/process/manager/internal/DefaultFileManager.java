package org.springframework.roo.process.manager.internal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.file.monitor.NotifiableFileMonitorService;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.file.undo.CreateDirectory;
import org.springframework.roo.file.undo.CreateFile;
import org.springframework.roo.file.undo.DeleteDirectory;
import org.springframework.roo.file.undo.DeleteFile;
import org.springframework.roo.file.undo.FilenameResolver;
import org.springframework.roo.file.undo.UndoEvent;
import org.springframework.roo.file.undo.UndoListener;
import org.springframework.roo.file.undo.UndoManager;
import org.springframework.roo.file.undo.UpdateFile;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.process.manager.ProcessManager;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;

/**
 * Default implementation of {@link FileManager}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
public class DefaultFileManager implements FileManager, UndoListener {
	@Reference private UndoManager undoManager;
	@Reference private NotifiableFileMonitorService fileMonitorService;
	@Reference private ProcessManager processManager;
	@Reference private FilenameResolver filenameResolver;

	protected void activate(ComponentContext context) {
		undoManager.addUndoListener(this);
	}

	protected void deactivate(ComponentContext context) {
		undoManager.removeUndoListener(this);
	}
	
	/** key: file identifier, value: new textual content */
	private Map<String, String> deferredFileWrites = new HashMap<String, String>();

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
		return new FileDetails(actual, actual.lastModified());
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
		renderer.setIncludeHashCode(processManager.isDevelopmentMode());
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
		renderer.setIncludeHashCode(processManager.isDevelopmentMode());
		return new DefaultMutableFile(actual, fileMonitorService, renderer);
	}

	public SortedSet<FileDetails> findMatchingAntPath(String antPath) {
		return fileMonitorService.findMatchingAntPath(antPath);
	}

	public int scan() {
		return fileMonitorService.scanNotified();
	}

	public void createOrUpdateTextFileIfRequired(String fileIdentifier, String newContents, boolean writeImmediately) {
		if (writeImmediately) {
			createOrUpdateTextFileIfRequired(fileIdentifier, newContents);
		} else {
			deferredFileWrites.put(fileIdentifier, newContents);
		}
	}
	
	public void createOrUpdateXmlFileIfRequired(String fileIdentifier, Document document, boolean writeImmediately) {
		Assert.notNull(document, "Document required");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		XmlUtils.writeXml(baos, document);
		String newContents = baos.toString();
		try {
			baos.close();
		} catch (IOException ignored) {}
		
		createOrUpdateTextFileIfRequired(fileIdentifier, newContents, writeImmediately);
	}

	public void commit() {
		try {
			for (String fileIdentifier : deferredFileWrites.keySet()) {
				String newContents = deferredFileWrites.get(fileIdentifier);
				if (newContents.length() == 0) {
					if (exists(fileIdentifier)) {
						delete(fileIdentifier);
					}
				} else {
					createOrUpdateTextFileIfRequired(fileIdentifier, newContents);
				}
			}
		} finally {
			deferredFileWrites.clear();
		}
	}
	
	private void createOrUpdateTextFileIfRequired(String fileIdentifier, String newContents) {
		MutableFile mutableFile = null;
		if (exists(fileIdentifier)) {
			// First verify if the file has even changed
			File f = new File(fileIdentifier);
			String existing = null;
			try {
				existing = FileCopyUtils.copyToString(new FileReader(f));
			} catch (IOException ignored) {}

			if (!newContents.equals(existing)) {
				mutableFile = updateFile(fileIdentifier);
			}
		} else {
			mutableFile = createFile(fileIdentifier);
			Assert.notNull(mutableFile, "Could not create file '" + fileIdentifier + "'");
		}

		if (mutableFile != null) {
			try {
				FileCopyUtils.copy(newContents.getBytes(), mutableFile.getOutputStream());
			} catch (IOException e) {
				throw new IllegalStateException("Could not output '" + mutableFile.getCanonicalPath() + "'", e);
			}
		}
	}

	public void clear() {
		deferredFileWrites.clear();
	}

	public void onUndoEvent(UndoEvent event) {
		if (event.isUndoing()) {
			clear();
		} else {
			// It's a flush or a reset event
			commit();
		}
	}

}