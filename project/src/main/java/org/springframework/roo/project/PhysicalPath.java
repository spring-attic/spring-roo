package org.springframework.roo.project;

import java.io.File;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileUtils;

/**
 * The physical location of a given {@link LogicalPath} within the user's project.
 * <p>
 * Renamed from <code>PathInformation</code> in version 1.2.0.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class PhysicalPath {

	// Fields
	private final LogicalPath logicalPath;
	private final File location;
	
	/**
	 * Constructor
	 *
	 * @param logicalPath (required)
	 * @param location the physical location of this path (required)
	 */
	public PhysicalPath(final LogicalPath logicalPath, final File location) {
		Assert.notNull(logicalPath, "Module path required");
		Assert.notNull(location, "Location required");
		this.logicalPath = logicalPath;
		this.location = location;
	}

	public LogicalPath getLogicalPath() {
		return logicalPath;
	}

	/**
	 * Indicates whether this path contains Java source code
	 * 
	 * @return see above
	 */
	public boolean isSource() {
		return logicalPath.getPath().isJavaSource();
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
		return FileUtils.getCanonicalPath(location);
	}

	public Path getPath() {
		return logicalPath.getPath();
	}
	
	@Override
	public final String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("logicalPath", logicalPath);
		tsc.append("location", location);
		return tsc.toString();
	}
}
