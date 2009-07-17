package org.springframework.roo.project;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.internal.AbstractMetadataItem;
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
 * @since 1.0
 *
 */
public class ProjectMetadata extends AbstractMetadataItem {

	private static final String PROJECT_IDENTIFIER = MetadataIdentificationUtils.create(ProjectMetadata.class.getName(), "the_project");
	
	private JavaPackage topLevelPackage;
	private String projectName;
	private Set<Dependency> dependencies;
	private Set<Dependency> buildPluginDependencies;
	private PathResolver pathResolver;
	
	public ProjectMetadata(JavaPackage topLevelPackage, String projectName, Set<Dependency> dependencies, Set<Dependency> buildPluginDependencies, PathResolver pathResolver) {
		super(PROJECT_IDENTIFIER);
		Assert.notNull(topLevelPackage, "Top level package required");
		Assert.notNull(projectName, "Project name required");
		Assert.notNull(dependencies, "Dependencies required");
		Assert.notNull(buildPluginDependencies, "Build plugin dependencies required");
		Assert.notNull(pathResolver, "Path resolver required");
		this.topLevelPackage = topLevelPackage;
		this.projectName = projectName;
		this.dependencies = dependencies;
		this.buildPluginDependencies = buildPluginDependencies;
		this.pathResolver = pathResolver;
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
	 * Convenience method for determining whether a particular build plugin dependency
	 * is registered.
	 * 
	 * @param dependency to check (required)
	 * @return whether the build plugin dependency is currently registered or not
	 */
	public boolean isBuildPluginDependencyRegistered(Dependency dependency) {
		Assert.notNull(dependency, "Dependency to check is required");
		return buildPluginDependencies.contains(dependency);
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
	 * @return an unmodifiable collection of the build plugin dependencies (never null, but
	 * may be empty).
	 */
	public Set<Dependency> getBuildPluginDependencies() {
		return Collections.unmodifiableSet(buildPluginDependencies);
	}

	/**
	 * Locates any build plugin dependencies which match the presented dependency, excluding the version number.
	 * This is useful for upgrade use cases, where it is necessary to locate any build plugin dependencies with
	 * the same group, artifact and type identifications so that they can be removed.
	 * 
	 * @param dependency to locate (required; note the version number is ignored in comparisons)
	 * @return any matching dependencies (never returns null, but may return an empty {@link Set})
	 */
	public Set<Dependency> getBuildPluginDependenciesExcludingVersion(Dependency dependency) {
		Assert.notNull(dependency, "Dependency to locate is required");
		Set<Dependency> result = new HashSet<Dependency>();
		for (Dependency d : buildPluginDependencies) {
			if (dependency.getArtifactId().equals(d.getArtifactId()) && dependency.getGroupId().equals(d.getGroupId()) && dependency.getType().equals(d.getType())) {
				result.add(d);
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
		tsc.append("buildPluginDependencies", buildPluginDependencies);
		tsc.append("pathResolver", pathResolver);
		return tsc.toString();
	}
	
}
