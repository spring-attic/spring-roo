package org.springframework.roo.project;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.FileUtils;

@Component(immediate = true)
@Service
@Reference(name = "pathResolvingStrategy", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = PathResolvingStrategy.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
public class DefaultPathResolvingStrategy extends AbstractPathResolvingStrategy {

    private final Collection<PathResolvingStrategy> otherPathResolvingStrategies = new ArrayList<PathResolvingStrategy>();
    private final Map<Path, PhysicalPath> rootModulePaths = new LinkedHashMap<Path, PhysicalPath>();

    // ------------ OSGi component methods ----------------

    @Override
    protected void activate(final ComponentContext context) {
        super.activate(context);
        populatePaths(getRoot());
    }

    protected void bindPathResolvingStrategy(
            final PathResolvingStrategy pathResolvingStrategy) {
        if (pathResolvingStrategy != this) {
            otherPathResolvingStrategies.add(pathResolvingStrategy);
        }
    }

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
        for (final PhysicalPath pi : rootModulePaths.values()) {
            final FileDetails possibleParent = new FileDetails(
                    pi.getLocation(), null);
            if (possibleParent.isParentOf(identifier)) {
                return pi;
            }
        }
        return null;
    }

    public String getCanonicalPath(final LogicalPath path,
            final JavaType javaType) {
        return null;
    }

    public String getFocusedCanonicalPath(final Path path,
            final JavaType javaType) {
        return null;
    }

    // ------------ PathResolvingStrategy methods ----------------

    public String getFocusedIdentifier(final Path path,
            final String relativePath) {
        return null;
    }

    public LogicalPath getFocusedPath(final Path path) {
        return null;
    }

    public String getFocusedRoot(final Path path) {
        return null;
    }

    public String getIdentifier(final LogicalPath path,
            final String relativePath) {
        return FileUtils.ensureTrailingSeparator(rootModulePaths.get(
                path.getPath()).getLocationPath())
                + relativePath;
    }

    @Override
    protected Collection<LogicalPath> getPaths(final boolean sourceOnly) {
        final List<LogicalPath> result = new ArrayList<LogicalPath>();
        for (final PhysicalPath modulePath : rootModulePaths.values()) {
            if (!sourceOnly || modulePath.isSource()) {
                result.add(modulePath.getLogicalPath());
            }
        }
        return result;
    }

    List<PhysicalPath> getPhysicalPaths() {
        return new ArrayList<PhysicalPath>(rootModulePaths.values());
    }

    public String getRoot(final LogicalPath logicalPath) {
        Validate.notNull(logicalPath, "Path required");
        final PhysicalPath pathInfo = rootModulePaths
                .get(logicalPath.getPath());
        Validate.notNull(pathInfo, "Unable to determine information for path '"
                + logicalPath + "'");
        final File root = pathInfo.getLocation();
        return FileUtils.getCanonicalPath(root);
    }

    public boolean isActive() {
        for (final PathResolvingStrategy otherStrategy : otherPathResolvingStrategies) {
            if (otherStrategy.isActive()) {
                return false;
            }
        }
        return true;
    }

    private void populatePaths(final String projectDirectory) {
        for (final Path subPath : Path.values()) {
            rootModulePaths.put(subPath,
                    subPath.getRootModulePath(projectDirectory));
        }
    }

    protected void unbindPathResolvingStrategy(
            final PathResolvingStrategy pathResolvingStrategy) {
        otherPathResolvingStrategies.remove(pathResolvingStrategy);
    }
}
