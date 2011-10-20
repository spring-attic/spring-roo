package org.springframework.roo.project;

import java.io.File;

import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Used by {@link DelegatePathResolver} to permit subclasses to register path details.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class PathInformation {

	// Fields
	private ContextualPath contextualPath;
	private boolean source;
	private File location;
	
	public PathInformation(final ContextualPath contextualPath, final boolean source, final File location) {
		Assert.notNull(contextualPath, "Module path required");
		Assert.notNull(location, "Location required");
		this.contextualPath = contextualPath;
		this.source = source;
		this.location = location;
	}

	public ContextualPath getContextualPath() {
		return contextualPath;
	}

	public boolean isSource() {
		return source;
	}

	public File getLocation() {
		return location;
	}

	public String getLocationPath() {
		return FileDetails.getCanonicalPath(location);
	}

	public Path getPath() {
		return contextualPath.getPath();
	}
	
	public final String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("contextualPath", contextualPath);
		tsc.append("source", source);
		tsc.append("location", location);
		return tsc.toString();
	}
}
