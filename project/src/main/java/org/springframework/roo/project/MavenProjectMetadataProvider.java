package org.springframework.roo.project;

import java.io.InputStream;
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
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.springframework.roo.uaa.UaaRegistrationService;
import org.springframework.uaa.client.UaaDetectedProducts;
import org.springframework.uaa.client.VersionHelper;
import org.springframework.uaa.client.UaaDetectedProducts.ProductInfo;
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
		InputStream inputStream = fileManager.getInputStream(pom);

		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(inputStream);
		} catch (Exception ex) {
			throw new IllegalStateException("Could not open POM '" + pom + "'", ex);
		}

		Element root = (Element) document.getFirstChild();

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
		for (Element filter : XmlUtils.findElements("/project/build/filters/filter/*", root)) {
			filters.add(new Filter(filter));
		}

		// Resources list
		Set<Resource> resources = new HashSet<Resource>();
		for (Element resource : XmlUtils.findElements("/project/build/resources/resource/*", root)) {
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
				String versionSequence = first.getVersionId();
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
		ProjectMetadata md = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(md, "Project metadata is not yet available, so dependency addition is unavailable");
		if (md.isAllDependenciesRegistered(dependencies)) {
			return;
		}

		MutableFile mutableFile = fileManager.updateFile(pom);

		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(mutableFile.getInputStream());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not open POM '" + pom + "'", ex);
		}

		Element root = (Element) document.getFirstChild();
		Element dependenciesElement = XmlUtils.findFirstElement("/project/dependencies", root);
		Assert.notNull(dependenciesElement, "Dependencies unable to be found");

		StringBuilder builder = new StringBuilder();
		for (Dependency dependency : dependencies) {
			if (md.isDependencyRegistered(dependency)) {
				continue;
			}
			dependenciesElement.appendChild(createDependencyElement(dependency, document));
			builder.append(dependency.getSimpleDescription());
			builder.append(", ");
		}
		if (builder.lastIndexOf(",") != -1) {
			builder.delete(builder.lastIndexOf(","), builder.length());
		}
		builder.insert(0, "Added " + (builder.indexOf(",") == -1 ? "dependency " : "dependencies "));

		mutableFile.setDescriptionOfChange(builder.toString());

		XmlUtils.writeXml(mutableFile.getOutputStream(), document);
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

		Element root = (Element) document.getFirstChild();
		Element dependencies = XmlUtils.findFirstElement("/project/dependencies", root);
		Assert.notNull(dependencies, "dependencies element not found");

		dependencies.appendChild(createDependencyElement(dependency, document));

		mutableFile.setDescriptionOfChange("Added dependency " + dependency.getSimpleDescription());

		XmlUtils.writeXml(mutableFile.getOutputStream(), document);
	}

	private Element createDependencyElement(Dependency dependency, Document document) {
		Element depElement = document.createElement("dependency");
		Element groupId = document.createElement("groupId");
		Element artifactId = document.createElement("artifactId");
		Element version = document.createElement("version");

		groupId.setTextContent(dependency.getGroupId());
		artifactId.setTextContent(dependency.getArtifactId());
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
			if (DependencyScope.SYSTEM.equals(dependency.getScope()) && dependency.getSystemPath() != null) {
				Element systemPath = document.createElement("systemPath");
				systemPath.setTextContent(dependency.getSystemPath());
				depElement.appendChild(systemPath);
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
			depElement.appendChild(exclusionsElement);
		}
		return depElement;
	}

	public void removeDependency(Dependency dependency) {
		removeDependency(dependency, "/project/dependencies", "/project/dependencies/dependency");
	}

	public void addBuildPlugin(Plugin plugin) {
		Assert.notNull(plugin, "Plugin to add required");
		ProjectMetadata md = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(md, "Project metadata is not yet available, so build plugin addition is unavailable");
		if (md.isBuildPluginRegistered(plugin)) {
			return;
		}

		MutableFile mutableFile = fileManager.updateFile(pom);

		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(mutableFile.getInputStream());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not open POM '" + pom + "'", ex);
		}

		Element root = (Element) document.getFirstChild();
		Element plugins = XmlUtils.findFirstElement("/project/build/plugins", root);
		Assert.notNull(plugins, "Plugins unable to be found");

		Element pluginElement = document.createElement("plugin");
		Element groupId = document.createElement("groupId");
		Element artifactId = document.createElement("artifactId");
		Element version = document.createElement("version");

		groupId.setTextContent(plugin.getGroupId());
		artifactId.setTextContent(plugin.getArtifactId());
		version.setTextContent(plugin.getVersion());

		pluginElement.appendChild(groupId);
		pluginElement.appendChild(artifactId);
		pluginElement.appendChild(version);

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

		plugins.appendChild(pluginElement);

		mutableFile.setDescriptionOfChange("Added plugin " + plugin.getArtifactId());

		XmlUtils.writeXml(mutableFile.getOutputStream(), document);
	}

	public void removeBuildPlugin(Plugin plugin) {
		removeBuildPlugin(plugin, "/project/build/plugins", "/project/build/plugins/plugin");
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

		mutableFile.setDescriptionOfChange("Updated project type to " + projectType.getType());

		XmlUtils.writeXml(mutableFile.getOutputStream(), document);
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

	private void addRepositories(List<Repository> repositories, String containingPath, String path) {
		Assert.notNull(repositories, "Repositories to add required");
		ProjectMetadata md = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(md, "Project metadata is not yet available, so repository addition is unavailable");
		if (path.equals("pluginRepository")) {
			if (md.isAllPluginRepositoriesRegistered(repositories)) {
				return;
			}
		} else {
			if (md.isAllRepositoriesRegistered(repositories)) {
				return;
			}
		}

		MutableFile mutableFile = fileManager.updateFile(pom);

		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(mutableFile.getInputStream());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not open POM '" + pom + "'", ex);
		}

		Element root = (Element) document.getFirstChild();
		Element repositoriesElement = XmlUtils.findFirstElement("/project/" + containingPath, root);
		Assert.notNull(repositoriesElement, containingPath + " element not found");

		StringBuilder builder = new StringBuilder();
		for (Repository repository : repositories) {
			if (path.equals("pluginRepository")) {
				if (md.isPluginRepositoryRegistered(repository)) {
					continue;
				}
			} else {
				if (md.isRepositoryRegistered(repository)) {
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
		builder.insert(0, "Added " + (builder.indexOf(",") == -1 ? path : containingPath) + " ");

		mutableFile.setDescriptionOfChange(builder.toString());

		XmlUtils.writeXml(mutableFile.getOutputStream(), document);
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
		ProjectMetadata md = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(md, "Project metadata is not yet available, so repository addition is unavailable");
		if (path.equals("pluginRepository")) {
			if (md.isPluginRepositoryRegistered(repository)) {
				return;
			}
		} else {
			if (md.isRepositoryRegistered(repository)) {
				return;
			}
		}

		MutableFile mutableFile = fileManager.updateFile(pom);

		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(mutableFile.getInputStream());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not open POM '" + pom + "'", ex);
		}

		Element repositoriesElement = XmlUtils.findFirstElement("/project/" + containingPath, document.getDocumentElement());
		if (repositoriesElement == null) {
			repositoriesElement = document.createElement(containingPath);
		}
		repositoriesElement.appendChild(createRepositoryElement(document, repository, path));

		mutableFile.setDescriptionOfChange("Added " + path + " " + repository.getId());

		XmlUtils.writeXml(mutableFile.getOutputStream(), document);
	}

	private void removeRepository(Repository repository, String path) {
		Assert.notNull(repository, "Repository required");
		ProjectMetadata md = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(md, "Project metadata is not yet available, so plugin repository removal is unavailable");
		if (path.equals("pluginRepository")) {
			if (!md.isPluginRepositoryRegistered(repository)) {
				return;
			}
		} else {
			if (!md.isRepositoryRegistered(repository)) {
				return;
			}
		}

		MutableFile mutableFile = fileManager.updateFile(pom);

		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(mutableFile.getInputStream());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not open POM '" + pom + "'", ex);
		}

		for (Element candidate : XmlUtils.findElements(path, document.getDocumentElement())) {
			if (repository.equals(new Repository(candidate))) {
				// Found it
				candidate.getParentNode().removeChild(candidate);
				mutableFile.setDescriptionOfChange("Removed repository " + repository.getId());
				// We will not break the loop (even though we could theoretically), just in case it was declared in the POM more than once
			}
		}

		XmlUtils.writeXml(mutableFile.getOutputStream(), document);
	}

	public void addProperty(Property property) {
		Assert.notNull(property, "Property to add required");
		ProjectMetadata md = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(md, "Project metadata is not yet available, so property addition is unavailable");
		if (md.isPropertyRegistered(property)) {
			return;
		}

		MutableFile mutableFile = fileManager.updateFile(pom);

		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(mutableFile.getInputStream());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not open POM '" + pom + "'", ex);
		}

		Element existing = XmlUtils.findFirstElement("/project/properties/" + property.getName(), document.getDocumentElement());
		if (existing != null) {
			existing.setTextContent(property.getValue());
			mutableFile.setDescriptionOfChange("Updated property '" + property.getName() + "' to '" + property.getValue() + "'");
		} else {
			Element properties = XmlUtils.findFirstElement("/project/properties", document.getDocumentElement());
			if (null == properties) {
				properties = document.createElement("properties");
			}

			Element propertyElement = new XmlElementBuilder(property.getName(), document).setText(property.getValue()).build();
			properties.appendChild(propertyElement);
			mutableFile.setDescriptionOfChange("Added property '" + property.getName() + "' with value '" + property.getValue() + "'");
		}

		XmlUtils.writeXml(mutableFile.getOutputStream(), document);
	}

	public void removeProperty(Property property) {
		Assert.notNull(property, "Property to remove required");
		ProjectMetadata md = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(md, "Project metadata is not yet available, so property removal is unavailable");
		if (!md.isPropertyRegistered(property)) {
			return;
		}

		MutableFile mutableFile = fileManager.updateFile(pom);

		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(mutableFile.getInputStream());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not open POM '" + pom + "'", ex);
		}

		Element root = (Element) document.getFirstChild();
		Element properties = XmlUtils.findFirstElement("/project/properties", root);

		for (Element candidate : XmlUtils.findElements("/project/properties/*", document.getDocumentElement())) {
			if (property.equals(new Property(candidate))) {
				// Found it
				properties.removeChild(candidate);
				mutableFile.setDescriptionOfChange("Removed property " + property.getName());
				// We will not break the loop (even though we could theoretically), just in case it was declared in the POM more than once
			}
		}
		XmlUtils.removeTextNodes(properties);

		XmlUtils.writeXml(mutableFile.getOutputStream(), document);
	}

	public void addFilter(Filter filter) {
		Assert.notNull(filter, "Filter to add required");
		ProjectMetadata md = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(md, "Project metadata is not yet available, so filter addition is unavailable");
		if (md.isFilterRegistered(filter)) {
			return;
		}

		MutableFile mutableFile = fileManager.updateFile(pom);

		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(mutableFile.getInputStream());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not open POM '" + pom + "'", ex);
		}

		Element build = XmlUtils.findFirstElement("/project/build", document.getDocumentElement());

		Element existing = XmlUtils.findFirstElement("filters/filter['" + filter.getValue() + "']", build);
		if (existing != null) {
			existing.setTextContent(filter.getValue());
			mutableFile.setDescriptionOfChange("Updated filter '" + filter.getValue() + "'");
		} else {
			Element filtersElement = XmlUtils.findFirstElement("filters", build);
			if (null == filtersElement) {
				filtersElement = document.createElement("filters");
			}

			Element filterElement = document.createElement("filter");
			filterElement.setTextContent(filter.getValue());
			filtersElement.appendChild(filterElement);
			build.appendChild(filtersElement);

			mutableFile.setDescriptionOfChange("Added filter '" + filter.getValue() + "'");
		}

		XmlUtils.writeXml(mutableFile.getOutputStream(), document);
	}

	public void removeFilter(Filter filter) {
		Assert.notNull(filter, "Filter required");
		ProjectMetadata md = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(md, "Project metadata is not yet available, so filter removal is unavailable");
		if (!md.isFilterRegistered(filter)) {
			return;
		}

		MutableFile mutableFile = fileManager.updateFile(pom);

		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(mutableFile.getInputStream());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not open POM '" + pom + "'", ex);
		}

		for (Element candidate : XmlUtils.findElements("/project/build/filters/filter", document.getDocumentElement())) {
			if (filter.equals(new Filter(candidate))) {
				// Found it
				candidate.getParentNode().removeChild(candidate);
				mutableFile.setDescriptionOfChange("Removed filter '" + filter.getValue() + "'");
				// We will not break the loop (even though we could theoretically), just in case it was declared in the POM more than once
			}
		}

		XmlUtils.writeXml(mutableFile.getOutputStream(), document);
	}

	public void addResource(Resource resource) {
		Assert.notNull(resource, "Resource to add required");
		ProjectMetadata md = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(md, "Project metadata is not yet available, so resource addition is unavailable");
		if (md.isResourceRegistered(resource)) {
			return;
		}

		MutableFile mutableFile = fileManager.updateFile(pom);

		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(mutableFile.getInputStream());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not open POM '" + pom + "'", ex);
		}

		Element build = XmlUtils.findFirstElement("/project/build", document.getDocumentElement());

		Element resources = XmlUtils.findFirstElement("resources", build);
		if (null == resources) {
			resources = document.createElement("resources");
		}

		Element resourceElement = document.createElement("resource");

		Element directory = document.createElement("directory");
		directory.setTextContent(resource.getDirectory().getName());
		resourceElement.appendChild(directory);

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
		resources.appendChild(resourceElement);
		build.appendChild(resources);

		mutableFile.setDescriptionOfChange("Added resource");

		XmlUtils.writeXml(mutableFile.getOutputStream(), document);
	}

	public void removeResource(Resource resource) {
		Assert.notNull(resource, "Resource required");
		ProjectMetadata md = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(md, "Project metadata is not yet available, so resource removal is unavailable");
		if (!md.isResourceRegistered(resource)) {
			return;
		}

		MutableFile mutableFile = fileManager.updateFile(pom);

		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(mutableFile.getInputStream());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not open POM '" + pom + "'", ex);
		}

		for (Element candidate : XmlUtils.findElements("/project/build/resources/resource['" + resource.getDirectory() + "']", document.getDocumentElement())) {
			if (resource.equals(new Resource(candidate))) {
				// Found it
				candidate.getParentNode().removeChild(candidate);
				mutableFile.setDescriptionOfChange("Removed resource");
				// We will not break the loop (even though we could theoretically), just in case it was declared in the POM more than once
			}
		}

		XmlUtils.writeXml(mutableFile.getOutputStream(), document);
	}

	// Remove an element identified by dependency, whenever it occurs at path
	private void removeDependency(Dependency dependency, String containingPath, String path) {
		Assert.notNull(dependency, "Dependency to remove required");
		ProjectMetadata md = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(md, "Project metadata is not yet available, so dependency removal is unavailable");
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

		Element root = (Element) document.getFirstChild();
		Element dependencies = XmlUtils.findFirstElement(containingPath, root);

		for (Element candidate : XmlUtils.findElements(path, root)) {
			if (dependency.equals(new Dependency(candidate))) {
				// Found it
				dependencies.removeChild(candidate);
				mutableFile.setDescriptionOfChange("Removed dependency " + dependency.getSimpleDescription());
				// We will not break the loop (even though we could theoretically), just in case it was declared in the POM more than once
			}
		}
		XmlUtils.removeTextNodes(dependencies);

		XmlUtils.writeXml(mutableFile.getOutputStream(), document);
	}

	// Remove an element identified by plugin, whenever it occurs at path
	private void removeBuildPlugin(Plugin plugin, String containingPath, String path) {
		Assert.notNull(plugin, "Plugin to remove required");
		ProjectMetadata md = (ProjectMetadata) get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(md, "Project metadata is not yet available, so dependency addition is unavailable");
		if (!md.isBuildPluginRegistered(plugin)) {
			return;
		}

		MutableFile mutableFile = fileManager.updateFile(pom);

		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(mutableFile.getInputStream());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not open POM '" + pom + "'", ex);
		}

		Element root = (Element) document.getFirstChild();
		Element plugins = XmlUtils.findFirstElement(containingPath, root);

		for (Element candidate : XmlUtils.findElements(path, root)) {
			if (plugin.equals(new Plugin(candidate))) {
				// Found it
				plugins.removeChild(candidate);
				mutableFile.setDescriptionOfChange("Removed plugin " + plugin.getArtifactId());
				// We will not break the loop (even though we could theoretically), just in case it was declared in the POM more than once
			}
		}
		XmlUtils.removeTextNodes(plugins);

		XmlUtils.writeXml(mutableFile.getOutputStream(), document);
	}
}
