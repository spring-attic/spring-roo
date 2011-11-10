package org.springframework.roo.project;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileUtils;

@Component(immediate = true)
@Service
public class MavenPathResolvingStrategy extends AbstractPathResolvingStrategy {

	// Fields
	@Reference protected PomManagementService pomManagementService;

	// ------------ OSGi component methods ----------------
	
	// ------------ PathResolvingStrategy methods ----------------

	/**
	 * Locates the first {@link PhysicalPath} which can be construed as a parent
	 * of the presented identifier.
	 *
	 * @param identifier to locate the parent of (required)
	 * @return the first matching parent, or null if not found
	 */
	protected PhysicalPath getApplicablePathInformation(final String identifier) {
		Assert.notNull(identifier, "Identifier required");
		PhysicalPath pathInformation = null;
		int longest = 0;
		for (final Pom pom : pomManagementService.getPoms()) {
			if (removeTrailingSeparator(identifier).startsWith(removeTrailingSeparator(pom.getRoot())) && removeTrailingSeparator(pom.getRoot()).length() > longest) {
				longest = removeTrailingSeparator(pom.getRoot()).length();
				int nextLongest = 0;
				for (final PhysicalPath pi : pom.getPathInformation()) {
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
	
	public String getCanonicalPath(final LogicalPath path, final JavaType javaType) {
		final String relativePath = javaType.getFullyQualifiedTypeName().replace('.', File.separatorChar) + ".java";
		return getIdentifier(path, relativePath);
	}
	
	public String getFocusedCanonicalPath(final Path path, final JavaType javaType) {
		return getCanonicalPath(path.getModulePathId(pomManagementService.getFocusedModuleName()), javaType);
	}

	public String getFocusedIdentifier(final Path path, final String relativePath) {
		return getIdentifier(LogicalPath.getInstance(path, pomManagementService.getFocusedModuleName()), relativePath);
	}
	
	public LogicalPath getFocusedPath(final Path path) {
		return pomManagementService.getFocusedModule().getPathInformation(path).getContextualPath();
	}

	public String getFocusedRoot(final Path path) {
		return pomManagementService.getFocusedModule().getPathLocation(path);
	}
	
	public String getIdentifier(final LogicalPath contextualPath, final String relativePath) {
		Assert.notNull(contextualPath, "Path required");
		Assert.notNull(relativePath, "Relative path cannot be null, although it can be empty");
		
		String initialPath = FileUtils.getCanonicalPath(getPath(contextualPath));
		initialPath = FileUtils.ensureTrailingSeparator(initialPath);
		return initialPath + FileUtils.removeLeadingAndTrailingSeparators(relativePath);
	}
	
	private File getModuleRoot(final String module, final Pom pom) {
		if (pom == null) {
			// No POM exists for this module; we must be creating it
			return new File(pomManagementService.getFocusedModule().getRoot(), module);
		}
		// This is a known module; use its known root path
		return new File(pom.getRoot());
	}
	
	private File getPath(final LogicalPath contextualPath) {
		final Pom pom = pomManagementService.getPomFromModuleName(contextualPath.getModule());
		final File moduleRoot = getModuleRoot(contextualPath.getModule(), pom);
		final String pathRelativeToPom = contextualPath.getPathRelativeToPom(pom);
		return new File(moduleRoot, pathRelativeToPom);
	}
	
	protected Collection<LogicalPath> getPaths(final boolean sourceOnly) {
		final Collection<LogicalPath> pathIds = new ArrayList<LogicalPath>();
		for (final Pom pom : pomManagementService.getPoms()) {
			for (final PhysicalPath modulePath : pom.getPathInformation()) {
				if (!sourceOnly || modulePath.isSource()) {
					pathIds.add(modulePath.getContextualPath());
				}
			}
		}
		return pathIds;
	}

	public String getRoot(final LogicalPath modulePathId) {
		final Pom pom = pomManagementService.getPomFromModuleName(modulePathId.getModule());
		return pom.getPathInformation(modulePathId.getPath()).getLocationPath();
	}
	
	public boolean isActive() {
		return pomManagementService.getRootPom() != null;
	}

	private String removeTrailingSeparator(final String pomPath) {
		if (pomPath.endsWith(File.separator)) {
			return pomPath.substring(0, pomPath.length() - 1);
		}
		return pomPath + File.separator;
	}
}
