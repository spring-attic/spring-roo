package org.springframework.roo.project;

import org.springframework.roo.metadata.MetadataProvider;

/**
 * Provides mutability services for {@link ProjectMetadata}.
 * 
 * @author Ben Alex
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
	
}
