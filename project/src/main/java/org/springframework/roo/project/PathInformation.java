package org.springframework.roo.project;

import java.io.File;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Used by {@link AbstractPathResolver} to permit subclasses to register path details.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class PathInformation {
	private Path path;
	private boolean source;
	private File location;
	
	public PathInformation(Path path, boolean source, File location) {
		Assert.notNull(path, "Path required");
		Assert.notNull(location, "Location required");
		this.path = path;
		this.source = source;
		this.location = location;
	}

	public Path getPath() {
		return path;
	}

	public boolean isSource() {
		return source;
	}

	public File getLocation() {
		return location;
	}
	
	public final String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("path", path);
		tsc.append("source", source);
		tsc.append("location", location);
		return tsc.toString();
	}

}
