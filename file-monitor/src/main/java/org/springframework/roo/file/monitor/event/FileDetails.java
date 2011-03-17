package org.springframework.roo.file.monitor.event;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.springframework.roo.file.monitor.FileMonitorService;
import org.springframework.roo.support.ant.AntPathMatcher;
import org.springframework.roo.support.ant.PathMatcher;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Represents the details of a file that once existed on the disk.
 * 
 * <p>
 * Instances of this class are usually included within a {@link FileEvent} object.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class FileDetails implements Comparable<FileDetails> {
	private File file;
	private Long lastModified;

	private static final PathMatcher pathMatcher;
	
	static {
		pathMatcher = new AntPathMatcher();
		((AntPathMatcher) pathMatcher).setPathSeparator(File.separator);
	}

	public FileDetails(File file, Long lastModified) {
		Assert.notNull(file, "File required");
		this.file = file;
		this.lastModified = lastModified;
	}
	
	public int hashCode() {
		return 7 * this.file.hashCode() * this.lastModified.hashCode();
	}

	public boolean equals(Object obj) {
		return obj != null && obj instanceof FileDetails && this.compareTo((FileDetails)obj) == 0;
	}

	public int compareTo(FileDetails o) {
		if (o == null) {
			throw new NullPointerException();
		}
		int result = o.file.compareTo(file);
		if (result == 0) {
			result = o.lastModified.compareTo(lastModified);
		}
		return result;
	}

	/**
	 * Each {@link FileDetails} is known by its canonical file name, which is also the format used for
	 * Ant path matching etc. This method provides the canoncial file name without forcing the user to
	 * deal with the exceptions that would arise from using {@link File} directly. 
	 * 
	 * @return the canonical path.
	 */
	public String getCanonicalPath() {
		try {
			return file.getCanonicalPath();
		} catch (IOException ioe) {
			throw new IllegalStateException("Cannot determine canonical path for '" + file + "'", ioe);
		}
	}
	
	/**
	 * Static convenience method to allow the acquisition of a canonical file path for a {@link File}.
	 * 
	 * @param file the File to find the canonical path for.
	 * @return the canonical path.
	 */
	public static String getCanonicalPath(File file) {
		Assert.notNull(file, "File required");
		try {
			return file.getCanonicalPath();
		} catch (IOException ioe) {
			throw new IllegalStateException("Cannot determine canoncial path for '" + file + "'", ioe);
		}
	}

	/**
	 * Indicates whether the presented canonical path is a child of the current
	 * {@link FileDetails} instance. Put differently, returning true indicates the
	 * current instance is a parent directory of the presented possibleChildCanonicalPath.
	 * 
	 * <p>
	 * This method will return true if the presented child is a child of the current
	 * instance, or if the presented child is identical to the current instance.
	 * 
	 * @param possibleChildCanonicalPath to evaluate (required)
	 * @return true if the presented possible child is indeed a child of the current instance
	 */
	public boolean isParentOf(String possibleChildCanonicalPath) {
		Assert.hasText(possibleChildCanonicalPath, "Possible child to evaluate is required");
		String parent = getCanonicalPath();
		return possibleChildCanonicalPath.startsWith(parent);
	}
	
	/**
	 * Determines whether the presented Ant path matches this {@link FileDetails} canoncial path.
	 * 
	 * <p>
	 * The presented path must be in Ant syntax. It should include a full prefix that is
	 * consistent with the {@link #getCanonicalPath()} method.
	 *
	 * @param antPath to evaluate (required and cannot be empty)
	 * @return whether the path matches or not
	 */
	public boolean matchesAntPath(String antPath) {
		Assert.hasText(antPath, "Ant path to match required");
		return matchesAntPath(antPath, getCanonicalPath());
	}

	public static boolean matchesAntPath(String antPath, String canonicalPath) {
		Assert.hasText(antPath, "Ant path to match required");
		Assert.hasText(canonicalPath, "Canonical path to match required");
		return pathMatcher.match(antPath, canonicalPath);
	}

	/**
	 * Returns the portion of the child identifier that is relative to the parent
	 * {@link FileDetails} instance. Note that this instance must be the parent.
	 * 
	 * <p>
	 * If an empty string is returned from this method, it denotes the child was actually
	 * the same identifier as the parent. 
	 * 
	 * @param childCanonicalPath the confirmed child of this instance (required; use canoncial path)
	 * @return the relative path within the parent instance (never null)
	 */
	public String getRelativeSegment(String childCanonicalPath) {
		Assert.notNull(childCanonicalPath, "Child identifier is required");
		Assert.isTrue(isParentOf(childCanonicalPath), "Identifier '" + childCanonicalPath + "' is not a child of '" + this + "'");
		return childCanonicalPath.substring(getCanonicalPath().length());
	}

	/**
	 * The {@link FileMonitorService} is required to advise of last modification times. This method provides access to the
	 * modification time according to {@link FileMonitorService}, which may be out of date due to the polling mechanisms
	 * often used by implementations. Instead you should generally use {@link #getFile()#lastModified} for
	 * the most accurate disk-derived representation of the last modification time.
	 * 
	 * @return the time the file was last modified, or in the case of a delete, it is implementation-specific (may return null)
	 */
	public Long getLastModified() {
		return lastModified;
	}

	/**
	 * @return the file that is subject of this status object (indicates the new name in the case of a {@link FileOperation#RENAMED}).
	 */
	public File getFile() {
		return file;
	}
	
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("file", file);
		tsc.append("exists", file.exists());
		tsc.append("lastModified", lastModified == null ? "Unavailable" : new Date(lastModified).toString());
		return tsc.toString();
	}
	
}
