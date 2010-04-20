package org.springframework.roo.project;

import java.util.HashSet;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
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
@Component(componentAbstract=true)
public abstract class AbstractProjectOperations implements ProjectOperations {
	@Reference protected MetadataService metadataService;
	@Reference protected ProjectMetadataProvider projectMetadataProvider;

	private Set<DependencyListener> listeners = new HashSet<DependencyListener>();
	private Set<RepositoryListener> repoListeners = new HashSet<RepositoryListener>();
	private Set<PluginRepositoryListener> pluginRepoListeners = new HashSet<PluginRepositoryListener>();
	private Set<PluginListener> pluginListeners = new HashSet<PluginListener>();
	private Set<PropertyListener> propertyListeners = new HashSet<PropertyListener>();

	public final boolean isDependencyModificationAllowed() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) != null;
	}

	public final boolean isPerformCommandAllowed() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) != null;
	}
	
	public void dependencyUpdate(Dependency dependency) {
		Assert.isTrue(isDependencyModificationAllowed(), "Dependency modification prohibited at this time");
		Assert.notNull(dependency, "Dependency required");
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata unavailable");
		
		if (projectMetadata.isDependencyRegistered(dependency)) {
			// Already exists, so just quit
			return;
		}
		
		// Delete any existing dependencies with a different version
		for (Dependency existing : projectMetadata.getDependenciesExcludingVersion(dependency)) {
			projectMetadataProvider.removeDependency(existing);
			sendDependencyRemovalNotifications(existing);
		}
		
		// Add the dependency
		projectMetadataProvider.addDependency(dependency);
		sendDependencyAdditionNotifications(dependency);
	}

	public void addDependencyListener(DependencyListener listener) {
		this.listeners.add(listener);
	}
	
	public void removeDependencyListener(DependencyListener listener) {
		this.listeners.remove(listener);
	}
	
	private void sendDependencyAdditionNotifications(Dependency d) {
		for (DependencyListener listener : listeners) {
			listener.dependencyAdded(d);
		}
	}
	
	private void sendDependencyRemovalNotifications(Dependency d) {
		for (DependencyListener listener :listeners) {
			listener.dependencyRemoved(d);
		}
	}
	
	public void addRepositoryListener(RepositoryListener listener) {
		this.repoListeners.add(listener);
	}
	
	public void removeRepositoryListener(RepositoryListener listener) {
		this.repoListeners.remove(listener);
	}
	
	private void sendRepositoryAdditionNotifications(Repository r) {
		for (RepositoryListener listener : repoListeners) {
			listener.repositoryAdded(r);
		}
	}
	
	private void sendRepositoryRemovalNotifications(Repository r) {
		for (RepositoryListener listener : repoListeners) {
			listener.repositoryRemoved(r);
		}
	}
	
	public void addPluginRepositoryListener(PluginRepositoryListener listener) {
		this.pluginRepoListeners.add(listener);
	}
	
	public void removePluginRepositoryListener(PluginRepositoryListener listener) {
		this.pluginRepoListeners.remove(listener);
	}
	
	private void sendPluginRepositoryAdditionNotifications(PluginRepository pluginRepository) {
		for (PluginRepositoryListener listener : pluginRepoListeners) {
			listener.pluginRepositoryAdded(pluginRepository);
		}
	}
	
	private void sendPluginRepositoryRemovalNotifications(PluginRepository pluginRepository) {
		for (PluginRepositoryListener listener : pluginRepoListeners) {
			listener.pluginRepositoryRemoved(pluginRepository);
		}
	}
	
	public void addPluginListener(PluginListener listener) {
		this.pluginListeners.add(listener);
	}
	
	public void removePluginListener(PluginListener listener) {
		this.pluginListeners.remove(listener);
	}
	
	private void sendPluginAdditionNotifications(Plugin p) {
		for (PluginListener listener : pluginListeners) {
			listener.pluginAdded(p);
		}
	}
	
	private void sendPluginRemovalNotifications(Plugin p) {
		for (PluginListener listener : pluginListeners) {
			listener.pluginRemoved(p);
		}
	}

	public void addPropertyListener(PropertyListener listener) {
		this.propertyListeners.add(listener);
	}
	
	public void removePropertyListener(PropertyListener listener) {
		this.propertyListeners.remove(listener);
	}
	
	private void sendPropertyAdditionNotifications(Property p) {
		for (PropertyListener listener : propertyListeners) {
			listener.propertyAdded(p);
		}
	}
	
	private void sendPropertyRemovalNotifications(Property p) {
		for (PropertyListener listener : propertyListeners) {
			listener.propertyRemoved(p);
		}
	}

	public void updateProjectType(ProjectType projectType) {
		Assert.notNull(projectType, "ProjectType required");
		projectMetadataProvider.updateProjectType(projectType);
	}
	
	public final void addDependency(Dependency dependency) {
		Assert.isTrue(isDependencyModificationAllowed(), "Dependency modification prohibited at this time");
		Assert.notNull(dependency, "Dependency required");
		projectMetadataProvider.addDependency(dependency);
		sendDependencyAdditionNotifications(dependency);		
	}
	
	public final void addDependency(JavaPackage groupId, JavaSymbolName artifactId, String version) {
		Assert.isTrue(isDependencyModificationAllowed(), "Dependency modification prohibited at this time");
		Assert.notNull(groupId, "Group ID required");
		Assert.notNull(artifactId, "Artifact ID required");
		Assert.hasText(version, "Version required");
		Dependency dependency = new Dependency(groupId, artifactId, version, DependencyType.JAR, DependencyScope.COMPILE);
		projectMetadataProvider.addDependency(dependency);
		sendDependencyAdditionNotifications(dependency);
	}
	
	public final void removeDependency(Dependency dependency) {
		Assert.isTrue(isDependencyModificationAllowed(), "Dependency modification prohibited at this time");
		Assert.notNull(dependency, "Dependency required");
		projectMetadataProvider.removeDependency(dependency);
		sendDependencyAdditionNotifications(dependency);		
	}

	public final void removeDependency(JavaPackage groupId, JavaSymbolName artifactId, String version) {
		Assert.isTrue(isDependencyModificationAllowed(), "Dependency modification prohibited at this time");
		Assert.notNull(groupId, "Group ID required");
		Assert.notNull(artifactId, "Artifact ID required");
		Assert.hasText(version, "Version required");
		Dependency dependency = new Dependency(groupId, artifactId, version, DependencyType.JAR, DependencyScope.COMPILE);
		projectMetadataProvider.removeDependency(dependency);
		sendDependencyRemovalNotifications(dependency);
	}
	
	public final void addRepository(String id, String name, String url) {
		Assert.isTrue(isDependencyModificationAllowed(), "Repository modification prohibited at this time");
		Assert.hasText(id, "ID required");
		Assert.hasText(url, "URL required");
		Repository repository = new Repository(id, name, url);
		projectMetadataProvider.addRepository(repository);
		sendRepositoryAdditionNotifications(repository);
	}
	
	public final void removeRepository(String id, String name, String url) {
		Assert.isTrue(isDependencyModificationAllowed(), "Repository modification prohibited at this time");
		Assert.hasText(id, "ID required");
		Assert.hasText(url, "URL required");
		Repository repository = new Repository(id, name, url);
		projectMetadataProvider.removeRepository(repository);
		sendRepositoryRemovalNotifications(repository);
	}
	
	public final void addPluginRepository(String id, String name, String url) {
		Assert.isTrue(isDependencyModificationAllowed(), "Plugin repository modification prohibited at this time");
		Assert.hasText(id, "ID required");
		Assert.hasText(url, "URL required");
		PluginRepository pluginRepository = new PluginRepository(id, name, url);
		projectMetadataProvider.addPluginRepository(pluginRepository);
		sendPluginRepositoryAdditionNotifications(pluginRepository);
	}
	
	public final void removePluginRepository(String id, String name, String url) {
		Assert.isTrue(isDependencyModificationAllowed(), "Plugin repository modification prohibited at this time");
		Assert.hasText(id, "ID required");
		Assert.hasText(url, "URL required");
		PluginRepository pluginRepository = new PluginRepository(id, name, url);
		projectMetadataProvider.removePluginRepository(pluginRepository);
		sendPluginRepositoryRemovalNotifications(pluginRepository);
	}
	
	public final void addBuildPlugin(Plugin plugin) {
		Assert.isTrue(isDependencyModificationAllowed(), "Plugin modification prohibited at this time");
		Assert.notNull(plugin, "Plugin required");
		projectMetadataProvider.addBuildPlugin(plugin);
		sendPluginAdditionNotifications(plugin);
	}
	
	public final void removeBuildPlugin(Plugin plugin) {
		Assert.isTrue(isDependencyModificationAllowed(), "Plugin modification prohibited at this time");
		Assert.notNull(plugin, "Plugin required");
		projectMetadataProvider.removeBuildPlugin(plugin);
		sendPluginRemovalNotifications(plugin);
	}
	
	public void buildPluginUpdate(Plugin plugin) {
		Assert.isTrue(isDependencyModificationAllowed(), "Plugin modification prohibited at this time");
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
		}
		
		// Add the plugin
		projectMetadataProvider.addBuildPlugin(plugin);
	}
	
	public final void addProperty(String name, String value) {
		Assert.isTrue(isDependencyModificationAllowed(), "Property modification prohibited at this time");
		Assert.hasText(name, "Name required");
		Assert.hasText(value, "Value required");
		Property property = new Property(name, value);
		projectMetadataProvider.addProperty(property);
		sendPropertyAdditionNotifications(property);
	}
	
	public final void removeProperty(String name, String value) {
		Assert.isTrue(isDependencyModificationAllowed(), "Property modification prohibited at this time");
		Assert.hasText(name, "Name required");
		Assert.hasText(value, "Value required");
		Property property = new Property(name, value);
		projectMetadataProvider.removeProperty(property);
		sendPropertyRemovalNotifications(property);
	}
}
