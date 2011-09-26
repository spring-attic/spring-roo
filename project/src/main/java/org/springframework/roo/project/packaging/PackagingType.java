package org.springframework.roo.project.packaging;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.project.GAV;

/**
 * A Maven packaging type.
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public interface PackagingType {

	/**
	 * Returns the Maven name for this type of packaging, i.e. the text that
	 * appears in the POM's <code>&lt;packaging&gt;</code> element.
	 * 
	 * @return a non-blank name
	 */
	String getName();

	/**
	 * Creates the initial set of artifacts (files and directories) for a project or module having this type of packaging
	 * 
	 * @param topLevelPackage the top-level Java package for the new project or module (required)
	 * @param nullableProjectName the project name provided by the user (can be blank)
	 * @param javaVersion the Java version for this project or module (required)
	 * @param parentPom the Maven coordinates of the parent POM (can be <code>null</code> for none)
	 */
	void createArtifacts(JavaPackage topLevelPackage, String projectName, String javaVersion, GAV parentPom);
}