package org.springframework.roo.project;

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
		rootPath = FileDetails.getCanonicalPath(projectDirectory);
	}

	public String getFriendlyName(final String identifier) {
		Assert.notNull(identifier, "Identifier required");
		ContextualPath p = getPath(identifier);

		if (p == null) {
			return identifier;
		}
		return p.getName() + getRelativeSegment(identifier);
	}

	public String getCanonicalPath(final ContextualPath path, final JavaType javaType) {
		String relativePath = javaType.getFullyQualifiedTypeName().replace('.', File.separatorChar) + ".java";
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
		PathInformation pathInfo = pomManagementService.getFocusedModule().getPathInformation(path);
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
	private List<ContextualPath> getPaths(final Boolean requireSource) {
		List<ContextualPath> result = new ArrayList<ContextualPath>();
		for(Pom pom : pomManagementService.getPomMap().values()) {
			for (PathInformation pi : pom.getPathInformation()) {
				if (requireSource == null) {
					result.add(pi.getContextualPath());
				} else {
					if ((requireSource && pi.isSource()) || (!requireSource && !pi.isSource())) {
						result.add(pi.getContextualPath());
					}
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
		for (Pom pom : pomManagementService.getPomMap().values()) {
			if (removeTrailingSeparator(identifier).startsWith(removeTrailingSeparator(pom.getRoot())) && removeTrailingSeparator(pom.getRoot()).length() > longest) {
				longest = removeTrailingSeparator(pom.getRoot()).length();
				int nextLongest = 0;
				for (PathInformation pi : pom.getPathInformation()) {
					String possibleParent = new FileDetails(pi.getLocation(), null).getCanonicalPath();
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
		PathInformation parent = getApplicablePathInformation(identifier);
		if (parent == null) {
			return null;
		}
		return parent.getContextualPath();
	}

	public String getRelativeSegment(final String identifier) {
		PathInformation parent = getApplicablePathInformation(identifier);
		if (parent == null) {
			return null;
		}
		FileDetails parentFileDetails = new FileDetails(parent.getLocation(), null);
		return parentFileDetails.getRelativeSegment(identifier);
	}

	public boolean isActive() {
		return !pomManagementService.getPomMap().isEmpty();
	}

	public PathInformation getPathInformation(final ContextualPath contextualPath) {
		Pom module = pomManagementService.getPomFromModuleName(contextualPath.getModule());
		StringBuilder sb = new StringBuilder();
		Path path = contextualPath.getPath();
		if (module == null) {
			sb.append(pomManagementService.getFocusedModule().getRoot()).append(File.separator);
			if (StringUtils.hasText(contextualPath.getModule())) {
				sb.append(contextualPath.getModule()).append(File.separator);
			}
		} else {
			sb.append(module.getRoot()).append(File.separator);
		}

		if (path.equals(Path.SRC_MAIN_JAVA)) {
			String sourceDirectory = Pom.DEFAULT_SOURCE_DIRECTORY;
			if (module != null) {
				if (StringUtils.hasText(module.getSourceDirectory())) {
					sourceDirectory = module.getSourceDirectory().replace("${project.basedir}/", "");
				}
			}
			sb.append(sourceDirectory);
		} else if (path.equals(Path.SRC_MAIN_RESOURCES)) {
			sb.append(File.separator).append(Pom.DEFAULT_RESOURCES_DIRECTORY);
		} else if (path.equals(Path.SRC_TEST_JAVA)) {
			String testSourceDirectory = Pom.DEFAULT_TEST_SOURCE_DIRECTORY;
			if (module != null) {
				if (StringUtils.hasText(module.getTestSourceDirectory())) {
					testSourceDirectory = module.getTestSourceDirectory().replace("${project.basedir}/", "");
				}
			}
			sb.append(testSourceDirectory);
		}  else if (path.equals(Path.SRC_TEST_RESOURCES)) {
			sb.append(Pom.DEFAULT_TEST_RESOURCES_DIRECTORY);
		}  else if (path.equals(Path.SRC_MAIN_WEBAPP)) {
			sb.append(Pom.DEFAULT_WAR_SOURCE_DIRECTORY);
		} else if (path.equals(Path.SPRING_CONFIG_ROOT)) {
			sb.append(Pom.DEFAULT_SPRING_CONFIG_ROOT);
		} else if (path.equals(Path.ROOT)) {
			// do nothing
		}
		return new PathInformation(contextualPath, true, new File(sb.toString()));
	}

	public String getIdentifier(final ContextualPath contextualPath, final String relativePath) {
		Assert.notNull(contextualPath, "Path required");
		Assert.notNull(relativePath, "Relative path cannot be null, although it can be empty");

		String initialPath = getPathInformation(contextualPath).getLocationPath();
		initialPath = FileUtils.normalise(initialPath);
		return initialPath + FileUtils.removePrePostSeparator(relativePath);
	}

	public String getRoot() {
		return rootPath;
	}
}
