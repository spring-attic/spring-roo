package org.springframework.roo.project;

import static org.springframework.roo.support.util.FileUtils.CURRENT_DIRECTORY;

import java.io.File;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.support.osgi.OSGiUtils;
import org.springframework.roo.support.util.FileUtils;

/**
 * Convenient superclass for {@link PathResolvingStrategy} implementations.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component(componentAbstract = true)
public abstract class AbstractPathResolvingStrategy implements
        PathResolvingStrategy {

    protected static final String ROOT_MODULE = "";

    private String rootPath;

    // ------------ OSGi component methods ----------------

    protected void activate(final ComponentContext context) {
        final File projectDirectory = new File(StringUtils.defaultIfEmpty(
                OSGiUtils.getRooWorkingDirectory(context), CURRENT_DIRECTORY));
        rootPath = FileUtils.getCanonicalPath(projectDirectory);
    }

    // ------------ PathResolvingStrategy methods ----------------

    protected abstract PhysicalPath getApplicablePhysicalPath(String identifier);

    public String getFriendlyName(final String identifier) {
        Validate.notNull(identifier, "Identifier required");
        final LogicalPath p = getPath(identifier);
        if (p == null) {
            return identifier;
        }
        return p.getName() + getRelativeSegment(identifier);
    }

    public LogicalPath getPath(final String identifier) {
        final PhysicalPath parent = getApplicablePhysicalPath(identifier);
        if (parent == null) {
            return null;
        }
        return parent.getLogicalPath();
    }

    public Collection<LogicalPath> getPaths() {
        return getPaths(false);
    }

    /**
     * Obtains the {@link Path}s.
     * 
     * @param requireSource <code>true</code> to return only paths containing
     *            Java source code, or <code>false</code> to return all paths
     * @return the matching paths (never <code>null</code>)
     */
    protected abstract Collection<LogicalPath> getPaths(boolean sourceOnly);

    public String getRelativeSegment(final String identifier) {
        final PhysicalPath parent = getApplicablePhysicalPath(identifier);
        if (parent == null) {
            return null;
        }
        final FileDetails parentFile = new FileDetails(parent.getLocation(),
                null);
        return parentFile.getRelativeSegment(identifier);
    }

    public String getRoot() {
        return rootPath;
    }

    public Collection<LogicalPath> getSourcePaths() {
        return getPaths(true);
    }
}
