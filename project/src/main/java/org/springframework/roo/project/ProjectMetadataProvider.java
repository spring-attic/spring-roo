package org.springframework.roo.project;

import org.springframework.roo.metadata.MetadataProvider;

/**
 * Provides mutability services for {@link ProjectMetadata}.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @since 1.0
 *
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
	public void addDependency(Dependency dependency);
	
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
	public void removeDependency(Dependency dependency);
	
	/**
	 * Attempts to add the specified build plugin dependency. If the dependency already exists 
	 * according to {@link ProjectMetadata#isBuildPluginDependencyRegistered(Dependency)}, 
	 * the method silently returns. Otherwise the dependency is added.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param dependency to add (required)
	 * @param execution specifications for build plugin (optional)
	 */
	public void addBuildPluginDependency(Dependency dependency,Execution... executions);
	
	/**
	 * Attempts to remove the specified build plugin dependency. If the dependency does not 
	 * exist according to {@link ProjectMetadata#isBuildPluginDependencyRegistered(Dependency)},
	 * the method silently returns. Otherwise the located dependency is removed.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param dependency to remove (required)
	 */
	public void removeBuildPluginDependency(Dependency dependency);
	
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
	public void addRepository(Repository repository);
	
	
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
	public void removeRepository(Repository repository);
	
	
	/**
	 * Attempts to update the project packaging type as defined via {@link ProjectType}. If the 
	 * project packaging is not defined it will create a new definition.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param ProjectType to update (required)
	 */
	public void updateProjectType(ProjectType projectType);
	
}
