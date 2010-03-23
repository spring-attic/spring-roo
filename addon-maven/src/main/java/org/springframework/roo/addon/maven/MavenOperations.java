package org.springframework.roo.addon.maven;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.project.ProjectOperations;

/**
 * Interface to methods available in {@link MavenOperationsImpl}.
 * 
 * @author Ben Alex
 * @since 1.1
 *
 */
public interface MavenOperations extends ProjectOperations {

	boolean isCreateProjectAvailable();

	String getProjectRoot();

	void createProject(Template template, JavaPackage topLevelPackage,
			String projectName, Integer majorJavaVersion);

}