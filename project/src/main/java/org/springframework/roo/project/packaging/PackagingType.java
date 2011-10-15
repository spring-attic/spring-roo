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
	 * Returns the unique identifier of this {@link PackagingType}, for use in
	 * the Roo user interface.
	 * <p>
	 * The intent of this method is to allow third-party addons to provide
	 * alternative behaviour for a given Maven packaging type. For example, the
	 * core Roo WAR packaging type will have an ID of "war". If the user wants
	 * to customise how WAR modules are generated, they can implement their own
	 * {@link PackagingType} with an ID of (say) "my-war". The Roo shell will
	 * then offer them a choice of "WAR" and "MY-WAR" when entering the "new
	 * module" command; both of these options will result in the new module
	 * having a Maven packaging type of "war" (as both the core and third-party
	 * {@link PackagingType}s will return that value when {@link #getName()} is
	 * called).
	 * 
	 * @return a non-blank ID, unique when case is ignored
	 */
	String getId();

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