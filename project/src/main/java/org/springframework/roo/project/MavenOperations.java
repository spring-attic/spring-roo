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
	 * @param projectName the name of the project (can be blank to generate it from the top-level package)
	 * @param majorJavaVersion the major Java version for which this project is targetted (can be <code>null</code> to autodetect)
	 * @param parent the Maven coordinates of the parent POM, in the form "groupId:artifactId:version" (can be blank for none)
	 * @param packagingType the Maven packaging of the project or module to create (pom, war, jar, ear, etc.) (required)
	 */
	void createProject(JavaPackage topLevelPackage, String projectName, Integer majorJavaVersion, String parent, PackagingType packagingType);

	void executeMvnCommand(String extra) throws IOException;
}