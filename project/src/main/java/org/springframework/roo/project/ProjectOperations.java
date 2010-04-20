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
	 * Register a listener to track changes in plugin repositories
	 */
	void addPluginRepositoryListener(PluginRepositoryListener listener);

	/**
	 * Remove a plugin repository listener from change tracking
	 */
	void removePluginRepositoryListener(PluginRepositoryListener listener);

	/**
	 * Register a listener to track changes in build plugins
	 */
	void addPluginListener(PluginListener listener);

	/**
	 * Remove a build plugin listener from change tracking
	 */
	void removePluginListener(PluginListener listener);
	
	/**
	 * Register a listener to track changes in pom properties
	 */
	void addPropertyListener(PropertyListener listener);

	/**
	 * Remove a property listener from change tracking
	 */
	void removePropertyListener(PropertyListener listener);

	void updateProjectType(ProjectType projectType);

	/**
	 * Allows addition of a JAR dependency to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param dependency to add (required)
	 */
	void addDependency(Dependency dependency);

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
	 * Allows removal of a JAR dependency to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * dependency from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param dependency to remove (required)
	 */
	void removeDependency(Dependency dependency);

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
	 * @param repository to add (required)
	 */
	void addRepository(Repository repository);

	/**
	 * Allows addition of a repository to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param id to add (required)
	 * @param name to add (required)
	 * @param url to add (required)
	 */
	void addRepository(String id, String name, String url);

	/**
	 * Allows remove of an existing repository from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * repository from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param repository to remove (required)
	 */
	void removeRepository(Repository repository);

	/**
	 * Allows remove of an existing repository from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * repository from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param id to remove (required)
	 * @param name to remove (required)
	 * @param url to remove (required)
	 */
	void removeRepository(String id, String name, String url);

	/**
	 * Allows addition of a plugin repository to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param id to add (required)
	 * @param name to add (required)
	 * @param url to add (required)
	 */
	void addPluginRepository(String id, String name, String url);

	/**
	 * Allows remove of an existing plugin repository from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * plugin repository from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param id to remove (required)
	 * @param name to remove (required)
	 * @param url to remove (required)
	 */
	void removePluginRepository(String id, String name, String url);

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
	 * @param plugin to remove (required)
	 */
	void buildPluginUpdate(Plugin buildPlugin);
	
	/**
	 * Allows addition of a property to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param property to add (required)
	 */
	void addProperty(Property property);

	/**
	 * Allows addition of a property to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param name to add (required)
	 * @param value to add (required)
	 */
	void addProperty(String name, String value);

	/**
	 * Allows remove of an existing property from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * property from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param property to remove (required)
	 */
	void removeProperty(Property property);

	/**
	 * Allows remove of an existing property from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * property from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param name to remove (required)
	 */
	void removeProperty(String name);
}