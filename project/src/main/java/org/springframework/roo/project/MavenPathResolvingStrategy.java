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
	 * Obtains the {@link ContextualPath}s.
	 *
	 * @param sourceOnly <code>true</code> to return only source paths,
	 * <code>false</code> to return all paths
	 * @return a list of the matching paths (never null)
	 */
	private List<ContextualPath> getPaths(final boolean sourceOnly) {
		final List<ContextualPath> pathIds = new ArrayList<ContextualPath>();
		for (final Pom pom : pomManagementService.getPoms()) {
			for (final PathInformation modulePath : pom.getPathInformation()) {
				if (!sourceOnly || modulePath.isSource()) {
					pathIds.add(modulePath.getContextualPath());
				}
			}
		}
		return pathIds;
	}

	public List<ContextualPath> getPaths() {
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
	
	public String getIdentifier(final ContextualPath contextualPath, final String relativePath) {
		Assert.notNull(contextualPath, "Path required");
		Assert.notNull(relativePath, "Relative path cannot be null, although it can be empty");

		String initialPath = FileUtils.getCanonicalPath(getPath(contextualPath));
		initialPath = FileUtils.ensureTrailingSeparator(initialPath);
		return initialPath + FileUtils.removeLeadingAndTrailingSeparators(relativePath);
	}
	
	private File getPath(final ContextualPath contextualPath) {
		final Pom pom = pomManagementService.getPomFromModuleName(contextualPath.getModule());
		final File moduleRoot = getModuleRoot(contextualPath.getModule(), pom);
		final String pathRelativeToPom = contextualPath.getPathRelativeToPom(pom);
		return new File(moduleRoot, pathRelativeToPom);
	}
	
	private File getModuleRoot(final String module, final Pom pom) {
		if (pom == null) {
			// No POM exists for this module; we must be creating it
			return new File(pomManagementService.getFocusedModule().getRoot(), module);
		}
		// This is a known module; use its known root path
		return new File(pom.getRoot());
	}

	public String getRoot() {
		return rootPath;
	}
}
