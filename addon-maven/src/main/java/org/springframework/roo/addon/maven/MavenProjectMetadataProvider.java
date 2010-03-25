package org.springframework.roo.addon.maven;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.file.monitor.event.FileEvent;
import org.springframework.roo.file.monitor.event.FileEventListener;
import org.springframework.roo.file.monitor.event.FileOperation;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.DependencyScope;
import org.springframework.roo.project.DependencyType;
import org.springframework.roo.project.Execution;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectMetadataProvider;
import org.springframework.roo.project.ProjectType;
import org.springframework.roo.project.Repository;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provides {@link ProjectMetadata}.
 * 
 * <p>
 * For simplicity of operation, this is the only implementation shipping with ROO that supports {@link ProjectMetadata}.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @since 1.0
 */
@Component
@Service
public class MavenProjectMetadataProvider implements ProjectMetadataProvider, FileEventListener {

	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(MetadataIdentificationUtils.getMetadataClass(ProjectMetadata.getProjectIdentifier()));
	
	@Reference private PathResolver pathResolver;
	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;

	protected void activate(ComponentContext context) {
		this.pom = pathResolver.getIdentifier(Path.ROOT, "/pom.xml");
	}

	private String pom;
	
	public MetadataItem get(String metadataIdentificationString) {
		Assert.isTrue(ProjectMetadata.getProjectIdentifier().equals(metadataIdentificationString), "Unexpected metadata request '" + metadataIdentificationString + "' for this provider");

		// Just rebuild on demand. We always do this as we expect MetadataService to cache on our behalf

		// Read the file, if it is available
		if (!fileManager.exists(pom)) {
			return null;
		}
		InputStream inputStream = fileManager.getInputStream(pom);

		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(inputStream);
		} catch (Exception ex) {
			throw new IllegalStateException("Could not open POM '" + pom + "'", ex);
		}

		Element rootElement = (Element) document.getFirstChild();

		// Obtain project name
		Element artifactIdElement = XmlUtils.findFirstElement("/project/artifactId", rootElement);
		String artifactId = artifactIdElement.getTextContent();
		Assert.hasText(artifactId, "Project name could not be determined from POM '" + pom + "'");
		String projectName = artifactId;

		// Obtain top level package
		Element groupIdElement = XmlUtils.findFirstElement("/project/groupId", rootElement);
		String topLevelPackageString = groupIdElement.getTextContent();
		Assert.hasText(topLevelPackageString, "Top level package name could not be determined from POM '" + pom + "'");
		Assert.isTrue(!topLevelPackageString.endsWith("."), "Top level package name cannot end with a period (was '" + topLevelPackageString + "')");
		JavaPackage topLevelPackage = new JavaPackage(topLevelPackageString);

		// Build dependencies list
		Set<Dependency> dependencies = new HashSet<Dependency>();

		for (Element dependency : XmlUtils.findElements("/project/dependencies/dependency", rootElement)) {
			Dependency d = new Dependency(dependency);
			dependencies.add(d);
		}

		// Build build plugin dependencies list
		Set<Dependency> buildPluginDependencies = new HashSet<Dependency>();

		for (Element plugin : XmlUtils.findElements("/project/build/plugins/plugin", rootElement)) {
			Dependency d = new Dependency(plugin);
			buildPluginDependencies.add(d);
		}

		// Build repositories list
		Set<Repository> repositories = new HashSet<Repository>();
		for (Element repo : XmlUtils.findElements("/project/repositories/repository", rootElement)) {
			repositories.add(new Repository(repo));
		}

		return new ProjectMetadata(topLevelPackage, projectName, dependencies, buildPluginDependencies, repositories, pathResolver);
	}

	public String getProvidesType() {
		return PROVIDES_TYPE;
	}

	public void addDependency(Dependency dependency) {
		Assert.notNull(dependency, "Dependency to add required");
		ProjectMetadata md = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(md, "Project metadata is not yet available, so dependency addition is unavailable");
		if (md.isDependencyRegistered(dependency)) {
			return;
		}

		MutableFile mutableFile = fileManager.updateFile(pom);

		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(mutableFile.getInputStream());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not open POM '" + pom + "'", ex);
		}

		Element rootElement = (Element) document.getFirstChild();
		Element dependencies = XmlUtils.findFirstElement("/project/dependencies", rootElement);
		Assert.notNull(dependencies, "Dependencies unable to be found");

		Element depElement = document.createElement("dependency");
		Element groupId = document.createElement("groupId");
		Element artifactId = document.createElement("artifactId");
		Element version = document.createElement("version");

		groupId.setTextContent(dependency.getGroupId().getFullyQualifiedPackageName());
		artifactId.setTextContent(dependency.getArtifactId().getSymbolName());
		version.setTextContent(dependency.getVersionId());

		depElement.appendChild(groupId);
		depElement.appendChild(artifactId);
		depElement.appendChild(version);
		
		if (dependency.getType() != null) {
			Element type = document.createElement("type");
			type.setTextContent(dependency.getType().toString().toLowerCase());
			if (!DependencyType.JAR.equals(dependency.getType())) {
				// Keep the XML short, we don't need "JAR" given it's the default
				depElement.appendChild(type);
			}
		}
		
		if (dependency.getScope() != null) {
			Element scope = document.createElement("scope");
			scope.setTextContent(dependency.getScope().toString().toLowerCase());
			if (!DependencyScope.COMPILE.equals(dependency.getScope())) {
				// Keep the XML short, we don't need "compile" given it's the default
				depElement.appendChild(scope);
			}
		}

		// Add exclusions if they are defined
		List<Dependency> exclusions = dependency.getExclusions();
		if (exclusions.size() > 0) {
			Element exclusionsElement = document.createElement("exclusions");
			for (Dependency exclusion : exclusions) {
				Element exclusionElement = document.createElement("exclusion");

				Element exclusionGroupId = document.createElement("groupId");
				exclusionGroupId.setTextContent(exclusion.getGroupId().getFullyQualifiedPackageName());
				exclusionElement.appendChild(exclusionGroupId);

				Element exclusionArtifactId = document.createElement("artifactId");
				exclusionArtifactId.setTextContent(exclusion.getArtifactId().getSymbolName());
				exclusionElement.appendChild(exclusionArtifactId);

				exclusionsElement.appendChild(exclusionElement);
			}
			depElement.appendChild(exclusionsElement);
		}

		dependencies.appendChild(depElement);

		XmlUtils.writeXml(mutableFile.getOutputStream(), document);
	}

	public void removeDependency(Dependency dependency) {
		removeDependency(dependency, "/project/dependencies", "/project/dependencies/dependency");
	}

	public void addBuildPluginDependency(Dependency dependency, Execution... executions) {
		Assert.notNull(dependency, "Dependency to add required");
		ProjectMetadata md = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(md, "Project metadata is not yet available, so dependency addition is unavailable");
		if (md.isDependencyRegistered(dependency)) {
			return;
		}

		MutableFile mutableFile = fileManager.updateFile(pom);

		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(mutableFile.getInputStream());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not open POM '" + pom + "'", ex);
		}

		Element rootElement = (Element) document.getFirstChild();
		Element plugins = XmlUtils.findFirstElement("/project/build/plugins", rootElement);
		Assert.notNull(plugins, "Plugins unable to be found");

		Element pluginElement = document.createElement("plugin");
		Element groupId = document.createElement("groupId");
		Element artifactId = document.createElement("artifactId");
		Element version = document.createElement("version");

		groupId.setTextContent(dependency.getGroupId().getFullyQualifiedPackageName());
		artifactId.setTextContent(dependency.getArtifactId().getSymbolName());
		version.setTextContent(dependency.getVersionId());

		pluginElement.appendChild(groupId);
		pluginElement.appendChild(artifactId);
		pluginElement.appendChild(version);

		// Add executions if they are defined
		if (executions.length > 0) {
			Element executionsElement = document.createElement("executions");
			for (Execution execution : executions) {
				Element executionElement = document.createElement("execution");
				Element executionId = document.createElement("id");
				executionId.setTextContent(execution.getId());
				Element goalsElement = document.createElement("goals");
				for (String goal : execution.getGoals()) {
					Element goalElement = document.createElement("goal");
					goalElement.setTextContent(goal);
					goalsElement.appendChild(goalElement);
				}
				executionElement.appendChild(executionId);
				executionElement.appendChild(goalsElement);
				executionsElement.appendChild(executionElement);
			}
			pluginElement.appendChild(executionsElement);
		}

		plugins.appendChild(pluginElement);

		XmlUtils.writeXml(mutableFile.getOutputStream(), document);
	}

	public void removeBuildPluginDependency(Dependency dependency) {
		removeDependency(dependency, "/project/build/plugins", "/project/build/plugins/plugin");
	}

	public void updateProjectType(ProjectType projectType) {
		Assert.notNull(projectType, "Project type required");
		ProjectMetadata md = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(md, "Project metadata is not yet available, so dependency addition is unavailable");

		MutableFile mutableFile = fileManager.updateFile(pom);

		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(mutableFile.getInputStream());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not open POM '" + pom + "'", ex);
		}

		Element packaging = XmlUtils.findFirstElement("/project/packaging", document.getDocumentElement());

		if (packaging == null) {
			packaging = document.createElement("packaging");
			document.getDocumentElement().appendChild(packaging);
		} else if (packaging.getTextContent().equals(projectType.getType())) {
			return;
		}

		packaging.setTextContent(projectType.getType());

		XmlUtils.writeXml(mutableFile.getOutputStream(), document);
	}

	public void addRepository(Repository repository) {
		Assert.notNull(repository, "Repository to add required");
		ProjectMetadata md = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(md, "Project metadata is not yet available, so dependency addition is unavailable");
		if (md.isRepositoryRegistered(repository)) {
			return;
		}

		MutableFile mutableFile = fileManager.updateFile(pom);

		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(mutableFile.getInputStream());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not open POM '" + pom + "'", ex);
		}

		Element repositories = XmlUtils.findFirstElement("/project/repositories", document.getDocumentElement());
		if (null == repositories) {
			repositories = document.createElement("repositories");
		}
		
		Element repositoryE = new XmlElementBuilder("repository", document).addChild(new XmlElementBuilder("id", document).setText(repository.getId()).build()).addChild(new XmlElementBuilder("url", document).setText(repository.getUrl()).build()).build();
		if (repository.getName() != null) {
			repositoryE.appendChild(new XmlElementBuilder("name", document).setText(repository.getName()).build());
		}
		repositories.appendChild(repositoryE);
		
		XmlUtils.writeXml(mutableFile.getOutputStream(), document);
	}

	public void removeRepository(Repository repository) {
		Assert.notNull(repository, "Repository to remove required");
		ProjectMetadata md = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(md, "Project metadata is not yet available, so dependency addition is unavailable");
		if (!md.isRepositoryRegistered(repository)) {
			return;
		}

		MutableFile mutableFile = fileManager.updateFile(pom);

		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(mutableFile.getInputStream());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not open POM '" + pom + "'", ex);
		}

		for (Element candidate : XmlUtils.findElements("/project/repositories/repository", document.getDocumentElement())) {
			if (repository.equals(new Repository(candidate))) {
				// Found it
				candidate.getParentNode().removeChild(candidate);
				// We will not break the loop (even though we could theoretically), just in case it was declared in the POM more than once
			}
		}

		XmlUtils.writeXml(mutableFile.getOutputStream(), document);
	}

	public void onFileEvent(FileEvent fileEvent) {
		Assert.notNull(fileEvent, "File event required");

		if (fileEvent.getFileDetails().getCanonicalPath().equals(pom)) {
			// Something happened to the POM

			// Don't notify if we're shutting down
			if (fileEvent.getOperation() == FileOperation.MONITORING_FINISH) {
				return;
			}

			// Otherwise let everyone know something has happened of interest, plus evict any cached entries from the MetadataService
			metadataService.evict(ProjectMetadata.getProjectIdentifier());
			metadataDependencyRegistry.notifyDownstream(ProjectMetadata.getProjectIdentifier());
		}
	}

	// Remove an element identified by dependency, whenever it occurs at path
	private void removeDependency(Dependency dependency, String containingPath, String path) {
		Assert.notNull(dependency, "Dependency to remove required");
		ProjectMetadata md = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(md, "Project metadata is not yet available, so dependency addition is unavailable");
		if (!md.isDependencyRegistered(dependency)) {
			return;
		}

		MutableFile mutableFile = fileManager.updateFile(pom);

		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(mutableFile.getInputStream());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not open POM '" + pom + "'", ex);
		}

		Element rootElement = (Element) document.getFirstChild();
		Element dependencies = XmlUtils.findFirstElement(containingPath, rootElement);

		for (Element candidate : XmlUtils.findElements(path, rootElement)) {
			if (dependency.equals(new Dependency(candidate))) {
				// Found it
				dependencies.removeChild(candidate);
				// We will not break the loop (even though we could theoretically), just in case it was declared in the POM more than once
			}
		}

		XmlUtils.writeXml(mutableFile.getOutputStream(), document);
	}
}
