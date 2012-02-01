package org.springframework.roo.project;

import java.io.File;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.support.util.FileUtils;

/**
 * The physical location of a given {@link LogicalPath} within the user's
 * project.
 * <p>
 * Renamed from <code>PathInformation</code> in version 1.2.0.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class PhysicalPath {

    private final String canonicalPath;
    private final File location;
    private final LogicalPath logicalPath;

    /**
     * Constructor
     * 
     * @param logicalPath (required)
     * @param location the physical location of this path (required)
     */
    public PhysicalPath(final LogicalPath logicalPath, final File location) {
        Validate.notNull(logicalPath, "Module path required");
        Validate.notNull(location, "Location required");
        canonicalPath = FileUtils.getCanonicalPath(location);
        this.logicalPath = logicalPath;
        this.location = location;
    }

    /**
     * Returns the physical location of this path
     * 
     * @return a non-<code>null</code> location
     */
    public File getLocation() {
        return location;
    }

    /**
     * Returns the canonical path of this {@link PhysicalPath}
     * 
     * @return a non-blank canonical path
     */
    public String getLocationPath() {
        return canonicalPath;
    }

    public LogicalPath getLogicalPath() {
        return logicalPath;
    }

    public Path getPath() {
        return logicalPath.getPath();
    }

    /**
     * Indicates whether this path contains Java source code
     * 
     * @return see above
     */
    public boolean isSource() {
        return logicalPath.getPath().isJavaSource();
    }

    @Override
    public final String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("logicalPath", logicalPath);
        builder.append("location", location);
        return builder.toString();
    }
}
