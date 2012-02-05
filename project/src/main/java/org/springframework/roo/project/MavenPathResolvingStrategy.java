package org.springframework.roo.project;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.util.FileUtils;

@Component(immediate = true)
@Service
public class MavenPathResolvingStrategy extends AbstractPathResolvingStrategy {

    @Reference protected PomManagementService pomManagementService;

    /**
     * Locates the first {@link PhysicalPath} which can be construed as a parent
     * of the presented identifier.
     * 
     * @param identifier to locate the parent of (required)
     * @return the first matching parent, or null if not found
     */
    @Override
    protected PhysicalPath getApplicablePhysicalPath(final String identifier) {
        Validate.notNull(identifier, "Identifier required");
        PhysicalPath physicalPath = null;
        int longest = 0;
        for (final Pom pom : pomManagementService.getPoms()) {
            if (removeTrailingSeparator(identifier).startsWith(
                    removeTrailingSeparator(pom.getRoot()))
                    && removeTrailingSeparator(pom.getRoot()).length() > longest) {
                longest = removeTrailingSeparator(pom.getRoot()).length();
                int nextLongest = 0;
                for (final PhysicalPath thisPhysicalPath : pom
                        .getPhysicalPaths()) {
                    final String possibleParent = new FileDetails(
                            thisPhysicalPath.getLocation(), null)
                            .getCanonicalPath();
                    if (removeTrailingSeparator(identifier).startsWith(
                            possibleParent)
                            && possibleParent.length() > nextLongest) {
                        nextLongest = possibleParent.length();
                        physicalPath = thisPhysicalPath;
                    }
                }
            }
        }
        return physicalPath;
    }

    public String getCanonicalPath(final LogicalPath path,
            final JavaType javaType) {
        return getIdentifier(path, javaType.getRelativeFileName());
    }

    public String getFocusedCanonicalPath(final Path path,
            final JavaType javaType) {
        return getCanonicalPath(path.getModulePathId(pomManagementService
                .getFocusedModuleName()), javaType);
    }

    public String getFocusedIdentifier(final Path path,
            final String relativePath) {
        return getIdentifier(
                LogicalPath.getInstance(path,
                        pomManagementService.getFocusedModuleName()),
                relativePath);
    }

    public LogicalPath getFocusedPath(final Path path) {
        final PhysicalPath physicalPath = pomManagementService
                .getFocusedModule().getPhysicalPath(path);
        Validate.notNull(physicalPath, "Physical path for '" + path.name()
                + "' not found");
        return physicalPath.getLogicalPath();
    }

    public String getFocusedRoot(final Path path) {
        return pomManagementService.getFocusedModule().getPathLocation(path);
    }

    public String getIdentifier(final LogicalPath logicalPath,
            final String relativePath) {
        Validate.notNull(logicalPath, "Path required");
        Validate.notNull(relativePath,
                "Relative path cannot be null, although it can be empty");

        String initialPath = FileUtils.getCanonicalPath(getPath(logicalPath));
        initialPath = FileUtils.ensureTrailingSeparator(initialPath);
        return initialPath + StringUtils.strip(relativePath, File.separator);
    }

    private File getModuleRoot(final String module, final Pom pom) {
        if (pom == null) {
            // No POM exists for this module; we must be creating it
            return new File(pomManagementService.getFocusedModule().getRoot(),
                    module);
        }
        // This is a known module; use its known root path
        return new File(pom.getRoot());
    }

    private File getPath(final LogicalPath logicalPath) {
        final Pom pom = pomManagementService.getPomFromModuleName(logicalPath
                .getModule());
        final File moduleRoot = getModuleRoot(logicalPath.getModule(), pom);
        final String pathRelativeToPom = logicalPath.getPathRelativeToPom(pom);
        return new File(moduleRoot, pathRelativeToPom);
    }

    @Override
    protected Collection<LogicalPath> getPaths(final boolean sourceOnly) {
        final Collection<LogicalPath> pathIds = new ArrayList<LogicalPath>();
        for (final Pom pom : pomManagementService.getPoms()) {
            for (final PhysicalPath modulePath : pom.getPhysicalPaths()) {
                if (!sourceOnly || modulePath.isSource()) {
                    pathIds.add(modulePath.getLogicalPath());
                }
            }
        }
        return pathIds;
    }

    public String getRoot(final LogicalPath modulePathId) {
        final Pom pom = pomManagementService.getPomFromModuleName(modulePathId
                .getModule());
        return pom.getPhysicalPath(modulePathId.getPath()).getLocationPath();
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
