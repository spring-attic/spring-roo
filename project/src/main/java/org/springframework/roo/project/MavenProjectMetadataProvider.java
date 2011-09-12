package org.springframework.roo.project;

import static org.springframework.roo.support.util.AnsiEscapeCode.FG_CYAN;
import static org.springframework.roo.support.util.AnsiEscapeCode.decorate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.springframework.roo.support.util.CollectionUtils;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.StringUtils;
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
	
	// Constants
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(MetadataIdentificationUtils.getMetadataClass(ProjectMetadata.getProjectIdentifier()));

	private static final String ADDED = "added";
	private static final String CHANGED = "changed";
	private static final String REMOVED = "removed";
	private static final String UPDATED = "updated";

	/**
	 * Highlights the given text
	 * 
	 * @param text the text to highlight (can be blank)
	 * @return the highlighted text
	 */
	private static String highlight(final String text) {
		return decorate(text, FG_CYAN);
	}
	
	// Fields
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

	public void addDependencies(final Collection<? extends Dependency> dependencies) {
		if (CollectionUtils.isEmpty(dependencies)) {
			return;
		}
		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so dependency addition is unavailable");
		if (projectMetadata.isAllDependenciesRegistered(dependencies)) {
			// No need to spend time parsing pom.xml
			return;
		}

		final Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		final Element dependenciesElement = DomUtils.createChildIfNotExists("dependencies", document.getDocumentElement(), document);
		final List<Element> existingDependencyElements = XmlUtils.findElements("dependency", dependenciesElement);

		final List<String> newDependencies = new ArrayList<String>();
		final List<String> removedDependencies = new ArrayList<String>();
		for (final Dependency newDependency : dependencies) {
			if (newDependency != null && !projectMetadata.isDependencyRegistered(newDependency)) {
				// Look for any existing instances of this dependency
				boolean inserted = false;
				for (final Element existingDependencyElement : existingDependencyElements) {
					final Dependency existingDependency = new Dependency(existingDependencyElement);
					if (existingDependency.hasSameCoordinates(newDependency)) {
						// It's the same artifact, but might have a different version, exclusions, etc.
						if (!inserted) {
							// We haven't added the new one yet; do so now
							dependenciesElement.insertBefore(newDependency.getElement(document), existingDependencyElement);
							inserted = true;
							if (!newDependency.getVersion().equals(existingDependency.getVersion())) {
								// It's a genuine version change => mention the old and new versions in the message
								newDependencies.add(newDependency.getSimpleDescription());
								removedDependencies.add(existingDependency.getSimpleDescription());
							}
						}
						// Either way, we remove the previous one in case it was different in any way
						dependenciesElement.removeChild(existingDependencyElement);
					}
					// Keep looping in case it's present more than once
				}
				if (!inserted) {
					// We didn't encounter any existing dependencies with the same coordinates; add it now
					dependenciesElement.appendChild(newDependency.getElement(document));
					newDependencies.add(newDependency.getSimpleDescription());
				}
			}
		}
		if (!newDependencies.isEmpty()) {
			final String addMessage = getDescriptionOfChange(ADDED, newDependencies, "dependency", "dependencies");
			final String removeMessage = getDescriptionOfChange(REMOVED, removedDependencies, "dependency", "dependencies");
			final String message = StringUtils.hasText(removeMessage) ? addMessage + "; " + removeMessage : addMessage;
			fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), message, false);
		}
	}

	public void addDependency(final Dependency dependency) {
		addDependencies(Collections.singletonList(dependency));
	}

	public void removeDependencies(final Collection<? extends Dependency> dependencies) {
		if (CollectionUtils.isEmpty(dependencies)) {
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
		if (dependenciesElement == null) {
			return;
		}

		final List<Element> existingDependencyElements = XmlUtils.findElements("dependency", dependenciesElement);
		final List<String> removedDependencies = new ArrayList<String>();
		final Set<Element> toRemove = new HashSet<Element>();
		for (final Dependency dependency : dependencies) {
			if (projectMetadata.isDependencyRegistered(dependency)) {
				for (final Element candidate : existingDependencyElements) {
					Dependency candidateDependency = new Dependency(candidate);
					if (candidateDependency.equals(dependency)) {
						// The identifying coordinates match; remove this element
						toRemove.add(candidate);
						removedDependencies.add(dependency.getSimpleDescription());
					}
					// Keep looping in case it's in the POM more than once
				}
			}
		}
		for (Element dependency : toRemove) {
			dependenciesElement.removeChild(dependency);
		}
		if (removedDependencies.isEmpty()) {
			return;
		}
		DomUtils.removeTextNodes(dependenciesElement);
		final String message = getDescriptionOfChange(REMOVED, removedDependencies, "dependency", "dependencies");
		
		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), message, false);
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
		
		final Element scopeElement = XmlUtils.findFirstElement("scope", dependencyElement);
		final String descriptionOfChange;
		if (scopeElement == null) {
			if (dependencyScope != null) {
				dependencyElement.appendChild(new XmlElementBuilder("scope", document).setText(dependencyScope.name().toLowerCase()).build());
				descriptionOfChange = highlight(ADDED + " scope") + " " + dependencyScope.name().toLowerCase() + " to dependency " + dependency.getSimpleDescription();
			} else {
				descriptionOfChange = null;
			}
		} else {
			if (dependencyScope != null) {
				scopeElement.setTextContent(dependencyScope.name().toLowerCase());
				descriptionOfChange = highlight(CHANGED + " scope") + " to " + dependencyScope.name().toLowerCase() + " in dependency " + dependency.getSimpleDescription();
			} else {
				dependencyElement.removeChild(scopeElement);
				descriptionOfChange = highlight(REMOVED + " scope") + " from dependency " + dependency.getSimpleDescription();
			}
		}

		if (descriptionOfChange != null) {
			fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
		}
	}

	public void addBuildPlugins(final Collection<? extends Plugin> plugins) {
		if (CollectionUtils.isEmpty(plugins)) {
			return;
		}
		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so plugin addition is unavailable");
		if (projectMetadata.isAllPluginsRegistered(plugins)) {
			return;
		}

		final Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		final Element root = document.getDocumentElement();
		final Element pluginsElement = DomUtils.createChildIfNotExists("/project/build/plugins", root, document);

		final List<String> newPlugins = new ArrayList<String>();
		for (final Plugin plugin : plugins) {
			if (plugin != null && !projectMetadata.isBuildPluginRegistered(plugin)) {
				pluginsElement.appendChild(plugin.getElement(document));
				newPlugins.add(plugin.getSimpleDescription());
			}
		}
		final String message = getDescriptionOfChange(ADDED, newPlugins, "plugin", "plugins");
		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), message, false);
	}

	/**
	 * Generates a message about the addition of the given items to the POM
	 * 
	 * @param action the past tense of the action that was performed
	 * @param items the items that were acted upon (required, can be empty)
	 * @param singular the singular of this type of item (required)
	 * @param plural the plural of this type of item (required)
	 * @return a non-<code>null</code> message
	 * @since 1.2.0
	 */
	private String getDescriptionOfChange(final String action, final Collection<String> items, final String singular, final String plural) {
		if (items.isEmpty()) {
			return "";
		}
		return highlight(action + " " + (items.size() == 1 ? singular : plural)) + " " + StringUtils.collectionToDelimitedString(items, ", "); 
	}

	public void addBuildPlugin(final Plugin plugin) {
		addBuildPlugins(Collections.singletonList(plugin));
	}

	public void removeBuildPlugins(final Collection<? extends Plugin> plugins) {
		if (CollectionUtils.isEmpty(plugins)) {
			return;
		}
		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so plugin removal is unavailable");

		final Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		final Element pluginsElement = XmlUtils.findFirstElement("/project/build/plugins", document.getDocumentElement());
		if (pluginsElement == null) {
			return;
		}

		final List<String> removedPlugins = new ArrayList<String>();
		for (final Plugin plugin : plugins) {
			// Can't filter the XPath on groupId, as it's optional in the POM for Apache-owned plugins
			for (final Element candidate : XmlUtils.findElements("plugin[artifactId = '" + plugin.getArtifactId() + "']", pluginsElement)) {
				if (Plugin.getGroupId(candidate).equals(plugin.getGroupId())) {
					// This element has the same groupId and artifactId as the plugin to be removed; remove it
					pluginsElement.removeChild(candidate);
					removedPlugins.add(plugin.getSimpleDescription());
					// Keep looping in case this plugin is in the POM more than once
				}
			}
		}
		if (removedPlugins.isEmpty()) {
			return;
		}
		DomUtils.removeTextNodes(pluginsElement);
		final String message = getDescriptionOfChange(REMOVED, removedPlugins, "plugin", "plugins");
		
		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), message, false);
	}

	public void removeBuildPlugin(final Plugin plugin) {
		removeBuildPlugins(Collections.singletonList(plugin));
	}
	
	public void addRepositories(final Collection<? extends Repository> repositories) {
		addRepositories(repositories, "repositories", "repository");
	}

	public void addRepository(final Repository repository) {
		addRepository(repository, "repositories", "repository");
	}

	public void removeRepository(final Repository repository) {
		removeRepository(repository, "/project/repositories/repository");
	}

	public void addPluginRepositories(final Collection<? extends Repository> repositories) {
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
		final Element packaging = DomUtils.createChildIfNotExists("packaging", document.getDocumentElement(), document);
		if (packaging.getTextContent().equals(projectType.getType())) {
			return;
		}

		packaging.setTextContent(projectType.getType());
		final String descriptionOfChange = highlight(UPDATED + " project type") + " to " + projectType.getType();

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	private void addRepositories(final Collection<? extends Repository> repositories, final String containingPath, final String path) {
		if (CollectionUtils.isEmpty(repositories)) {
			return;
		}
		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so repository addition is unavailable");
		if ("pluginRepository".equals(path)) {
			if (projectMetadata.isAllPluginRepositoriesRegistered(repositories)) {
				return;
			}
		} else if (projectMetadata.isAllRepositoriesRegistered(repositories)) {
			return;
		}

		final Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		final Element repositoriesElement = DomUtils.createChildIfNotExists(containingPath, document.getDocumentElement(), document);

		final List<String> addedRepositories = new ArrayList<String>();
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
			if (repository != null) {
				repositoriesElement.appendChild(repository.getElement(document, path));
				addedRepositories.add(repository.getUrl());
			}
		}
		final String message = getDescriptionOfChange(ADDED, addedRepositories, path, containingPath);
		
		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), message, false);
	}

	private void addRepository(final Repository repository, final String containingPath, final String path) {
		addRepositories(Collections.singletonList(repository), containingPath, path);
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
				descriptionOfChange = highlight(REMOVED + " repository") + " " + repository.getUrl();
				// We stay in the loop just in case it was in the POM more than once
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

		final String descriptionOfChange;
		final Element existing = XmlUtils.findFirstElement("/project/properties/" + property.getName(), root);
		if (existing == null) {
			// No existing property of this name; add it
			final Element properties = DomUtils.createChildIfNotExists("properties", document.getDocumentElement(), document);
			properties.appendChild(XmlUtils.createTextElement(document, property.getName(), property.getValue()));
			descriptionOfChange = highlight(ADDED + " property") + " '" + property.getName() + "' = '" + property.getValue() + "'";
		} else {
			// A property of this name exists; update it
			existing.setTextContent(property.getValue());
			descriptionOfChange = highlight(UPDATED + " property") + " '" + property.getName() + "' to '" + property.getValue() + "'";
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
				descriptionOfChange = highlight(REMOVED + " property") + " " + property.getName();
				// Stay in the loop just in case it was in the POM more than once
			}
		}

		DomUtils.removeTextNodes(propertiesElement);

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	public void addFilter(final Filter filter) {
		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so filter addition is unavailable");
		if (filter == null || projectMetadata.isFilterRegistered(filter)) {
			return;
		}

		final Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		final Element root = document.getDocumentElement();

		final Element buildElement = XmlUtils.findFirstElement("/project/build", root);
		final String descriptionOfChange;
		final Element existingFilter = XmlUtils.findFirstElement("filters/filter['" + filter.getValue() + "']", buildElement);
		if (existingFilter == null) {
			// No such filter; add it
			final Element filtersElement = DomUtils.createChildIfNotExists("filters", buildElement, document);
			filtersElement.appendChild(XmlUtils.createTextElement(document, "filter", filter.getValue()));
			descriptionOfChange = highlight(ADDED + " filter") + " '" + filter.getValue() + "'";
		} else {
			existingFilter.setTextContent(filter.getValue());
			descriptionOfChange = highlight(UPDATED + " filter") + " '" + filter.getValue() + "'";
		}

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	public void removeFilter(final Filter filter) {
		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata is not yet available, so filter removal is unavailable");
		if (filter == null || !projectMetadata.isFilterRegistered(filter)) {
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
				descriptionOfChange = highlight(REMOVED + " filter") + " '" + filter.getValue() + "'";
				// We will not break the loop (even though we could theoretically), just in case it was in the POM more than once
			}
		}

		final List<Element> filterElements = XmlUtils.findElements("filter", filtersElement);
		if (filterElements.isEmpty()) {
			filtersElement.getParentNode().removeChild(filtersElement);
		}

		DomUtils.removeTextNodes(root);

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
		final Element buildElement = XmlUtils.findFirstElement("/project/build", document.getDocumentElement());
		final Element resourcesElement = DomUtils.createChildIfNotExists("resources", buildElement, document);
		resourcesElement.appendChild(resource.getElement(document));
		final String descriptionOfChange = highlight(ADDED + " resource") + " " + resource.getSimpleDescription();

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
				descriptionOfChange = highlight(REMOVED + " resource") + " " + resource.getSimpleDescription();
				// Stay in the loop just in case it was in the POM more than once
			}
		}

		final List<Element> resourceElements = XmlUtils.findElements("resource", resourcesElement);
		if (resourceElements.isEmpty()) {
			resourcesElement.getParentNode().removeChild(resourcesElement);
		}

		DomUtils.removeTextNodes(root);

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	/**
	 * Removes an element identified by the given dependency, whenever it occurs at the given path
	 * 
	 * @param dependency
	 * @param containingPath
	 * @param path
	 */
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
				descriptionOfChange = highlight(REMOVED + " dependency") + " " + dependency.getSimpleDescription();
				// Stay in the loop, just in case it was in the POM more than once
			}
		}

		DomUtils.removeTextNodes(dependenciesElement);

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}
}
