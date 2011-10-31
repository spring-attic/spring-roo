package org.springframework.roo.project.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.roo.project.ContextualPath;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Filter;
import org.springframework.roo.project.GAV;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathInformation;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.Property;
import org.springframework.roo.project.Repository;
import org.springframework.roo.project.Resource;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.CollectionUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.StringUtils;

public class Pom {

	// Constants
	private static final String DEFAULT_PACKAGING = "jar";	// Maven behaviour
	public static final String DEFAULT_RESOURCES_DIRECTORY = "src/main/resources";
	public static final String DEFAULT_SOURCE_DIRECTORY = "src/main/java";
	public static final String DEFAULT_SPRING_CONFIG_ROOT = DEFAULT_RESOURCES_DIRECTORY + "/META-INF/spring";
	public static final String DEFAULT_TEST_RESOURCES_DIRECTORY = "src/test/resources";
	public static final String DEFAULT_TEST_SOURCE_DIRECTORY = "src/test/java";
	public static final String DEFAULT_WAR_SOURCE_DIRECTORY = "src/main/webapp";

	// Fields
	private final Map<Path, PathInformation> pathCache = new LinkedHashMap<Path, PathInformation>();
	private final Parent parent;
	private final Set<Dependency> dependencies = new LinkedHashSet<Dependency>();
	private final Set<Filter> filters = new LinkedHashSet<Filter>();
	private final Set<Module> modules = new LinkedHashSet<Module>();
	private final Set<Plugin> buildPlugins = new LinkedHashSet<Plugin>();
	private final Set<Property> pomProperties = new LinkedHashSet<Property>();
	private final Set<Repository> pluginRepositories = new LinkedHashSet<Repository>();
	private final Set<Repository> repositories = new LinkedHashSet<Repository>();
	private final Set<Resource> resources = new LinkedHashSet<Resource>();
	private final String artifactId;
	private final String groupId;
	private final String moduleName;
	private final String name;
	private final String packaging;
	private final String path;
	private final String sourceDirectory;
	private final String testSourceDirectory;
	private final String version;

	/**
	 * Constructor
	 *
	 * @param groupId the Maven groupId, explicit or inherited (required)
	 * @param artifactId the Maven artifactId (required)
	 * @param version the version of the artifact being built (required)
	 * @param packaging the Maven packaging (can be blank for the default)
	 * @param dependencies (can be <code>null</code> for none)
	 * @param parent the POM's parent declaration (can be <code>null</code> for none)
	 * @param modules the modules defined by this POM (only applies when packaging is "pom"; can be <code>null</code> for none)
	 * @param pomProperties any properties defined in the POM (can be <code>null</code> for none)
	 * @param name the Maven name of the artifact being built (can be blank)
	 * @param repositories any repositories defined in the POM (can be <code>null</code> for none)
	 * @param pluginRepositories any plugin repositories defined in the POM (can be <code>null</code> for none)
	 * @param sourceDirectory the directory relative to the POM that contains production code (can be blank for the Maven default)
	 * @param testSourceDirectory the directory relative to the POM that contains test code (can be blank for the Maven default)
	 * @param filters any filters defined in the POM (can be <code>null</code> for none)
	 * @param buildPlugins any plugins defined in the POM (can be <code>null</code> for none)
	 * @param resources any build resources defined in the POM (can be <code>null</code> for none)
	 * @param path the canonical path of this POM (required)
	 * @param moduleName the Maven name of this module (blank for the project's root or only POM) 
	 */
	public Pom(final String groupId, final String artifactId, final String version, final String packaging, final Collection<? extends Dependency> dependencies, final Parent parent, final Collection<? extends Module> modules, final Collection<? extends Property> pomProperties, final String name, final Collection<? extends Repository> repositories, final Collection<? extends Repository> pluginRepositories, final String sourceDirectory, final String testSourceDirectory, final Collection<? extends Filter> filters, final Collection<? extends Plugin> buildPlugins, final Collection<? extends Resource> resources, final String path, final String moduleName) {
		Assert.hasText(groupId, "Invalid groupId '" + groupId + "'");
		Assert.hasText(artifactId, "Invalid artifactId '" + artifactId + "'");
		Assert.hasText(version, "Invalid version '" + version + "'");
		Assert.hasText(path, "Invalid path '" + path + "'");
		
		this.artifactId = artifactId;
		this.groupId = groupId;
		this.moduleName = StringUtils.trimToEmpty(moduleName);
		this.name = StringUtils.trimToEmpty(name);
		this.packaging = StringUtils.defaultIfEmpty(packaging, DEFAULT_PACKAGING);
		this.parent = parent;
		this.path = path;
		this.sourceDirectory = StringUtils.defaultIfEmpty(sourceDirectory, DEFAULT_SOURCE_DIRECTORY);
		this.testSourceDirectory = StringUtils.defaultIfEmpty(testSourceDirectory, DEFAULT_TEST_SOURCE_DIRECTORY);
		this.version = version;

		CollectionUtils.populate(this.buildPlugins, buildPlugins);
		CollectionUtils.populate(this.dependencies, dependencies);
		CollectionUtils.populate(this.filters, filters);
		CollectionUtils.populate(this.modules, modules);
		CollectionUtils.populate(this.pluginRepositories, pluginRepositories);
		CollectionUtils.populate(this.pomProperties, pomProperties);
		CollectionUtils.populate(this.repositories, repositories);
		CollectionUtils.populate(this.resources, resources);

		cachePathInformation(Path.SRC_MAIN_JAVA);
		cachePathInformation(Path.SRC_TEST_JAVA);
		cachePathInformation(Path.SRC_TEST_RESOURCES);
		cachePathInformation(Path.SRC_MAIN_RESOURCES);
		cachePathInformation(Path.SRC_MAIN_WEBAPP);
		cachePathInformation(Path.SPRING_CONFIG_ROOT);
		cachePathInformation(Path.ROOT);
	}

	/**
	 * Returns the canonical path of the given logical {@link Path} within this module, plus a trailing separator
	 * 
	 * @param path the logical path for which to get the canonical location (required)
	 * @return a valid canonical path
	 */
	public String getPathLocation(final Path path) {
		return FileUtils.ensureTrailingSeparator(getPathInformation(path).getLocationPath());
	}

	public PathInformation getPathInformation(final Path path) {
		return pathCache.get(path);
	}

	public PathInformation getPathInformation(final ContextualPath path) {
		return pathCache.get(path.getPath());
	}

	private void cachePathInformation(final Path path) {
		String moduleRoot = moduleRoot(getPath());
		StringBuilder sb = new StringBuilder();
		sb.append(moduleRoot).append(File.separator);
		if (path.equals(Path.SRC_MAIN_JAVA)) {
			String sourceDirectory = getSourceDirectory();
			if (!StringUtils.hasText(sourceDirectory)) {
				sourceDirectory = DEFAULT_SOURCE_DIRECTORY;
			}
			sb.append(sourceDirectory);
		} else if (path.equals(Path.SRC_MAIN_RESOURCES)) {
			sb.append(File.separator).append(DEFAULT_RESOURCES_DIRECTORY);
		} else if (path.equals(Path.SRC_TEST_JAVA)) {
			String testSourceDirectory = getTestSourceDirectory();
			if (!StringUtils.hasText(testSourceDirectory)) {
				testSourceDirectory = DEFAULT_TEST_SOURCE_DIRECTORY;
			}
			sb.append(testSourceDirectory);
		}  else if (path.equals(Path.SRC_TEST_RESOURCES)) {
			sb.append(DEFAULT_TEST_RESOURCES_DIRECTORY);
		}  else if (path.equals(Path.SRC_MAIN_WEBAPP)) {
			sb.append(DEFAULT_WAR_SOURCE_DIRECTORY);
		} else if (path.equals(Path.SPRING_CONFIG_ROOT)) {
			sb.append(DEFAULT_SPRING_CONFIG_ROOT);
		} else if (path.equals(Path.ROOT)) {
			// do nothing
		}
		PathInformation pathInformation = new PathInformation(ContextualPath.getInstance(path, moduleName), true, new File(sb.toString()));
		pathCache.put(path, pathInformation);
	}

	/**
	 * Returns the canonical path of this module's root directory, plus a trailing separator
	 * 
	 * @return a valid canonical path
	 */
	public String getRoot() {
		return getPathLocation(Path.ROOT);
	}

	public List<PathInformation> getPathInformation() {
		return new ArrayList<PathInformation>(pathCache.values());
	}

	/**
	 * Indicates whether all of the given dependencies are registered, using
	 * {@link Dependency#equals(Object)} to evaluate each one against the
	 * existing dependencies.
	 *
	 * @param dependencies the dependencies to check (required)
	 * @return whether all the dependencies are currently registered or not
	 */
	public boolean isAllDependenciesRegistered(final Collection<? extends Dependency> dependencies) {
		Assert.notNull(dependencies, "Dependencies to check is required");
		return this.dependencies.containsAll(dependencies);
	}

	/**
	 * Convenience method for determining whether any of the presented dependencies are registered.
	 *
	 * @param dependencies the dependencies to check (required)
	 * @return whether any of the dependencies are currently registered or not
	 */
	public boolean isAnyDependenciesRegistered(final Collection<? extends Dependency> dependencies) {
		Assert.notNull(dependencies, "Dependencies to check is required");
		return CollectionUtils.containsAny(this.dependencies, dependencies);
	}

	/**
	 * Convenience method for determining whether a particular dependency is registered.
	 *
	 * @param dependency the dependency to check (can be <code>null</code>)
	 * @return <code>false</code> if a <code>null</code> dependency is given
	 */
	public boolean isDependencyRegistered(final Dependency dependency) {
		return dependency != null && dependencies.contains(dependency);
	}

	/**
	 * Convenience method for determining whether all presented repositories are registered.
	 *
	 * @param repositories the repositories to check (required)
	 * @return whether all the repositories are currently registered or not
	 */
	public boolean isAllRepositoriesRegistered(final Collection<? extends Repository> repositories) {
		Assert.notNull(repositories, "Repositories to check is required");
		return this.repositories.containsAll(repositories);
	}

	/**
	 * Convenience method for determining whether a particular repository
	 * is registered.
	 *
	 * @param repository to check (required)
	 * @return whether the repository is currently registered or not
	 */
	public boolean isRepositoryRegistered(final Repository repository) {
		Assert.notNull(repository, "Repository to check is required");
		return repositories.contains(repository);
	}

	/**
	 * Convenience method for determining whether all presented plugin repositories are registered.
	 *
	 * @param repositories the plugin repositories to check (required)
	 * @return whether all the plugin repositories are currently registered or not
	 */
	public boolean isAllPluginRepositoriesRegistered(final Collection<? extends Repository> repositories) {
		Assert.notNull(repositories, "Plugin repositories to check is required");
		return pluginRepositories.containsAll(repositories);
	}

	/**
	 * Convenience method for determining whether a particular plugin repository
	 * is registered.
	 *
	 * @param repository repository to check (required)
	 * @return whether the plugin repository is currently registered or not
	 */
	public boolean isPluginRepositoryRegistered(final Repository repository) {
		Assert.notNull(repository, "Plugin repository to check is required");
		return pluginRepositories.contains(repository);
	}

	/**
	 * Convenience method for determining whether all of the presented plugins
	 * are registered based on the groupId, artifactId, and version.
	 *
	 * @param plugins the plugins to check (required)
	 * @return whether all the plugins are currently registered or not
	 */
	public boolean isAllPluginsRegistered(final Collection<? extends Plugin> plugins) {
		Assert.notNull(plugins, "Plugins to check is required");
		for (final Plugin plugin : plugins) {
			if (!isBuildPluginRegistered(plugin)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Indicates whether any of the given plugins are registered, by evaluating
	 * the result of calling {@link #isBuildPluginRegistered(Plugin)} for each
	 * one.
	 *
	 * @param plugins the plugins to check (required)
	 * @return whether any of the plugins are currently registered or not
	 */
	public boolean isAnyPluginsRegistered(final Collection<? extends Plugin> plugins) {
		Assert.notNull(plugins, "Plugins to check is required");
		for (final Plugin plugin : plugins) {
			if (isBuildPluginRegistered(plugin)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Convenience method for determining whether a particular build plugin
	 * is registered based on the groupId, artifactId, and version.
	 *
	 * @param plugin to check (required)
	 * @return whether the build plugin is currently registered or not
	 */
	public boolean isBuildPluginRegistered(final Plugin plugin) {
		Assert.notNull(plugin, "Plugin to check is required");
		for (final Plugin existingPlugin : buildPlugins) {
			boolean matchFound = existingPlugin.getGroupId().equals(plugin.getGroupId());
			matchFound &= existingPlugin.getArtifactId().equals(plugin.getArtifactId());
			matchFound &= existingPlugin.getVersion().equals(plugin.getVersion());
			if (matchFound) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Convenience method for determining whether a particular pom property
	 * is registered.
	 *
	 * @param property to check (required)
	 * @return whether the property is currently registered or not
	 */
	public boolean isPropertyRegistered(final Property property) {
		Assert.notNull(property, "Property to check is required");
		return pomProperties.contains(property);
	}

	/**
	 * Convenience method for determining whether a particular filter
	 * is registered.
	 *
	 * @param filter to check (required)
	 * @return whether the filter is currently registered or not
	 */
	public boolean isFilterRegistered(final Filter filter) {
		Assert.notNull(filter, "Filter to check is required");
		return filters.contains(filter);
	}

	/**
	 * Convenience method for determining whether a particular resource
	 * is registered.
	 *
	 * @param resource to check (required)
	 * @return whether the resource is currently registered or not
	 */
	public boolean isResourceRegistered(final Resource resource) {
		Assert.notNull(resource, "Resource to check is required");
		return resources.contains(resource);
	}

	/**
	 * Locates any dependencies which match the presented dependency, excluding the version number.
	 * This is useful for upgrade use cases, where it is necessary to locate any dependencies with
	 * the same group, artifact and type identifications so that they can be removed.
	 *
	 * @param dependency to locate (required; note the version number is ignored in comparisons)
	 * @return any matching dependencies (never returns null, but may return an empty {@link Set})
	 */
	public Set<Dependency> getDependenciesExcludingVersion(final Dependency dependency) {
		Assert.notNull(dependency, "Dependency to locate is required");
		final Set<Dependency> result = new HashSet<Dependency>();
		for (final Dependency d : dependencies) {
			if (dependency.getArtifactId().equals(d.getArtifactId()) && dependency.getGroupId().equals(d.getGroupId()) && dependency.getType().equals(d.getType())) {
				result.add(d);
			}
		}
		return result;
	}

	/**
	 * Returns any build plugins with the same groupId and artifactId as the
	 * given plugin. This is useful for upgrade cases.
	 *
	 * @param plugin to locate (required; note the version number is ignored in comparisons)
	 * @return any matching plugins (never returns null, but may return an empty {@link Set})
	 */
	public Set<Plugin> getBuildPluginsExcludingVersion(final Plugin plugin) {
		Assert.notNull(plugin, "Plugin to locate is required");
		final Set<Plugin> result = new HashSet<Plugin>();
		for (final Plugin p : buildPlugins) {
			if (plugin.getArtifactId().equals(p.getArtifactId()) && plugin.getGroupId().equals(p.getGroupId())) {
				result.add(p);
			}
		}
		return result;
	}

	/**
	 * Locates any properties which match the presented property, excluding the value.
	 * This is useful for upgrade use cases, where it is necessary to locate any properties with
	 * the name so that they can be removed.
	 *
	 * @param property to locate (required; note the value is ignored in comparisons)
	 * @return any matching properties (never returns null, but may return an empty {@link Set})
	 */
	public Set<Property> getPropertiesExcludingValue(final Property property) {
		Assert.notNull(property, "Property to locate is required");
		final Set<Property> result = new HashSet<Property>();
		for (final Property p : pomProperties) {
			if (property.getName().equals(p.getName())) {
				result.add(p);
			}
		}
		return result;
	}

	/**
	 * Locates the first occurrence of a property for a given name and returns it.
	 *
	 * @param name the property name (required)
	 * @return the property if found otherwise null
	 */
	public Property getProperty(final String name) {
		Assert.hasText(name, "Property name to locate is required");
		for (final Property p : pomProperties) {
			if (name.equals(p.getName())) {
				return p;
			}
		}
		return null;
	}

	/**
	 * Determines whether the GWT Maven plugin exists in the pom.
	 *
	 * @return true if the gwt-maven-plugin is present in the pom.xml, otherwise false
	 */
	public boolean isGwtEnabled() {
		for (final Plugin buildPlugin : getBuildPlugins()) {
			if ("gwt-maven-plugin".equals(buildPlugin.getArtifactId())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determines whether the Google App Engine Maven plugin exists in the pom.
	 *
	 * @return true if the maven-gae-plugin is present in the pom.xml, otherwise false
	 */
	public boolean isGaeEnabled() {
		for (final Plugin buildPlugin : getBuildPlugins()) {
			if ("maven-gae-plugin".equals(buildPlugin.getArtifactId())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determines whether the Database.com Maven dependency exists in the pom.
	 *
	 * @return true if the com.force.sdk is present in the pom.xml, otherwise false
	 */
	public boolean isDatabaseDotComEnabled() {
		for (final Dependency dependency : getDependencies()) {
			if ("com.force.sdk".equals(dependency.getGroupId())) {
				return true;
			}
		}
		return false;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version;
	}

	public String getPackaging() {
		return packaging;
	}

	public Set<Dependency> getDependencies() {
		return dependencies;
	}

	public Parent getParent() {
		return parent;
	}

	public Set<Module> getModules() {
		return modules;
	}

	public Set<Property> getPomProperties() {
		return pomProperties;
	}

	public String getName() {
		return name;
	}

	public Set<Repository> getRepositories() {
		return repositories;
	}

	public Set<Repository> getPluginRepositories() {
		return pluginRepositories;
	}

	public String getSourceDirectory() {
		return sourceDirectory;
	}

	public String getTestSourceDirectory() {
		return testSourceDirectory;
	}

	public Set<Filter> getFilters() {
		return filters;
	}

	public Set<Plugin> getBuildPlugins() {
		return buildPlugins;
	}

	public Set<Resource> getResources() {
		return resources;
	}

	/**
	 * Returns this POM's canonical path on the file system
	 * 
	 * @return a valid canonical path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Returns the name of the Maven module to which this POM belongs
	 * 
	 * @return a non-<code>null</code> name; empty for the root POM
	 */
	public String getModuleName() {
		return moduleName;
	}

	private String moduleRoot(final String pomPath) {
		return FileUtils.getFirstDirectory(pomPath);
	}
	
	@Override
	public String toString() {
		// For debugging
		return new GAV(groupId, artifactId, version) + " at " + path;
	}
}
