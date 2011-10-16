package org.springframework.roo.project.packaging;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.project.GAV;

/**
 * Creates the initial set of artifacts for a given Maven packaging type.
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public interface PackagingProvider {
	
	/**
	 * Returns the unique identifier of this {@link PackagingProvider}, for use
	 * in the Roo user interface.
	 * <p>
	 * The intent of this method is to allow third-party addons to provide
	 * alternative behaviour for a given Maven packaging type. For example, the
	 * core Roo WAR packaging type will have an ID of "war". If the user wants
	 * to customise how WAR modules are generated, they can implement their own
	 * {@link PackagingProvider} with an ID of (say) "custom-war". Then when the
	 * user adds a new module to their project, the shell will offer them the
	 * choice of "WAR" and "CUSTOM-WAR" for the packaging type.
	 * 
	 * @return a non-blank ID, unique when case is ignored
	 */
	String getId();

	/**
	 * Creates the initial set of artifacts (files and directories) for a module
	 * with this type of packaging; this includes setting the POM's
	 * <code>/project/packaging</code> element to the desired value.
	 *
	 * @param topLevelPackage the top-level Java package for the new project or module (required)
	 * @param nullableProjectName the project name provided by the user (can be blank)
	 * @param javaVersion the Java version for this project or module (required)
	 * @param parentPom the Maven coordinates of the parent POM (can be <code>null</code> for none)
	 */
	void createArtifacts(JavaPackage topLevelPackage, String projectName, String javaVersion, GAV parentPom);
}