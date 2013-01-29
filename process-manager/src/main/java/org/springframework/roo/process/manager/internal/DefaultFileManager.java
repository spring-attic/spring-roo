package org.springframework.roo.process.manager.internal;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
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

/**
 * Default implementation of {@link FileManager}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
public class DefaultFileManager implements FileManager, UndoListener {

    /** key: file identifier, value: new description of change */
    private final Map<String, String> deferredDescriptionOfChanges = new LinkedHashMap<String, String>();
    /** key: file identifier, value: new textual content */
    private final Map<String, String> deferredFileWrites = new LinkedHashMap<String, String>();

    @Reference private NotifiableFileMonitorService fileMonitorService;
    @Reference private FilenameResolver filenameResolver;
    @Reference private ProcessManager processManager;
    @Reference private UndoManager undoManager;

    protected void activate(final ComponentContext context) {
        undoManager.addUndoListener(this);
    }

    public void clear() {
        deferredFileWrites.clear();
        deferredDescriptionOfChanges.clear();
    }

    public void commit() {
        final Map<String, String> toRemove = new LinkedHashMap<String, String>(
                deferredFileWrites);
        try {
            for (final Entry<String, String> entry : toRemove.entrySet()) {
                final String fileIdentifier = entry.getKey();
                final String newContents = entry.getValue();
                if (StringUtils.isNotBlank(newContents)) {
                    createOrUpdateTextFileIfRequired(fileIdentifier,
                            newContents,
                            StringUtils
                                    .stripToEmpty(deferredDescriptionOfChanges
                                            .get(fileIdentifier)));
                }
                else if (exists(fileIdentifier)) {
                    delete(fileIdentifier, "empty");
                }
            }
        }
        finally {
            for (final String remove : toRemove.keySet()) {
                deferredFileWrites.remove(remove);
            }
            deferredDescriptionOfChanges.clear();
        }
    }

    public FileDetails createDirectory(final String fileIdentifier) {
        Validate.notNull(fileIdentifier, "File identifier required");
        final File actual = new File(fileIdentifier);
        Validate.isTrue(!actual.exists(), "File '%s' already exists",
                fileIdentifier);
        try {
            fileMonitorService.notifyCreated(actual.getCanonicalPath());
        }
        catch (final IOException ignored) {
        }
        new CreateDirectory(undoManager, filenameResolver, actual);
        return new FileDetails(actual, actual.lastModified());
    }

    public MutableFile createFile(final String fileIdentifier) {
        Validate.notNull(fileIdentifier, "File identifier required");
        final File actual = new File(fileIdentifier);
        Validate.isTrue(!actual.exists(), "File '%s' already exists",
                fileIdentifier);
        try {
            fileMonitorService.notifyCreated(actual.getCanonicalPath());
            final File parentDirectory = new File(actual.getParent());
            if (!parentDirectory.exists()) {
                createDirectory(parentDirectory.getCanonicalPath());
            }
        }
        catch (final IOException ignored) {
        }
        new CreateFile(undoManager, filenameResolver, actual);
        final ManagedMessageRenderer renderer = new ManagedMessageRenderer(
                filenameResolver, actual, true);
        renderer.setIncludeHashCode(processManager.isDevelopmentMode());
        return new DefaultMutableFile(actual, null, renderer);
    }

    public void createOrUpdateTextFileIfRequired(final String fileIdentifier,
            final String newContents, final boolean writeImmediately) {
        createOrUpdateTextFileIfRequired(fileIdentifier, newContents, "",
                writeImmediately);
    }

    private void createOrUpdateTextFileIfRequired(final String fileIdentifier,
            final String newContents, final String descriptionOfChange) {
        MutableFile mutableFile = null;
        if (exists(fileIdentifier)) {
            // First verify if the file has even changed
            final File file = new File(fileIdentifier);
            String existing = null;
            try {
                existing = FileUtils.readFileToString(file);
            }
            catch (final IOException ignored) {
            }

            if (!newContents.equals(existing)) {
                mutableFile = updateFile(fileIdentifier);
            }
        }
        else {
            mutableFile = createFile(fileIdentifier);
            Validate.notNull(mutableFile, "Could not create file '%s'",
                    fileIdentifier);
        }

        if (mutableFile != null) {
            OutputStream outputStream = null;
            try {
                if (StringUtils.isNotBlank(descriptionOfChange)) {
                    mutableFile.setDescriptionOfChange(descriptionOfChange);
                }
                outputStream = mutableFile.getOutputStream();
                IOUtils.write(newContents, outputStream);
            }
            catch (final IOException e) {
                throw new IllegalStateException("Could not output '"
                        + mutableFile.getCanonicalPath() + "'", e);
            }
            finally {
                IOUtils.closeQuietly(outputStream);
            }
        }
    }

    public void createOrUpdateTextFileIfRequired(final String fileIdentifier,
            final String newContents, final String descriptionOfChange,
            final boolean writeImmediately) {
        if (writeImmediately) {
            createOrUpdateTextFileIfRequired(fileIdentifier, newContents,
                    descriptionOfChange);
        }
        else {
            deferredFileWrites.put(fileIdentifier, newContents);

            String deferredDescriptionOfChange = StringUtils.defaultIfEmpty(
                    deferredDescriptionOfChanges.get(fileIdentifier), "");
            if (StringUtils.isNotBlank(deferredDescriptionOfChange)
                    && !deferredDescriptionOfChange.trim().endsWith(";")) {
                deferredDescriptionOfChange += "; ";
            }
            deferredDescriptionOfChanges.put(
                    fileIdentifier,
                    deferredDescriptionOfChange
                            + StringUtils.stripToEmpty(descriptionOfChange));
        }
    }

    protected void deactivate(final ComponentContext context) {
        undoManager.removeUndoListener(this);
    }

    public void delete(final String fileIdentifier) {
        delete(fileIdentifier, null);
    }

    public void delete(final String fileIdentifier,
            final String reasonForDeletion) {
        if (StringUtils.isBlank(fileIdentifier)) {
            return;
        }

        final File actual = new File(fileIdentifier);
        Validate.isTrue(actual.exists(), "File '%s' does not exist",
                fileIdentifier);
        try {
            fileMonitorService.notifyDeleted(actual.getCanonicalPath());
        }
        catch (final IOException ignored) {
        }
        if (actual.isDirectory()) {
            new DeleteDirectory(undoManager, filenameResolver, actual,
                    reasonForDeletion);
        }
        else {
            new DeleteFile(undoManager, filenameResolver, actual,
                    reasonForDeletion);
        }
    }

    public boolean exists(final String fileIdentifier) {
        Validate.notBlank(fileIdentifier, "File identifier required");
        return new File(fileIdentifier).exists();
    }

    public SortedSet<FileDetails> findMatchingAntPath(final String antPath) {
        return fileMonitorService.findMatchingAntPath(antPath);
    }

    public InputStream getInputStream(final String fileIdentifier) {
        if (deferredFileWrites.containsKey(fileIdentifier)) {
            return new BufferedInputStream(new ByteArrayInputStream(
                    deferredFileWrites.get(fileIdentifier).getBytes()));
        }

        final File file = new File(fileIdentifier);
        Validate.isTrue(file.exists(), "File '%s' does not exist",
                fileIdentifier);
        Validate.isTrue(file.isFile(), "Path '%s' is not a file",
                fileIdentifier);
        try {
            return new BufferedInputStream(new FileInputStream(new File(
                    fileIdentifier)));
        }
        catch (final IOException ioe) {
            throw new IllegalStateException(
                    "Could not obtain input stream to file '" + fileIdentifier
                            + "'", ioe);
        }
    }

    public void onUndoEvent(final UndoEvent event) {
        if (event.isUndoing()) {
            clear();
        }
        else {
            // It's a flush or a reset event
            commit();
        }
    }

    public FileDetails readFile(final String fileIdentifier) {
        Validate.notNull(fileIdentifier, "File identifier required");
        final File f = new File(fileIdentifier);
        if (!f.exists()) {
            return null;
        }
        return new FileDetails(f, f.lastModified());
    }

    public int scan() {
        return fileMonitorService.scanNotified();
    }

    public MutableFile updateFile(final String fileIdentifier) {
        Validate.notNull(fileIdentifier, "File identifier required");
        final File actual = new File(fileIdentifier);
        Validate.isTrue(actual.exists(), "File '%s' does not exist",
                fileIdentifier);
        new UpdateFile(undoManager, filenameResolver, actual);
        final ManagedMessageRenderer renderer = new ManagedMessageRenderer(
                filenameResolver, actual, false);
        renderer.setIncludeHashCode(processManager.isDevelopmentMode());
        return new DefaultMutableFile(actual, fileMonitorService, renderer);
    }
}