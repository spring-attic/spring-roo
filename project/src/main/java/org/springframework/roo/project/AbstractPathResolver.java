package org.springframework.roo.project;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.support.util.Assert;

/**
 * Abstract {@link PathResolver} implementation.
 * 
 * <p>
 * Subclasses should be created for common build system structures.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public abstract class AbstractPathResolver implements PathResolver {

	/** Paths provided to constructor */
	private List<PathInformation> pathOrder = new ArrayList<PathInformation>();
	
	/** Cached map of the paths */
	private Map<Path,PathInformation> pathCache = new HashMap<Path, PathInformation>();
	
	/**
	 * Called by the {@link #init()} method when it wishes to obtain a list of paths to register.
	 * 
	 * @return an unmodifiable list of path (required) 
	 */
	protected abstract List<PathInformation> getPathInformation();
	
	/**
	 * Called by the subclass when they are ready to complete initialization. This means their
	 * {@link #getPathInformation()} method is ready to be called.
	 */
	protected void init()  {
		List<PathInformation> pathInformation = getPathInformation();
		Assert.notEmpty(pathInformation, "Path information required");
		for (PathInformation pi : pathInformation) {
			Assert.isTrue(!pathCache.containsKey(pi.getPath()), "Cannot specify '" + pi.getPath() + "' more than once");
			pathOrder.add(pi);
			pathCache.put(pi.getPath(), pi);
		}
	}
	
	public String getFriendlyName(String identifier) {
		Assert.notNull(identifier, "Identifier required");
		Path p = getPath(identifier);
		if (p == null) {
			return identifier;
		}
		return p.getName() + getRelativeSegment(identifier);
	}

	public String getRoot(Path path) {
		Assert.notNull(path, "Path required");
		PathInformation pathInfo = pathCache.get(path);
		Assert.notNull(pathInfo, "Unable to determine information for path '" + path + "'");
		File root = pathInfo.getLocation();
		return FileDetails.getCanonicalPath(root);
	}
	
	/**
	 * Obtains the {@link Path}s.
	 * 
	 * @param requireSource true if the path is source, false if the path is NOT source, or null if source is ignored
	 * @return a list of the matching paths (never null)
	 */
	private List<Path> getPaths(Boolean requireSource) {
		List<Path> result = new ArrayList<Path>();
		for (PathInformation pi : pathOrder) {
			if (requireSource == null) {
				result.add(pi.getPath());
			} else {
				if ((requireSource && pi.isSource()) || (!requireSource && !pi.isSource())) {
					result.add(pi.getPath());
				}
			}
		}
		return result;
	}

	public List<Path> getPaths() {
		return getPaths(null);
	}

	public List<Path> getNonSourcePaths() {
		return getPaths(false);
	}

	public List<Path> getSourcePaths() {
		return getPaths(true);
	}

	/**
	 * Locates the first {@link PathInformation} which can be construed as a parent
	 * of the presented identifier.
	 * 
	 * @param identifier to locate the parent of (required)
	 * @return the first matching parent, or null if not found
	 */
	private PathInformation getApplicablePathInformation(String identifier) {
		Assert.notNull(identifier, "Identifier required");
		for (PathInformation pi : pathOrder) {
			FileDetails possibleParent = new FileDetails(pi.getLocation(), null);
			if (possibleParent.isParentOf(identifier)) {
				return pi;
			}
		}
		return null;
	}
	
	public Path getPath(String identifier) {
		PathInformation parent = getApplicablePathInformation(identifier);
		if (parent == null) {
			return null;
		}
		return parent.getPath();
	}

	public String getRelativeSegment(String identifier) {
		PathInformation parent = getApplicablePathInformation(identifier);
		if (parent == null) {
			return null;
		}
		FileDetails parentFi = new FileDetails(parent.getLocation(), null);
		return parentFi.getRelativeSegment(identifier);
	}

	public String getIdentifier(Path path, String relativePath) {
		Assert.notNull(path, "Path required");
		Assert.notNull(relativePath, "Relative path cannot be null, although it can be empty");
		PathInformation pi = pathCache.get(path);
		Assert.notNull(pi, "Path '" + path + "' is unknown to the path resolver");
		File newPath;
		newPath = new File(pi.getLocation(), relativePath);
		return FileDetails.getCanonicalPath(newPath);
	}
}
