package org.springframework.roo.project;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.project.maven.Pom;

/**
 * A given {@link Path} within the context of a specific project module.
 * <p>
 * To obtain the physical location on the file system, pass an instance of this
 * class to the {@link PathResolver}.
 * 
 * @author James Tyrrell
 * @since 1.2.0
 */
public class LogicalPath {

    /**
     * The character that appears between the module name and the path name in
     * the textual representation of a {@link LogicalPath}. This cannot be any
     * character that could feasibly occur in a module name or {@link Path}
     * name.
     */
    public static final String MODULE_PATH_SEPARATOR = "|";

    /**
     * Creates an instance with the given path in the given module
     * 
     * @param path the path to set (required)
     * @param module can be blank for none
     * @return a non-<code>null</code> instance
     */
    public static LogicalPath getInstance(final Path path, final String module) {
        return new LogicalPath(module, path);
    }

    /**
     * Creates a {@link LogicalPath} by parsing the given concatenation of
     * optional module name and mandatory path name.
     * 
     * @param modulePlusPath a string consisting of an optional module name plus
     *            the {@link #MODULE_PATH_SEPARATOR} plus the path, or more
     *            precisely:
     *            <code>[<i>module_name</i>{@value #MODULE_PATH_SEPARATOR}]<i>path</i></code>
     */
    public static LogicalPath getInstance(final String modulePlusPath) {
        Validate.notBlank(modulePlusPath, "Module path required");
        final int separatorIndex = modulePlusPath
                .indexOf(MODULE_PATH_SEPARATOR);
        if (separatorIndex == -1) {
            return new LogicalPath(null, Path.valueOf(modulePlusPath));
        }
        final Path path = Path.valueOf(modulePlusPath.substring(
                separatorIndex + 1, modulePlusPath.length()));
        final String module = modulePlusPath.substring(0, separatorIndex);
        return new LogicalPath(module, path);
    }

    private final String module;
    private final Path path;

    /**
     * Constructor
     * 
     * @param module the module containing the given path (can be blank)
     * @param path the path within the module, if any (required)
     */
    private LogicalPath(final String module, final Path path) {
        Validate.notNull(path, "Path required");
        this.module = StringUtils.stripToEmpty(module);
        this.path = path;
    }

    public int compareTo(final LogicalPath o) {
        if (o == null) {
            throw new NullPointerException();
        }
        return getName().compareTo(o.getName());
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof LogicalPath && compareTo((LogicalPath) obj) == 0;
    }

    /**
     * Returns the name of the module containing this path, if any
     * 
     * @return a non-<code>null</code> name (might be empty)
     */
    public String getModule() {
        return module;
    }

    /**
     * Returns the display name of this {@link LogicalPath}.
     * 
     * @return a non-blank name
     */
    public String getName() {
        final StringBuilder name = new StringBuilder();
        if (StringUtils.isNotBlank(module)) {
            name.append(module).append(MODULE_PATH_SEPARATOR);
        }
        name.append(path);
        return name.toString();
    }

    /**
     * Returns the path component of this {@link LogicalPath}
     * 
     * @return a non-<code>null</code> path
     */
    public Path getPath() {
        return path;
    }

    /**
     * Returns the physical path of this logical path relative to the given POM
     * 
     * @param pom can be <code>null</code>
     * @return a non-<code>null</code> path
     */
    public String getPathRelativeToPom(final Pom pom) {
        return path.getPathRelativeToPom(pom);
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    /**
     * Indicates whether this is the root of the owning module.
     * 
     * @return see above
     */
    public boolean isModuleRoot() {
        return path == Path.ROOT;
    }

    /**
     * Indicates whether this is the root of the entire user project.
     * 
     * @return see above
     */
    public boolean isProjectRoot() {
        return isModuleRoot() && StringUtils.isBlank(module);
    }

    @Override
    public final String toString() {
        return getName();
    }
}
