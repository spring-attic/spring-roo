package org.springframework.roo.file.monitor;

import static org.springframework.roo.file.monitor.event.FileOperation.CREATED;
import static org.springframework.roo.file.monitor.event.FileOperation.DELETED;
import static org.springframework.roo.file.monitor.event.FileOperation.RENAMED;
import static org.springframework.roo.file.monitor.event.FileOperation.UPDATED;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.file.monitor.event.FileOperation;

/**
 * A {@link PropertyEditor} for {@link MonitoringRequest}s.
 * <p>
 * The syntax expected by the editor is as follows:
 * <p>
 * <code>fullyQualifiedName + "," + fileOperationCodes + {"," + "**"}<code>
 * 
 * <p>
 * Where:
 * <ul>
 * <li>fullyQualifiedName is a {@link File}-resolvable name (required)</li>
 * <li>fileOperationCodes is one or more of characters "C" (for create), "R" (for rename),
 * "U" (for update) and "D" (for delete), as per {@link FileOperation} (required)</li>
 * <li>literal "**" indicates to watch the subtree, which is only valid if the
 * fullyQualifiedName is an existing directory (optional, but can only be used
 * for a directory)</li>
 * </ul>
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class MonitoringRequestEditor extends PropertyEditorSupport {

    private static final FileOperation[] MONITORED_OPERATIONS = { CREATED,
            RENAMED, UPDATED, DELETED };
    private static final String SUBTREE_WILDCARD = "**";

    /**
     * @return this object in accordance with the string specification given in
     *         the JavaDocs (or null if the object null)
     */
    @Override
    public String getAsText() {
        final MonitoringRequest req = getValue();
        if (req == null) {
            return null;
        }
        final StringBuilder text = new StringBuilder();
        try {
            text.append(req.getFile().getCanonicalPath());
        }
        catch (final IOException ioe) {
            throw new IllegalStateException(
                    "Failure retrieving path for request '" + req + "'", ioe);
        }
        text.append(",");
        for (final FileOperation fileOperation : MONITORED_OPERATIONS) {
            if (req.getNotifyOn().contains(fileOperation)) {
                text.append(fileOperation.name().charAt(0));
            }
        }
        if (req instanceof DirectoryMonitoringRequest) {
            final DirectoryMonitoringRequest dmr = (DirectoryMonitoringRequest) req;
            if (dmr.isWatchSubtree()) {
                text.append(",").append(SUBTREE_WILDCARD);
            }
        }
        return text.toString();
    }

    @Override
    public MonitoringRequest getValue() {
        return (MonitoringRequest) super.getValue();
    }

    private Collection<FileOperation> parseFileOperations(
            final String fileOperationCodes) {
        final Set<FileOperation> fileOperations = new HashSet<FileOperation>();
        for (final FileOperation fileOperation : MONITORED_OPERATIONS) {
            if (fileOperationCodes.contains(fileOperation.name()
                    .substring(0, 1))) {
                fileOperations.add(fileOperation);
            }
        }
        return fileOperations;
    }

    @Override
    public void setAsText(final String text) throws IllegalArgumentException {
        if (StringUtils.isBlank(text)) {
            setValue(null);
            return;
        }

        final String[] segments = StringUtils.split(text, ",");
        Validate.isTrue(segments.length == 2 || segments.length == 3,
                "Text '%s' is invalid for a MonitoringRequest", text);
        final File file = new File(segments[0]);
        Validate.isTrue(file.exists(), "File '%s' does not exist", file);

        final Collection<FileOperation> fileOperations = parseFileOperations(segments[1]);
        Validate.notEmpty(
                fileOperations,
                "One or more valid operation codes ('CRUD') required for file '%s'",
                file);

        if (file.isFile()) {
            Validate.isTrue(segments.length == 2,
                    "Can only have two values for file '%s'", file);
            setValue(new FileMonitoringRequest(file, fileOperations));
        }
        else {
            setValueToDirectoryMonitoringRequest(segments, file, fileOperations);
        }
    }

    private void setValueToDirectoryMonitoringRequest(final String[] segments,
            final File file, final Collection<FileOperation> fileOperations) {
        if (segments.length == 3) {
            Validate.isTrue(
                    SUBTREE_WILDCARD.equals(segments[2]),
                    "The third value for directory '%s' can only be '%s' (or completely remove the third parameter if you do not want to watch the subtree)",
                    file, SUBTREE_WILDCARD);
            setValue(new DirectoryMonitoringRequest(file, true, fileOperations));
        }
        else {
            setValue(new DirectoryMonitoringRequest(file, false, fileOperations));
        }
    }
}
