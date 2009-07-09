package org.springframework.roo.project;

import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.support.util.Assert;

/**
 * Provides common project operations. Should be subclassed by a project-specific operations subclass. 
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public abstract class ProjectOperations {
	private MetadataService metadataService;
	private ProjectMetadataProvider projectMetadataProvider;
	
	public ProjectOperations(MetadataService metadataService, ProjectMetadataProvider projectMetadataProvider) {
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(projectMetadataProvider, "Project metadata provider required");
		this.metadataService = metadataService;
		this.projectMetadataProvider = projectMetadataProvider;
	}
	
	public final boolean isDependencyModificationAllowed() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) != null;
	}
	
	/**
	 * Allows addition of a JAR dependency to the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to add support
	 * for their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param groupId to add (required)
	 * @param artifactId to add (required)
	 * @param versionId to add (requireD)
	 */
	public final void addDependency(JavaPackage groupId, JavaSymbolName artifactId, String version) {
		Assert.isTrue(isDependencyModificationAllowed(), "Dependency modification prohibited at this time");
		Assert.notNull(groupId, "Group ID required");
		Assert.notNull(artifactId, "Artifact ID required");
		Assert.hasText(version, "Version required");
		Dependency dependency = new Dependency(groupId, artifactId, version, DependencyType.JAR);
		projectMetadataProvider.addDependency(dependency);
	}
	
	/**
	 * Allows remove of an existing JAR dependency from the POM. 
	 * 
	 * <p>
	 * Provides a convenient way for third parties to instruct end users how to use the CLI to remove an unwanted
	 * dependency from their projects without requiring the user to manually edit a pom.xml or write an add-on.
	 * 
	 * @param groupId to add (required)
	 * @param artifactId to add (required)
	 * @param versionId to add (requireD)
	 */
	public final void removeDependency(JavaPackage groupId, JavaSymbolName artifactId, String version) {
		Assert.isTrue(isDependencyModificationAllowed(), "Dependency modification prohibited at this time");
		Assert.notNull(groupId, "Group ID required");
		Assert.notNull(artifactId, "Artifact ID required");
		Assert.hasText(version, "Version required");
		Dependency dependency = new Dependency(groupId, artifactId, version, DependencyType.JAR);
		projectMetadataProvider.removeDependency(dependency);
	}
	
	/**
	 * Verifies if the specified dependency is present. If it is present, silently returns. If it is not
	 * present, removes any dependency which matches {@link ProjectMetadata#getDependenciesExcludingVersion(Dependency)}.
	 * Always adds the presented dependency.
	 * 
	 * @param dependency to add (required)
	 */
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
		}
		
		// Add the dependency
		projectMetadataProvider.addDependency(dependency);
	}
	
	public void updateProjectType(ProjectType projectType) {
		Assert.notNull(projectType, "ProjectType required");
		
		projectMetadataProvider.updateProjectType(projectType);
	}

}
