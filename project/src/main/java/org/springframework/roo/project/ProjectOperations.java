package org.springframework.roo.project;


import java.util.List;

import org.springframework.roo.project.listeners.DependencyListener;
import org.springframework.roo.project.listeners.FilterListener;
import org.springframework.roo.project.listeners.PluginListener;
import org.springframework.roo.project.listeners.PropertyListener;
import org.springframework.roo.project.listeners.RepositoryListener;

/**
 * Specifies methods for various project-related operations.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@SuppressWarnings("deprecation")
public interface ProjectOperations {

	/** 
	 * Determines where is project is available.
	 * 
	 * @return true if the project exists, otherwise false
	 */
	boolean isProjectAvailable();
	
	/**
	 * Returns the {@link ProjectMetadata} for the project.
	 * 
	 * @return the {@link ProjectMetadata} object;
	 */
	ProjectMetadata getProjectMetadata();
	
	/**
	 * Convenience method to return the {@link PathResolver} from the project's {@link ProjectMetadata}.
	 * 
	 * @return the {@link PathResolver}, or null if the project is unavailable
	 */
	PathResolver getPathResolver();
	
	/**
	 * Register a listener to track changes in build dependencies.
	 * 
	 * @param listener the DependencyListener to register.
	 */
	@Deprecated
	void addDependencyListener(DependencyListener listener);

	/**
	 * Remove a dependency listener from change tracking.
	 *
	 * @param listener the DependencyListener to remove.
	 */
	@Deprecated
	void removeDependencyListener(DependencyListener listener);

	/**
	 * Register a listener to track changes in repositories.
	 *
	 * @param listener the RepositoryListener to register.
	 */
	@Deprecated
	void addRepositoryListener(RepositoryListener listener);

	/**
	 * Remove a repository listener from change tracking
	 *
	 * @param listener the RepositoryListener to remove.
	 */
	@Deprecated
	void removeRepositoryListener(RepositoryListener listener);
	
	/**
	 * Register a listener to track changes in plugin repositories
	 *
	 * @param listener the RepositoryListener to register.
	 */
	@Deprecated
	void addPluginRepositoryListener(RepositoryListener listener);

	/**
	 * Remove a plugin repository listener from change tracking
	 *
	 * @param listener the RepositoryListener to remove.
	 */
	@Deprecated
	void removePluginRepositoryListener(RepositoryListener listener);

	/**
	 * Register a listener to track changes in build plugins
	 *
	 * @param listener the PluginListener to register.
	 */
	@Deprecated
	void addPluginListener(PluginListener listener);

	/**
	 * Remove a build plugin listener from change tracking
	 *
	 * @param listener the PluginListener to remove.
	 */
	@Deprecated
	void removePluginListener(PluginListener listener);
	
	/**
	 * Register a listener to track changes in pom properties
	 */
	@Deprecated
	void addPropertyListener(PropertyListener listener);

	/**
	 * Remove a property listener from change tracking
	 *
	 * @param listener the PropertyListener to remove.
	 */
	@Deprecated
	void removePropertyListener(PropertyListener listener);
	
	/**
	 * Register a listener to track changes in pom filters

	 * @param listener the FilterListener to register.
	 */
	@Deprecated
	void addFilterListener(FilterListener listener);
	
	/**
	 * Remove a filter listener from change tracking
	 *
	 * @param listener the FilterListener to remove.
	 */
	@Deprecated
	void removeFilterListener(FilterListener listener);
	
	/**
	 * Updates the project type.
	 * 
	 * @param projectType the ProjectType to update.
	 */
	void updateProjectType(ProjectType projectType);
	
	/**
	 * Allows addition of JAR dependencies to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param dependencies a list of dependencies to add (required)
	 */
	void addDependencies(List<Dependency> dependencies);

	/**
	 * Allows addition of a JAR dependency to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param dependency the dependency to add (required)
	 */
	void addDependency(Dependency dependency);

	/**
	 * Allows addition of a JAR dependency to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param groupId the group id of the dependency (required)
	 * @param artifactId the artifact id of the dependency (required)
	 * @param version the version of the dependency (required)
	 * @param scope the scope of the dependency
	 * @param classifier the classifier of the dependency
	 */
	void addDependency(String groupId, String artifactId, String version, DependencyScope scope, String classifier);

	/**
	 * Allows addition of a JAR dependency to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param groupId the group id of the dependency (required)
	 * @param artifactId the artifact id of the dependency (required)
	 * @param version the version of the dependency (required)
	 * @param scope the scope of the dependency
	 */
	void addDependency(String groupId, String artifactId, String version, DependencyScope scope);

	/**
	 * Allows addition of a JAR dependency to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param groupId the group id of the dependency (required)
	 * @param artifactId the artifact id of the dependency (required)
	 * @param version the version of the dependency (required)
	 */
	void addDependency(String groupId, String artifactId, String version);

	/**
	 * Allows removal of JAR dependencies from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param dependencies a list of dependencies to remove (required)
	 */
	void removeDependencies(List<Dependency> dependencies);

	/**
	 * Allows removal of a JAR dependency from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * dependency from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param dependency the dependency to remove (required)
	 */
	void removeDependency(Dependency dependency);

	/**
	 * Allows remove of an existing JAR dependency from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * dependency from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param groupId the group id of the dependency (required)
	 * @param artifactId the artifact id of the dependency (required)
	 * @param version the version of the dependency (required)
	 * @param classifier the classifier of the dependency
	 */
	void removeDependency(String groupId, String artifactId, String version, String classifier);

	/**
	 * Allows remove of an existing JAR dependency from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * dependency from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param groupId the group id of the dependency (required)
	 * @param artifactId the artifact id of the dependency (required)
	 * @param version the version of the dependency (required)
	 */
	void removeDependency(String groupId, String artifactId, String version);

	/**
	 * Allows addition of repositories to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param repositories a list of repositories to add (required)
	 */
	void addRepositories(List<Repository> repositories);

	/**
	 * Allows addition of a repository to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param repository the repository to add (required)
	 */
	void addRepository(Repository repository);

	/**
	 * Allows remove of an existing repository from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * repository from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param repository the repository to remove (required)
	 */
	void removeRepository(Repository repository);

	/**
	 * Allows addition of plugin repositories to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param repositories a list of plugin repositories to add (required)
	 */
	void addPluginRepositories(List<Repository> repositories);

	/**
	 * Allows addition of a plugin repository to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param repository the plugin repository to add (required)
	 */
	void addPluginRepository(Repository repository);
	
	/**
	 * Allows remove of an existing plugin repository from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * plugin repository from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param repository the plugin repository to remove (required)
	 */
	void removePluginRepository(Repository repository);

	/**
	 * Allows addition of a build plugins to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add 
	 * a new build capability to their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param plugins a list build plugins to add (required)
	 */
	void addBuildPlugins(List<Plugin> plugins);

	/**
	 * Allows addition of a build plugin to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add 
	 * a new build capability to their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param plugin the build plugin to add (required)
	 */
	void addBuildPlugin(Plugin plugin);

	/**
	 * Allows removal of an existing build plugins from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * build plugins from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param plugins a list build plugins to remove (required)
	 */
	void removeBuildPlugins(List<Plugin> plugins);
	
	/**
	 * Allows removall of an existing build plugin from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * build plugin from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param plugin the build plugin to remove (required)
	 */
	void removeBuildPlugin(Plugin plugin);

	/**
	 * Verifies if the specified  build plugin is present. If it is present, silently returns. If it is not
	 * present, removes any build plugin which matches {@link ProjectMetadata#getBuildPluginsExcludingVersion(Plugin)}.
	 * Always adds the presented plugin.
	 * 
	 * @param plugin the build plugin to update (required)
	 */
	void updateBuildPlugin(Plugin plugin);
	
	/**
	 * Verifies if the specified  build plugin is present. If it is present, silently returns. If it is not
	 * present, removes any build plugin which matches {@link ProjectMetadata#getBuildPluginsExcludingVersion(Plugin)}.
	 * Always adds the presented plugin.
	 * 
	 * <p>
	 * This method is deprecated - use {@link #updateBuildPlugin(Plugin)} instead.
	 * 
	 * @param plugin the build plugin to update (required)
	 */
	@Deprecated
	void buildPluginUpdate(Plugin plugin);

	/**
	 * Allows addition of a property to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param property the POM property to add (required)
	 */
	void addProperty(Property property);

	/**
	 * Allows remove of an existing property from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * property from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param property the POM property to remove (required)
	 */
	void removeProperty(Property property);
	
	/**
	 * Allows addition of a filter to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param filter the filter to add (required)
	 */
	void addFilter(Filter filter);
	
	/**
	 * Allows remove of an existing filter from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * filter from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param filter the filter to remove (required)
	 */
	void removeFilter(Filter filter);

	/**
	 * Allows addition of a resource to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param resource the resource to add (required)
	 */
	void addResource(Resource resource);
	
	/**
	 * Allows remove of an existing resource from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * resource from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param resource the resource to remove (required)
	 */
	void removeResource(Resource resource);
}