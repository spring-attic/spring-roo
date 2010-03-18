package org.springframework.roo.file.monitor;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.springframework.roo.file.monitor.event.FileOperation;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Provides a convenient {@link PropertyEditor} for specifying {@link MonitoringRequest}s.
 * 
 * <p>
 * The syntax expected by the editor is as follows:
 * 
 * <code>
 * fullyQualifiedName + "," + fileOperationCodes + {"," + "**"}
 * <code>
 * 
 * <p>
 * Where:
 * <ul>
 * <li>fullyQualifiedName is a {@link File}-resolvable name (required)</li>
 * <li>fileOperationCodes is one or more of characters "C" (for create), "R" (for rename), 
 * "U" (for update) and "D" (for delete), as per {@link FileOperation} (required)</li>
 * <li>literal "**" indicates to watch the subtree, which is only valid if the
 * fullyQualifiedName related to a {@link File} is resolvable as a directory (optional, but
 * must NOT be specified unless a directory was indicated)</li>
 * </ul>
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class MonitoringRequestEditor extends PropertyEditorSupport {
	
	/**
	 * @return this object in accordance with the string specification given in the JavaDocs (or null if the object null)
	 */
	public String getAsText() {
		MonitoringRequest req = (MonitoringRequest) getValue();
		if (req == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		try {
			sb.append(req.getFile().getCanonicalPath());
		} catch (IOException ioe) {
			throw new IllegalStateException("Failure retrieving path for request '" + req + "'", ioe);
		}
		sb.append(",");
		if (req.getNotifyOn().contains(FileOperation.CREATED)) {
			sb.append("C");
		}
		if (req.getNotifyOn().contains(FileOperation.RENAMED)) {
			sb.append("R");
		}
		if (req.getNotifyOn().contains(FileOperation.UPDATED)) {
			sb.append("U");
		}
		if (req.getNotifyOn().contains(FileOperation.DELETED)) {
			sb.append("D");
		}
		if (req instanceof DirectoryMonitoringRequest) {
			DirectoryMonitoringRequest dmr = (DirectoryMonitoringRequest) req;
			if (dmr.isWatchSubtree()) {
				sb.append(",**");
			}
		}
		return sb.toString();
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (text == null || "".equals(text)) {
			setValue(null);
		}
		
		String[] segments = StringUtils.commaDelimitedListToStringArray(text);
		if (segments.length < 2) {
			throw new IllegalArgumentException("Text '" + text + "' is invalid for a MonitoringRequest");
		}
		
		File file = new File(segments[0]);
		Assert.isTrue(file.exists(), "File '" + file + "' does not exist");
		
		Set<FileOperation> ops = new HashSet<FileOperation>();
		if (segments[1].contains("C")) {
			ops.add(FileOperation.CREATED);
		}
		if (segments[1].contains("R")) {
			ops.add(FileOperation.RENAMED);
		}
		if (segments[1].contains("U")) {
			ops.add(FileOperation.UPDATED);
		}
		if (segments[1].contains("D")) {
			ops.add(FileOperation.DELETED);
		}
		Assert.notEmpty(ops, "One or more valid operation codes ('CRUD') required for file '" + file + "'");
		
		if (file.isFile()) {
			Assert.isTrue(segments.length == 2, "Can only have two values for file '" + file + "'");
			setValue(new FileMonitoringRequest(file, ops));
		} else {
			if (segments.length == 3) {
				Assert.isTrue("**".equals(segments[2]), "The third value for directory '" + file + "' can only be '**' (or completely remove the third parameter if you do not want to watch the subtree)");
				setValue(new DirectoryMonitoringRequest(file, true, ops));
			} else {
				setValue(new DirectoryMonitoringRequest(file, false, ops));
			}
		}
	}
	
}
