package org.springframework.roo.project;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.roo.metadata.AbstractMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.CollectionUtils;

/**
 * Represents a project.
 * 
 * <p>
 * Each ROO instance has a single project active at any time. Different project add-ons are expected
 * to subclass this {@link ProjectMetadata} and implement the abstract methods.
 * 
 * <p>
 * The {@link ProjectMetadata} offers convenience methods for acquiring the project name,
 * top level project package name, registered dependencies and path name resolution services.
 * 
 * <p>
 * Concrete subclasses should register the correct dependencies the particular project build
 * system requires, plus read those files whenever they change. Subclasses should also provide a valid
 * {@link PathResolver} implementation that understands the target project layout.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @since 1.0
 */
public class ProjectMetadata extends AbstractMetadataItem {
	// MID:org.springframework.roo.project.ProjectMetadata#the_project
	private static final String PROJECT_IDENTIFIER = MetadataIdentificationUtils.create(ProjectMetadata.class.getName(), "the_project");
	
	private JavaPackage topLevelPackage;
	private String projectName;
	private Set<Dependency> dependencies;
	private Set<Plugin> buildPlugins;
	private Set<Repository> repositories;
	private Set<Repository> pluginRepositories;
	private Set<Property> pomProperties;
	private Set<Filter> filters;
	private Set<Resource> resources;
	private PathResolver pathResolver;
	
	public ProjectMetadata(JavaPackage topLevelPackage, String projectName, Set<Dependency> dependencies, Set<Plugin> buildPlugins, Set<Repository> repositories, Set<Repository> pluginRepositories, Set<Property> pomProperties, Set<Filter> filters, Set<Resource> resources, PathResolver pathResolver) {
		super(PROJECT_IDENTIFIER);
		Assert.notNull(topLevelPackage, "Top level package required");
		Assert.notNull(projectName, "Project name required");
		Assert.notNull(dependencies, "Dependencies required");
		Assert.notNull(buildPlugins, "Build plugins required");
		Assert.notNull(repositories, "Repositories required");
		Assert.notNull(pluginRepositories, "Plugin repositories required");
		Assert.notNull(pomProperties, "POM properties required");
		Assert.notNull(filters, "Filters required");
		Assert.notNull(resources, "Resources required");
		Assert.notNull(pathResolver, "Path resolver required");
		this.topLevelPackage = topLevelPackage;
		this.projectName = projectName;
		this.dependencies = dependencies;
		this.buildPlugins = buildPlugins;
		this.repositories = repositories;
		this.pluginRepositories = pluginRepositories;
		this.pomProperties = pomProperties;
		this.filters = filters;
		this.resources = resources;
		this.pathResolver = pathResolver;
	}

	public static final String getProjectIdentifier() {
		return PROJECT_IDENTIFIER;
	}
	
	/**
	 * Convenience method for determining whether all of the presented dependencies are registered. 
	 *
	 * @param dependencies the dependencies to check (required)
	 * @return whether all the dependencies are currently registered or not
	 */
	public boolean isAllDependenciesRegistered(List<Dependency> dependencies) {
		Assert.notNull(dependencies, "Dependencies to check is required");
		return this.dependencies.containsAll(dependencies);
	}

	/**
	 * Convenience method for determining whether any of the presented dependencies are registered. 
	 *
	 * @param dependencies the dependencies to check (required)
	 * @return whether any of the dependencies are currently registered or not
	 */
	public boolean isAnyDependenciesRegistered(List<Dependency> dependencies) {
		Assert.notNull(dependencies, "Dependencies to check is required");
		return CollectionUtils.containsAny(this.dependencies, dependencies);
	}

	/**
	 * Convenience method for determining whether a particular dependency is registered. 
	 *
	 * @param dependency the dependency to check (required)
	 * @return whether the dependency is currently registered or not
	 */
	public boolean isDependencyRegistered(Dependency dependency) {
		Assert.notNull(dependency, "Dependency to check is required");
		return dependencies.contains(dependency);
	}
	
	/**
	 * Convenience method for determining whether all presented repositories are registered. 
	 *
	 * @param repositories the repositories to check (required)
	 * @return whether all the repositories are currently registered or not
	 */
	public boolean isAllRepositoriesRegistered(List<Repository> repositories) {
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
	public boolean isRepositoryRegistered(Repository repository) {
		Assert.notNull(repository, "Repository to check is required");
		return repositories.contains(repository);
	}

	/**
	 * Convenience method for determining whether all presented plugin repositories are registered. 
	 *
	 * @param repositories the plugin repositories to check (required)
	 * @return whether all the plugin repositories are currently registered or not
	 */
	public boolean isAllPluginRepositoriesRegistered(List<Repository> repositories) {
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
	public boolean isPluginRepositoryRegistered(Repository repository) {
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
	public boolean isAllPluginsRegistered(List<Plugin> plugins) {
		Assert.notNull(plugins, "Plugins to check is required");
		for (Plugin plugin : plugins) {
			if (!isBuildPluginRegistered(plugin)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Convenience method for determining whether any of the presented plugins
	 * are registered based on the groupId, artifactId, and version.
	 *
	 * @param plugins the plugins to check (required)
	 * @return whether any of the plugins are currently registered or not
	 */
	public boolean isAnyPluginsRegistered(List<Plugin> plugins) {
		Assert.notNull(plugins, "Plugins to check is required");
		for (Plugin plugin : plugins) {
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
	public boolean isBuildPluginRegistered(Plugin plugin) {
		Assert.notNull(plugin, "Plugin to check is required");
		for (Plugin existingPlugin : buildPlugins) {
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
	public boolean isPropertyRegistered(Property property) {
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
	public boolean isFilterRegistered(Filter filter) {
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
	public boolean isResourceRegistered(Resource resource) {
		Assert.notNull(resource, "Resource to check is required");
		return resources.contains(resource);
	}

	public JavaPackage getTopLevelPackage() {
		return topLevelPackage;
	}

	public String getProjectName() {
		return projectName;
	}
	
	public PathResolver getPathResolver() {
		return pathResolver;
	}

	/**
	 * Returns an unmodifiable set of the project's dependencies.
	 * 
	 * @return an unmodifiable representation of the dependencies (never null, but may be empty)
	 */
	public Set<Dependency> getDependencies() {
		return Collections.unmodifiableSet(dependencies);
	}
	
	/**
	 * Locates any dependencies which match the presented dependency, excluding the version number.
	 * This is useful for upgrade use cases, where it is necessary to locate any dependencies with
	 * the same group, artifact and type identifications so that they can be removed.
	 * 
	 * @param dependency to locate (required; note the version number is ignored in comparisons)
	 * @return any matching dependencies (never returns null, but may return an empty {@link Set})
	 */
	public Set<Dependency> getDependenciesExcludingVersion(Dependency dependency) {
		Assert.notNull(dependency, "Dependency to locate is required");
		Set<Dependency> result = new HashSet<Dependency>();
		for (Dependency d : dependencies) {
			if (dependency.getArtifactId().equals(d.getArtifactId()) && dependency.getGroupId().equals(d.getGroupId()) && dependency.getType().equals(d.getType())) {
				result.add(d);
			}
		}
		return result;
	}
	
	/**
	 * @return an unmodifiable collection of the build plugins (never null, but may be empty).
	 */
	public Set<Plugin> getBuildPlugins() {
		return Collections.unmodifiableSet(buildPlugins);
	}

	/**
	 * Locates any build plugins which match the presented plugin, excluding the version number.
	 * This is useful for upgrade use cases, where it is necessary to locate any build plugins with
	 * the same group and artifact identifications so that they can be removed.
	 * 
	 * @param plugin to locate (required; note the version number is ignored in comparisons)
	 * @return any matching plugins (never returns null, but may return an empty {@link Set})
	 */
	public Set<Plugin> getBuildPluginsExcludingVersion(Plugin plugin) {
		Assert.notNull(plugin, "Plugin to locate is required");
		Set<Plugin> result = new HashSet<Plugin>();
		for (Plugin p : buildPlugins) {
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
	public Set<Property> getPropertiesExcludingValue(Property property) {
		Assert.notNull(property, "Property to locate is required");
		Set<Property> result = new HashSet<Property>();
		for (Property p : pomProperties) {
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
	public Property getProperty(String name) {
		Assert.hasText(name, "Property name to locate is required");
		for (Property p : pomProperties) {
			if (name.equals(p.getName())) {
				return p;
			}
		}
		return null;
	}
	
	/**
	 * @return an unmodifiable representation of the filters (never null, but may be empty)
	 */
	public Set<Filter> getFilters() {
		return Collections.unmodifiableSet(filters);
	}
	
	/**
	 * @return an unmodifiable representation of the resources (never null, but may be empty)
	 */
	public Set<Resource> getResources() {
		return Collections.unmodifiableSet(resources);
	}

	/**
	 * Determines whether GWT is enabled in the project.
	 * 
	 * @return true if the gwt-maven-plugin is present in the pom.xml or the GWT module XML file exists, otherwise false
	 */
	public boolean isGwtEnabled() {
		boolean gwtEnabled = false;
		for (Plugin buildPlugin : buildPlugins) {
			if ("gwt-maven-plugin".equals(buildPlugin.getArtifactId())) {
				gwtEnabled = true;
				break;
			}
		}
		// TODO This is hacky - should not rely on pathResolver, the use of java.io.File, and the XML artifact itself being present. Should just be able to detect build plugin
		if (!gwtEnabled) {
			String gwtModuleXml = pathResolver.getIdentifier(Path.SRC_MAIN_JAVA, topLevelPackage.getFullyQualifiedPackageName().replace('.', File.separatorChar) + File.separator + "ApplicationScaffold.gwt.xml");
			gwtEnabled = new File(gwtModuleXml).exists();
		}
		return gwtEnabled;
	}

	
	/**
	 * Determines whether the Google App Engine Maven plugin exists in the pom.
	 * 
	 * @return true if the maven-gae-plugin is present in the pom.xml, otherwise false
	 */
	public boolean isGaeEnabled() {
		for (Plugin buildPlugin : buildPlugins) {
			if ("maven-gae-plugin".equals(buildPlugin.getArtifactId())) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Determines whether the DataNucleus Maven plugin exists in the pom.
	 * 
	 * @return true if the maven-datanucleus-plugin is present in the pom.xml, otherwise false
	 */
	public boolean isDataNucleusEnabled() {
		for (Plugin buildPlugin : buildPlugins) {
			if ("maven-datanucleus-plugin".equals(buildPlugin.getArtifactId())) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Determines whether the VMforce Maven dependency exists in the pom.
	 * 
	 * @return true if the com.force.sdk is present in the pom.xml, otherwise false
	 */
	public boolean isVMforceEnabled() {
		for (Dependency dependency : dependencies) {
			if ("com.force.sdk".equals(dependency.getGroupId())) {
				return true;
			}
		}
		return false;
	}

	public final String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", isValid());
		tsc.append("topLevelPackage", topLevelPackage);
		tsc.append("projectName", projectName);
		tsc.append("dependencies", dependencies);
		tsc.append("buildPlugins", buildPlugins);
		tsc.append("repositories", repositories);
		tsc.append("pluginRepositories", pluginRepositories);
		tsc.append("pomProperties", pomProperties);
		tsc.append("filters", filters);
		tsc.append("resources", resources);
		tsc.append("pathResolver", pathResolver);
		return tsc.toString();
	}
}
