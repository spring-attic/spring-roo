package org.springframework.roo.project;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

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
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.springframework.roo.uaa.UaaRegistrationService;
import org.springframework.uaa.client.UaaDetectedProducts;
import org.springframework.uaa.client.UaaDetectedProducts.ProductInfo;
import org.springframework.uaa.client.VersionHelper;
import org.springframework.uaa.client.protobuf.UaaClient.Product;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
@Component(immediate = true)
@Service
public class MavenProjectMetadataProvider implements ProjectMetadataProvider, FileEventListener {
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(MetadataIdentificationUtils.getMetadataClass(ProjectMetadata.getProjectIdentifier()));
	@Reference private PathResolver pathResolver;
	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference private Shell shell;
	@Reference private UaaRegistrationService uaaRegistrationService;
	@Reference private UaaDetectedProducts uaaDetectedProducts;
	private String pom;

	protected void activate(ComponentContext context) {
		this.pom = pathResolver.getIdentifier(Path.ROOT, "/pom.xml");
	}

	public MetadataItem get(String metadataIdentificationString) {
		Assert.isTrue(ProjectMetadata.getProjectIdentifier().equals(metadataIdentificationString), "Unexpected metadata request '" + metadataIdentificationString + "' for this provider");
		// Just rebuild on demand. We always do this as we expect MetadataService to cache on our behalf

		// Read the file, if it is available
		if (!fileManager.exists(pom)) {
			return null;
		}

		Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		Element root = document.getDocumentElement();

		// Obtain project name
		Element artifactIdElement = XmlUtils.findFirstElement("/project/artifactId", root);
		String artifactId = artifactIdElement.getTextContent();
		Assert.hasText(artifactId, "Project name could not be determined from POM '" + pom + "'");
		String projectName = artifactId;

		// Obtain top level package
		Element groupIdElement = XmlUtils.findFirstElement("/project/groupId", root);
		if (groupIdElement == null) {
			// Fall back to a group ID assumed to be the same as any possible <parent> (ROO-1193)
			groupIdElement = XmlUtils.findFirstElement("/project/parent/groupId", root);
		}

		Assert.notNull(groupIdElement, "Maven pom.xml must provide a <groupId> for the <project>");
		String topLevelPackageString = groupIdElement.getTextContent();
		Assert.hasText(topLevelPackageString, "Top level package name could not be determined from POM '" + pom + "'");
		Assert.isTrue(!topLevelPackageString.endsWith("."), "Top level package name cannot end with a period (was '" + topLevelPackageString + "')");
		JavaPackage topLevelPackage = new JavaPackage(topLevelPackageString);

		// Build dependencies list
		Set<Dependency> dependencies = new HashSet<Dependency>();
		for (Element dependency : XmlUtils.findElements("/project/dependencies/dependency", root)) {
			dependencies.add(new Dependency(dependency));
		}

		// Build plugins list
		Set<Plugin> buildPlugins = new HashSet<Plugin>();
		for (Element plugin : XmlUtils.findElements("/project/build/plugins/plugin", root)) {
			buildPlugins.add(new Plugin(plugin));
		}

		// Build repositories list
		Set<Repository> repositories = new HashSet<Repository>();
		for (Element repository : XmlUtils.findElements("/project/repositories/repository", root)) {
			repositories.add(new Repository(repository));
		}

		// Build plugin repositories list
		Set<Repository> pluginRepositories = new HashSet<Repository>();
		for (Element pluginRepository : XmlUtils.findElements("/project/pluginRepositories/pluginRepository", root)) {
			pluginRepositories.add(new Repository(pluginRepository));
		}

		// Build properties list
		Set<Property> pomProperties = new HashSet<Property>();
		for (Element prop : XmlUtils.findElements("/project/properties/*", root)) {
			pomProperties.add(new Property(prop));
		}

		// Filters list
		Set<Filter> filters = new HashSet<Filter>();
		for (Element filter : XmlUtils.findElements("/project/build/filters/filter", root)) {
			filters.add(new Filter(filter));
		}

		// Resources list
		Set<Resource> resources = new HashSet<Resource>();
		for (Element resource : XmlUtils.findElements("/project/build/resources/resource", root)) {
			resources.add(new Resource(resource));
		}

		// Update window title with project name
		shell.flash(Level.FINE, "Spring Roo: " + topLevelPackage, Shell.WINDOW_TITLE_SLOT);

		ProjectMetadata result = new ProjectMetadata(topLevelPackage, projectName, dependencies, buildPlugins, repositories, pluginRepositories, pomProperties, filters, resources, pathResolver);

		// Update UAA with the project name
		uaaRegistrationService.registerProject(UaaRegistrationService.SPRING_ROO, topLevelPackage.getFullyQualifiedPackageName());

		// Update UAA with the well-known Spring-related open source dependencies
		for (ProductInfo productInfo : uaaDetectedProducts.getDetectedProductInfos()) {
			if (productInfo.getProductName().equals(UaaRegistrationService.SPRING_ROO.getName())) {
				// No need to register with a less robust pom.xml-declared dependency metadata when we did it ourselves with a proper bundle version number lookup a moment ago...
				continue;
			}
			if (productInfo.getProductName().equals(UaaDetectedProducts.SPRING_UAA.getProductName())) {
				// No need to register Spring UAA as this happens automatically internal to UAA
				continue;
			}
			Dependency dependency = new Dependency(productInfo.getGroupId(), productInfo.getArtifactId(), "version_is_ignored_for_searching");
			Set<Dependency> dependenciesExcludingVersion = result.getDependenciesExcludingVersion(dependency);
			if (dependenciesExcludingVersion.size() > 0) {
				// This dependency was detected
				Dependency first = dependenciesExcludingVersion.iterator().next();
				// Convert the detected dependency into a Product as best we can
				String versionSequence = first.getVersion();
				// Version sequence given; see if it looks like a property
				if (versionSequence != null && versionSequence.startsWith("${") && versionSequence.endsWith("}")) {
					// Strip the ${ } from the version sequence
					String propertyName = versionSequence.replace("${", "").replace("}", "");
					Set<Property> prop = result.getPropertiesExcludingValue(new Property(propertyName));
					if (prop.size() > 0) {
						// Take the first one's value and treat that as the version sequence
						versionSequence = prop.iterator().next().getValue();
					}
				}
				// Handle there being no version sequence
				if (versionSequence == null || "".equals(versionSequence)) {
					versionSequence = "0.0.0.UNKNOWN";
				}
				Product product = VersionHelper.getProduct(productInfo.getProductName(), versionSequence);
				// Register the Spring Product with UAA
				uaaRegistrationService.registerProject(product, topLevelPackage.getFullyQualifiedPackageName());
			}
		}

		return result;
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
			metadataService.get(ProjectMetadata.getProjectIdentifier(), true);
			metadataDependencyRegistry.notifyDownstream(ProjectMetadata.getProjectIdentifier());
		}
	}

	public String getProvidesType() {
		return PROVIDES_TYPE;
	}

	public void addDependencies(List<Dependency> dependencies) {
		Assert.notNull(dependencies, "Dependencies to add required");
		if (dependencies.isEmpty()) {
			return;
		}
		ProjectMetadata projectMetadata = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so dependency addition is unavailable");
		if (projectMetadata.isAllDependenciesRegistered(dependencies)) {
			return;
		}

		Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		Element root = document.getDocumentElement();

		Element dependenciesElement = XmlUtils.findFirstElement("/project/dependencies", root);
		Assert.notNull(dependenciesElement, "Dependencies unable to be found");

		StringBuilder builder = new StringBuilder();
		for (Dependency dependency : dependencies) {
			if (projectMetadata.isDependencyRegistered(dependency)) {
				continue;
			}
			dependenciesElement.appendChild(createDependencyElement(dependency, document));
			builder.append(dependency.getSimpleDescription());
			builder.append(", ");
		}
		if (builder.lastIndexOf(",") != -1) {
			builder.delete(builder.lastIndexOf(","), builder.length());
		}
		builder.insert(0, "added " + (builder.indexOf(",") == -1 ? "dependency " : "dependencies "));

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), builder.toString(), false);
	}

	public void addDependency(Dependency dependency) {
		Assert.notNull(dependency, "Dependency to add required");
		ProjectMetadata projectMetadata = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so dependency addition is unavailable");
		if (projectMetadata.isDependencyRegistered(dependency)) {
			return;
		}

		Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		Element root = document.getDocumentElement();

		Element dependencies = XmlUtils.findFirstElement("/project/dependencies", root);
		Assert.notNull(dependencies, "dependencies element not found");
		dependencies.appendChild(createDependencyElement(dependency, document));
		String descriptionOfChange = "added dependency " + dependency.getSimpleDescription();

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	public void removeDependencies(List<Dependency> dependencies) {
		Assert.notNull(dependencies, "Dependencies to remove required");
		if (dependencies.isEmpty()) {
			return;
		}
		ProjectMetadata projectMetadata = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so dependency removal is unavailable");
		if (!projectMetadata.isAnyDependenciesRegistered(dependencies)) {
			return;
		}

		Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		Element root = document.getDocumentElement();

		Element dependenciesElement = XmlUtils.findFirstElement("/project/dependencies", root);
		Assert.notNull(dependencies, "dependencies element not found");

		int removeCount = 0;
		StringBuilder builder = new StringBuilder();
		for (Dependency dependency : dependencies) {
			for (Element candidate : XmlUtils.findElements("dependency[artifactId = '" + dependency.getArtifactId() + "' and version = '" + dependency.getVersion() + "']", dependenciesElement)) {
				dependenciesElement.removeChild(candidate);
				builder.append(dependency.getSimpleDescription());
				builder.append(", ");
				removeCount++;
			}
		}
		if (removeCount == 0) {
			return;
		}
		if (builder.lastIndexOf(",") != -1) {
			builder.delete(builder.lastIndexOf(","), builder.length());
		}
		builder.insert(0, "removed " + (builder.indexOf(",") == -1 ? "dependency " : "dependencies "));

		XmlUtils.removeTextNodes(dependenciesElement);

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), builder.toString(), false);
	}

	public void removeDependency(Dependency dependency) {
		removeDependency(dependency, "/project/dependencies", "/project/dependencies/dependency");
	}

	public void addBuildPlugins(List<Plugin> plugins) {
		Assert.notNull(plugins, "Plugins to add required");
		if (plugins.isEmpty()) {
			return;
		}
		ProjectMetadata projectMetadata = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so plugin addition is unavailable");
		if (projectMetadata.isAllPluginsRegistered(plugins)) {
			return;
		}

		Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		Element root = document.getDocumentElement();

		Element pluginsElement = XmlUtils.findFirstElement("/project/build/plugins", root);
		Assert.notNull(pluginsElement, "Plugins unable to be found");

		StringBuilder builder = new StringBuilder();
		for (Plugin plugin : plugins) {
			if (projectMetadata.isBuildPluginRegistered(plugin)) {
				continue;
			}
			pluginsElement.appendChild(getPluginElement(plugin, document));
			builder.append(plugin.getSimpleDescription());
			builder.append(", ");
		}
		if (builder.lastIndexOf(",") != -1) {
			builder.delete(builder.lastIndexOf(","), builder.length());
		}
		builder.insert(0, "added " + (builder.indexOf(",") == -1 ? "plugin " : "plugins "));

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), builder.toString(), false);
	}

	public void addBuildPlugin(Plugin plugin) {
		Assert.notNull(plugin, "Plugin to add required");
		ProjectMetadata projectMetadata = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so build plugin addition is unavailable");
		if (projectMetadata.isBuildPluginRegistered(plugin)) {
			return;
		}

		Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		Element root = document.getDocumentElement();

		Element pluginsElement = XmlUtils.findFirstElement("/project/build/plugins", root);
		Assert.notNull(pluginsElement, "plugins element not found");
		pluginsElement.appendChild(getPluginElement(plugin, document));
		String descriptionOfChange = "added plugin " + plugin.getArtifactId();

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	public void removeBuildPlugins(List<Plugin> plugins) {
		Assert.notNull(plugins, "Plugins to remove required");
		if (plugins.isEmpty()) {
			return;
		}
		ProjectMetadata projectMetadata = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so plugin removal is unavailable");
		if (!projectMetadata.isAnyPluginsRegistered(plugins)) {
			return;
		}

		Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		Element root = document.getDocumentElement();

		Element pluginsElement = XmlUtils.findFirstElement("/project/build/plugins", root);
		Assert.notNull(plugins, "plugins element not found");

		int removeCount = 0;
		StringBuilder builder = new StringBuilder();
		for (Plugin plugin : plugins) {
			for (Element candidate : XmlUtils.findElements("plugin[artifactId = '" + plugin.getArtifactId() + "' and version = '" + plugin.getVersion() + "']", pluginsElement)) {
				pluginsElement.removeChild(candidate);
				builder.append(plugin.getSimpleDescription());
				builder.append(", ");
				removeCount++;
			}
		}
		if (removeCount == 0) {
			return;
		}
		if (builder.lastIndexOf(",") != -1) {
			builder.delete(builder.lastIndexOf(","), builder.length());
		}
		builder.insert(0, "removed " + (builder.indexOf(",") == -1 ? "plugin " : "plugins "));

		XmlUtils.removeTextNodes(pluginsElement);

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), builder.toString(), false);
	}

	public void removeBuildPlugin(Plugin plugin) {
		removeBuildPlugin(plugin, "/project/build/plugins", "/project/build/plugins/plugin");
	}

	public void addRepositories(List<Repository> repositories) {
		addRepositories(repositories, "repositories", "repository");
	}

	public void addRepository(Repository repository) {
		addRepository(repository, "repositories", "repository");
	}

	public void removeRepository(Repository repository) {
		removeRepository(repository, "/project/repositories/repository");
	}

	public void addPluginRepositories(List<Repository> repositories) {
		addRepositories(repositories, "pluginRepositories", "pluginRepository");
	}

	public void addPluginRepository(Repository repository) {
		addRepository(repository, "pluginRepositories", "pluginRepository");
	}

	public void removePluginRepository(Repository repository) {
		removeRepository(repository, "/project/pluginRepositories/pluginRepository");
	}

	public void updateProjectType(ProjectType projectType) {
		Assert.notNull(projectType, "Project type required");
		ProjectMetadata projectMetadata = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so dependency addition is unavailable");

		Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		Element root = document.getDocumentElement();

		Element packaging = XmlUtils.findFirstElement("/project/packaging", root);
		if (packaging == null) {
			packaging = document.createElement("packaging");
			document.getDocumentElement().appendChild(packaging);
		} else if (packaging.getTextContent().equals(projectType.getType())) {
			return;
		}

		packaging.setTextContent(projectType.getType());
		String descriptionOfChange = "updated project type to " + projectType.getType();

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	private void addRepositories(List<Repository> repositories, String containingPath, String path) {
		Assert.notNull(repositories, "Repositories to add required");
		if (repositories.isEmpty()) {
			return;
		}
		ProjectMetadata projectMetadata = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so repository addition is unavailable");
		if (path.equals("pluginRepository")) {
			if (projectMetadata.isAllPluginRepositoriesRegistered(repositories)) {
				return;
			}
		} else {
			if (projectMetadata.isAllRepositoriesRegistered(repositories)) {
				return;
			}
		}

		Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		Element root = document.getDocumentElement();

		Element repositoriesElement = XmlUtils.findFirstElement("/project/" + containingPath, root);
		Assert.notNull(repositoriesElement, containingPath + " element not found");

		StringBuilder builder = new StringBuilder();
		for (Repository repository : repositories) {
			if (path.equals("pluginRepository")) {
				if (projectMetadata.isPluginRepositoryRegistered(repository)) {
					continue;
				}
			} else {
				if (projectMetadata.isRepositoryRegistered(repository)) {
					continue;
				}
			}

			repositoriesElement.appendChild(createRepositoryElement(document, repository, path));
			builder.append(repository.getUrl());
			builder.append(", ");
		}
		if (builder.lastIndexOf(",") != -1) {
			builder.delete(builder.lastIndexOf(","), builder.length());
		}
		builder.insert(0, "added " + (builder.indexOf(",") == -1 ? path : containingPath) + " ");

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), builder.toString(), false);
	}

	private Element createDependencyElement(Dependency dependency, Document document) {
		Element dependencyElement = document.createElement("dependency");
		Element groupIdElement = document.createElement("groupId");
		Element artifactIdElement = document.createElement("artifactId");
		Element versionElement = document.createElement("version");

		groupIdElement.setTextContent(dependency.getGroupId());
		artifactIdElement.setTextContent(dependency.getArtifactId());
		versionElement.setTextContent(dependency.getVersion());

		dependencyElement.appendChild(groupIdElement);
		dependencyElement.appendChild(artifactIdElement);
		dependencyElement.appendChild(versionElement);

		if (dependency.getType() != null) {
			Element type = document.createElement("type");
			type.setTextContent(dependency.getType().toString().toLowerCase());
			if (!DependencyType.JAR.equals(dependency.getType())) {
				// Keep the XML short, we don't need "JAR" given it's the default
				dependencyElement.appendChild(type);
			}
		}

		if (dependency.getScope() != null) {
			Element scope = document.createElement("scope");
			scope.setTextContent(dependency.getScope().toString().toLowerCase());
			if (!DependencyScope.COMPILE.equals(dependency.getScope())) {
				// Keep the XML short, we don't need "compile" given it's the default
				dependencyElement.appendChild(scope);
			}
			if (DependencyScope.SYSTEM.equals(dependency.getScope()) && dependency.getSystemPath() != null) {
				Element systemPath = document.createElement("systemPath");
				systemPath.setTextContent(dependency.getSystemPath());
				dependencyElement.appendChild(systemPath);
			}
		}

		// Add exclusions if they are defined
		List<Dependency> exclusions = dependency.getExclusions();
		if (exclusions.size() > 0) {
			Element exclusionsElement = document.createElement("exclusions");
			for (Dependency exclusion : exclusions) {
				Element exclusionElement = document.createElement("exclusion");

				Element exclusionGroupId = document.createElement("groupId");
				exclusionGroupId.setTextContent(exclusion.getGroupId());
				exclusionElement.appendChild(exclusionGroupId);

				Element exclusionArtifactId = document.createElement("artifactId");
				exclusionArtifactId.setTextContent(exclusion.getArtifactId());
				exclusionElement.appendChild(exclusionArtifactId);

				exclusionsElement.appendChild(exclusionElement);
			}
			dependencyElement.appendChild(exclusionsElement);
		}
		return dependencyElement;
	}

	private Element createRepositoryElement(Document document, Repository repository, String path) {
		Element repositoryElement = new XmlElementBuilder(path, document).addChild(new XmlElementBuilder("id", document).setText(repository.getId()).build()).addChild(new XmlElementBuilder("url", document).setText(repository.getUrl()).build()).build();
		if (repository.getName() != null) {
			repositoryElement.appendChild(new XmlElementBuilder("name", document).setText(repository.getName()).build());
		}
		if (repository.isEnableSnapshots()) {
			repositoryElement.appendChild(new XmlElementBuilder("snapshots", document).addChild(new XmlElementBuilder("enabled", document).setText("true").build()).build());
		}
		return repositoryElement;
	}

	private void addRepository(Repository repository, String containingPath, String path) {
		Assert.notNull(repository, "Repository required");
		ProjectMetadata projectMetadata = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so repository addition is unavailable");
		if (path.equals("pluginRepository")) {
			if (projectMetadata.isPluginRepositoryRegistered(repository)) {
				return;
			}
		} else {
			if (projectMetadata.isRepositoryRegistered(repository)) {
				return;
			}
		}

		Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		Element root = document.getDocumentElement();

		Element repositoriesElement = XmlUtils.findFirstElement("/project/" + containingPath, root);
		if (repositoriesElement == null) {
			repositoriesElement = document.createElement(containingPath);
		}
		repositoriesElement.appendChild(createRepositoryElement(document, repository, path));
		String descriptionOfChange = "added " + path + " " + repository.getId();

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	private void removeRepository(Repository repository, String path) {
		Assert.notNull(repository, "Repository required");
		ProjectMetadata projectMetadata = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so plugin repository removal is unavailable");
		if (path.equals("pluginRepository")) {
			if (!projectMetadata.isPluginRepositoryRegistered(repository)) {
				return;
			}
		} else {
			if (!projectMetadata.isRepositoryRegistered(repository)) {
				return;
			}
		}

		Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		Element root = document.getDocumentElement();

		String descriptionOfChange = "";
		for (Element candidate : XmlUtils.findElements(path, root)) {
			if (repository.equals(new Repository(candidate))) {
				candidate.getParentNode().removeChild(candidate);
				descriptionOfChange = "removed repository " + repository.getId();
				// We will not break the loop (even though we could theoretically), just in case it was declared in the POM more than once
			}
		}

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	private Element getPluginElement(Plugin plugin, Document document) {
		Element pluginElement = document.createElement("plugin");
		Element groupIdElement = document.createElement("groupId");
		Element artifactIdElement = document.createElement("artifactId");
		Element versionElement = document.createElement("version");

		groupIdElement.setTextContent(plugin.getGroupId());
		artifactIdElement.setTextContent(plugin.getArtifactId());
		versionElement.setTextContent(plugin.getVersion());

		pluginElement.appendChild(groupIdElement);
		pluginElement.appendChild(artifactIdElement);
		pluginElement.appendChild(versionElement);

		// Add configuration if not null
		if (plugin.getConfiguration() != null) {
			Node configuration = document.importNode(plugin.getConfiguration().getConfiguration(), true);
			pluginElement.appendChild(configuration);
		}

		// Add executions if they are defined
		List<Execution> executions = plugin.getExecutions();
		if (executions.size() > 0) {
			Element executionsElement = document.createElement("executions");
			for (Execution execution : executions) {
				Element executionElement = document.createElement("execution");

				String id = execution.getId();
				if (id != null && id.length() > 0) {
					Element executionId = document.createElement("id");
					executionId.setTextContent(id);
					executionElement.appendChild(executionId);
				}

				String phase = execution.getPhase();
				if (phase != null && phase.length() > 0) {
					Element executionPhase = document.createElement("phase");
					executionPhase.setTextContent(phase);
					executionElement.appendChild(executionPhase);
				}

				Element goalsElement = document.createElement("goals");
				for (String goal : execution.getGoals()) {
					Element goalElement = document.createElement("goal");
					goalElement.setTextContent(goal);
					goalsElement.appendChild(goalElement);
				}
				executionElement.appendChild(goalsElement);
				executionsElement.appendChild(executionElement);
			}
			pluginElement.appendChild(executionsElement);
		}

		// Add dependencies if they are defined
		List<Dependency> dependencies = plugin.getDependencies();
		if (dependencies.size() > 0) {
			Element dependenciesElement = document.createElement("dependencies");
			for (Dependency dependency : dependencies) {
				dependenciesElement.appendChild(createDependencyElement(dependency, document));
			}
			pluginElement.appendChild(dependenciesElement);
		}

		return pluginElement;
	}

	public void addProperty(Property property) {
		Assert.notNull(property, "Property to add required");
		ProjectMetadata projectMetadata = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so property addition is unavailable");
		if (projectMetadata.isPropertyRegistered(property)) {
			return;
		}

		Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		Element root = document.getDocumentElement();

		String descriptionOfChange = "";
		Element existing = XmlUtils.findFirstElement("/project/properties/" + property.getName(), root);
		if (existing != null) {
			existing.setTextContent(property.getValue());
			descriptionOfChange = "updated property '" + property.getName() + "' to '" + property.getValue() + "'";
		} else {
			Element properties = XmlUtils.findFirstElement("/project/properties", root);
			if (null == properties) {
				properties = document.createElement("properties");
			}
			Element propertyElement = new XmlElementBuilder(property.getName(), document).setText(property.getValue()).build();
			properties.appendChild(propertyElement);
		}

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	public void removeProperty(Property property) {
		Assert.notNull(property, "Property to remove required");
		ProjectMetadata projectMetadata = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so property removal is unavailable");
		if (!projectMetadata.isPropertyRegistered(property)) {
			return;
		}

		Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		Element root = document.getDocumentElement();

		String descriptionOfChange = "";
		Element propertiesElement = XmlUtils.findFirstElement("/project/properties", root);
		for (Element candidate : XmlUtils.findElements("/project/properties/*", document.getDocumentElement())) {
			if (property.equals(new Property(candidate))) {
				propertiesElement.removeChild(candidate);
				descriptionOfChange = "removed property " + property.getName();
				// We will not break the loop (even though we could theoretically), just in case it was declared in the POM more than once
			}
		}

		XmlUtils.removeTextNodes(propertiesElement);

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	public void addFilter(Filter filter) {
		Assert.notNull(filter, "Filter to add required");
		ProjectMetadata projectMetadata = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so filter addition is unavailable");
		if (projectMetadata.isFilterRegistered(filter)) {
			return;
		}

		Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		Element root = document.getDocumentElement();

		Element buildElement = XmlUtils.findFirstElement("/project/build", root);
		String descriptionOfChange = "";
		Element existing = XmlUtils.findFirstElement("filters/filter['" + filter.getValue() + "']", buildElement);
		if (existing != null) {
			existing.setTextContent(filter.getValue());
			descriptionOfChange = "updated filter '" + filter.getValue() + "'";
		} else {
			Element filtersElement = XmlUtils.findFirstElement("filters", buildElement);
			if (filtersElement == null) {
				filtersElement = document.createElement("filters");
			}

			Element filterElement = document.createElement("filter");
			filterElement.setTextContent(filter.getValue());
			filtersElement.appendChild(filterElement);
			buildElement.appendChild(filtersElement);
			descriptionOfChange = "added filter '" + filter.getValue() + "'";
		}

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	public void removeFilter(Filter filter) {
		Assert.notNull(filter, "Filter required");
		ProjectMetadata projectMetadata = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so filter removal is unavailable");
		if (!projectMetadata.isFilterRegistered(filter)) {
			return;
		}

		Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		Element root = document.getDocumentElement();

		Element filtersElement = XmlUtils.findFirstElement("/project/build/filters", root);
		if (filtersElement == null) {
			return;
		}

		String descriptionOfChange = "";
		for (Element candidate : XmlUtils.findElements("filter", filtersElement)) {
			if (filter.equals(new Filter(candidate))) {
				filtersElement.removeChild(candidate);
				descriptionOfChange = "Removed filter '" + filter.getValue() + "'";
				// We will not break the loop (even though we could theoretically), just in case it was declared in the POM more than once
			}
		}

		List<Element> filterElements = XmlUtils.findElements("filter", filtersElement);
		if (filterElements.isEmpty()) {
			filtersElement.getParentNode().removeChild(filtersElement);
		}

		XmlUtils.removeTextNodes(root);

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	public void addResource(Resource resource) {
		Assert.notNull(resource, "Resource to add required");
		ProjectMetadata projectMetadata = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so resource addition is unavailable");
		if (projectMetadata.isResourceRegistered(resource)) {
			return;
		}

		Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		Element root = document.getDocumentElement();

		Element buildElement = XmlUtils.findFirstElement("/project/build", root);
		Element resourcesElement = XmlUtils.findFirstElement("resources", buildElement);
		if (resourcesElement == null) {
			resourcesElement = document.createElement("resources");
		}

		Element resourceElement = document.createElement("resource");
		Element directoryElement = document.createElement("directory");
		directoryElement.setTextContent(resource.getDirectory().getName());
		resourceElement.appendChild(directoryElement);

		if (resource.getFiltering() != null) {
			Element filtering = document.createElement("filtering");
			filtering.setTextContent(resource.getFiltering().toString());
			resourceElement.appendChild(filtering);
		}

		if (!resource.getIncludes().isEmpty()) {
			Element includes = XmlUtils.findFirstElement("includes", resourceElement);
			if (null == includes) {
				includes = document.createElement("includes");
			}
			for (String include : resource.getIncludes()) {
				Element includeElement = document.createElement("include");
				includeElement.setTextContent(include);
				includes.appendChild(includeElement);
			}
			resourceElement.appendChild(includes);
		}
		resourcesElement.appendChild(resourceElement);
		buildElement.appendChild(resourcesElement);
		String descriptionOfChange = "added resource with " + resource.getSimpleDescription();

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	public void removeResource(Resource resource) {
		Assert.notNull(resource, "Resource required");
		ProjectMetadata projectMetadata = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so resource removal is unavailable");
		if (!projectMetadata.isResourceRegistered(resource)) {
			return;
		}

		Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		Element root = document.getDocumentElement();

		Element resourcesElement = XmlUtils.findFirstElement("/project/build/resources", root);
		if (resourcesElement == null) {
			return;
		}
		String descriptionOfChange = "";
		for (Element candidate : XmlUtils.findElements("resource[directory = '" + resource.getDirectory().getName() + "']", resourcesElement)) {
			if (resource.equals(new Resource(candidate))) {
				resourcesElement.removeChild(candidate);
				descriptionOfChange = "Removed resource with " + resource.getSimpleDescription();
				// We will not break the loop (even though we could theoretically), just in case it was declared in the POM more than once
			}
		}

		List<Element> resourceElements = XmlUtils.findElements("resource", resourcesElement);
		if (resourceElements.isEmpty()) {
			resourcesElement.getParentNode().removeChild(resourcesElement);
		}

		XmlUtils.removeTextNodes(root);

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	// Remove an element identified by dependency, whenever it occurs at path
	private void removeDependency(Dependency dependency, String containingPath, String path) {
		Assert.notNull(dependency, "Dependency to remove required");
		ProjectMetadata projectMetadata = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so dependency removal is unavailable");
		if (!projectMetadata.isDependencyRegistered(dependency)) {
			return;
		}

		Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		Element root = document.getDocumentElement();

		String descriptionOfChange = "";
		Element dependenciesElement = XmlUtils.findFirstElement(containingPath, root);
		for (Element candidate : XmlUtils.findElements(path, root)) {
			if (dependency.equals(new Dependency(candidate))) {
				dependenciesElement.removeChild(candidate);
				descriptionOfChange = "removed dependency " + dependency.getSimpleDescription();
				// We will not break the loop (even though we could theoretically), just in case it was declared in the POM more than once
			}
		}

		XmlUtils.removeTextNodes(dependenciesElement);

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	// Remove an element identified by plugin, whenever it occurs at path
	private void removeBuildPlugin(Plugin plugin, String containingPath, String path) {
		Assert.notNull(plugin, "Plugin to remove required");
		ProjectMetadata projectMetadata = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so dependency addition is unavailable");
		if (!projectMetadata.isBuildPluginRegistered(plugin)) {
			return;
		}

		Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		Element root = document.getDocumentElement();

		String descriptionOfChange = "";
		Element pluginsElement = XmlUtils.findFirstElement(containingPath, root);
		for (Element candidate : XmlUtils.findElements(path, root)) {
			if (plugin.equals(new Plugin(candidate))) {
				pluginsElement.removeChild(candidate);
				descriptionOfChange = "removed plugin " + plugin.getArtifactId();
				// We will not break the loop (even though we could theoretically), just in case it was declared in the POM more than once
			}
		}

		XmlUtils.removeTextNodes(pluginsElement);

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}
}
