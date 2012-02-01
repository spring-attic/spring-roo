package org.springframework.roo.project;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.util.FileUtils;

/**
 * Common file paths used in Maven projects.
 * <p>
 * {@link PathResolver}s can convert these paths to and from physical locations.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public enum Path {

    // These paths might be in a special order => don't reorder them here

    /**
     * The module's root directory.
     */
    ROOT(false, ""),

    /**
     * The module's base directory for production Spring-related resource files.
     */
    SPRING_CONFIG_ROOT(false, "src/main/resources/META-INF/spring"),

    /**
     * The module sub-path containing production Java source code.
     */
    SRC_MAIN_JAVA(true, "src/main/java") {
        @Override
        public String getPathRelativeToPom(final Pom pom) {
            if (pom != null && StringUtils.isNotBlank(pom.getSourceDirectory())) {
                return pom.getSourceDirectory();
            }
            return getDefaultLocation();
        }
    },

    /**
     * The module sub-path containing production resource files.
     */
    SRC_MAIN_RESOURCES(false, "src/main/resources"),

    /**
     * The module sub-path containing web resource files.
     */
    SRC_MAIN_WEBAPP(false, "src/main/webapp"),

    /**
     * The module sub-path containing test Java source code.
     */
    SRC_TEST_JAVA(true, "src/test/java") {
        @Override
        public String getPathRelativeToPom(final Pom pom) {
            if (pom != null
                    && StringUtils.isNotBlank(pom.getTestSourceDirectory())) {
                return pom.getTestSourceDirectory();
            }
            return getDefaultLocation();
        }
    },

    /**
     * The module sub-path containing test resource files.
     */
    SRC_TEST_RESOURCES(false, "src/test/resources");

    private final String defaultLocation;
    private final boolean javaSource;

    /**
     * Constructor
     * 
     * @param javaSource indicates whether this path contains Java source code
     * @param defaultLocation the location relative to the module's root
     *            directory in which this path is located by default (can't be
     *            <code>null</code>)
     */
    private Path(final boolean javaSource, final String defaultLocation) {
        Validate.notNull(defaultLocation, "Default location is required");
        this.defaultLocation = defaultLocation;
        this.javaSource = javaSource;
    }

    /**
     * Returns the default location of this path relative to the module's root
     * directory
     * 
     * @return a relative file path, e.g. "src/main/java"
     */
    public String getDefaultLocation() {
        return defaultLocation;
    }

    /**
     * Returns the {@link PhysicalPath} of this {@link Path} within the module
     * to which the given POM belongs.
     * 
     * @param pom the POM of the module in question (required)
     * @return a non-<code>null</code> instance
     */
    public PhysicalPath getModulePath(final Pom pom) {
        return getModulePath(pom.getModuleName(),
                FileUtils.getFirstDirectory(pom.getPath()), pom);
    }

    private PhysicalPath getModulePath(final String moduleName,
            final String moduleRoot, final Pom pom) {
        return new PhysicalPath(getModulePathId(moduleName), new File(
                moduleRoot, getPathRelativeToPom(pom)));
    }

    /**
     * Returns the {@link LogicalPath} for this path in the given module
     * 
     * @param moduleName can be blank for the root or only module
     * @return a non-<code>null</code> instance
     */
    public LogicalPath getModulePathId(final String moduleName) {
        return LogicalPath.getInstance(this, moduleName);
    }

    /**
     * Returns the physical path of this logical {@link Path} relative to the
     * given POM. This implementation simply delegates to
     * {@link #getDefaultLocation()}; individual enum values can override this.
     * 
     * @param pom can be <code>null</code>
     * @return
     */
    public String getPathRelativeToPom(final Pom pom) {
        return getDefaultLocation();
    }

    /**
     * Returns the {@link PhysicalPath} of this {@link Path} within the root
     * module, when no POM exists to customise its location.
     * 
     * @param projectDirectory the root directory of the user project
     * @return a non-<code>null</code> instance
     */
    public PhysicalPath getRootModulePath(final String projectDirectory) {
        return getModulePath("", projectDirectory, null);
    }

    /**
     * Indicates whether this path contains Java source code
     * 
     * @return <code>false</code> if it only contains other types of source
     *         code, e.g. XML config files, JSPX files, property files, etc.
     */
    public boolean isJavaSource() {
        return javaSource;
    }
}
