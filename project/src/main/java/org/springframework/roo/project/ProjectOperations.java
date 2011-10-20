package org.springframework.roo.project;

import java.util.Collection;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.project.maven.Pom;

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
	 * @param moduleName the name of the module to act upon (required)
	 * @return true if the project exists, otherwise false
	 */
	boolean isProjectAvailable(String moduleName);

	/**
	 *
	 * @return true if the focused project exists, otherwise false
	 */
	boolean isFocusedProjectAvailable();
	
	/**
	 * Returns the {@link ProjectMetadata} for the project.
	 * 
	 * @param moduleName the name of the module to act upon (required)
	 * @return the {@link ProjectMetadata} object;
	 */
	ProjectMetadata getProjectMetadata(String moduleName);

	/**
	 *
	 * @return the focused {@link ProjectMetadata} object;
	 */
	ProjectMetadata getFocusedProjectMetadata();
	
	/**
	 * Convenience method to return the {@link PathResolver} from the project's {@link ProjectMetadata}.
	 * 
	 * @return the {@link PathResolver}, or null if the project is unavailable
	 */
	PathResolver getPathResolver();

	
	/**
	 * Attempts to update the project packaging type as defined via {@link ProjectType}. If the
	 * project packaging is not defined it will create a new definition.
	 *
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 *
	 * @param moduleName the name of the module to act upon (required)
	 * @param projectType the project type to update (required)
	 */
	void updateProjectType(final String moduleName, ProjectType projectType);
	
	/**
	 * Attempts to add the specified dependencies. If all the dependencies already exist according
	 * to {@link ProjectMetadata#isAllDependencyRegistered(Dependency)}, the method silently returns.
	 * Otherwise each new dependency is added.
	 *
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 *
	 * @param moduleName the name of the module to act upon (required)
	 * @param dependencies the dependencies to add (required)
	 */
	void addDependencies(final String moduleName, Collection<? extends Dependency> dependencies);

	/**
	 * Attempts to add the specified dependency. If the dependency already exists according to
	 * to {@link ProjectMetadata#isDependencyRegistered(org.springframework.roo.project.Dependency)}, the method silently returns.
	 * Otherwise the dependency is added.
	 *
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param moduleName the name of the module to act upon (required)
	 * @param dependency the dependency to add (required)
	 */
	void addDependency(final String moduleName, Dependency dependency);

	/**
	 * Allows addition of a JAR dependency to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param moduleName the name of the module to act upon (required)
	 * @param groupId the group id of the dependency (required)
	 * @param artifactId the artifact id of the dependency (required)
	 * @param version the version of the dependency (required)
	 * @param scope the scope of the dependency
	 * @param classifier the classifier of the dependency
	 */
	void addDependency(final String moduleName, String groupId, String artifactId, String version, DependencyScope scope, String classifier);

	/**
	 * Allows addition of a JAR dependency to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param moduleName the name of the module to act upon (required)
	 * @param groupId the group id of the dependency (required)
	 * @param artifactId the artifact id of the dependency (required)
	 * @param version the version of the dependency (required)
	 * @param scope the scope of the dependency
	 */
	void addDependency(final String moduleName, String groupId, String artifactId, String version, DependencyScope scope);

	/**
	 * Allows addition of a JAR dependency to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param moduleName the name of the module to act upon (required)
	 * @param groupId the group id of the dependency (required)
	 * @param artifactId the artifact id of the dependency (required)
	 * @param version the version of the dependency (required)
	 */
	void addDependency(final String moduleName, String groupId, String artifactId, String version);

	/**
	 * Attempts to remove the specified dependencies. If all the dependencies do not exist according
	 * to {@link ProjectMetadata#isDependencyRegistered(Dependency)}, the method silently returns.
	 * Otherwise each located dependency is removed.
	 *
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 *
	 * @param moduleName the name of the module to act upon (required)
	 * @param dependencies the dependencies to remove (required)
	 */
	void removeDependencies(final String moduleName, Collection<? extends Dependency> dependencies);

	/**
	 * Attempts to remove the specified dependency. If the dependency does not exist according
	 * to {@link ProjectMetadata#isDependencyRegistered(Dependency)}, the method silently returns.
	 * Otherwise the located dependency is removed.
	 *
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param moduleName the name of the module to act upon (required)
	 * @param dependency the dependency to remove (required)
	 */
	void removeDependency(final String moduleName, Dependency dependency);

	/**
	 * Allows remove of an existing JAR dependency from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * dependency from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param moduleName the name of the module to act upon (required)
	 * @param groupId the group id of the dependency (required)
	 * @param artifactId the artifact id of the dependency (required)
	 * @param version the version of the dependency (required)
	 * @param classifier the classifier of the dependency
	 */
	void removeDependency(final String moduleName, String groupId, String artifactId, String version, String classifier);

	/**
	 * Allows remove of an existing JAR dependency from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * dependency from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param moduleName the name of the module to act upon (required)
	 * @param groupId the group id of the dependency (required)
	 * @param artifactId the artifact id of the dependency (required)
	 * @param version the version of the dependency (required)
	 */
	void removeDependency(final String moduleName, String groupId, String artifactId, String version);

	/**
	 * Attempts to update the scope of the specified dependency. If the dependency does not exist according
	 * to {@link ProjectMetadata#isDependencyRegistered(Dependency)}, the method silently returns.
	 * Otherwise the located dependency is updated.
	 * 
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param moduleName the name of the module to act upon (required)
	 * @param dependency the dependency to update (required)
	 * @param dependencyScope the dependency scope. May be null, in which case the <scope> element will be removed
	 */
	void updateDependencyScope(final String moduleName, Dependency dependency, DependencyScope dependencyScope);

	/**
	 * Attempts to add the specified repositories. If all the repositories already exists according
	 * to {@link ProjectMetadata#isRepositoryRegistered(Repository)}, the method silently returns.
	 * Otherwise each new repository is added.
	 *
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param moduleName the name of the module to act upon (required)
	 * @param repositories a list of repositories to add (required)
	 */
	void addRepositories(final String moduleName, Collection<? extends Repository> repositories);

	/**
	 * Attempts to add the specified repository. If the repository already exists according
	 * to {@link ProjectMetadata#isRepositoryRegistered(Repository)}, the method silently returns.
	 * Otherwise the repository is added.
	 *
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param moduleName the name of the module to act upon (required)
	 * @param repository the repository to add (required)
	 */
	void addRepository(final String moduleName, Repository repository);

	/**
	 * Attempts to remove the specified repository. If the repository does not
	 * exist according to {@link ProjectMetadata#isRepositoryRegistered(Repository)},
	 * the method silently returns. Otherwise the located repository is removed.
	 *
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param moduleName the name of the module to act upon (required)
	 * @param repository the repository to remove (required)
	 */
	void removeRepository(final String moduleName, Repository repository);

	/**
	 * Attempts to add the specified plugin repositories. If all the repositories already exists according
	 * to {@link ProjectMetadata#isPluginRepositoryRegistered(Repository)}, the method silently returns.
	 * Otherwise each new repository is added.
	 *
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param moduleName the name of the module to act upon (required)
	 * @param repositories a list of plugin repositories to add (required)
	 */
	void addPluginRepositories(final String moduleName, Collection<? extends Repository> repositories);

	/**
	 * Attempts to add the specified plugin repository. If the plugin repository already exists according
	 * to {@link ProjectMetadata#isPluginRepositoryRegistered(Repository)}, the method silently returns.
	 * Otherwise the repository is added.
	 *
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param moduleName the name of the module to act upon (required)
	 * @param repository the plugin repository to add (required)
	 */
	void addPluginRepository(final String moduleName, Repository repository);
	
	/**
	 * Attempts to remove the specified plugin repository. If the plugin repository does not
	 * exist according to {@link ProjectMetadata#isPluginRepositoryRegistered(Repository)},
	 * the method silently returns. Otherwise the located plugin repository is removed.
	 *
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param moduleName the name of the module to act upon (required)
	 * @param repository the plugin repository to remove (required)
	 */
	void removePluginRepository(final String moduleName, Repository repository);

	/**
	 * Attempts to add the specified plugins. If all the plugins already exist according
	 * to {@link ProjectMetadata#isAllPluginRegistered(Plugin)}, the method silently returns.
	 * Otherwise each new dependency is added.
	 *
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 *
	 * @param moduleName the name of the module to act upon (required)
	 * @param plugins the plugins to add (required)
	 */
	void addBuildPlugins(final String moduleName, Collection<? extends Plugin> plugins);

	/**
	 * Attempts to add the specified build plugin. If the plugin already exists
	 * according to {@link ProjectMetadata#isBuildPluginRegistered(org.springframework.roo.project.Plugin)},
	 * the method silently returns. Otherwise the plugin is added.
	 *
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 *
	 * @param moduleName the name of the module to act upon (required)
	 * @param plugin the plugin to add (required)
	 */
	void addBuildPlugin(final String moduleName, Plugin plugin);

	/**
	 * Removes any plugins with the same groupId and artifactId as any of the
	 * given plugins.
	 *
	 * @param moduleName the name of the module to act upon (required)
	 * @param plugins the plugins to remove; can be <code>null</code>, any
	 * <code>null</code> elements will be quietly ignored
	 * @throws IllegalArgumentException if this method is called before the
	 * {@link ProjectMetadata} is available, or if the on-disk representation
	 * cannot be modified for any reason
	 */
	void removeBuildPlugins(final String moduleName, Collection<? extends Plugin> plugins);
	
	/**
	 * Removes any plugins with the same groupId and artifactId as the given
	 * plugin.
	 *
	 * @param moduleName the name of the module to act upon (required)
	 * @param plugin the plugin to remove (can be <code>null</code>)
	 * @throws IllegalArgumentException if this method is called before the
	 * {@link ProjectMetadata} is available, or if the on-disk representation
	 * cannot be modified for any reason
	 */
	void removeBuildPlugin(final String moduleName, Plugin plugin);

	/**
	 * Verifies if the specified  build plugin is present. If it is present, silently returns. If it is not
	 * present, removes any build plugin which matches {@link ProjectMetadata#getBuildPluginsExcludingVersion(Plugin)}.
	 * Always adds the presented plugin.
	 * 
	 * @param moduleName the name of the module to act upon (required)
	 * @param plugin the build plugin to update (required)
	 */
	void updateBuildPlugin(final String moduleName, Plugin plugin);
	
	/**
	 * Verifies if the specified  build plugin is present. If it is present, silently returns. If it is not
	 * present, removes any build plugin which matches {@link ProjectMetadata#getBuildPluginsExcludingVersion(Plugin)}.
	 * Always adds the presented plugin.
	 * 
	 * <p>
	 * This method is deprecated - use {@link #updateBuildPlugin(Plugin)} instead.
	 * 
	 * @param moduleName the name of the module to act upon (required)
	 * @param plugin the build plugin to update (required)
	 */
	@Deprecated
	void buildPluginUpdate(final String moduleName, Plugin plugin);

	/**
	 * Attempts to add the specified property. If the property already exists
	 * according to {@link ProjectMetadata#isPropertyRegistered(org.springframework.roo.project.Property)},
	 * the method silently returns. Otherwise the property is added.
	 *
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 *
	 * @param moduleName the name of the module to act upon (required)
	 * @param property the property to add (required)
	 */
	void addProperty(final String moduleName, Property property);

	/**
	 * Attempts to remove the specified property dependency. If the dependency does not
	 * exist according to {@link ProjectMetadata#isPropertyRegistered(Property)},
	 * the method silently returns. Otherwise the located dependency is removed.
	 *
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 *
	 * @param moduleName the name of the module to act upon (required)
	 * @param property the property to remove (required)
	 */
	void removeProperty(final String moduleName, Property property);
	
	/**
	 * Attempts to add the specified filter. If the filter already exists according
	 * to {@link ProjectMetadata#isFilterRegistered(org.springframework.roo.project.Filter)}, the method silently returns.
	 * Otherwise the filter is added.
	 *
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param moduleName the name of the module to act upon (required)
	 * @param filter the filter to add (required)
	 */
	void addFilter(final String moduleName, Filter filter);
	
	/**
	 * Attempts to remove the specified filter. If the filter does not
	 * exist according to {@link ProjectMetadata#isFilterRegistered(org.springframework.roo.project.Filter)},
	 * the method silently returns. Otherwise the located filter is removed.
	 *
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param moduleName the name of the module to act upon (required)
	 * @param filter the filter to remove (required)
	 */
	void removeFilter(final String moduleName, Filter filter);

	/**
	 * Attempts to add the specified resource. If the resource already exists according
	 * to {@link ProjectMetadata#isResourceRegistered(Resource)}, the method silently returns.
	 * Otherwise the resource is added.
	 *
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param moduleName the name of the module to act upon (required)
	 * @param resource the resource to add (required)
	 */
	void addResource(final String moduleName, Resource resource);
	
	/**
	 * Attempts to remove the specified resource. If the resource does not
	 * exist according to {@link ProjectMetadata#isResourceRegistered(Resource)},
	 * the method silently returns. Otherwise the located resource is removed.
	 *
	 * <p>
	 * An exception is thrown if this method is called before there is {@link ProjectMetadata}
	 * available, or if the on-disk representation cannot be modified for any reason.
	 * 
	 * @param moduleName the name of the module to act upon (required)
	 * @param resource the resource to remove (required)
	 */
	void removeResource(String moduleName, Resource resource);

	/**
	 * Determines whether the Database.com Maven dependency exists in the pom.
	 *
	 * @param moduleName the name of the module to act upon (required)
	 * @return true if the com.force.sdk is present in the pom.xml, otherwise false
	 */
	boolean isDatabaseDotComEnabled(String moduleName);

	/**
	 * Determines whether the DataNucleus Maven plugin exists in the pom.
	 *
	 * @param moduleName the name of the module to act upon (required)
	 * @return true if the maven-datanucleus-plugin is present in the pom.xml, otherwise false
	 */
	boolean isDataNucleusEnabled(String moduleName);

	/**
	 * Determines whether the Google App Engine Maven plugin exists in the pom.
	 *
	 * @param moduleName the name of the module to act upon (required)
	 * @return true if the maven-gae-plugin is present in the pom.xml, otherwise false
	 */
	boolean isGaeEnabled(String moduleName);

	/**
	 * Determines whether the GWT Maven plugin exists in the pom.
	 *
	 * @param moduleName the name of the module to act upon (required)
	 * @return true if the gwt-maven-plugin is present in the pom.xml, otherwise false
	 */
	boolean isGwtEnabled(String moduleName);

	/**
	 *
	 * @param moduleName the name of the module to act upon (required)
	 * @return
	 */
	JavaPackage getTopLevelPackage(String moduleName);

	/**
	 *
	 * @param moduleName the name of the module to act upon (required)
	 * @return
	 */
	String getProjectName(String moduleName);

	/**
	 *
	 * @return
	 */
	JavaPackage getFocusedTopLevelPackage();

	/**
	 *
	 * @return
	 */
	String getFocusedProjectName();

	/**
	 *
	 * @param module
	 */
	void setModule(Pom module);

	/**
	 *
	 * @return
	 */
	Pom getFocusedModule();

	/**
	 *
	 * @return
	 */
	String getFocusedModuleName();

	/**
	 *
	 * @param moduleName the name of the module to act upon (required)
	 * @return
	 */
	Pom getPomFromModuleName(String moduleName);

	/**
	 *
	 * @return
	 */
	PomManagementService getPomManagementService();

	/**
	 *
	 * @param moduleName the name of the module to act upon (required)
	 */
	void addModuleDependency(String moduleName);
}