package org.springframework.roo.project;

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
import org.springframework.roo.file.monitor.MonitoringRequest;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.osgi.OSGiUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileUtils;

@Component(immediate = true)
@Service
public class DefaultPathResolvingStrategy implements PathResolvingStrategy {

	// Fields
	@Reference private PomManagementService pomManagementService;

	private Map<Path, PathInformation> pathInformation = new HashMap<Path, PathInformation>();
	private String rootPath;

	protected void activate(ComponentContext context) {
		String workingDir = OSGiUtils.getRooWorkingDirectory(context);
		File root = MonitoringRequest.getInitialMonitoringRequest(workingDir).getFile();
		rootPath = FileDetails.getCanonicalPath(root);
		pathInformation.put(Path.SRC_MAIN_JAVA, new PathInformation(Path.SRC_MAIN_JAVA.contextualize(), true, new File(root, "src/main/java")));
		pathInformation.put(Path.SRC_MAIN_RESOURCES, new PathInformation(Path.SRC_MAIN_RESOURCES.contextualize(), true, new File(root, "src/main/resources")));
		pathInformation.put(Path.SRC_TEST_JAVA, new PathInformation(Path.SRC_TEST_JAVA.contextualize(), true, new File(root, "src/test/java")));
		pathInformation.put(Path.SRC_TEST_RESOURCES, new PathInformation(Path.SRC_TEST_RESOURCES.contextualize(), true, new File(root, "src/test/resources")));
		pathInformation.put(Path.SRC_MAIN_WEBAPP, new PathInformation(Path.SRC_MAIN_WEBAPP.contextualize(), false, new File(root, "src/main/webapp")));
		pathInformation.put(Path.ROOT, new PathInformation(Path.ROOT.contextualize(), true, root));
		pathInformation.put(Path.SPRING_CONFIG_ROOT, new PathInformation(Path.SPRING_CONFIG_ROOT.contextualize(), false, new File(root, "src/main/resources/META-INF/spring")));
		init();
	}

	public boolean inactive() {
		return pomManagementService.getPomMap().size() > 0;
	}

	public String getIdentifier(ContextualPath path, String relativePath) {
		return FileUtils.normalise(pathCache.get(path.getPath()).getLocationPath()) + relativePath;
	}

	/** Paths provided to constructor */
	private List<PathInformation> pathOrder = new ArrayList<PathInformation>();

	/** Cached map of the paths */
	private Map<Path, PathInformation> pathCache = new LinkedHashMap<Path, PathInformation>();

	/**
	 * Called by the {@link #init()} method when it wishes to obtain a list of paths to register.
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
		ContextualPath p = getPath(identifier);
		if (p == null) {
			return identifier;
		}
		return p.getName() + getRelativeSegment(identifier);
	}

	public String getRoot(ContextualPath path) {
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
	private List<ContextualPath> getPaths(Boolean requireSource) {
		List<ContextualPath> result = new ArrayList<ContextualPath>();
		for (PathInformation pi : pathOrder) {
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

	public ContextualPath getPath(String identifier) {
		PathInformation parent = getApplicablePathInformation(identifier);
		if (parent == null) {
			return null;
		}
		return parent.getContextualPath();
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

	public String getRoot() {
		return rootPath;
	}

	public String getCanonicalPath(ContextualPath path, JavaType javaType) {
		return null;
	}

	public String getCanonicalPath(Path path, JavaType javaType) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public String getFocusedIdentifier(Path path, String relativePath) {
		return null;
	}

	public String getFocusedRoot(Path path) {
		return null;
	}

	public ContextualPath getFocusedPath(Path path) {
		return null;
	}

	public String getFocusedCanonicalPath(Path path, JavaType javaType) {
		return null;
	}
}
