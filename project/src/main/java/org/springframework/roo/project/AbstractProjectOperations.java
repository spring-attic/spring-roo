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
 *
 */
@Component(componentAbstract=true)
public abstract class AbstractProjectOperations implements ProjectOperations {
	@Reference protected MetadataService metadataService;
	@Reference protected ProjectMetadataProvider projectMetadataProvider;

	private Set<DependencyListener> listeners = new HashSet<DependencyListener>();
	private Set<RepositoryListener> repoListeners = new HashSet<RepositoryListener>();

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
	
	public void updateProjectType(ProjectType projectType) {
		Assert.notNull(projectType, "ProjectType required");
		
		projectMetadataProvider.updateProjectType(projectType);
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
		Assert.hasText(name, "Name required");
		Assert.hasText(url, "URL required");
		Repository repository = new Repository(id, name, url);
		projectMetadataProvider.addRepository(repository);
		sendRepositoryAdditionNotifications(repository);
	}
	
	public final void removeRepository(String id, String name, String url) {
		Assert.isTrue(isDependencyModificationAllowed(), "Dependency modification prohibited at this time");
		Assert.hasText(id, "ID required");
		Assert.hasText(name, "Name required");
		Assert.hasText(url, "URL required");
		Repository repository = new Repository(id, name, url);
		projectMetadataProvider.removeRepository(repository);
		sendRepositoryRemovalNotifications(repository);
	}
	
	public final void addPluginDependency(
				JavaPackage groupId, JavaSymbolName artifactId, String version,
				Execution... executions) {
		Assert.isTrue(isDependencyModificationAllowed(), "Dependency modification prohibited at this time");
		Assert.notNull(groupId, "Group ID required");
		Assert.notNull(artifactId, "Artifact ID required");
		Assert.hasText(version, "Version required");
		Dependency dependency = new Dependency(groupId, artifactId, version, DependencyType.JAR, DependencyScope.COMPILE);
		projectMetadataProvider.addBuildPluginDependency(dependency,executions);
	}
	
	public final void removeBuildPlugin(JavaPackage groupId, JavaSymbolName artifactId, String version) {
		Assert.isTrue(isDependencyModificationAllowed(), "Dependency modification prohibited at this time");
		Assert.notNull(groupId, "Group ID required");
		Assert.notNull(artifactId, "Artifact ID required");
		Assert.hasText(version, "Version required");
		Dependency dependency = new Dependency(groupId, artifactId, version, DependencyType.JAR, DependencyScope.COMPILE);
		projectMetadataProvider.removeBuildPluginDependency(dependency);
	}
	
	public void buildPluginUpdate(Dependency buildPluginDependency, Execution... executions) {
		Assert.isTrue(isDependencyModificationAllowed(), "Dependency modification prohibited at this time");
		Assert.notNull(buildPluginDependency, "Dependency required");
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata unavailable");
		
		if (projectMetadata.isBuildPluginDependencyRegistered(buildPluginDependency)) {
			// Already exists, so just quit
			return;
		}
		
		// Delete any existing dependencies with a different version
		for (Dependency existing : projectMetadata.getBuildPluginDependenciesExcludingVersion(buildPluginDependency)) {
			projectMetadataProvider.removeBuildPluginDependency(existing);
		}
		
		// Add the dependency
		projectMetadataProvider.addBuildPluginDependency(buildPluginDependency,executions);
	}

}
