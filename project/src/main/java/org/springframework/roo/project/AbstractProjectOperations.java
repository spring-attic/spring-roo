package org.springframework.roo.project;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.project.listeners.DependencyListener;
import org.springframework.roo.project.listeners.FilterListener;
import org.springframework.roo.project.listeners.PluginListener;
import org.springframework.roo.project.listeners.PropertyListener;
import org.springframework.roo.project.listeners.RepositoryListener;
import org.springframework.roo.project.listeners.ResourceListener;
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

	private Set<DependencyListener> listeners = new HashSet<DependencyListener>();
	private Set<RepositoryListener> repositoryListeners = new HashSet<RepositoryListener>();
	private Set<RepositoryListener> pluginRepositoryListeners = new HashSet<RepositoryListener>();
	private Set<PluginListener> pluginListeners = new HashSet<PluginListener>();
	private Set<PropertyListener> propertyListeners = new HashSet<PropertyListener>();
	private Set<FilterListener> filterListeners = new HashSet<FilterListener>();
	private Set<ResourceListener> resourceListeners = new HashSet<ResourceListener>();

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

	public void addDependencyListener(DependencyListener listener) {
		this.listeners.add(listener);
	}
	
	public void removeDependencyListener(DependencyListener listener) {
		this.listeners.remove(listener);
	}
	
	private void sendDependencyAdditionNotifications(Dependency dependency) {
		for (DependencyListener listener : listeners) {
			listener.dependencyAdded(dependency);
		}
	}
	
	private void sendDependencyRemovalNotifications(Dependency dependency) {
		for (DependencyListener listener :listeners) {
			listener.dependencyRemoved(dependency);
		}
	}
	
	public void addRepositoryListener(RepositoryListener listener) {
		this.repositoryListeners.add(listener);
	}
	
	public void removeRepositoryListener(RepositoryListener listener) {
		this.repositoryListeners.remove(listener);
	}
	
	private void sendRepositoryAdditionNotifications(Repository repository) {
		for (RepositoryListener listener : repositoryListeners) {
			listener.repositoryAdded(repository);
		}
	}
	
	private void sendRepositoryRemovalNotifications(Repository repository) {
		for (RepositoryListener listener : repositoryListeners) {
			listener.repositoryRemoved(repository);
		}
	}
	
	public void addPluginRepositoryListener(RepositoryListener listener) {
		this.pluginRepositoryListeners.add(listener);
	}
	
	public void removePluginRepositoryListener(RepositoryListener listener) {
		this.pluginRepositoryListeners.remove(listener);
	}
	
	private void sendPluginRepositoryAdditionNotifications(Repository repository) {
		for (RepositoryListener listener : pluginRepositoryListeners) {
			listener.repositoryAdded(repository);
		}
	}
	
	private void sendPluginRepositoryRemovalNotifications(Repository repository) {
		for (RepositoryListener listener : pluginRepositoryListeners) {
			listener.repositoryRemoved(repository);
		}
	}
	
	public void addPluginListener(PluginListener listener) {
		this.pluginListeners.add(listener);
	}
	
	public void removePluginListener(PluginListener listener) {
		this.pluginListeners.remove(listener);
	}
	
	private void sendPluginAdditionNotifications(Plugin plugin) {
		for (PluginListener listener : pluginListeners) {
			listener.pluginAdded(plugin);
		}
	}
	
	private void sendPluginRemovalNotifications(Plugin plugin) {
		for (PluginListener listener : pluginListeners) {
			listener.pluginRemoved(plugin);
		}
	}

	public void addPropertyListener(PropertyListener listener) {
		this.propertyListeners.add(listener);
	}
	
	public void removePropertyListener(PropertyListener listener) {
		this.propertyListeners.remove(listener);
	}
	
	private void sendPropertyAdditionNotifications(Property property) {
		for (PropertyListener listener : propertyListeners) {
			listener.propertyAdded(property);
		}
	}
	
	private void sendPropertyRemovalNotifications(Property property) {
		for (PropertyListener listener : propertyListeners) {
			listener.propertyRemoved(property);
		}
	}

	public void addFilterListener(FilterListener listener) {
		this.filterListeners.add(listener);
	}
	
	public void removeFilterListener(FilterListener listener) {
		this.filterListeners.remove(listener);
	}
	
	private void sendFilterAdditionNotifications(Filter filter) {
		for (FilterListener listener : filterListeners) {
			listener.filterAdded(filter);
		}
	}
	
	private void sendFilterRemovalNotifications(Filter filter) {
		for (FilterListener listener : filterListeners) {
			listener.filterRemoved(filter);
		}
	}

	public void addResourceListener(ResourceListener listener) {
		this.resourceListeners.add(listener);
	}
	
	public void removeResourceListener(ResourceListener listener) {
		this.resourceListeners.remove(listener);
	}
	
	private void sendResourceAdditionNotifications(Resource resource) {
		for (ResourceListener listener : resourceListeners) {
			listener.resourceAdded(resource);
		}
	}
	
	private void sendResourceRemovalNotifications(Resource resource) {
		for (ResourceListener listener : resourceListeners) {
			listener.resourceRemoved(resource);
		}
	}

	public void updateProjectType(ProjectType projectType) {
		Assert.notNull(projectType, "ProjectType required");
		projectMetadataProvider.updateProjectType(projectType);
	}
	
	public final void addDependencies(List<Dependency> dependencies) {
		Assert.isTrue(isProjectAvailable(), "Dependency modification prohibited at this time");
		Assert.notNull(dependencies, "Dependencies required");
		projectMetadataProvider.addDependencies(dependencies);
		for (Dependency dependency : dependencies) {
			sendDependencyAdditionNotifications(dependency);
		}
	}

	public final void addDependency(Dependency dependency) {
		Assert.isTrue(isProjectAvailable(), "Dependency modification prohibited at this time");
		Assert.notNull(dependency, "Dependency required");
		projectMetadataProvider.addDependency(dependency);
		sendDependencyAdditionNotifications(dependency);		
	}
	
	public final void addDependency(String groupId, String artifactId, String version) {
		Assert.isTrue(isProjectAvailable(), "Dependency modification prohibited at this time");
		Assert.notNull(groupId, "Group ID required");
		Assert.notNull(artifactId, "Artifact ID required");
		Assert.hasText(version, "Version required");
		Dependency dependency = new Dependency(groupId, artifactId, version, DependencyType.JAR, DependencyScope.COMPILE);
		projectMetadataProvider.addDependency(dependency);
		sendDependencyAdditionNotifications(dependency);
	}
	
	public final void removeDependency(Dependency dependency) {
		Assert.isTrue(isProjectAvailable(), "Dependency modification prohibited at this time");
		Assert.notNull(dependency, "Dependency required");
		projectMetadataProvider.removeDependency(dependency);
		sendDependencyAdditionNotifications(dependency);		
	}

	public final void removeDependency(String groupId, String artifactId, String version) {
		Assert.isTrue(isProjectAvailable(), "Dependency modification prohibited at this time");
		Assert.notNull(groupId, "Group ID required");
		Assert.notNull(artifactId, "Artifact ID required");
		Assert.hasText(version, "Version required");
		Dependency dependency = new Dependency(groupId, artifactId, version, DependencyType.JAR, DependencyScope.COMPILE);
		projectMetadataProvider.removeDependency(dependency);
		sendDependencyRemovalNotifications(dependency);
	}

	public final void addRepositories(List<Repository> repositories) {
		Assert.isTrue(isProjectAvailable(), "Repository modification prohibited at this time");
		Assert.notNull(repositories, "Repositories required");
		projectMetadataProvider.addRepositories(repositories);
		for (Repository repository : repositories) {
			sendRepositoryAdditionNotifications(repository);
		}
	}

	public final void addRepository(Repository repository) {
		Assert.isTrue(isProjectAvailable(), "Repository modification prohibited at this time");
		Assert.notNull(repository, "Repository required");
		projectMetadataProvider.addRepository(repository);
		sendRepositoryAdditionNotifications(repository);
	}

	public final void removeRepository(Repository repository) {
		Assert.isTrue(isProjectAvailable(), "Repository modification prohibited at this time");
		Assert.notNull(repository, "Repository required");
		projectMetadataProvider.removeRepository(repository);
		sendRepositoryRemovalNotifications(repository);
	}

	public final void addPluginRepositories(List<Repository> repositories) {
		Assert.isTrue(isProjectAvailable(), "Plugin repository modification prohibited at this time");
		Assert.notNull(repositories, "Plugin repositories required");
		projectMetadataProvider.addPluginRepositories(repositories);
		for (Repository repository : repositories) {
			sendPluginRepositoryAdditionNotifications(repository);
		}
	}

	public final void addPluginRepository(Repository repository) {
		Assert.isTrue(isProjectAvailable(), "Plugin repository modification prohibited at this time");
		Assert.notNull(repository, "Repository required");
		projectMetadataProvider.addPluginRepository(repository);
		sendPluginRepositoryAdditionNotifications(repository);
	}

	public final void removePluginRepository(Repository repository) {
		Assert.isTrue(isProjectAvailable(), "Plugin repository modification prohibited at this time");
		Assert.notNull(repository, "Repository required");
		projectMetadataProvider.removePluginRepository(repository);
		sendPluginRepositoryRemovalNotifications(repository);
	}

	public final void addBuildPlugin(Plugin plugin) {
		Assert.isTrue(isProjectAvailable(), "Plugin modification prohibited at this time");
		Assert.notNull(plugin, "Plugin required");
		projectMetadataProvider.addBuildPlugin(plugin);
		sendPluginAdditionNotifications(plugin);
	}
	
	public final void removeBuildPlugin(Plugin plugin) {
		Assert.isTrue(isProjectAvailable(), "Plugin modification prohibited at this time");
		Assert.notNull(plugin, "Plugin required");
		projectMetadataProvider.removeBuildPlugin(plugin);
		sendPluginRemovalNotifications(plugin);
	}
	
	public void buildPluginUpdate(Plugin plugin) {
		Assert.isTrue(isProjectAvailable(), "Plugin modification prohibited at this time");
		Assert.notNull(plugin, "Plugin required");
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata unavailable");
		
		if (projectMetadata.isBuildPluginRegistered(plugin)) {
			// Already exists, so just quit
			return;
		}
		
		// Delete any existing plugin with a different version
		for (Plugin existing : projectMetadata.getBuildPluginsExcludingVersion(plugin)) {
			projectMetadataProvider.removeBuildPlugin(existing);
			sendPluginRemovalNotifications(existing);
		}
		
		// Add the plugin
		projectMetadataProvider.addBuildPlugin(plugin);
		sendPluginAdditionNotifications(plugin);
	}
	
	public final void addProperty(Property property) {
		Assert.isTrue(isProjectAvailable(), "Property modification prohibited at this time");
		Assert.notNull(property, "Property required");		
		projectMetadataProvider.addProperty(property);
		sendPropertyAdditionNotifications(property);
	}
	
	public final void removeProperty(Property property) {
		Assert.isTrue(isProjectAvailable(), "Property modification prohibited at this time");
		Assert.notNull(property, "Property required");		
		projectMetadataProvider.removeProperty(property);
		sendPropertyRemovalNotifications(property);
	}
	
	public final void addFilter(Filter filter) {
		Assert.isTrue(isProjectAvailable(), "Filter modification prohibited at this time");
		Assert.notNull(filter, "Filter required");
		projectMetadataProvider.addFilter(filter);
		sendFilterAdditionNotifications(filter);
	}

	public final void removeFilter(Filter filter) {
		Assert.isTrue(isProjectAvailable(), "Filter modification prohibited at this time");
		Assert.notNull(filter, "Filter required");
		projectMetadataProvider.removeFilter(filter);
		sendFilterRemovalNotifications(filter);
	}
	
	public final void addResource(Resource resource) {
		Assert.isTrue(isProjectAvailable(), "Resource modification prohibited at this time");
		Assert.notNull(resource, "Resource required");
		projectMetadataProvider.addResource(resource);
		sendResourceAdditionNotifications(resource);
	}

	public final void removeResource(Resource resource) {
		Assert.isTrue(isProjectAvailable(), "Resource modification prohibited at this time");
		Assert.notNull(resource, "Resource required");
		projectMetadataProvider.removeResource(resource);
		sendResourceRemovalNotifications(resource);
	}
}
