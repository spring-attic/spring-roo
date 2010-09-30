package org.springframework.roo.project;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;

/**
 * Specifies methods for various project-related operations.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface ProjectOperations {

	boolean isDependencyModificationAllowed();

	boolean isPerformCommandAllowed();

	/**
	 * Register a listener to track changes in build dependencies.
	 * 
	 * @param listener the DependencyListener to register.
	 */
	void addDependencyListener(DependencyListener listener);

	/**
	 * Remove a dependency listener from change tracking.
	 *
	 * @param listener the DependencyListener to remove.
	 */
	void removeDependencyListener(DependencyListener listener);

	/**
	 * Register a listener to track changes in repositories.
	 *
	 * @param listener the RepositoryListener to register.
	 */
	void addRepositoryListener(RepositoryListener listener);

	/**
	 * Remove a repository listener from change tracking
	 *
	 * @param listener the RepositoryListener to remove.
	 */
	void removeRepositoryListener(RepositoryListener listener);
	
	/**
	 * Register a listener to track changes in plugin repositories
	 *
	 * @param listener the RepositoryListener to register.
	 */
	void addPluginRepositoryListener(RepositoryListener listener);

	/**
	 * Remove a plugin repository listener from change tracking
	 *
	 * @param listener the RepositoryListener to remove.
	 */
	void removePluginRepositoryListener(RepositoryListener listener);

	/**
	 * Register a listener to track changes in build plugins
	 *
	 * @param listener the PluginListener to register.
	 */
	void addPluginListener(PluginListener listener);

	/**
	 * Remove a build plugin listener from change tracking
	 *
	 * @param listener the PluginListener to remove.
	 */
	void removePluginListener(PluginListener listener);
	
	/**
	 * Register a listener to track changes in pom properties
	 */
	void addPropertyListener(PropertyListener listener);

	/**
	 * Remove a property listener from change tracking
	 *
	 * @param listener the PropertyListener to remove.
	 */
	void removePropertyListener(PropertyListener listener);

	/**
	 * Updates the project type.
	 * 
	 * @param projectType the ProjectType to update.
	 */
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
	 * @param groupId the group id of the dependency (required)
	 * @param artifactId the artifact id of the dependency (required)
	 * @param version the version of the dependency (required)
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
	 * @param groupId the group id of the dependency (required)
	 * @param artifactId the artifact id of the dependency (required)
	 * @param version the version of the dependency (required)
	 */
	void removeDependency(JavaPackage groupId, JavaSymbolName artifactId, String version);

	/**
	 * Verifies if the specified dependency is present. If it is present, silently returns. If it is not
	 * present, removes any dependency which matches {@link ProjectMetadata#getDependenciesExcludingVersion(Dependency)}.
	 * Always adds the presented dependency.
	 * 
	 * @param dependency dependency to add (required)
	 */
	void dependencyUpdate(Dependency dependency);

	/**
	 * Allows addition of a repository to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param repository repository to add (required)
	 */
	void addRepository(Repository repository);

	/**
	 * Allows remove of an existing repository from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * repository from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param repository repository to remove (required)
	 */
	void removeRepository(Repository repository);

	/**
	 * Allows addition of a plugin repository to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param repository plugin repository to add (required)
	 */
	void addPluginRepository(Repository repository);
	
	/**
	 * Allows remove of an existing plugin repository from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * plugin repository from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param repository plugin repository to remove (required)
	 */
	void removePluginRepository(Repository repository);

	/**
	 * Allows addition of a build plugin to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add 
	 * a new build capability to their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param plugin build plugin to add (required)
	 */
	void addBuildPlugin(Plugin plugin);

	/**
	 * Allows remove of an existing build plugin from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * build plugin from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param plugin build plugin to remove (required)
	 */
	void removeBuildPlugin(Plugin plugin);

	/**
	 * Verifies if the specified  build plugin is present. If it is present, silently returns. If it is not
	 * present, removes any build plugin which matches {@link ProjectMetadata#getBuildPluginsExcludingVersion(Plugin)}.
	 * Always adds the presented plugin.
	 * 
	 * @param plugin build plugin to update (required)
	 */
	void buildPluginUpdate(Plugin plugin);
	
	/**
	 * Allows addition of a property to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param property POM property to add (required)
	 */
	void addProperty(Property property);

	/**
	 * Allows remove of an existing property from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * property from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param property POM property to remove (required)
	 */
	void removeProperty(Property property);
	
	/**
	 * Allows addition of a filter to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param filter filter to add (required)
	 */
	void addFilter(Filter filter);
	
	/**
	 * Allows remove of an existing filter from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * filter from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param filter filter to remove (required)
	 */
	void removeFilter(Filter filter);

	/**
	 * Allows addition of a resource to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param resource resource to add (required)
	 */
	void addResource(Resource resource);
	
	/**
	 * Allows remove of an existing resource from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * resource from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param resource resource to remove (required)
	 */
	void removeResource(Resource resource);
}