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

	protected void activate(final ComponentContext context) {
		this.pom = pathResolver.getIdentifier(Path.ROOT, "/pom.xml");
	}

	public MetadataItem get(final String metadataIdentificationString) {
		Assert.isTrue(ProjectMetadata.getProjectIdentifier().equals(metadataIdentificationString), "Unexpected metadata request '" + metadataIdentificationString + "' for this provider");
		// Just rebuild on demand. We always do this as we expect MetadataService to cache on our behalf

		// Read the file, if it is available
		if (!fileManager.exists(pom)) {
			return null;
		}

		final Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		final Element root = document.getDocumentElement();

		// Obtain project name
		final Element artifactIdElement = XmlUtils.findFirstElement("/project/artifactId", root);
		final String artifactId = artifactIdElement.getTextContent();
		Assert.hasText(artifactId, "Project name could not be determined from POM '" + pom + "'");
		final String projectName = artifactId;

		// Obtain top level package
		Element groupIdElement = XmlUtils.findFirstElement("/project/groupId", root);
		if (groupIdElement == null) {
			// Fall back to a group ID assumed to be the same as any possible <parent> (ROO-1193)
			groupIdElement = XmlUtils.findFirstElement("/project/parent/groupId", root);
		}

		Assert.notNull(groupIdElement, "Maven pom.xml must provide a <groupId> for the <project>");
		final String topLevelPackageString = groupIdElement.getTextContent();
		Assert.hasText(topLevelPackageString, "Top level package name could not be determined from POM '" + pom + "'");
		Assert.isTrue(!topLevelPackageString.endsWith("."), "Top level package name cannot end with a period (was '" + topLevelPackageString + "')");
		final JavaPackage topLevelPackage = new JavaPackage(topLevelPackageString);

		// Build dependencies list
		final Set<Dependency> dependencies = new HashSet<Dependency>();
		for (final Element dependencyElement : XmlUtils.findElements("/project/dependencies/dependency", root)) {
			dependencies.add(new Dependency(dependencyElement));
		}

		// Build plugins list
		final Set<Plugin> buildPlugins = new HashSet<Plugin>();
		for (final Element pluginElement : XmlUtils.findElements("/project/build/plugins/plugin", root)) {
			buildPlugins.add(new Plugin(pluginElement));
		}

		// Build repositories list
		final Set<Repository> repositories = new HashSet<Repository>();
		for (final Element repositoryElement : XmlUtils.findElements("/project/repositories/repository", root)) {
			repositories.add(new Repository(repositoryElement));
		}

		// Build plugin repositories list
		final Set<Repository> pluginRepositories = new HashSet<Repository>();
		for (final Element pluginRepositoryElement : XmlUtils.findElements("/project/pluginRepositories/pluginRepository", root)) {
			pluginRepositories.add(new Repository(pluginRepositoryElement));
		}

		// Build properties list
		final Set<Property> pomProperties = new HashSet<Property>();
		for (final Element propertyElement : XmlUtils.findElements("/project/properties/*", root)) {
			pomProperties.add(new Property(propertyElement));
		}

		// Filters list
		final Set<Filter> filters = new HashSet<Filter>();
		for (final Element filterElement : XmlUtils.findElements("/project/build/filters/filter", root)) {
			filters.add(new Filter(filterElement));
		}

		// Resources list
		final Set<Resource> resources = new HashSet<Resource>();
		for (final Element resourceElement : XmlUtils.findElements("/project/build/resources/resource", root)) {
			resources.add(new Resource(resourceElement));
		}

		// Update window title with project name
		shell.flash(Level.FINE, "Spring Roo: " + topLevelPackage, Shell.WINDOW_TITLE_SLOT);

		final ProjectMetadata result = new ProjectMetadata(topLevelPackage, projectName, dependencies, buildPlugins, repositories, pluginRepositories, pomProperties, filters, resources, pathResolver);

		// Update UAA with the project name
		uaaRegistrationService.registerProject(UaaRegistrationService.SPRING_ROO, topLevelPackage.getFullyQualifiedPackageName());

		// Update UAA with the well-known Spring-related open source dependencies
		for (final ProductInfo productInfo : uaaDetectedProducts.getDetectedProductInfos()) {
			if (productInfo.getProductName().equals(UaaRegistrationService.SPRING_ROO.getName())) {
				// No need to register with a less robust pom.xml-declared dependency metadata when we did it ourselves with a proper bundle version number lookup a moment ago...
				continue;
			}
			if (productInfo.getProductName().equals(UaaDetectedProducts.SPRING_UAA.getProductName())) {
				// No need to register Spring UAA as this happens automatically internal to UAA
				continue;
			}
			final Dependency dependency = new Dependency(productInfo.getGroupId(), productInfo.getArtifactId(), "version_is_ignored_for_searching");
			final Set<Dependency> dependenciesExcludingVersion = result.getDependenciesExcludingVersion(dependency);
			if (!dependenciesExcludingVersion.isEmpty()) {
				// This dependency was detected
				final Dependency first = dependenciesExcludingVersion.iterator().next();
				// Convert the detected dependency into a Product as best we can
				String versionSequence = first.getVersion();
				// Version sequence given; see if it looks like a property
				if (versionSequence != null && versionSequence.startsWith("${") && versionSequence.endsWith("}")) {
					// Strip the ${ } from the version sequence
					final String propertyName = versionSequence.replace("${", "").replace("}", "");
					final Set<Property> prop = result.getPropertiesExcludingValue(new Property(propertyName));
					if (!prop.isEmpty()) {
						// Take the first one's value and treat that as the version sequence
						versionSequence = prop.iterator().next().getValue();
					}
				}
				// Handle there being no version sequence
				if (versionSequence == null || "".equals(versionSequence)) {
					versionSequence = "0.0.0.UNKNOWN";
				}
				final Product product = VersionHelper.getProduct(productInfo.getProductName(), versionSequence);
				// Register the Spring Product with UAA
				uaaRegistrationService.registerProject(product, topLevelPackage.getFullyQualifiedPackageName());
			}
		}
		
		return result;
	}

	public void onFileEvent(final FileEvent fileEvent) {
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

	public void addDependencies(final List<Dependency> dependencies) {
		Assert.notNull(dependencies, "Dependencies to add required");
		if (dependencies.isEmpty()) {
			return;
		}
		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so dependency addition is unavailable");
		if (projectMetadata.isAllDependenciesRegistered(dependencies)) {
			return;
		}

		final Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		final Element root = document.getDocumentElement();

		final Element dependenciesElement = XmlUtils.findFirstElement("/project/dependencies", root);
		Assert.notNull(dependenciesElement, "Dependencies unable to be found");

		final StringBuilder builder = new StringBuilder();
		for (final Dependency dependency : dependencies) {
			if (projectMetadata.isDependencyRegistered(dependency)) {
				continue;
			}
			dependenciesElement.appendChild(dependency.getElement(document));
			builder.append(dependency.getSimpleDescription());
			builder.append(", ");
		}
		if (builder.lastIndexOf(",") != -1) {
			builder.delete(builder.lastIndexOf(","), builder.length());
		}
		builder.insert(0, "added " + (builder.indexOf(",") == -1 ? "dependency " : "dependencies "));

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), builder.toString(), false);
	}

	public void addDependency(final Dependency dependency) {
		Assert.notNull(dependency, "Dependency to add required");
		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so dependency addition is unavailable");
		if (projectMetadata.isDependencyRegistered(dependency)) {
			return;
		}

		final Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		final Element root = document.getDocumentElement();

		final Element dependencies = XmlUtils.findFirstElement("/project/dependencies", root);
		Assert.notNull(dependencies, "dependencies element not found");
		dependencies.appendChild(dependency.getElement(document));
		final String descriptionOfChange = "added dependency " + dependency.getSimpleDescription();

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	public void removeDependencies(final List<Dependency> dependencies) {
		Assert.notNull(dependencies, "Dependencies to remove required");
		if (dependencies.isEmpty()) {
			return;
		}
		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so dependency removal is unavailable");
		if (!projectMetadata.isAnyDependenciesRegistered(dependencies)) {
			return;
		}

		final Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		final Element root = document.getDocumentElement();

		final Element dependenciesElement = XmlUtils.findFirstElement("/project/dependencies", root);
		Assert.notNull(dependencies, "dependencies element not found");

		int removeCount = 0;
		final StringBuilder builder = new StringBuilder();
		for (final Dependency dependency : dependencies) {
			for (final Element candidate : XmlUtils.findElements("dependency[artifactId = '" + dependency.getArtifactId() + "' and version = '" + dependency.getVersion() + "']", dependenciesElement)) {
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

	public void removeDependency(final Dependency dependency) {
		removeDependency(dependency, "/project/dependencies", "/project/dependencies/dependency");
	}
	
	public void updateDependencyScope(final Dependency dependency, final DependencyScope dependencyScope) {
		Assert.notNull(dependency, "Dependency to update required");
		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so dependency updating is unavailable");
		if (!projectMetadata.isDependencyRegistered(dependency)) {
			return;
		}

		final Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		final Element root = document.getDocumentElement();
	
		final Element dependencyElement = XmlUtils.findFirstElement("/project/dependencies/dependency[groupId = '" + dependency.getGroupId() + "' and artifactId = '" + dependency.getArtifactId() + "' and version = '" + dependency.getVersion() + "']", root);
		if (dependencyElement == null) {
			return;
		}
		
		String descriptionOfChange = "";
		final Element scopeElement = XmlUtils.findFirstElement("scope", dependencyElement);
		if (scopeElement == null) {
			if (dependencyScope != null) {
				dependencyElement.appendChild(new XmlElementBuilder("scope", document).setText(dependencyScope.name().toLowerCase()).build());
				descriptionOfChange = "added <scope>" + dependencyScope.name().toLowerCase() + "</scope> to dependency " + dependency.getSimpleDescription();
			}
		} else {
			if (dependencyScope != null) {
				scopeElement.setTextContent(dependencyScope.name().toLowerCase());
				descriptionOfChange = "changed <scope> to " + dependencyScope.name().toLowerCase() + " in dependency " + dependency.getSimpleDescription();
			} else {
				dependencyElement.removeChild(scopeElement);
				descriptionOfChange = "removed <scope> from dependency " + dependency.getSimpleDescription();
			}
		}

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	public void addBuildPlugins(final List<Plugin> plugins) {
		Assert.notNull(plugins, "Plugins to add required");
		if (plugins.isEmpty()) {
			return;
		}
		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so plugin addition is unavailable");
		if (projectMetadata.isAllPluginsRegistered(plugins)) {
			return;
		}

		final Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		final Element root = document.getDocumentElement();

		final Element pluginsElement = XmlUtils.findFirstElement("/project/build/plugins", root);
		Assert.notNull(pluginsElement, "Plugins unable to be found");

		final StringBuilder builder = new StringBuilder();
		for (final Plugin plugin : plugins) {
			if (projectMetadata.isBuildPluginRegistered(plugin)) {
				continue;
			}
			pluginsElement.appendChild(plugin.getElement(document));
			builder.append(plugin.getSimpleDescription());
			builder.append(", ");
		}
		if (builder.lastIndexOf(",") != -1) {
			builder.delete(builder.lastIndexOf(","), builder.length());
		}
		builder.insert(0, "added " + (builder.indexOf(",") == -1 ? "plugin " : "plugins "));

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), builder.toString(), false);
	}

	public void addBuildPlugin(final Plugin plugin) {
		Assert.notNull(plugin, "Plugin to add required");
		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so build plugin addition is unavailable");
		if (projectMetadata.isBuildPluginRegistered(plugin)) {
			return;
		}

		final Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		final Element root = document.getDocumentElement();

		final Element pluginsElement = XmlUtils.findFirstElement("/project/build/plugins", root);
		Assert.notNull(pluginsElement, "plugins element not found");
		pluginsElement.appendChild(plugin.getElement(document));
		final String descriptionOfChange = "added plugin " + plugin.getArtifactId();

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	public void removeBuildPlugins(final List<Plugin> plugins) {
		Assert.notNull(plugins, "Plugins to remove required");
		if (plugins.isEmpty()) {
			return;
		}
		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so plugin removal is unavailable");
		if (!projectMetadata.isAnyPluginsRegistered(plugins)) {
			return;
		}

		final Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		final Element root = document.getDocumentElement();

		final Element pluginsElement = XmlUtils.findFirstElement("/project/build/plugins", root);
		Assert.notNull(plugins, "plugins element not found");

		int removeCount = 0;
		final StringBuilder builder = new StringBuilder();
		for (final Plugin plugin : plugins) {
			for (final Element candidate : XmlUtils.findElements("plugin[artifactId = '" + plugin.getArtifactId() + "' and version = '" + plugin.getVersion() + "']", pluginsElement)) {
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

	public void removeBuildPlugin(final Plugin plugin) {
		removeBuildPlugin(plugin, "/project/build/plugins", "/project/build/plugins/plugin");
	}

	public void addRepositories(final List<Repository> repositories) {
		addRepositories(repositories, "repositories", "repository");
	}

	public void addRepository(final Repository repository) {
		addRepository(repository, "repositories", "repository");
	}

	public void removeRepository(final Repository repository) {
		removeRepository(repository, "/project/repositories/repository");
	}

	public void addPluginRepositories(final List<Repository> repositories) {
		addRepositories(repositories, "pluginRepositories", "pluginRepository");
	}

	public void addPluginRepository(final Repository repository) {
		addRepository(repository, "pluginRepositories", "pluginRepository");
	}

	public void removePluginRepository(final Repository repository) {
		removeRepository(repository, "/project/pluginRepositories/pluginRepository");
	}

	public void updateProjectType(final ProjectType projectType) {
		Assert.notNull(projectType, "Project type required");
		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so dependency addition is unavailable");

		final Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		final Element root = document.getDocumentElement();

		Element packaging = XmlUtils.findFirstElement("/project/packaging", root);
		if (packaging == null) {
			packaging = document.createElement("packaging");
			document.getDocumentElement().appendChild(packaging);
		} else if (packaging.getTextContent().equals(projectType.getType())) {
			return;
		}

		packaging.setTextContent(projectType.getType());
		final String descriptionOfChange = "updated project type to " + projectType.getType();

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	private void addRepositories(final List<Repository> repositories, final String containingPath, final String path) {
		Assert.notNull(repositories, "Repositories to add required");
		if (repositories.isEmpty()) {
			return;
		}
		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so repository addition is unavailable");
		if ("pluginRepository".equals(path)) {
			if (projectMetadata.isAllPluginRepositoriesRegistered(repositories)) {
				return;
			}
		} else {
			if (projectMetadata.isAllRepositoriesRegistered(repositories)) {
				return;
			}
		}

		final Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		final Element root = document.getDocumentElement();

		final Element repositoriesElement = XmlUtils.findFirstElement("/project/" + containingPath, root);
		Assert.notNull(repositoriesElement, containingPath + " element not found");

		final StringBuilder builder = new StringBuilder();
		for (final Repository repository : repositories) {
			if ("pluginRepository".equals(path)) {
				if (projectMetadata.isPluginRepositoryRegistered(repository)) {
					continue;
				}
			} else {
				if (projectMetadata.isRepositoryRegistered(repository)) {
					continue;
				}
			}

			repositoriesElement.appendChild(repository.getElement(document, path));
			builder.append(repository.getUrl());
			builder.append(", ");
		}
		if (builder.lastIndexOf(",") != -1) {
			builder.delete(builder.lastIndexOf(","), builder.length());
		}
		builder.insert(0, "added " + (builder.indexOf(",") == -1 ? path : containingPath) + " ");

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), builder.toString(), false);
	}

	private void addRepository(final Repository repository, final String containingPath, final String path) {
		Assert.notNull(repository, "Repository required");
		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so repository addition is unavailable");
		if ("pluginRepository".equals(path)) {
			if (projectMetadata.isPluginRepositoryRegistered(repository)) {
				return;
			}
		} else {
			if (projectMetadata.isRepositoryRegistered(repository)) {
				return;
			}
		}

		final Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		final Element root = document.getDocumentElement();

		Element repositoriesElement = XmlUtils.findFirstElement("/project/" + containingPath, root);
		if (repositoriesElement == null) {
			repositoriesElement = document.createElement(containingPath);
		}
		repositoriesElement.appendChild(repository.getElement(document, path));
		final String descriptionOfChange = "added " + path + " " + repository.getId();

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	private void removeRepository(final Repository repository, final String path) {
		Assert.notNull(repository, "Repository required");
		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so plugin repository removal is unavailable");
		if ("pluginRepository".equals(path)) {
			if (!projectMetadata.isPluginRepositoryRegistered(repository)) {
				return;
			}
		} else {
			if (!projectMetadata.isRepositoryRegistered(repository)) {
				return;
			}
		}

		final Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		final Element root = document.getDocumentElement();

		String descriptionOfChange = "";
		for (final Element candidate : XmlUtils.findElements(path, root)) {
			if (repository.equals(new Repository(candidate))) {
				candidate.getParentNode().removeChild(candidate);
				descriptionOfChange = "removed repository " + repository.getId();
				// We will not break the loop (even though we could theoretically), just in case it was declared in the POM more than once
			}
		}

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	public void addProperty(final Property property) {
		Assert.notNull(property, "Property to add required");
		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so property addition is unavailable");
		if (projectMetadata.isPropertyRegistered(property)) {
			return;
		}

		final Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		final Element root = document.getDocumentElement();

		String descriptionOfChange = "";
		final Element existing = XmlUtils.findFirstElement("/project/properties/" + property.getName(), root);
		if (existing != null) {
			existing.setTextContent(property.getValue());
			descriptionOfChange = "updated property '" + property.getName() + "' to '" + property.getValue() + "'";
		} else {
			Element properties = XmlUtils.findFirstElement("/project/properties", root);
			if (null == properties) {
				properties = document.createElement("properties");
			}
			final Element propertyElement = new XmlElementBuilder(property.getName(), document).setText(property.getValue()).build();
			properties.appendChild(propertyElement);
		}

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	public void removeProperty(final Property property) {
		Assert.notNull(property, "Property to remove required");
		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so property removal is unavailable");
		if (!projectMetadata.isPropertyRegistered(property)) {
			return;
		}

		final Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		final Element root = document.getDocumentElement();

		String descriptionOfChange = "";
		final Element propertiesElement = XmlUtils.findFirstElement("/project/properties", root);
		for (final Element candidate : XmlUtils.findElements("/project/properties/*", document.getDocumentElement())) {
			if (property.equals(new Property(candidate))) {
				propertiesElement.removeChild(candidate);
				descriptionOfChange = "removed property " + property.getName();
				// We will not break the loop (even though we could theoretically), just in case it was declared in the POM more than once
			}
		}

		XmlUtils.removeTextNodes(propertiesElement);

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	public void addFilter(final Filter filter) {
		Assert.notNull(filter, "Filter to add required");
		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so filter addition is unavailable");
		if (projectMetadata.isFilterRegistered(filter)) {
			return;
		}

		final Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		final Element root = document.getDocumentElement();

		final Element buildElement = XmlUtils.findFirstElement("/project/build", root);
		String descriptionOfChange = "";
		final Element existing = XmlUtils.findFirstElement("filters/filter['" + filter.getValue() + "']", buildElement);
		if (existing != null) {
			existing.setTextContent(filter.getValue());
			descriptionOfChange = "updated filter '" + filter.getValue() + "'";
		} else {
			Element filtersElement = XmlUtils.findFirstElement("filters", buildElement);
			if (filtersElement == null) {
				filtersElement = document.createElement("filters");
			}

			final Element filterElement = document.createElement("filter");
			filterElement.setTextContent(filter.getValue());
			filtersElement.appendChild(filterElement);
			buildElement.appendChild(filtersElement);
			descriptionOfChange = "added filter '" + filter.getValue() + "'";
		}

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	public void removeFilter(final Filter filter) {
		Assert.notNull(filter, "Filter required");
		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so filter removal is unavailable");
		if (!projectMetadata.isFilterRegistered(filter)) {
			return;
		}

		final Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		final Element root = document.getDocumentElement();

		final Element filtersElement = XmlUtils.findFirstElement("/project/build/filters", root);
		if (filtersElement == null) {
			return;
		}

		String descriptionOfChange = "";
		for (final Element candidate : XmlUtils.findElements("filter", filtersElement)) {
			if (filter.equals(new Filter(candidate))) {
				filtersElement.removeChild(candidate);
				descriptionOfChange = "Removed filter '" + filter.getValue() + "'";
				// We will not break the loop (even though we could theoretically), just in case it was declared in the POM more than once
			}
		}

		final List<Element> filterElements = XmlUtils.findElements("filter", filtersElement);
		if (filterElements.isEmpty()) {
			filtersElement.getParentNode().removeChild(filtersElement);
		}

		XmlUtils.removeTextNodes(root);

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	public void addResource(final Resource resource) {
		Assert.notNull(resource, "Resource to add required");
		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so resource addition is unavailable");
		if (projectMetadata.isResourceRegistered(resource)) {
			return;
		}

		final Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		final Element root = document.getDocumentElement();

		final Element buildElement = XmlUtils.findFirstElement("/project/build", root);
		Element resourcesElement = XmlUtils.findFirstElement("resources", buildElement);
		if (resourcesElement == null) {
			resourcesElement = document.createElement("resources");
		}
		resourcesElement.appendChild(resource.getElement(document));
		buildElement.appendChild(resourcesElement);
		final String descriptionOfChange = "added resource with " + resource.getSimpleDescription();

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	public void removeResource(final Resource resource) {
		Assert.notNull(resource, "Resource required");
		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so resource removal is unavailable");
		if (!projectMetadata.isResourceRegistered(resource)) {
			return;
		}

		final Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		final Element root = document.getDocumentElement();

		final Element resourcesElement = XmlUtils.findFirstElement("/project/build/resources", root);
		if (resourcesElement == null) {
			return;
		}
		String descriptionOfChange = "";
		for (final Element candidate : XmlUtils.findElements("resource[directory = '" + resource.getDirectory().getName() + "']", resourcesElement)) {
			if (resource.equals(new Resource(candidate))) {
				resourcesElement.removeChild(candidate);
				descriptionOfChange = "Removed resource with " + resource.getSimpleDescription();
				// We will not break the loop (even though we could theoretically), just in case it was declared in the POM more than once
			}
		}

		final List<Element> resourceElements = XmlUtils.findElements("resource", resourcesElement);
		if (resourceElements.isEmpty()) {
			resourcesElement.getParentNode().removeChild(resourcesElement);
		}

		XmlUtils.removeTextNodes(root);

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	// Remove an element identified by dependency, whenever it occurs at path
	private void removeDependency(final Dependency dependency, final String containingPath, final String path) {
		Assert.notNull(dependency, "Dependency to remove required");
		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so dependency removal is unavailable");
		if (!projectMetadata.isDependencyRegistered(dependency)) {
			return;
		}

		final Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		final Element root = document.getDocumentElement();

		String descriptionOfChange = "";
		final Element dependenciesElement = XmlUtils.findFirstElement(containingPath, root);
		for (final Element candidate : XmlUtils.findElements(path, root)) {
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
	private void removeBuildPlugin(final Plugin plugin, final String containingPath, final String path) {
		Assert.notNull(plugin, "Plugin to remove required");
		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so dependency addition is unavailable");
		if (!projectMetadata.isBuildPluginRegistered(plugin)) {
			return;
		}

		final Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		final Element root = document.getDocumentElement();

		String descriptionOfChange = "";
		final Element pluginsElement = XmlUtils.findFirstElement(containingPath, root);
		for (final Element candidate : XmlUtils.findElements(path, root)) {
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
