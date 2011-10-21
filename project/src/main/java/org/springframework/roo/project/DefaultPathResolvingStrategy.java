package org.springframework.roo.project;

import static org.springframework.roo.support.util.FileUtils.CURRENT_DIRECTORY;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.osgi.OSGiUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.StringUtils;

@Component(immediate = true)
@Service
public class DefaultPathResolvingStrategy implements PathResolvingStrategy {

	// Fields
	@Reference private PomManagementService pomManagementService;

	private final List<PathInformation> pathOrder = new ArrayList<PathInformation>();
	private final Map<Path, PathInformation> pathCache = new LinkedHashMap<Path, PathInformation>();
	private final Map<Path, PathInformation> pathInformation = new HashMap<Path, PathInformation>();
	
	private String rootPath;

	protected void activate(final ComponentContext context) {
		final File projectDirectory = new File(StringUtils.defaultIfEmpty(OSGiUtils.getRooWorkingDirectory(context), CURRENT_DIRECTORY));
		rootPath = FileDetails.getCanonicalPath(projectDirectory);
		populatePathsMap(projectDirectory);
		initialisePathCollections();
	}

	private void populatePathsMap(final File projectDirectory) {
		pathInformation.put(Path.SRC_MAIN_JAVA, new PathInformation(Path.SRC_MAIN_JAVA.contextualize(), true, new File(projectDirectory, "src/main/java")));
		pathInformation.put(Path.SRC_MAIN_RESOURCES, new PathInformation(Path.SRC_MAIN_RESOURCES.contextualize(), true, new File(projectDirectory, "src/main/resources")));
		pathInformation.put(Path.SRC_TEST_JAVA, new PathInformation(Path.SRC_TEST_JAVA.contextualize(), true, new File(projectDirectory, "src/test/java")));
		pathInformation.put(Path.SRC_TEST_RESOURCES, new PathInformation(Path.SRC_TEST_RESOURCES.contextualize(), true, new File(projectDirectory, "src/test/resources")));
		pathInformation.put(Path.SRC_MAIN_WEBAPP, new PathInformation(Path.SRC_MAIN_WEBAPP.contextualize(), false, new File(projectDirectory, "src/main/webapp")));
		pathInformation.put(Path.ROOT, new PathInformation(Path.ROOT.contextualize(), true, projectDirectory));
		pathInformation.put(Path.SPRING_CONFIG_ROOT, new PathInformation(Path.SPRING_CONFIG_ROOT.contextualize(), false, new File(projectDirectory, "src/main/resources/META-INF/spring")));
	}

	public boolean isActive() {
		return pomManagementService.getPomMap().isEmpty();
	}

	public String getIdentifier(final ContextualPath path, final String relativePath) {
		return FileUtils.normalise(pathCache.get(path.getPath()).getLocationPath()) + relativePath;
	}

	/**
	 * Called by the {@link #initialisePathCollections()} method when it wishes to obtain a list of paths to register.
	 *
	 * @return an unmodifiable list of path (required)
	 */
	protected List<PathInformation> getPathInformation() {
		return new ArrayList<PathInformation>(pathInformation.values());
	}

	/**
	 * Called by the subclass when they are ready to complete initialization. This means their
	 * {@link #getPathInformation()} method is ready to be called.
	 */
	protected void initialisePathCollections()  {
		final List<PathInformation> pathInformation = getPathInformation();
		Assert.notEmpty(pathInformation, "Path information required");
		for (final PathInformation pi : pathInformation) {
			Assert.isTrue(!pathCache.containsKey(pi.getPath()), "Cannot specify '" + pi.getPath() + "' more than once");
			pathOrder.add(pi);
			pathCache.put(pi.getPath(), pi);
		}
	}

	public String getFriendlyName(final String identifier) {
		Assert.notNull(identifier, "Identifier required");
		final ContextualPath p = getPath(identifier);
		if (p == null) {
			return identifier;
		}
		return p.getName() + getRelativeSegment(identifier);
	}

	public String getRoot(final ContextualPath contextualPath) {
		Assert.notNull(contextualPath, "Path required");
		final PathInformation pathInfo = pathCache.get(contextualPath.getPath());
		Assert.notNull(pathInfo, "Unable to determine information for path '" + contextualPath + "'");
		final File root = pathInfo.getLocation();
		return FileDetails.getCanonicalPath(root);
	}

	/**
	 * Obtains the {@link Path}s.
	 *
	 * @param requireSource true if the path is source, false if the path is NOT source, or null if source is ignored
	 * @return a list of the matching paths (never null)
	 */
	private List<ContextualPath> getPaths(final Boolean requireSource) {
		final List<ContextualPath> result = new ArrayList<ContextualPath>();
		for (final PathInformation pi : pathOrder) {
			if (requireSource == null) {
				result.add(pi.getContextualPath());
			} else {
				if ((requireSource && pi.isSource()) || (!requireSource && !pi.isSource())) {
					result.add(pi.getContextualPath());
				}
			}
		}
		return result;
	}

	public List<ContextualPath> getPaths() {
		return getPaths(null);
	}

	public List<ContextualPath> getNonSourcePaths() {
		return getPaths(false);
	}

	public List<ContextualPath> getSourcePaths() {
		return getPaths(true);
	}

	/**
	 * Locates the first {@link PathInformation} which can be construed as a parent
	 * of the presented identifier.
	 *
	 * @param identifier to locate the parent of (required)
	 * @return the first matching parent, or null if not found
	 */
	private PathInformation getApplicablePathInformation(final String identifier) {
		Assert.notNull(identifier, "Identifier required");
		for (final PathInformation pi : pathOrder) {
			final FileDetails possibleParent = new FileDetails(pi.getLocation(), null);
			if (possibleParent.isParentOf(identifier)) {
				return pi;
			}
		}
		return null;
	}

	public ContextualPath getPath(final String identifier) {
		final PathInformation parent = getApplicablePathInformation(identifier);
		if (parent == null) {
			return null;
		}
		return parent.getContextualPath();
	}

	public String getRelativeSegment(final String identifier) {
		final PathInformation parent = getApplicablePathInformation(identifier);
		if (parent == null) {
			return null;
		}
		final FileDetails parentFile = new FileDetails(parent.getLocation(), null);
		return parentFile.getRelativeSegment(identifier);
	}

	public String getIdentifier(final Path path, final String relativePath) {
		Assert.notNull(path, "Path required");
		Assert.notNull(relativePath, "Relative path cannot be null, although it can be empty");
		final PathInformation pi = pathCache.get(path);
		Assert.notNull(pi, "Path '" + path + "' is unknown to the path resolver");
		final File newPath = new File(pi.getLocation(), relativePath);
		return FileDetails.getCanonicalPath(newPath);
	}

	public String getRoot() {
		return rootPath;
	}

	public String getCanonicalPath(final ContextualPath path, final JavaType javaType) {
		return null;	// TODO JTT to review
	}

	public String getCanonicalPath(final Path path, final JavaType javaType) {
		return null;	// TODO JTT to review
	}

	public String getFocusedIdentifier(final Path path, final String relativePath) {
		return null;	// TODO JTT to review
	}

	public String getFocusedRoot(final Path path) {
		return null;	// TODO JTT to review
	}

	public ContextualPath getFocusedPath(final Path path) {
		return null;	// TODO JTT to review
	}

	public String getFocusedCanonicalPath(final Path path, final JavaType javaType) {
		return null;	// TODO JTT to review
	}
}
