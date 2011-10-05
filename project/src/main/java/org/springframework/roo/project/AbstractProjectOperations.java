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
@SuppressWarnings("deprecation")
@Component(componentAbstract = true)
public abstract class AbstractProjectOperations implements ProjectOperations {

	// Fields
	@Reference protected MetadataService metadataService;
	@Reference protected PathResolver pathResolver;
	@Reference protected ProjectMetadataProvider projectMetadataProvider;

	private final Set<DependencyListener> listeners = new HashSet<DependencyListener>();
	private final Set<RepositoryListener> repositoryListeners = new HashSet<RepositoryListener>();
	private final Set<RepositoryListener> pluginRepositoryListeners = new HashSet<RepositoryListener>();
	private final Set<PluginListener> pluginListeners = new HashSet<PluginListener>();
	private final Set<PropertyListener> propertyListeners = new HashSet<PropertyListener>();
	private final Set<FilterListener> filterListeners = new HashSet<FilterListener>();
	private final Set<ResourceListener> resourceListeners = new HashSet<ResourceListener>();

	public final boolean isProjectAvailable() {
		return getProjectMetadata() != null;
	}

	public final ProjectMetadata getProjectMetadata() {
		return (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
	}

	public PathResolver getPathResolver() {
		return pathResolver;
	}

	@Deprecated
	public void addDependencyListener(final DependencyListener listener) {
		this.listeners.add(listener);
	}

	@Deprecated
	public void removeDependencyListener(final DependencyListener listener) {
		this.listeners.remove(listener);
	}

	@Deprecated
	private void sendDependencyAdditionNotifications(final Dependency dependency) {
		for (DependencyListener listener : listeners) {
			listener.dependencyAdded(dependency);
		}
	}

	@Deprecated
	private void sendDependencyRemovalNotifications(final Dependency dependency) {
		for (DependencyListener listener :listeners) {
			listener.dependencyRemoved(dependency);
		}
	}

	@Deprecated
	public void addRepositoryListener(final RepositoryListener listener) {
		this.repositoryListeners.add(listener);
	}

	@Deprecated
	public void removeRepositoryListener(final RepositoryListener listener) {
		this.repositoryListeners.remove(listener);
	}

	@Deprecated
	private void sendRepositoryAdditionNotifications(final Repository repository) {
		for (RepositoryListener listener : repositoryListeners) {
			listener.repositoryAdded(repository);
		}
	}

	@Deprecated
	private void sendRepositoryRemovalNotifications(final Repository repository) {
		for (RepositoryListener listener : repositoryListeners) {
			listener.repositoryRemoved(repository);
		}
	}

	@Deprecated
	public void addPluginRepositoryListener(final RepositoryListener listener) {
		this.pluginRepositoryListeners.add(listener);
	}

	@Deprecated
	public void removePluginRepositoryListener(final RepositoryListener listener) {
		this.pluginRepositoryListeners.remove(listener);
	}

	@Deprecated
	private void sendPluginRepositoryAdditionNotifications(final Repository repository) {
		for (RepositoryListener listener : pluginRepositoryListeners) {
			listener.repositoryAdded(repository);
		}
	}

	@Deprecated
	private void sendPluginRepositoryRemovalNotifications(final Repository repository) {
		for (RepositoryListener listener : pluginRepositoryListeners) {
			listener.repositoryRemoved(repository);
		}
	}

	@Deprecated
	public void addPluginListener(final PluginListener listener) {
		this.pluginListeners.add(listener);
	}

	@Deprecated
	public void removePluginListener(final PluginListener listener) {
		this.pluginListeners.remove(listener);
	}

	@Deprecated
	private void sendPluginAdditionNotifications(final Plugin plugin) {
		for (PluginListener listener : pluginListeners) {
			listener.pluginAdded(plugin);
		}
	}

	@Deprecated
	private void sendPluginRemovalNotifications(final Plugin plugin) {
		for (PluginListener listener : pluginListeners) {
			listener.pluginRemoved(plugin);
		}
	}

	@Deprecated
	public void addPropertyListener(final PropertyListener listener) {
		this.propertyListeners.add(listener);
	}

	@Deprecated
	public void removePropertyListener(final PropertyListener listener) {
		this.propertyListeners.remove(listener);
	}

	@Deprecated
	private void sendPropertyAdditionNotifications(final Property property) {
		for (PropertyListener listener : propertyListeners) {
			listener.propertyAdded(property);
		}
	}

	@Deprecated
	private void sendPropertyRemovalNotifications(final Property property) {
		for (PropertyListener listener : propertyListeners) {
			listener.propertyRemoved(property);
		}
	}

	@Deprecated
	public void addFilterListener(final FilterListener listener) {
		this.filterListeners.add(listener);
	}

	@Deprecated
	public void removeFilterListener(final FilterListener listener) {
		this.filterListeners.remove(listener);
	}

	@Deprecated
	private void sendFilterAdditionNotifications(final Filter filter) {
		for (FilterListener listener : filterListeners) {
			listener.filterAdded(filter);
		}
	}

	@Deprecated
	private void sendFilterRemovalNotifications(final Filter filter) {
		for (FilterListener listener : filterListeners) {
			listener.filterRemoved(filter);
		}
	}

	@Deprecated
	public void addResourceListener(final ResourceListener listener) {
		this.resourceListeners.add(listener);
	}

	@Deprecated
	public void removeResourceListener(final ResourceListener listener) {
		this.resourceListeners.remove(listener);
	}

	@Deprecated
	private void sendResourceAdditionNotifications(final Resource resource) {
		for (ResourceListener listener : resourceListeners) {
			listener.resourceAdded(resource);
		}
	}

	@Deprecated
	private void sendResourceRemovalNotifications(final Resource resource) {
		for (ResourceListener listener : resourceListeners) {
			listener.resourceRemoved(resource);
		}
	}

	public void updateProjectType(final ProjectType projectType) {
		Assert.notNull(projectType, "ProjectType required");
		projectMetadataProvider.updateProjectType(projectType);
	}

	public final void addDependencies(final List<Dependency> dependencies) {
		Assert.isTrue(isProjectAvailable(), "Dependency modification prohibited at this time");
		Assert.notNull(dependencies, "Dependencies required");
		projectMetadataProvider.addDependencies(dependencies);
		for (Dependency dependency : dependencies) {
			sendDependencyAdditionNotifications(dependency);
		}
	}

	public final void addDependency(final Dependency dependency) {
		Assert.isTrue(isProjectAvailable(), "Dependency modification prohibited at this time");
		Assert.notNull(dependency, "Dependency required");
		projectMetadataProvider.addDependency(dependency);
		sendDependencyAdditionNotifications(dependency);
	}

	public final void addDependency(final String groupId, final String artifactId, final String version, DependencyScope scope, final String classifier) {
		Assert.isTrue(isProjectAvailable(), "Dependency modification prohibited at this time");
		Assert.notNull(groupId, "Group ID required");
		Assert.notNull(artifactId, "Artifact ID required");
		Assert.hasText(version, "Version required");
		if (scope == null) {
			scope = DependencyScope.COMPILE;
		}
		Dependency dependency = new Dependency(groupId, artifactId, version, DependencyType.JAR, scope, classifier);
		projectMetadataProvider.addDependency(dependency);
		sendDependencyAdditionNotifications(dependency);
	}

	public final void addDependency(final String groupId, final String artifactId, final String version, final DependencyScope scope) {
		addDependency(groupId, artifactId, version, scope, "");
	}

	public final void addDependency(final String groupId, final String artifactId, final String version) {
		addDependency(groupId, artifactId, version, DependencyScope.COMPILE);
	}

	public final void removeDependencies(final List<Dependency> dependencies) {
		Assert.isTrue(isProjectAvailable(), "Dependency modification prohibited at this time");
		Assert.notNull(dependencies, "Dependencies required");
		projectMetadataProvider.removeDependencies(dependencies);
		for (Dependency dependency : dependencies) {
			sendDependencyRemovalNotifications(dependency);
		}
	}

	public final void removeDependency(final Dependency dependency) {
		Assert.isTrue(isProjectAvailable(), "Dependency modification prohibited at this time");
		Assert.notNull(dependency, "Dependency required");
		projectMetadataProvider.removeDependency(dependency);
		sendDependencyAdditionNotifications(dependency);
	}

	public final void removeDependency(final String groupId, final String artifactId, final String version, final String classifier) {
		Assert.isTrue(isProjectAvailable(), "Dependency modification prohibited at this time");
		Assert.notNull(groupId, "Group ID required");
		Assert.notNull(artifactId, "Artifact ID required");
		Assert.hasText(version, "Version required");
		Dependency dependency = new Dependency(groupId, artifactId, version, DependencyType.JAR, DependencyScope.COMPILE, classifier);
		projectMetadataProvider.removeDependency(dependency);
		sendDependencyRemovalNotifications(dependency);
	}

	public final void updateDependencyScope(final Dependency dependency, final DependencyScope dependencyScope) {
		Assert.isTrue(isProjectAvailable(), "Dependency modification prohibited at this time");
		Assert.notNull(dependency, "Dependency required");
		projectMetadataProvider.updateDependencyScope(dependency, dependencyScope);
	}

	public final void removeDependency(final String groupId, final String artifactId, final String version) {
		removeDependency(groupId, artifactId, version, "");
	}

	public final void addRepositories(final List<Repository> repositories) {
		Assert.isTrue(isProjectAvailable(), "Repository modification prohibited at this time");
		Assert.notNull(repositories, "Repositories required");
		projectMetadataProvider.addRepositories(repositories);
		for (Repository repository : repositories) {
			sendRepositoryAdditionNotifications(repository);
		}
	}

	public final void addRepository(final Repository repository) {
		Assert.isTrue(isProjectAvailable(), "Repository modification prohibited at this time");
		Assert.notNull(repository, "Repository required");
		projectMetadataProvider.addRepository(repository);
		sendRepositoryAdditionNotifications(repository);
	}

	public final void removeRepository(final Repository repository) {
		Assert.isTrue(isProjectAvailable(), "Repository modification prohibited at this time");
		Assert.notNull(repository, "Repository required");
		projectMetadataProvider.removeRepository(repository);
		sendRepositoryRemovalNotifications(repository);
	}

	public final void addPluginRepositories(final List<Repository> repositories) {
		Assert.isTrue(isProjectAvailable(), "Plugin repository modification prohibited at this time");
		Assert.notNull(repositories, "Plugin repositories required");
		projectMetadataProvider.addPluginRepositories(repositories);
		for (Repository repository : repositories) {
			sendPluginRepositoryAdditionNotifications(repository);
		}
	}

	public final void addPluginRepository(final Repository repository) {
		Assert.isTrue(isProjectAvailable(), "Plugin repository modification prohibited at this time");
		Assert.notNull(repository, "Repository required");
		projectMetadataProvider.addPluginRepository(repository);
		sendPluginRepositoryAdditionNotifications(repository);
	}

	public final void removePluginRepository(final Repository repository) {
		Assert.isTrue(isProjectAvailable(), "Plugin repository modification prohibited at this time");
		Assert.notNull(repository, "Repository required");
		projectMetadataProvider.removePluginRepository(repository);
		sendPluginRepositoryRemovalNotifications(repository);
	}

	public final void addBuildPlugins(final List<Plugin> plugins) {
		Assert.isTrue(isProjectAvailable(), "Plugin modification prohibited at this time");
		Assert.notNull(plugins, "BuildPlugins required");
		projectMetadataProvider.addBuildPlugins(plugins);
		for (Plugin plugin : plugins) {
			sendPluginAdditionNotifications(plugin);
		}
	}

	public final void addBuildPlugin(final Plugin plugin) {
		Assert.isTrue(isProjectAvailable(), "Plugin modification prohibited at this time");
		Assert.notNull(plugin, "Plugin required");
		projectMetadataProvider.addBuildPlugin(plugin);
		sendPluginAdditionNotifications(plugin);
	}

	public final void removeBuildPlugins(final List<Plugin> plugins) {
		Assert.isTrue(isProjectAvailable(), "Plugin modification prohibited at this time");
		Assert.notNull(plugins, "Plugins required");
		projectMetadataProvider.removeBuildPlugins(plugins);
		for (Plugin plugin : plugins) {
			sendPluginRemovalNotifications(plugin);
		}
	}

	public final void removeBuildPlugin(final Plugin plugin) {
		Assert.isTrue(isProjectAvailable(), "Plugin modification prohibited at this time");
		Assert.notNull(plugin, "Plugin required");
		projectMetadataProvider.removeBuildPlugin(plugin);
		sendPluginRemovalNotifications(plugin);
	}

	public void updateBuildPlugin(final Plugin plugin) {
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
		projectMetadataProvider.removeBuildPlugin(plugin);

		// Add the plugin
		projectMetadataProvider.addBuildPlugin(plugin);
		sendPluginAdditionNotifications(plugin);
	}

	@Deprecated
	public void buildPluginUpdate(final Plugin plugin) {
		updateBuildPlugin(plugin);
	}

	public final void addProperty(final Property property) {
		Assert.isTrue(isProjectAvailable(), "Property modification prohibited at this time");
		Assert.notNull(property, "Property required");
		projectMetadataProvider.addProperty(property);
		sendPropertyAdditionNotifications(property);
	}

	public final void removeProperty(final Property property) {
		Assert.isTrue(isProjectAvailable(), "Property modification prohibited at this time");
		Assert.notNull(property, "Property required");
		projectMetadataProvider.removeProperty(property);
		sendPropertyRemovalNotifications(property);
	}

	public final void addFilter(final Filter filter) {
		Assert.isTrue(isProjectAvailable(), "Filter modification prohibited at this time");
		Assert.notNull(filter, "Filter required");
		projectMetadataProvider.addFilter(filter);
		sendFilterAdditionNotifications(filter);
	}

	public final void removeFilter(final Filter filter) {
		Assert.isTrue(isProjectAvailable(), "Filter modification prohibited at this time");
		Assert.notNull(filter, "Filter required");
		projectMetadataProvider.removeFilter(filter);
		sendFilterRemovalNotifications(filter);
	}

	public final void addResource(final Resource resource) {
		Assert.isTrue(isProjectAvailable(), "Resource modification prohibited at this time");
		Assert.notNull(resource, "Resource required");
		projectMetadataProvider.addResource(resource);
		sendResourceAdditionNotifications(resource);
	}

	public final void removeResource(final Resource resource) {
		Assert.isTrue(isProjectAvailable(), "Resource modification prohibited at this time");
		Assert.notNull(resource, "Resource required");
		projectMetadataProvider.removeResource(resource);
		sendResourceRemovalNotifications(resource);
	}
}
