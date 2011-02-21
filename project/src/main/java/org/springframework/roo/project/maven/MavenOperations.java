package org.springframework.roo.project.maven;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.project.ProjectOperations;

/**
 * Interface to methods available in {@link MavenOperationsImpl}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public interface MavenOperations extends ProjectOperations {

	boolean isCreateProjectAvailable();

	String getProjectRoot();

	void createProject(JavaPackage topLevelPackage, String projectName, Integer majorJavaVersion);
}