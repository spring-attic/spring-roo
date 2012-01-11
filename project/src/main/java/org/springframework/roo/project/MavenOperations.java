package org.springframework.roo.project;

import java.io.IOException;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.project.packaging.PackagingProvider;

/**
 * Provides Maven project operations.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public interface MavenOperations extends ProjectOperations {

    /**
     * Creates a module within an existing Maven project
     * 
     * @param topLevelPackage the top-level Java package (required)
     * @param parentPom the Maven coordinates of the parent POM (can be
     *            <code>null</code> for none)
     * @param moduleName the name and artifactId of the new module
     * @param packagingType the packaging of the module (can be
     *            <code>null</code> to use the default)
     * @param majorJavaVersion the major Java version to which this module is
     *            targetted (can be <code>null</code> to autodetect)
     */
    void createModule(JavaPackage topLevelPackage, GAV parentPom,
            String moduleName, PackagingProvider packagingType,
            Integer majorJavaVersion);

    /**
     * Creates a Maven-based project
     * 
     * @param topLevelPackage the top-level Java package (required)
     * @param projectName the name of the project (can be blank to generate it
     *            from the top-level package)
     * @param majorJavaVersion the major Java version to which this project is
     *            targetted (can be <code>null</code> to autodetect)
     * @param parentPom the Maven coordinates of the parent POM (can be
     *            <code>null</code> for none)
     * @param packagingType the packaging of the project (can be
     *            <code>null</code> to use the default)
     */
    void createProject(JavaPackage topLevelPackage, String projectName,
            Integer majorJavaVersion, GAV parentPom,
            PackagingProvider packagingType);

    /**
     * Executes the given Maven command
     * 
     * @param command the command and any arguments it requires (e.g.
     *            "-o clean install")
     * @throws IOException
     */
    void executeMvnCommand(String command) throws IOException;

    String getProjectRoot();

    /**
     * Indicates whether a new Maven project can be created
     * 
     * @return see above
     */
    boolean isCreateProjectAvailable();
}