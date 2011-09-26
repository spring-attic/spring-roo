package org.springframework.roo.project;

import java.io.IOException;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.project.packaging.PackagingType;

/**
 * Provides Maven project operations.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public interface MavenOperations extends ProjectOperations {

	/**
	 * Indicates whether the "create project" command is available
	 * 
	 * @return see above
	 */
	boolean isCreateProjectAvailable();

	String getProjectRoot();

	/**
	 * Creates a Maven-based project
	 * 
	 * @param topLevelPackage the top-level Java package (required)
	 * @param projectName the name of the project (can be blank to infer it from the top-level package)
	 * @param majorJavaVersion the major Java version for which this project is targetted (can be <code>null</code> to autodetect)
	 * @param parent the Maven coordinates of the parent POM (can be <code>null</code> for none)
	 * @param packagingType the Maven packaging of the project to create (pom, war, jar, ear, etc.) (required)
	 */
	void createProject(JavaPackage topLevelPackage, String projectName, Integer majorJavaVersion, GAV parent, PackagingType packagingType);

	/**
	 * Executes the given Maven command
	 * 
	 * @param command the options and arguments to pass to the Maven executable (required)
	 * @throws IOException
	 */
	void executeMvnCommand(String command) throws IOException;

	/**
	 * Indicates whether new modules can be created in the current project
	 * 
	 * @return see above
	 */
	boolean isCreateModuleAvailable();

	/**
	 * Creates a module within an existing multi-module Maven project
	 * 
	 * @param topLevelPackage the top-level Java package (required)
	 * @param name the name of the project (can be blank to infer it from the top-level package)
	 * @param parent the Maven coordinates of the parent POM (can be <code>null</code> for none)
	 * @param packagingType the Maven packaging of the module to create (pom, war, jar, ear, etc.) (required)
	 */
	void createModule(JavaPackage topLevelPackage, String name, GAV parent,	PackagingType packagingType);

	/**
	 * Changes the focus to the given module (or root) of the project.
	 * 
	 * @param module the module to focus on (required)
	 */
	void focus(final GAV module);
}