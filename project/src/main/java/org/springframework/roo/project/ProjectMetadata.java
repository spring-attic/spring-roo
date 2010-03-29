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
	private PathResolver pathResolver;
	
	public ProjectMetadata(JavaPackage topLevelPackage, String projectName, Set<Dependency> dependencies, Set<Plugin> buildPlugins, Set<Repository> repositories, PathResolver pathResolver) {
		super(PROJECT_IDENTIFIER);
		Assert.notNull(topLevelPackage, "Top level package required");
		Assert.notNull(projectName, "Project name required");
		Assert.notNull(dependencies, "Dependencies required");
		Assert.notNull(buildPlugins, "Build plugins required");
		Assert.notNull(repositories, "Repositories required");
		Assert.notNull(pathResolver, "Path resolver required");
		this.topLevelPackage = topLevelPackage;
		this.projectName = projectName;
		this.dependencies = dependencies;
		this.buildPlugins = buildPlugins;
		this.pathResolver = pathResolver;
		this.repositories = repositories;
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
	 * Convenience method for determining whether a particular repository
	 * is registered.
	 * 
	 * @param reopository to check (required)
	 * @return whether the repository is currently registered or not
	 */
	public boolean isRepositoryRegistered(Repository repository) {
		Assert.notNull(repository, "Repository to check is required");
		return repositories.contains(repository);
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
	public Set<Plugin> getBuildPlugin() {
		return Collections.unmodifiableSet(buildPlugins);
	}

	/**
	 * Locates any build plugins which match the presented plugin, excluding the version number.
	 * This is useful for upgrade use cases, where it is necessary to locate any build plugins with
	 * the same group, artifact and type identifications so that they can be removed.
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

	
	public final String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", isValid());
		tsc.append("topLevelPackage", topLevelPackage);
		tsc.append("projectName", projectName);
		tsc.append("dependencies", dependencies);
		tsc.append("buildPlugins", buildPlugins);
		tsc.append("pathResolver", pathResolver);
		return tsc.toString();
	}
}
