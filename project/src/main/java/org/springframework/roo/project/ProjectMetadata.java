package org.springframework.roo.project;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.AbstractMetadataItem;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

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
 *
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
	private PathResolver pathResolver;
	
	public ProjectMetadata(JavaPackage topLevelPackage, String projectName, Set<Dependency> dependencies, Set<Plugin> buildPlugins, Set<Repository> repositories, Set<Repository> pluginRepositories, Set<Property> pomProperties, PathResolver pathResolver) {
		super(PROJECT_IDENTIFIER);
		Assert.notNull(topLevelPackage, "Top level package required");
		Assert.notNull(projectName, "Project name required");
		Assert.notNull(dependencies, "Dependencies required");
		Assert.notNull(buildPlugins, "Build plugins required");
		Assert.notNull(repositories, "Repositories required");
		Assert.notNull(pluginRepositories, "Plugin repositories required");
		Assert.notNull(pomProperties, "POM properties required");
		Assert.notNull(pathResolver, "Path resolver required");
		this.topLevelPackage = topLevelPackage;
		this.projectName = projectName;
		this.dependencies = dependencies;
		this.buildPlugins = buildPlugins;
		this.pathResolver = pathResolver;
		this.repositories = repositories;
		this.pomProperties = pomProperties;
		this.pluginRepositories = pluginRepositories;
	}

	public static final String getProjectIdentifier() {
		return PROJECT_IDENTIFIER;
	}
	
	/**
	 * Convenience method for determining whether a particular dependency is registered. 
	 *
	 * @param dependency to check (required)
	 * @return whether the dependency is currently registered or not
	 */
	public boolean isDependencyRegistered(Dependency dependency) {
		Assert.notNull(dependency, "Dependency to check is required");
		return dependencies.contains(dependency);
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
	 * Convenience method for determining whether a particular plugin repository
	 * is registered.
	 * 
	 * @param plugin repository to check (required)
	 * @return whether the plugin repository is currently registered or not
	 */
	public boolean isPluginRepositoryRegistered(Repository repository) {
		Assert.notNull(repository, "Plugin repository to check is required");
		return pluginRepositories.contains(repository);
	}

	/**
	 * Convenience method for determining whether a particular build plugin
	 * is registered.
	 * 
	 * @param plugin to check (required)
	 * @return whether the build plugin is currently registered or not
	 */
	public boolean isBuildPluginRegistered(Plugin plugin) {
		Assert.notNull(plugin, "Plugin to check is required");
		return buildPlugins.contains(plugin);
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
	 * @return an unmodifiable collection of the build plugins (never null, but
	 * may be empty).
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
	 * Determines whether the Google App Engine maven plugin exists in the pom.
	 * 
	 * @return true if the maven-gae-plugin is present in the pom.xml, otherwise false
	 */
	public boolean isGaeEnabled() {
		for (Plugin buildPlugin : buildPlugins) {
			if ("maven-gae-plugin".equals(buildPlugin.getArtifactId().getSymbolName())) {
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
		tsc.append("pathResolver", pathResolver);
		return tsc.toString();
	}
}
