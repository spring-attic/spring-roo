package org.springframework.roo.project;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;

public interface ProjectOperations {

	boolean isDependencyModificationAllowed();

	boolean isPerformCommandAllowed();

	/**
	 * Register a listener to track changes in build dependencies
	 */
	void addDependencyListener(DependencyListener listener);

	/**
	 * Remove a dependency listener from change tracking
	 */
	void removeDependencyListener(DependencyListener listener);

	/**
	 * Register a listener to track changes in repositories
	 */
	void addRepositoryListener(RepositoryListener listener);

	/**
	 * Remove a repository listener from change tracking
	 */
	void removeRepositoryListener(RepositoryListener listener);
	
	
	/**
	 * Register a listener to track changes in build plugins
	 */
	void addPluginListener(PluginListener listener);

	/**
	 * Remove a build plugin listener from change tracking
	 */
	void removePluginListener(PluginListener listener);
	
	void updateProjectType(ProjectType projectType);

	/**
	 * Allows addition of a JAR dependency to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param groupId to add (required)
	 * @param artifactId to add (required)
	 * @param version to add (required)
	 */
	void addDependency(JavaPackage groupId, JavaSymbolName artifactId, String version);

	/**
	 * Allows remove of an existing JAR dependency from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * dependency from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param groupId to remove (required)
	 * @param artifactId to remove (required)
	 * @param version to remove (required)
	 */
	void removeDependency(JavaPackage groupId, JavaSymbolName artifactId, String version);

	/**
	 * Verifies if the specified dependency is present. If it is present, silently returns. If it is not
	 * present, removes any dependency which matches {@link ProjectMetadata#getDependenciesExcludingVersion(Dependency)}.
	 * Always adds the presented dependency.
	 * 
	 * @param dependency to add (required)
	 */
	void dependencyUpdate(Dependency dependency);

	/**
	 * Allows addition of a repository to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param id to add (required)
	 * @param name to add (required)
	 * @param url to add (requireD)
	 */
	void addRepository(String id, String name, String url);

	/**
	 * Allows remove of an existing repository from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * dependency from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param id to add (required)
	 * @param name to add (required)
	 * @param url to add (requireD)
	 */
	void removeRepository(String id, String name, String url);

	/**
	 * Allows addition of a build plugin to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add 
	 * a new build capability to their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param plugin to add (required)
	 */
	void addBuildPlugin(Plugin plugin);

	/**
	 * Allows remove of an existing build plugin from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * build plugin from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param plugin to add (required)
	 */
	void removeBuildPlugin(Plugin plugin);

	/**
	 * Verifies if the specified  build plugin is present. If it is present, silently returns. If it is not
	 * present, removes any build plugin which matches {@link ProjectMetadata#getBuildPluginsExcludingVersion(Plugin)}.
	 * Always adds the presented plugin.
	 * 
	 * @param plugin to add (required)
	 */
	void buildPluginUpdate(Plugin buildPlugin);
}