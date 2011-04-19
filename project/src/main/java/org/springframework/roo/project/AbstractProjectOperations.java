package org.springframework.roo.project;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.support.util.Assert;

/**
 * Provides common project operations. Should be subclassed by a project-specific operations subclass. 
 * 
 * @author Ben Alex
 * @author Adrian Colyer
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @since 1.0
 */
@Component(componentAbstract = true)
public abstract class AbstractProjectOperations implements ProjectOperations {
	@Reference protected MetadataService metadataService;
	@Reference protected ProjectMetadataProvider projectMetadataProvider;

	public final boolean isProjectAvailable() {
		return getProjectMetadata() != null;
	}
	
	public final ProjectMetadata getProjectMetadata() {
		return (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
	}

	public PathResolver getPathResolver() {
		ProjectMetadata projectMetadata = getProjectMetadata();
		return projectMetadata != null ? projectMetadata.getPathResolver() : null;
	}

	public void updateProjectType(ProjectType projectType) {
		Assert.notNull(projectType, "ProjectType required");
		projectMetadataProvider.updateProjectType(projectType);
	}
	
	public final void addDependencies(List<Dependency> dependencies) {
		Assert.isTrue(isProjectAvailable(), "Dependency modification prohibited at this time");
		Assert.notNull(dependencies, "Dependencies required");
		projectMetadataProvider.addDependencies(dependencies);
	}

	public final void addDependency(Dependency dependency) {
		Assert.isTrue(isProjectAvailable(), "Dependency modification prohibited at this time");
		Assert.notNull(dependency, "Dependency required");
		projectMetadataProvider.addDependency(dependency);
	}
	
	public final void addDependency(String groupId, String artifactId, String version) {
		Assert.isTrue(isProjectAvailable(), "Dependency modification prohibited at this time");
		Assert.notNull(groupId, "Group ID required");
		Assert.notNull(artifactId, "Artifact ID required");
		Assert.hasText(version, "Version required");
		Dependency dependency = new Dependency(groupId, artifactId, version, DependencyType.JAR, DependencyScope.COMPILE);
		projectMetadataProvider.addDependency(dependency);
	}
	
	public final void removeDependencies(List<Dependency> dependencies) {
		Assert.isTrue(isProjectAvailable(), "Dependency modification prohibited at this time");
		Assert.notNull(dependencies, "Dependencies required");
		projectMetadataProvider.removeDependencies(dependencies);
	}

	public final void removeDependency(Dependency dependency) {
		Assert.isTrue(isProjectAvailable(), "Dependency modification prohibited at this time");
		Assert.notNull(dependency, "Dependency required");
		projectMetadataProvider.removeDependency(dependency);
	}

	public final void removeDependency(String groupId, String artifactId, String version) {
		Assert.isTrue(isProjectAvailable(), "Dependency modification prohibited at this time");
		Assert.notNull(groupId, "Group ID required");
		Assert.notNull(artifactId, "Artifact ID required");
		Assert.hasText(version, "Version required");
		Dependency dependency = new Dependency(groupId, artifactId, version, DependencyType.JAR, DependencyScope.COMPILE);
		projectMetadataProvider.removeDependency(dependency);
	}

	public final void addRepositories(List<Repository> repositories) {
		Assert.isTrue(isProjectAvailable(), "Repository modification prohibited at this time");
		Assert.notNull(repositories, "Repositories required");
		projectMetadataProvider.addRepositories(repositories);
	}

	public final void addRepository(Repository repository) {
		Assert.isTrue(isProjectAvailable(), "Repository modification prohibited at this time");
		Assert.notNull(repository, "Repository required");
		projectMetadataProvider.addRepository(repository);
	}

	public final void removeRepository(Repository repository) {
		Assert.isTrue(isProjectAvailable(), "Repository modification prohibited at this time");
		Assert.notNull(repository, "Repository required");
		projectMetadataProvider.removeRepository(repository);
	}

	public final void addPluginRepositories(List<Repository> repositories) {
		Assert.isTrue(isProjectAvailable(), "Plugin repository modification prohibited at this time");
		Assert.notNull(repositories, "Plugin repositories required");
		projectMetadataProvider.addPluginRepositories(repositories);
	}

	public final void addPluginRepository(Repository repository) {
		Assert.isTrue(isProjectAvailable(), "Plugin repository modification prohibited at this time");
		Assert.notNull(repository, "Repository required");
		projectMetadataProvider.addPluginRepository(repository);
	}

	public final void removePluginRepository(Repository repository) {
		Assert.isTrue(isProjectAvailable(), "Plugin repository modification prohibited at this time");
		Assert.notNull(repository, "Repository required");
		projectMetadataProvider.removePluginRepository(repository);
	}

	public final void addBuildPlugins(List<Plugin> plugins) {
		Assert.isTrue(isProjectAvailable(), "Plugin modification prohibited at this time");
		Assert.notNull(plugins, "BuildPlugins required");
		projectMetadataProvider.addBuildPlugins(plugins);
	}
	
	public final void addBuildPlugin(Plugin plugin) {
		Assert.isTrue(isProjectAvailable(), "Plugin modification prohibited at this time");
		Assert.notNull(plugin, "Plugin required");
		projectMetadataProvider.addBuildPlugin(plugin);
	}
	
	public final void removeBuildPlugins(List<Plugin> plugins) {
		Assert.isTrue(isProjectAvailable(), "Plugin modification prohibited at this time");
		Assert.notNull(plugins, "Plugins required");
		projectMetadataProvider.removeBuildPlugins(plugins);
	}

	public final void removeBuildPlugin(Plugin plugin) {
		Assert.isTrue(isProjectAvailable(), "Plugin modification prohibited at this time");
		Assert.notNull(plugin, "Plugin required");
		projectMetadataProvider.removeBuildPlugin(plugin);
	}
	
	public void updateBuildPlugin(Plugin plugin) {
		ProjectMetadata projectMetadata = getProjectMetadata();
		Assert.notNull(projectMetadata, "Plugin modification prohibited at this time");
		Assert.notNull(plugin, "Plugin required");
		for (Plugin existingPlugin : projectMetadata.getBuildPlugins()) {
			if (existingPlugin.equals(plugin)) {
				// Already exists, so just quit
				return;
			}
		}
		
		// Delete any existing plugin with a different version
		for (Plugin existing : projectMetadata.getBuildPluginsExcludingVersion(plugin)) {
			projectMetadataProvider.removeBuildPlugin(existing);
		}
		
		// Add the plugin
		projectMetadataProvider.addBuildPlugin(plugin);
	}
	
	public final void addProperty(Property property) {
		Assert.isTrue(isProjectAvailable(), "Property modification prohibited at this time");
		Assert.notNull(property, "Property required");		
		projectMetadataProvider.addProperty(property);
	}
	
	public final void removeProperty(Property property) {
		Assert.isTrue(isProjectAvailable(), "Property modification prohibited at this time");
		Assert.notNull(property, "Property required");		
		projectMetadataProvider.removeProperty(property);
	}
	
	public final void addFilter(Filter filter) {
		Assert.isTrue(isProjectAvailable(), "Filter modification prohibited at this time");
		Assert.notNull(filter, "Filter required");
		projectMetadataProvider.addFilter(filter);
	}

	public final void removeFilter(Filter filter) {
		Assert.isTrue(isProjectAvailable(), "Filter modification prohibited at this time");
		Assert.notNull(filter, "Filter required");
		projectMetadataProvider.removeFilter(filter);
	}
	
	public final void addResource(Resource resource) {
		Assert.isTrue(isProjectAvailable(), "Resource modification prohibited at this time");
		Assert.notNull(resource, "Resource required");
		projectMetadataProvider.addResource(resource);
	}

	public final void removeResource(Resource resource) {
		Assert.isTrue(isProjectAvailable(), "Resource modification prohibited at this time");
		Assert.notNull(resource, "Resource required");
		projectMetadataProvider.removeResource(resource);
	}
}
