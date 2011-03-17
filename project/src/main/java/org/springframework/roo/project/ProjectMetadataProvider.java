package org.springframework.roo.project;

import java.util.List;

import org.springframework.roo.metadata.MetadataProvider;

/**
 * Provides mutability services for {@link ProjectMetadata}.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @since 1.0
 */
public interface ProjectMetadataProvider extends MetadataProvider {
	
	/**
	 * Attempts to add the specified dependencies. If all the dependencies already exist according
	 * to {@link ProjectMetadata#isDependencyRegistered(Dependency)}, the method silently returns.
	 * Otherwise each new dependency is added.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param dependencies the dependencies to add (required)
	 */
	void addDependencies(List<Dependency> dependencies);
	
	/**
	 * Attempts to add the specified dependency. If the dependency already exists according
	 * to {@link ProjectMetadata#isDependencyRegistered(org.springframework.roo.project.Dependency)}, the method silently returns.
	 * Otherwise the dependency is added.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param dependency the dependency to add (required)
	 */
	void addDependency(Dependency dependency);
	
	/**
	 * Attempts to remove the specified dependency. If the dependency does not exist according
	 * to {@link ProjectMetadata#isDependencyRegistered(Dependency)}, the method silently returns.
	 * Otherwise the located dependency is removed.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param dependency the dependency to remove (required)
	 */
	void removeDependency(Dependency dependency);
	
	/**
	 * Attempts to add the specified build plugin. If the plugin already exists 
	 * according to {@link ProjectMetadata#isBuildPluginRegistered(org.springframework.roo.project.Plugin)},
	 * the method silently returns. Otherwise the plugin is added.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param plugin the plugin to add (required)
	 */
	void addBuildPlugin(Plugin plugin);
	
	/**
	 * Attempts to remove the specified build plugin dependency. If the dependency does not 
	 * exist according to {@link ProjectMetadata#isBuildPluginRegistered(org.springframework.roo.project.Plugin)},
	 * the method silently returns. Otherwise the located dependency is removed.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param plugin the plugin to remove (required)
	 */
	void removeBuildPlugin(Plugin plugin);
	
	/**
	 * Attempts to add the specified repositories. If all the repositories already exists according
	 * to {@link ProjectMetadata#isRepositoryRegistered(Repository)}, the method silently returns.
	 * Otherwise each new repository is added.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param repositories a list of repositories to add (required)
	 */
	void addRepositories(List<Repository> repositories);
	
	/**
	 * Attempts to add the specified repository. If the repository already exists according
	 * to {@link ProjectMetadata#isRepositoryRegistered(Repository)}, the method silently returns.
	 * Otherwise the repository is added.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param repository the repository to add (required)
	 */
	void addRepository(Repository repository);
	
	/**
	 * Attempts to remove the specified repository. If the repository does not 
	 * exist according to {@link ProjectMetadata#isRepositoryRegistered(Repository)},
	 * the method silently returns. Otherwise the located repository is removed.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param repository the repository to remove (required)
	 */
	void removeRepository(Repository repository);
	
	/**
	 * Attempts to add the specified plugin repositories. If all the repositories already exists according
	 * to {@link ProjectMetadata#isPluginRepositoryRegistered(Repository)}, the method silently returns.
	 * Otherwise each new repository is added.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param repositories a list of plugin repositories to add (required)
	 */
	void addPluginRepositories(List<Repository> repositories);

	/**
	 * Attempts to add the specified plugin repository. If the plugin repository already exists according
	 * to {@link ProjectMetadata#isPluginRepositoryRegistered(Repository)}, the method silently returns.
	 * Otherwise the repository is added.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param repository the plugin repository to add (required)
	 */
	void addPluginRepository(Repository repository);
	
	/**
	 * Attempts to remove the specified plugin repository. If the plugin repository does not 
	 * exist according to {@link ProjectMetadata#isPluginRepositoryRegistered(Repository)},
	 * the method silently returns. Otherwise the located plugin repository is removed.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param repository the plugin repository to remove (required)
	 */
	void removePluginRepository(Repository repository);

	/**
	 * Attempts to add the specified property. If the property already exists 
	 * according to {@link ProjectMetadata#isPropertyRegistered(org.springframework.roo.project.Property)},
	 * the method silently returns. Otherwise the property is added.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param property the property to add (required)
	 */
	void addProperty(Property property);
	
	/**
	 * Attempts to remove the specified property dependency. If the dependency does not 
	 * exist according to {@link ProjectMetadata#isPropertyRegistered(Property)},
	 * the method silently returns. Otherwise the located dependency is removed.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param property the property to remove (required)
	 */
	void removeProperty(Property property);

	/**
	 * Attempts to add the specified filter. If the filter already exists according
	 * to {@link ProjectMetadata#isFilterRegistered(org.springframework.roo.project.Filter)}, the method silently returns.
	 * Otherwise the filter is added.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param filter the filter to add (required)
	 */
	void addFilter(Filter filter);
	
	/**
	 * Attempts to remove the specified filter. If the filter does not 
	 * exist according to {@link ProjectMetadata#isFilterRegistered(org.springframework.roo.project.Filter)},
	 * the method silently returns. Otherwise the located filter is removed.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param filter the filter to remove (required)
	 */
	void removeFilter(Filter filter);

	/**
	 * Attempts to add the specified resource. If the resource already exists according
	 * to {@link ProjectMetadata#isResourceRegistered(Resource)}, the method silently returns.
	 * Otherwise the resource is added.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param resource the resource to add (required)
	 */
	void addResource(Resource resource);
	
	/**
	 * Attempts to remove the specified resource. If the resource does not 
	 * exist according to {@link ProjectMetadata#isResourceRegistered(Resource)},
	 * the method silently returns. Otherwise the located resource is removed.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param resource the resource to remove (required)
	 */
	void removeResource(Resource resource);

	/**
	 * Attempts to update the project packaging type as defined via {@link ProjectType}. If the 
	 * project packaging is not defined it will create a new definition.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param projectType the project type to update (required)
	 */
	void updateProjectType(ProjectType projectType);
}
