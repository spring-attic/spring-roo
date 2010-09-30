package org.springframework.roo.project;

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
	 * Attempts to add the specified dependency. If the dependency already exists according
	 * to {@link ProjectMetadata#isDependencyRegistered(Dependency)}, the method silently returns.
	 * Otherwise the dependency is added.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param dependency to add (required)
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
	 * @param dependency to remove (required)
	 */
	void removeDependency(Dependency dependency);
	
	/**
	 * Attempts to add the specified build plugin. If the plugin already exists 
	 * according to {@link ProjectMetadata#isBuildPluginRegistered(Plugin)}, 
	 * the method silently returns. Otherwise the plugin is added.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param plugin to add (required)
	 */
	void addBuildPlugin(Plugin plugin);
	
	/**
	 * Attempts to remove the specified build plugin dependency. If the dependency does not 
	 * exist according to {@link ProjectMetadata#isBuildPluginRegistered(Plugin)},
	 * the method silently returns. Otherwise the located dependency is removed.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param plugin to remove (required)
	 */
	void removeBuildPlugin(Plugin plugin);
	
	/**
	 * Attempts to add the specified repository. If the repository already exists according
	 * to {@link ProjectMetadata#isRepositoryRegistered(Repository)}, the method silently returns.
	 * Otherwise the repository is added.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param repository to add (required)
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
	 * @param repository to remove (required)
	 */
	void removeRepository(Repository repository);
	
	/**
	 * Attempts to add the specified plugin repository. If the plugin repository already exists according
	 * to {@link ProjectMetadata#isPluginRepositoryRegistered(Repository)}, the method silently returns.
	 * Otherwise the repository is added.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param repository repository to add (required)
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
	 * @param repository repository to remove (required)
	 */
	void removePluginRepository(Repository repository);

	/**
	 * Attempts to add the specified property. If the property already exists 
	 * according to {@link ProjectMetadata#isPropertyRegistered(Property)}, 
	 * the method silently returns. Otherwise the property is added.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param property to add (required)
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
	 * @param property to remove (required)
	 */
	void removeProperty(Property property);

	/**
	 * Attempts to add the specified filter. If the filter already exists according
	 * to {@link ProjectMetadata#isFilterRegistered(Filter)}, the method silently returns.
	 * Otherwise the filter is added.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param filter to add (required)
	 */
	void addFilter(Filter filter);
	
	/**
	 * Attempts to remove the specified filter. If the filter does not 
	 * exist according to {@link ProjectMetadata#isFilterRegistered(Filter)},
	 * the method silently returns. Otherwise the located filter is removed.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param filter to remove (required)
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
	 * @param resource to add (required)
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
	 * @param resource to remove (required)
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
	 * @param projectType to update (required)
	 */
	void updateProjectType(ProjectType projectType);
}
