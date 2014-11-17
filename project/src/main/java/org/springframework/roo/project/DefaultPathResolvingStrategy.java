package org.springframework.roo.project;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.FileUtils;

@Component
@Service
public class DefaultPathResolvingStrategy extends AbstractPathResolvingStrategy {

    protected final static Logger LOGGER = HandlerUtils
            .getLogger(DefaultPathResolvingStrategy.class);

    private final Map<Path, PhysicalPath> rootModulePaths = new LinkedHashMap<Path, PhysicalPath>();

    // ------------ OSGi component attributes ----------------

    private BundleContext context;

    // ------------ OSGi component methods ----------------

    @Override
    protected void activate(final ComponentContext cContext) {
        super.activate(cContext);
        this.context = cContext.getBundleContext();
        populatePaths(getRoot());
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

    /**
     * {@inheritDoc}
     * This {@code PathResolvingStrategy} is not active if there are any other 
     * active strategy, otherwise it is active.
     */
    public boolean isActive() {
        try {
            // Get all Services implement PathResolvingStrategy interface
            ServiceReference<?>[] references = context.getAllServiceReferences(
                    PathResolvingStrategy.class.getName(), null);

            // There aren't any other implementation, this instance is Active
            if (references == null) {
                return true;
            }
            else if (references.length == 0) {
                return true;
            }

            // Search for other service implementations
            for (ServiceReference<?> ref : references) {
                PathResolvingStrategy strategy = (PathResolvingStrategy) context.getService(ref);

                if(!strategy.getClass().equals( this.getClass() )) {
                    // If there is any other impl active, this strategy is not
                    // active
                    if (strategy.isActive()) {
                        return false;
                    }
                }
            }

            // There aren't any other active strategy
            return true;
        }
        catch (InvalidSyntaxException ex) {
            // Cannot occur because filter param is not used
            LOGGER.warning("Invalid filter expression.");
            return true;
        }
    }

    private void populatePaths(final String projectDirectory) {
        for (final Path subPath : Path.values()) {
            rootModulePaths.put(subPath,
                    subPath.getRootModulePath(projectDirectory));
        }
    }
}
