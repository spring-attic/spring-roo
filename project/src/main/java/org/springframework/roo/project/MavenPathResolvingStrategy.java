package org.springframework.roo.project;

import static org.springframework.roo.project.Path.ROOT;
import static org.springframework.roo.project.Path.SPRING_CONFIG_ROOT;
import static org.springframework.roo.project.Path.SRC_MAIN_JAVA;
import static org.springframework.roo.project.Path.SRC_MAIN_RESOURCES;
import static org.springframework.roo.project.Path.SRC_MAIN_WEBAPP;
import static org.springframework.roo.project.Path.SRC_TEST_JAVA;
import static org.springframework.roo.project.Path.SRC_TEST_RESOURCES;
import static org.springframework.roo.project.maven.Pom.DEFAULT_RESOURCES_DIRECTORY;
import static org.springframework.roo.project.maven.Pom.DEFAULT_SOURCE_DIRECTORY;
import static org.springframework.roo.project.maven.Pom.DEFAULT_SPRING_CONFIG_ROOT;
import static org.springframework.roo.project.maven.Pom.DEFAULT_TEST_RESOURCES_DIRECTORY;
import static org.springframework.roo.project.maven.Pom.DEFAULT_TEST_SOURCE_DIRECTORY;
import static org.springframework.roo.project.maven.Pom.DEFAULT_WAR_SOURCE_DIRECTORY;
import static org.springframework.roo.support.util.FileUtils.CURRENT_DIRECTORY;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.osgi.OSGiUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.StringUtils;

@Component(immediate = true)
@Service
public class MavenPathResolvingStrategy implements PathResolvingStrategy {

	// Fields
	@Reference protected PomManagementService pomManagementService;

	private String rootPath;

	protected void activate(final ComponentContext context) {
		final File projectDirectory = new File(StringUtils.defaultIfEmpty(OSGiUtils.getRooWorkingDirectory(context), CURRENT_DIRECTORY));
		rootPath = FileUtils.getCanonicalPath(projectDirectory);
	}

	public String getFriendlyName(final String identifier) {
		Assert.notNull(identifier, "Identifier required");
		final ContextualPath p = getPath(identifier);

		if (p == null) {
			return identifier;
		}
		return p.getName() + getRelativeSegment(identifier);
	}

	public String getCanonicalPath(final ContextualPath path, final JavaType javaType) {
		final String relativePath = javaType.getFullyQualifiedTypeName().replace('.', File.separatorChar) + ".java";
		return getIdentifier(path, relativePath);
	}

	public String getFocusedIdentifier(final Path path, final String relativePath) {
		return getIdentifier(ContextualPath.getInstance(path, pomManagementService.getFocusedModuleName()), relativePath);
	}

	public String getFocusedRoot(final Path path) {
		return pomManagementService.getFocusedModule().getPathLocation(path);
	}

	public ContextualPath getFocusedPath(final Path path) {
		return pomManagementService.getFocusedModule().getPathInformation(path).getContextualPath();
	}

	public String getFocusedCanonicalPath(final Path path, final JavaType javaType) {
		return getCanonicalPath(path.contextualize(pomManagementService.getFocusedModuleName()), javaType);
	}

	public String getRoot(final ContextualPath path) {
		Assert.notNull(path, "Path required");
		final Pom focusedModule = pomManagementService.getFocusedModule();
		if (focusedModule == null) {
			return null;
		}
		final PathInformation pathInfo = focusedModule.getPathInformation(path);
		Assert.notNull(pathInfo, "Unable to determine information for path '" + path + "'");
		final File root = pathInfo.getLocation();
		return FileUtils.getCanonicalPath(root);
	}

	/**
	 * Obtains the {@link Path}s.
	 *
	 * @param sourcePaths <code>true</code> to return only source paths,
	 * <code>false</code> to return only non-source paths, or <code>null</code>
	 * to return all paths
	 * @return a list of the matching paths (never null)
	 */
	private List<ContextualPath> getPaths(final Boolean sourcePaths) {
		final List<ContextualPath> result = new ArrayList<ContextualPath>();
		for (final Pom pom : pomManagementService.getPoms()) {
			for (final PathInformation pi : pom.getPathInformation()) {
				if (sourcePaths == null || sourcePaths.equals(pi.isSource())) {
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
		PathInformation pathInformation = null;
		int longest = 0;
		for (final Pom pom : pomManagementService.getPoms()) {
			if (removeTrailingSeparator(identifier).startsWith(removeTrailingSeparator(pom.getRoot())) && removeTrailingSeparator(pom.getRoot()).length() > longest) {
				longest = removeTrailingSeparator(pom.getRoot()).length();
				int nextLongest = 0;
				for (final PathInformation pi : pom.getPathInformation()) {
					final String possibleParent = new FileDetails(pi.getLocation(), null).getCanonicalPath();
					if (removeTrailingSeparator(identifier).startsWith(possibleParent) && possibleParent.length() > nextLongest) {
						nextLongest = possibleParent.length();
						pathInformation = pi;
					}
				}
			}
		}

		return pathInformation;
	}

	private String removeTrailingSeparator(final String pomPath) {
		if (pomPath.endsWith(File.separator)) {
			return pomPath.substring(0, pomPath.length() - 1);
		}
		return pomPath + File.separator;
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
		final FileDetails parentFileDetails = new FileDetails(parent.getLocation(), null);
		return parentFileDetails.getRelativeSegment(identifier);
	}

	public boolean isActive() {
		return pomManagementService.getRootPom() != null;
	}

	private PathInformation getPathInformation(final ContextualPath contextualPath) {
		final Pom pom = pomManagementService.getPomFromModuleName(contextualPath.getModule());
		final StringBuilder location = new StringBuilder();
		final Path path = contextualPath.getPath();
		if (pom == null) {
			location.append(pomManagementService.getFocusedModule().getRoot()).append(File.separator);
			if (StringUtils.hasText(contextualPath.getModule())) {
				location.append(contextualPath.getModule()).append(File.separator);
			}
		} else {
			location.append(pom.getRoot()).append(File.separator);
		}

		if (path.equals(SRC_MAIN_JAVA)) {
			String sourceDirectory = DEFAULT_SOURCE_DIRECTORY;
			if (pom != null) {
				if (StringUtils.hasText(pom.getSourceDirectory())) {
					sourceDirectory = pom.getSourceDirectory();
				}
			}
			location.append(sourceDirectory);
		} else if (path.equals(SRC_MAIN_RESOURCES)) {
			location.append(File.separator).append(DEFAULT_RESOURCES_DIRECTORY);
		} else if (path.equals(SRC_TEST_JAVA)) {
			String testSourceDirectory = DEFAULT_TEST_SOURCE_DIRECTORY;
			if (pom != null) {
				if (StringUtils.hasText(pom.getTestSourceDirectory())) {
					testSourceDirectory = pom.getTestSourceDirectory();
				}
			}
			location.append(testSourceDirectory);
		} else if (path.equals(SRC_TEST_RESOURCES)) {
			location.append(DEFAULT_TEST_RESOURCES_DIRECTORY);
		} else if (path.equals(SRC_MAIN_WEBAPP)) {
			location.append(DEFAULT_WAR_SOURCE_DIRECTORY);
		} else if (path.equals(SPRING_CONFIG_ROOT)) {
			location.append(DEFAULT_SPRING_CONFIG_ROOT);
		} else if (path.equals(ROOT)) {
			// do nothing
		}
		return new PathInformation(contextualPath, true, new File(location.toString()));
	}

	public String getIdentifier(final ContextualPath contextualPath, final String relativePath) {
		Assert.notNull(contextualPath, "Path required");
		Assert.notNull(relativePath, "Relative path cannot be null, although it can be empty");

		String initialPath = getPathInformation(contextualPath).getLocationPath();
		initialPath = FileUtils.ensureTrailingSeparator(initialPath);
		return initialPath + FileUtils.removeLeadingAndTrailingSeparators(relativePath);
	}

	public String getRoot() {
		return rootPath;
	}
}
