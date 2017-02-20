package org.springframework.roo.project;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.project.maven.Pom;

/**
 * Methods for various project-related operations.
 *
 * @author Ben Alex
 * @author Paula Navarro
 * @author Juan Carlos García
 * @since 1.0
 */
public interface ProjectOperations {

  /**
   * Attempts to add the specified build plugin. If the plugin already exists
   * according to
   * {@link ProjectMetadata#isBuildPluginRegistered(org.springframework.roo.project.Plugin)}
   * , the method silently returns. Otherwise the plugin is added.
   * <p>
   * By default, includes the provided plugin into pluginManagement
   * element.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   *
   * @param moduleName the name of the module to act upon (required)
   * @param plugin the plugin to add (required)
   */
  void addBuildPlugin(final String moduleName, Plugin plugin);

  /**
   * Attempts to add the specified build plugin. If the plugin already exists
   * according to
   * {@link ProjectMetadata#isBuildPluginRegistered(org.springframework.roo.project.Plugin)}
   * , the method silently returns. Otherwise the plugin is added.
   * <p>
   * If addToPluginManagement parameter has true value, the new plugin will
   * be included into pluginManagement element. If false, the provided plugin
   * will be included only as plugin.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   * 
   * @param moduleName the name of the module to act upon (required)
   * @param addToPluginManagement boolean that indicates if the new Plugin
   * 		should be included into pluginManagement element or not.
   * @param plugin the plugin to add (required)
   */
  void addBuildPlugin(final String moduleName, Plugin plugin, boolean addToPluginManagement);

  /**
   * Attempts to add the specified plugins. If all the plugins already exist
   * according to {@link ProjectMetadata#isAllPluginRegistered(Plugin)}, the
   * method silently returns. Otherwise each new dependency is added.
   * <p>
   * By default, includes the provided plugin into pluginManagement
   * element.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   *
   * @param moduleName the name of the module to act upon (required)
   * @param plugins the plugins to add (required)
   */
  void addBuildPlugins(final String moduleName, Collection<? extends Plugin> plugins);

  /**
   * Attempts to add the specified plugins. If all the plugins already exist
   * according to {@link ProjectMetadata#isAllPluginRegistered(Plugin)}, the
   * method silently returns. Otherwise each new dependency is added.
   * <p>
   * If addToPluginManagement parameter has true value, the new plugin will
   * be included into pluginManagement element. If false, the provided plugin
   * will be included only as plugin.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   * 
   * @param moduleName the name of the module to act upon (required)
   * @param addToPluginManagement boolean that indicates if the new Plugin
   * 		should be included into pluginManagement element or not.
   * @param plugins the plugins to add (required)
   */
  void addBuildPlugins(final String moduleName, Collection<? extends Plugin> plugins,
      boolean addToPluginManagement);

  /**
   * Attempts to add the specified package into the specified plugin execution. 
   * If the package already exists or the execution does not exist in the specified plugin, the
   * method silently returns. Otherwise the package is added.
   * <p>
   * By default, includes the provided package into pluginManagement
   * element.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   * 
   * @param moduleName the name of the module to act upon (required)
   * @param plugin the plugin where add the package (required)
   * @param executionId the id of the plugin execution where add the package (required)
   * @param packageName the package to add (required)
   */
  void addPackageToPluginExecution(final String moduleName, final Plugin plugin,
      String executionId, final String packageName);

  /**
   * Attempts to add the specified package into the specified plugin execution. 
   * If the package already exists or the execution does not exist in the specified plugin, the
   * method silently returns. Otherwise the package is added.
   * <p>
   * If addToPluginManagement parameter has true value, the new plugin will
   * be included into pluginManagement element. If false, the provided plugin
   * will be included only as plugin.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   * 
   * @param moduleName the name of the module to act upon (required)
   * @param plugin the plugin where add the package (required)
   * @param executionId the id of the plugin execution where add the package (required)
   * @param packageName the package to add (required)
   * @param addToPluginManagement boolean that indicates if the new Plugin
   * 		should be included into pluginManagement element or not.
   */
  void addPackageToPluginExecution(final String moduleName, final Plugin plugin,
      String executionId, final String packageName, boolean addToPluginManagement);

  /**
   * Attempts to add the specified element into the specified plugin execution. 
   * If the element already exists or the execution does not exist in the specified plugin, the
   * method silently returns. Otherwise the element is added.
   * <p>
   * By default, includes the provided element into pluginManagement
   * element.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   * 
   * @param moduleName the name of the module to act upon (required)
   * @param plugin the plugin where add the package (required)
   * @param executionId the id of the plugin execution where add the package (required)
   * @param parentElementName the parentElement name that should be included inside configuration element
   * @param elementName the element name that should be included inside the parentElementName provided before
   * @param elementValue the value to add (required)
   */
  void addElementToPluginExecution(final String moduleName, final Plugin plugin,
      String executionId, String parentElementName, String elementName, final String elementValue);

  /**
   * Attempts to add the specified element into the specified plugin execution. 
   * If the element already exists or the execution does not exist in the specified plugin, the
   * method silently returns. Otherwise the element is added.
   * <p>
   * By default, includes the provided element into pluginManagement
   * element.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   * 
   * @param moduleName the name of the module to act upon (required)
   * @param plugin the plugin where add the package (required)
   * @param executionId the id of the plugin execution where add the package (required)
   * @param parentElementName the parentElement name that should be included inside configuration element
   * @param elementName the element name that should be included inside the parentElementName provided before
   * @param elementValue the value to add (required)
   * @param addToPluginManagement boolean that indicates if the new element
   * 		should be included into pluginManagement element or not.

   */
  void addElementToPluginExecution(final String moduleName, final Plugin plugin,
      String executionId, String parentElementName, String elementName, final String elementValue,
      boolean addToPluginManagement);

  /**
   * Attempts to add the specified element into the specified plugin execution. 
   * If the element already exists or the execution does not exist in the specified plugin, the
   * method silently returns. Otherwise the element is added.
   * <p>
   * By default, includes the provided element into pluginManagement
   * element.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   * 
   * @param moduleName the name of the module to act upon (required)
   * @param plugin the plugin where add the package (required)
   * @param executionId the id of the plugin execution where add the package (required)
   * @param parentElementName the parentElement name that should be included inside configuration element
   * @param elementName the element name that should be included inside the parentElementName provided before
   * @param elementValues the values to add (required)
   */
  void addElementToPluginExecution(final String moduleName, final Plugin plugin,
      String executionId, String parentElementName, String elementName,
      final Map<String, String> elementValues);

  /**
   * Attempts to add the specified element into the specified plugin execution. 
   * If the element already exists or the execution does not exist in the specified plugin, the
   * method silently returns. Otherwise the element is added.
   * <p>
   * By default, includes the provided element into pluginManagement
   * element.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   * 
   * @param moduleName the name of the module to act upon (required)
   * @param plugin the plugin where add the package (required)
   * @param executionId the id of the plugin execution where add the package (required)
   * @param parentElementName the parentElement name that should be included inside configuration element
   * @param elementName the element name that should be included inside the parentElementName provided before
   * @param elementValues the values to add (required)
   * @param addToPluginManagement boolean that indicates if the new element
   * 		should be included into pluginManagement element or not.

   */
  void addElementToPluginExecution(final String moduleName, final Plugin plugin,
      String executionId, String parentElementName, String elementName,
      final Map<String, String> elementValues, boolean addToPluginManagement);

  /**
   * Attempts to add the specified dependencies. If all the dependencies
   * already exist according to
   * {@link ProjectMetadata#isAllDependencyRegistered(Dependency)}, the method
   * silently returns. Otherwise each new dependency is added.
   * <p>
   * By default, includes the provided dependencies into dependencyManagement
   * element.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   *
   * @param moduleName the name of the module to act upon (required)
   * @param dependencies the dependencies to add (required)
   * @return List of dependencies added on current operation
   */
  List<Dependency> addDependencies(final String moduleName,
      Collection<? extends Dependency> dependencies);

  /**
   * Attempts to add the specified dependencies. If all the dependencies
   * already exist according to
   * {@link ProjectMetadata#isAllDependencyRegistered(Dependency)}, the method
   * silently returns. Otherwise each new dependency is added.
   * <p>
   * If addToDependencyManagement parameter has true value, the new dependency will
   * be included into dependencyManagement element. If false, the provided dependency
   * will be included only as dependency.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   * 
   * @param moduleName the name of the module to act upon (required)
   * @param dependencies the dependencies to add (required)
   * @param addToDependencyManagement boolean that indicates if the new Dependency
   * 		should be included into dependencyManagement element or not.
   * @param checkFullDependency whether should check the existence with full 
   *            dependency element or only compare 'artifactId' and 'groupId'.
   * @return List of dependencies added on current operation
   */
  List<Dependency> addDependencies(final String moduleName,
      Collection<? extends Dependency> dependencies, boolean addToDependencyManagement,
      boolean checkFullDependency);

  /**
   * Attempts to add the specified dependency. If the dependency already
   * exists according to to
   * {@link ProjectMetadata#isDependencyRegistered(org.springframework.roo.project.Dependency)}
   * , the method silently returns. Otherwise the dependency is added.
   * <p>
   * By default, includes the provided dependency into dependencyManagement
   * element.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   *
   * @param moduleName the name of the module to act upon (required)
   * @param dependency the dependency to add (required)
   * @return added dependency
   */
  Dependency addDependency(final String moduleName, Dependency dependency);

  /**
   * Attempts to add the specified dependency. If the dependency already
   * exists according to to
   * {@link ProjectMetadata#isDependencyRegistered(org.springframework.roo.project.Dependency)}
   * , the method silently returns. Otherwise the dependency is added.
   * <p>
   * If addToDependencyManagement parameter has true value, the new dependency will
   * be included into dependencyManagement element. If false, the provided dependency
   * will be included only as dependency.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   * 
   * @param moduleName the name of the module to act upon (required)
   * @param dependency the dependency to add (required)
   * @param addToDependencyManagement boolean that indicates if the new Dependency
   *        should be included into dependencyManagement element or not.
   * @return added dependency
   */
  Dependency addDependency(final String moduleName, Dependency dependency,
      boolean addToDependencyManagement);


  /**
   * Attempts to add the specified dependency. If the dependency already
   * exists according to to
   * {@link ProjectMetadata#isDependencyRegistered(org.springframework.roo.project.Dependency)}
   * , the method silently returns. Otherwise the dependency is added.
   * <p>
   * If addToDependencyManagement parameter has true value, the new dependency will
   * be included into dependencyManagement element. If false, the provided dependency
   * will be included only as dependency.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   * 
   * @param moduleName the name of the module to act upon (required)
   * @param dependency the dependency to add (required)
   * @param addToDependencyManagement boolean that indicates if the new Dependency
   * 		should be included into dependencyManagement element or not.
   * @param checkFullDependency whether should check the existence with full 
   *            dependency element or only compare 'artifactId' and 'groupId'.
   * @return added dependency
   */
  Dependency addDependency(final String moduleName, Dependency dependency,
      boolean addToDependencyManagement, boolean checkFullDependency);

  /**
   * Allows addition of a JAR dependency to the POM.
   * <p>
   * Provides a convenient way for third parties to instruct end users how to
   * use the CLI to add support for their projects without requiring the user
   * to manually edit a pom.xml or write an add-on.
   * <p>
   * By default, includes the provided dependency into dependencyManagement
   * element.
   * <p>
   * 
   * @param moduleName the name of the module to act upon (required)
   * @param groupId the group id of the dependency (required)
   * @param artifactId the artifact id of the dependency (required)
   * @param version the version of the dependency (required)
   * @return added dependency
   */
  Dependency addDependency(final String moduleName, String groupId, String artifactId,
      String version);

  /**
   * Allows addition of a JAR dependency to the POM.
   * <p>
   * Provides a convenient way for third parties to instruct end users how to
   * use the CLI to add support for their projects without requiring the user
   * to manually edit a pom.xml or write an add-on.
   * <p>
   * If addToDependencyManagement parameter has true value, the new dependency will
   * be included into dependencyManagement element. If false, the provided dependency
   * will be included only as dependency.
   * <p>
   * 
   * @param moduleName the name of the module to act upon (required)
   * @param groupId the group id of the dependency (required)
   * @param artifactId the artifact id of the dependency (required)
   * @param version the version of the dependency (required)
   * @param addToDependencyManagement boolean that indicates if the new Dependency
   * 		should be included into dependencyManagement element or not.
   * @return added dependency
   */
  Dependency addDependency(final String moduleName, String groupId, String artifactId,
      String version, boolean addToDependencyManagement);

  /**
   * Allows addition of a JAR dependency to the POM.
   * <p>
   * Provides a convenient way for third parties to instruct end users how to
   * use the CLI to add support for their projects without requiring the user
   * to manually edit a pom.xml or write an add-on.
   * <p>
   * By default, includes the provided dependency into dependencyManagement
   * element.
   * <p>
   * 
   * @param moduleName the name of the module to act upon (required)
   * @param groupId the group id of the dependency (required)
   * @param artifactId the artifact id of the dependency (required)
   * @param version the version of the dependency (required)
   * @param scope the scope of the dependency
   * @return added dependency
   */
  Dependency addDependency(final String moduleName, String groupId, String artifactId,
      String version, DependencyScope scope);

  /**
   * Allows addition of a JAR dependency to the POM.
   * <p>
   * Provides a convenient way for third parties to instruct end users how to
   * use the CLI to add support for their projects without requiring the user
   * to manually edit a pom.xml or write an add-on.
   * <p>
   * If addToDependencyManagement parameter has true value, the new dependency will
   * be included into dependencyManagement element. If false, the provided dependency
   * will be included only as dependency.
   * <p>
   * 
   * @param moduleName the name of the module to act upon (required)
   * @param groupId the group id of the dependency (required)
   * @param artifactId the artifact id of the dependency (required)
   * @param version the version of the dependency (required)
   * @param scope the scope of the dependency
   * @param addToDependencyManagement boolean that indicates if the new Dependency
   * 		should be included into dependencyManagement element or not.
   * @return added dependency
   */
  Dependency addDependency(final String moduleName, String groupId, String artifactId,
      String version, DependencyScope scope, boolean addToDependencyManagement);

  /**
   * Allows addition of a JAR dependency to the POM.
   * <p>
   * Provides a convenient way for third parties to instruct end users how to
   * use the CLI to add support for their projects without requiring the user
   * to manually edit a pom.xml or write an add-on.
   * <p>
   * By default, includes the provided dependency into dependencyManagement
   * element.
   * <p>
   * 
   * @param moduleName the name of the module to act upon (required)
   * @param groupId the group id of the dependency (required)
   * @param artifactId the artifact id of the dependency (required)
   * @param version the version of the dependency (required)
   * @param scope the scope of the dependency
   * @param classifier the classifier of the dependency
   * @return added dependency
   */
  Dependency addDependency(final String moduleName, String groupId, String artifactId,
      String version, DependencyScope scope, String classifier);

  /**
   * Allows addition of a JAR dependency to the POM.
   * <p>
   * Provides a convenient way for third parties to instruct end users how to
   * use the CLI to add support for their projects without requiring the user
   * to manually edit a pom.xml or write an add-on.
   * <p>
   * If addToDependencyManagement parameter has true value, the new dependency will
   * be included into dependencyManagement element. If false, the provided dependency
   * will be included only as dependency.
   * <p>
   * 
   * @param moduleName the name of the module to act upon (required)
   * @param groupId the group id of the dependency (required)
   * @param artifactId the artifact id of the dependency (required)
   * @param version the version of the dependency (required)
   * @param scope the scope of the dependency
   * @param classifier the classifier of the dependency
   * @param addToDependencyManagement boolean that indicates if the new Dependency
   * 		should be included into dependencyManagement element or not.
   * @return added dependency
   */
  Dependency addDependency(final String moduleName, String groupId, String artifactId,
      String version, DependencyScope scope, String classifier, boolean addToDependencyManagement);

  /**
   * Attempts to add the specified filter. If the filter already exists
   * according to
   * {@link ProjectMetadata#isFilterRegistered(org.springframework.roo.project.Filter)}
   * , the method silently returns. Otherwise the filter is added.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   *
   * @param moduleName the name of the module to act upon (required)
   * @param filter the filter to add (required)
   */
  void addFilter(final String moduleName, Filter filter);

  /**
   * Adds the given module as a dependency of the currently focused module.
   *
   * @param moduleToDependUpon the name of the module to act upon (required)
   */
  void addModuleDependency(String moduleToDependUpon);

  /**
   * Adds to the given module the dependency with a module.
   *
   * @param moduleName the name of the module where to install the dependency (required)
   * @param moduleToDependUpon the name of the module to act upon (required)
   */
  void addModuleDependency(String moduleName, String moduleToDependUpon);

  /**
   * Adds to the given module the dependency with a module and with the 
   * possibility of adding it only with test scope and type.
   *
   * @param moduleName the name of the module where to install the dependency (required)
   * @param moduleToDependUpon the name of the module to act upon (required)
   * @param testDependency whether the module dependency should be added with test 
   *            scope and as test type dependency.
   */
  void addModuleDependency(String moduleName, String moduleToDependUpon, boolean testDependency);

  /**
   * Attempts to add the specified plugin repositories. If all the
   * repositories already exists according to
   * {@link ProjectMetadata#isPluginRepositoryRegistered(Repository)}, the
   * method silently returns. Otherwise each new repository is added.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   *
   * @param moduleName the name of the module to act upon (required)
   * @param repositories a list of plugin repositories to add (required)
   */
  void addPluginRepositories(final String moduleName, Collection<? extends Repository> repositories);

  /**
   * Attempts to add the specified plugin repository. If the plugin repository
   * already exists according to
   * {@link ProjectMetadata#isPluginRepositoryRegistered(Repository)}, the
   * method silently returns. Otherwise the repository is added.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   *
   * @param moduleName the name of the module to act upon (required)
   * @param repository the plugin repository to add (required)
   */
  void addPluginRepository(final String moduleName, Repository repository);

  /**
   * Attempts to add the specified property. If the property already exists
   * according to
   * {@link ProjectMetadata#isPropertyRegistered(org.springframework.roo.project.Property)}
   * , the method silently returns. Otherwise the property is added.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   *
   * @param moduleName the name of the module to act upon (required)
   * @param property the property to add (required)
   */
  void addProperty(final String moduleName, Property property);

  /**
   * Attempts to add the specified repositories. If all the repositories
   * already exists according to
   * {@link ProjectMetadata#isRepositoryRegistered(Repository)}, the method
   * silently returns. Otherwise each new repository is added.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   *
   * @param moduleName the name of the module to act upon (required)
   * @param repositories a list of repositories to add (required)
   */
  void addRepositories(final String moduleName, Collection<? extends Repository> repositories);

  /**
   * Attempts to add the specified repository. If the repository already
   * exists according to
   * {@link ProjectMetadata#isRepositoryRegistered(Repository)}, the method
   * silently returns. Otherwise the repository is added.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   *
   * @param moduleName the name of the module to act upon (required)
   * @param repository the repository to add (required)
   */
  void addRepository(final String moduleName, Repository repository);

  /**
   * Attempts to add the specified resource. If the resource already exists
   * according to {@link ProjectMetadata#isResourceRegistered(Resource)}, the
   * method silently returns. Otherwise the resource is added.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   *
   * @param moduleName the name of the module to act upon (required)
   * @param resource the resource to add (required)
   */
  void addResource(final String moduleName, Resource resource);

  /**
   * Returns the {@link Pom} of the currently focussed module, or if no module
   * has the focus, the root {@link Pom}.
   *
   * @return <code>null</code> if none of the above descriptors exist
   */
  Pom getFocusedModule();

  /**
   * Returns the name of the currently focussed module.
   *
   * @return an empty string if no module has the focus, otherwise a
   *         fully-qualified name separated by {@link java.io.File#separator}
   */
  String getFocusedModuleName();

  /**
   * Returns the metadata for the currently focussed module.
   *
   * @return <code>null</code> if no project metadata is available
   */
  ProjectMetadata getFocusedProjectMetadata();

  /**
   * @return
   */
  String getFocusedProjectName();

  /**
   * @return
   */
  JavaPackage getFocusedTopLevelPackage();

  /**
   * Returns the module to which the given file belongs
   *
   * @param fileIdentifier the canonical path to look up
   * @return see above
   */
  Pom getModuleForFileIdentifier(String fileIdentifier);

  /**
   * Returns the names of each module in the user's project
   *
   * @return a non-<code>null</code> list
   */
  Collection<String> getModuleNames();

  /**
   * Convenience method to return the {@link PathResolver} from the project's
   * {@link ProjectMetadata}.
   *
   * @return the {@link PathResolver}, or null if the project is unavailable
   */
  PathResolver getPathResolver();

  /**
   * Returns the given module's {@link Pom}
   *
   * @param moduleName the fully-qualified name of the module (required)
   * @return
   */
  Pom getPomFromModuleName(String moduleName);

  /**
   * Returns the {@link Pom}s for all modules of the user's project
   *
   * @return a non-<code>null</code> collection
   */
  Collection<Pom> getPoms();

  /**
   * Returns the {@link ProjectMetadata} for the given module.
   *
   * @param moduleName the module whose metadata is being requested (can be
   *            empty to signify the root or only module)
   * @return <code>null</code> if the metadata is not available
   */
  ProjectMetadata getProjectMetadata(String moduleName);

  /**
   * @param moduleName the name of the module to act upon (required)
   * @return
   */
  String getProjectName(String moduleName);

  /**
   * @param moduleName the name of the module to act upon (required)
   * @return
   */
  JavaPackage getTopLevelPackage(String moduleName);

  /**
   * Indicates whether the supplied feature is installed in any module of a
   * project.
   *
   * @param featureName the name of the feature (see {@link FeatureNames} for
   *            available features)
   * @return true if the feature is installed in any module, otherwise false
   */
  boolean isFeatureInstalled(String featureName);

  /**
   * Indicates whether any of the supplied features are installed in any module of a
   * project.
   *
   * @param featureNames the names of the features (see {@link FeatureNames}
   *            for available features)
   * @return true if any of the supplied features are installed in the focused
   *         module, otherwise false
   */
  boolean isFeatureInstalled(String... featureNames);

  /**
   * Indicates whether any of the supplied features are installed in the 
   * provided module.
   *
   * @param module the {@link Pom} where to search the feature.
   * @param featureName the names of the feature (see {@link FeatureNames}
   *            for available features)
   * @return `true` if the supplied feature is installed in the provided module, 
   *            otherwise `false`.
   */
  boolean isFeatureInstalled(Pom module, String featureName);

  /**
   * Indicates whether the module whose name has the focus, if any, is
   * available.
   *
   * @return see above
   */
  boolean isFocusedProjectAvailable();

  /**
   * Indicates whether the user can create a new project module
   *
   * @return see above
   */
  boolean isModuleCreationAllowed();

  /**
   * Indicates whether the user can change the focused module
   *
   * @return see above
   */
  boolean isModuleFocusAllowed();

  /**
   * Indicates whether the project is multimodule
   *
   * @return see above
   */
  boolean isMultimoduleProject();


  /**
   * Indicates whether a module with the given name is available.
   *
   * @param moduleName the name of the module to act upon (can be blank)
   * @return see above
   */
  boolean isProjectAvailable(String moduleName);

  /**
   * Removes any plugins with the same groupId and artifactId as the given
   * plugin.
   *
   * @param moduleName the name of the module to act upon (required)
   * @param plugin the plugin to remove (can be <code>null</code>)
   * @throws IllegalArgumentException if this method is called before the
   *             {@link ProjectMetadata} is available, or if the on-disk
   *             representation cannot be modified for any reason
   */
  void removeBuildPlugin(final String moduleName, Plugin plugin);

  /**
   * Removes any plugins with the same groupId and artifactId as the given
   * plugin and immediately writes the pom to the file system.
   *
   * @param moduleName the name of the module to act upon (required)
   * @param plugin the plugin to remove (can be <code>null</code>)
   * @throws IllegalArgumentException if this method is called before the
   *             {@link ProjectMetadata} is available, or if the on-disk
   *             representation cannot be modified for any reason
   */
  void removeBuildPluginImmediately(String moduleName, Plugin plugin);

  /**
   * Removes any plugins with the same groupId and artifactId as any of the
   * given plugins.
   *
   * @param moduleName the name of the module to act upon (required)
   * @param plugins the plugins to remove; can be <code>null</code>, any
   *            <code>null</code> elements will be quietly ignored
   * @throws IllegalArgumentException if this method is called before the
   *             {@link ProjectMetadata} is available, or if the on-disk
   *             representation cannot be modified for any reason
   */
  void removeBuildPlugins(final String moduleName, Collection<? extends Plugin> plugins);

  /**
   * Attempts to remove the specified dependencies. If all the dependencies do
   * not exist according to
   * {@link ProjectMetadata#isDependencyRegistered(Dependency)}, the method
   * silently returns. Otherwise each located dependency is removed.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   *
   * @param moduleName the name of the module to act upon (required)
   * @param dependencies the dependencies to remove (required)
   */
  void removeDependencies(final String moduleName, Collection<? extends Dependency> dependencies);

  /**
   * Attempts to remove the specified dependency. If the dependency does not
   * exist according to
   * {@link ProjectMetadata#isDependencyRegistered(Dependency)}, the method
   * silently returns. Otherwise the located dependency is removed.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   *
   * @param moduleName the name of the module to act upon (required)
   * @param dependency the dependency to remove (required)
   */
  void removeDependency(final String moduleName, Dependency dependency);

  /**
   * Allows remove of an existing JAR dependency from the POM.
   * <p>
   * Provides a convenient way for third parties to instruct end users how to
   * use the CLI to remove an unwanted dependency from their projects without
   * requiring the user to manually edit a pom.xml or write an add-on.
   *
   * @param moduleName the name of the module to act upon (required)
   * @param groupId the group id of the dependency (required)
   * @param artifactId the artifact id of the dependency (required)
   * @param version the version of the dependency (required)
   */
  void removeDependency(final String moduleName, String groupId, String artifactId, String version);

  /**
   * Allows remove of an existing JAR dependency from the POM.
   * <p>
   * Provides a convenient way for third parties to instruct end users how to
   * use the CLI to remove an unwanted dependency from their projects without
   * requiring the user to manually edit a pom.xml or write an add-on.
   *
   * @param moduleName the name of the module to act upon (required)
   * @param groupId the group id of the dependency (required)
   * @param artifactId the artifact id of the dependency (required)
   * @param version the version of the dependency (required)
   * @param classifier the classifier of the dependency
   */
  void removeDependency(final String moduleName, String groupId, String artifactId, String version,
      String classifier);

  /**
   * Attempts to remove the specified filter. If the filter does not exist
   * according to
   * {@link ProjectMetadata#isFilterRegistered(org.springframework.roo.project.Filter)}
   * , the method silently returns. Otherwise the located filter is removed.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   *
   * @param moduleName the name of the module to act upon (required)
   * @param filter the filter to remove (required)
   */
  void removeFilter(final String moduleName, Filter filter);

  /**
   * Attempts to remove the specified plugin repository. If the plugin
   * repository does not exist according to
   * {@link ProjectMetadata#isPluginRepositoryRegistered(Repository)}, the
   * method silently returns. Otherwise the located plugin repository is
   * removed.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   *
   * @param moduleName the name of the module to act upon (required)
   * @param repository the plugin repository to remove (required)
   */
  void removePluginRepository(final String moduleName, Repository repository);

  /**
   * Attempts to remove the specified property dependency. If the dependency
   * does not exist according to
   * {@link ProjectMetadata#isPropertyRegistered(Property)}, the method
   * silently returns. Otherwise the located dependency is removed.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   *
   * @param moduleName the name of the module to act upon (required)
   * @param property the property to remove (required)
   */
  void removeProperty(final String moduleName, Property property);

  /**
   * Attempts to remove the specified repository. If the repository does not
   * exist according to
   * {@link ProjectMetadata#isRepositoryRegistered(Repository)}, the method
   * silently returns. Otherwise the located repository is removed.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   *
   * @param moduleName the name of the module to act upon (required)
   * @param repository the repository to remove (required)
   */
  void removeRepository(final String moduleName, Repository repository);

  /**
   * Attempts to remove the specified resource. If the resource does not exist
   * according to {@link ProjectMetadata#isResourceRegistered(Resource)}, the
   * method silently returns. Otherwise the located resource is removed.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   *
   * @param moduleName the name of the module to act upon (required)
   * @param resource the resource to remove (required)
   */
  void removeResource(String moduleName, Resource resource);

  /**
   * Sets the currently focused module
   *
   * @param module the module to focus upon (required)
   */
  void setModule(Pom module);

  /**
   * Verifies if the specified build plugin is present. If it is present,
   * silently returns. If it is not present, removes any build plugin which
   * matches {@link ProjectMetadata#getBuildPluginsExcludingVersion(Plugin)}.
   * Always adds the presented plugin.
   *
   * @param moduleName the name of the module to act upon (required)
   * @param plugin the build plugin to update (required)
   */
  void updateBuildPlugin(final String moduleName, Plugin plugin);

  /**
   * Attempts to update the scope of the specified dependency. If the
   * dependency does not exist according to
   * {@link ProjectMetadata#isDependencyRegistered(Dependency)}, the method
   * silently returns. Otherwise the located dependency is updated.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   *
   * @param moduleName the name of the module to act upon (required)
   * @param dependency the dependency to update (required)
   * @param dependencyScope the dependency scope. May be null, in which case
   *            the <scope> element will be removed
   */
  void updateDependencyScope(final String moduleName, Dependency dependency,
      DependencyScope dependencyScope);

  /**
   * Attempts to update the project packaging type as defined via
   * {@link ProjectType}. If the project packaging is not defined it will
   * create a new definition.
   * <p>
   * An exception is thrown if this method is called before there is
   * {@link ProjectMetadata} available, or if the on-disk representation
   * cannot be modified for any reason.
   *
   * @param moduleName the name of the module to act upon (required)
   * @param projectType the project type to update (required)
   */
  void updateProjectType(final String moduleName, ProjectType projectType);

}
