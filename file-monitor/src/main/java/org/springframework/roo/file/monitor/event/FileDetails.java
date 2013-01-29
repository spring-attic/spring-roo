package org.springframework.roo.file.monitor.event;

import java.io.File;
import java.util.Date;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.file.monitor.FileMonitorService;
import org.springframework.roo.support.util.FileUtils;

/**
 * The details of a file that once existed on the disk.
 * <p>
 * Instances of this class are usually included within a {@link FileEvent}
 * object.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class FileDetails implements Comparable<FileDetails> {

    /**
     * Returns the canonical path of the given {@link File}.
     * 
     * @param file the file for which to find the canonical path (required)
     * @return the canonical path
     * @deprecated use {@link FileUtils#getCanonicalPath(File)} instead
     */
    @Deprecated
    public static String getCanonicalPath(final File file) {
        return FileUtils.getCanonicalPath(file);
    }

    /**
     * Indicates whether the given canonical path matches the given Ant-style
     * pattern
     * 
     * @param antPattern the pattern to check against (can't be blank)
     * @param canonicalPath the path to check (can't be blank)
     * @return see above
     * @deprecated use {@link FileUtils#matchesAntPath(String, String)} instead
     */
    @Deprecated
    public static boolean matchesAntPath(final String antPattern,
            final String canonicalPath) {
        return FileUtils.matchesAntPath(antPattern, canonicalPath);
    }

    private final File file;
    private final Long lastModified;

    /**
     * Constructor
     * 
     * @param file the file for which these are the details (required)
     * @param lastModified the system clock in milliseconds when this file was
     *            last modified (can be <code>null</code>)
     */
    public FileDetails(final File file, final Long lastModified) {
        Validate.notNull(file, "File required");
        this.file = file;
        this.lastModified = lastModified;
    }

    public int compareTo(final FileDetails o) {
        if (o == null) {
            throw new NullPointerException();
        }
        // N.B. this is in reverse order to how we'd normally compare
        int result = o.getFile().compareTo(file);
        if (result == 0) {
            result = ObjectUtils.compare(o.getLastModified(), lastModified);
        }
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof FileDetails && compareTo((FileDetails) obj) == 0;
    }

    /**
     * Each {@link FileDetails} is known by its canonical file name, which is
     * also the format used for Ant path matching etc. This method provides the
     * canonical file name without forcing the user to deal with the exceptions
     * that would arise from using {@link File} directly.
     * 
     * @return the canonical path.
     */
    public String getCanonicalPath() {
        return FileUtils.getCanonicalPath(file);
    }

    /**
     * @return the file that is subject of this status object (indicates the new
     *         name in the case of a {@link FileOperation#RENAMED}).
     */
    public File getFile() {
        return file;
    }

    /**
     * The {@link FileMonitorService} is required to advise of last modification
     * times. This method provides access to the modification time according to
     * {@link FileMonitorService}, which may be out of date due to the polling
     * mechanisms often used by implementations. Instead you should generally
     * use {@link #getFile()#lastModified} for the most accurate disk-derived
     * representation of the last modification time.
     * 
     * @return the time the file was last modified, or in the case of a delete,
     *         it is implementation-specific (may return null)
     */
    public Long getLastModified() {
        return lastModified;
    }

    /**
     * Returns the portion of the child identifier that is relative to the
     * parent {@link FileDetails} instance. Note that this instance must be the
     * parent.
     * <p>
     * If an empty string is returned from this method, it denotes the child was
     * actually the same identifier as the parent.
     * 
     * @param childCanonicalPath the confirmed child of this instance (required;
     *            use canonical path)
     * @return the relative path within the parent instance (never null)
     */
    public String getRelativeSegment(final String childCanonicalPath) {
        Validate.notNull(childCanonicalPath, "Child identifier is required");
        Validate.isTrue(isParentOf(childCanonicalPath),
                "Identifier '%s' is not a child of '%s'", childCanonicalPath,
                this);
        return childCanonicalPath.substring(getCanonicalPath().length());
    }

    @Override
    public int hashCode() {
        return 7 * file.hashCode() * ObjectUtils.hashCode(lastModified);
    }

    /**
     * Indicates whether the presented canonical path is a child of the current
     * {@link FileDetails} instance. Put differently, returning true indicates
     * the current instance is a parent directory of the presented
     * possibleChildCanonicalPath.
     * <p>
     * This method will return true if the presented child is a child of the
     * current instance, or if the presented child is identical to the current
     * instance.
     * 
     * @param possibleChildCanonicalPath to evaluate (required)
     * @return true if the presented possible child is indeed a child of the
     *         current instance
     */
    public boolean isParentOf(final String possibleChildCanonicalPath) {
        Validate.notBlank(possibleChildCanonicalPath,
                "Possible child to evaluate is required");
        return FileUtils.ensureTrailingSeparator(possibleChildCanonicalPath)
                .startsWith(
                        FileUtils.ensureTrailingSeparator(getCanonicalPath()));
    }

    /**
     * Indicates whether this file's canonical path matches the given Ant-style
     * pattern.
     * <p>
     * The presented path must be in Ant syntax. It should include a full prefix
     * that is consistent with the {@link #getCanonicalPath()} method.
     * 
     * @param antPattern the pattern to check this file against (cannot be
     *            blank)
     * @return whether the path matches or not
     */
    public boolean matchesAntPath(final String antPattern) {
        return FileUtils.matchesAntPath(antPattern, getCanonicalPath());
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("file", file);
        builder.append("exists", file.exists());
        builder.append("lastModified", lastModified == null ? "Unavailable"
                : new Date(lastModified).toString());
        return builder.toString();
    }
}
