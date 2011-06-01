package org.springframework.roo.addon.gwt;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.web.mvc.controller.WebMvcOperations;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataNotificationListener;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Repository;
import org.springframework.roo.support.osgi.UrlFindingUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.WebXmlUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of {@link GwtOperations}.
 *
 * @author Ben Alex
 * @author Alan Stewart
 * @author Stefan Schmidt
 * @author Ray Cromwell
 * @author Amit Manjhi
 * @since 1.1
 */
@Component
@Service
public class GwtOperationsImpl implements GwtOperations, MetadataNotificationListener {
	@Reference private FileManager fileManager;
	@Reference private GwtTypeService gwtTypeService;
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference private MetadataService metadataService;
	@Reference private WebMvcOperations mvcOperations;
	@Reference private ProjectOperations projectOperations;
	@Reference private TypeLocationService typeLocationService;
	private ComponentContext context;
	private Boolean wasGaeEnabled = null;

	protected void activate(ComponentContext context) {
		this.context = context;
		metadataDependencyRegistry.addNotificationListener(this);
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
	}

	public boolean isSetupAvailable() {
		if (!projectOperations.isProjectAvailable()) {
			return false;
		}

		// Do not permit installation if they have a gwt package already in their project shared is allowed
		for (GwtPath path : GwtPath.values()) {
			if (path == GwtPath.MANAGED_REQUEST || path == GwtPath.SCAFFOLD || path == GwtPath.MANAGED || path == GwtPath.MANAGED_UI) {
				if (fileManager.exists(path.canonicalFileSystemPath(projectOperations.getProjectMetadata()))) {
					return false;
				}
			}
		}
		return true;
	}

	public void setup() {
		// Install web pieces if not already installed
		if (!fileManager.exists(projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/web.xml"))) {
			mvcOperations.installAllWebMvcArtifacts();
		}

		copyDirectoryContents();

		updateGaeHelper(projectOperations.getProjectMetadata().isGaeEnabled());

		// Add GWT natures and builder names to maven eclipse plugin
		updateEclipsePlugin();

		// Add outputDirectory to build element of pom
		updateBuildOutputDirectory();

		Element configuration = XmlUtils.getConfiguration(getClass());

		// Add POM repositories
		updateRepositories(configuration);

		// Add dependencies
		updateDependencies(configuration);

		// Update web.xml
		updateWebXml();

		// Update persistence.xml
		updatePersistenceXml();

		// Do a "get" for every .java file, thus ensuring the metadata is fired
		for (ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails : typeLocationService.getProjectJavaTypes(Path.SRC_MAIN_JAVA)) {
			metadataService.get(GwtMetadata.createIdentifier(classOrInterfaceTypeDetails.getName(), Path.SRC_MAIN_JAVA));
		}
	}

	public void notify(String upstreamDependency, String downstreamDependency) {
		if (!StringUtils.hasText(upstreamDependency) || !MetadataIdentificationUtils.isValid(upstreamDependency)) {
			return;
		}
		if (!upstreamDependency.equals(ProjectMetadata.getProjectIdentifier())) {
			return;
		}
		ProjectMetadata projectMetdata = projectOperations.getProjectMetadata();
		if (projectMetdata == null) {
			return;
		}
		boolean isGaeEnabled = projectMetdata.isGaeEnabled();
		boolean hasGaeStateChanged = wasGaeEnabled == null || isGaeEnabled != wasGaeEnabled;
		if (projectMetdata.isGwtEnabled() && hasGaeStateChanged) {
			wasGaeEnabled = isGaeEnabled;

			// Update ApplicationRequestFactory
			gwtTypeService.buildType(GwtType.APP_REQUEST_FACTORY);

			// Update the GaeHelper type
			updateGaeHelper(isGaeEnabled);

			// Ensure the gwt-maven-plugin appropriate to a GAE enabled or disabled environment is updated
			updateBuildPlugins(isGaeEnabled);

			//If there is a class that could possibly import from the appengine sdk, denoted here as having Gae in the type name, then we need to add the appengine-api-1.0-sdk dependency to the pom.xml file
			String rootPath = projectOperations.getPathResolver().getRoot(Path.ROOT);
			Set<FileDetails> files = fileManager.findMatchingAntPath(rootPath + "/**/*Gae*.java");
			if (!files.isEmpty()) {
				Element configuration = XmlUtils.getConfiguration(getClass());
				Element gaeDependency = XmlUtils.findFirstElement("/configuration/gae/dependencies/dependency", configuration);
				projectOperations.addDependency(new Dependency(gaeDependency));
			}

			// Copy across any missing files, only if GAE state has changed and is now enabled
			if (isGaeEnabled) {
				copyDirectoryContents();
			}
		}
	}
	
	private void updateEclipsePlugin() {
		String pom = projectOperations.getPathResolver().getIdentifier(Path.ROOT, "pom.xml");
		Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		Element root = document.getDocumentElement();

		// Add GWT buildCommand
		Element additionalBuildcommandsElement = XmlUtils.findFirstElement("/project/build/plugins/plugin[artifactId = 'maven-eclipse-plugin']/configuration/additionalBuildcommands", root);
		Assert.notNull(additionalBuildcommandsElement, "additionalBuildcommands element of the maven-eclipse-plugin required");
		String gwtBuildCommandName = "com.google.gwt.eclipse.core.gwtProjectValidator";
		Element gwtBuildCommandElement = XmlUtils.findFirstElement("buildCommand[name = '" + gwtBuildCommandName + "']", additionalBuildcommandsElement);
		if (gwtBuildCommandElement == null) {
			Element nameElement = document.createElement("name");
			nameElement.setTextContent(gwtBuildCommandName);
			gwtBuildCommandElement = document.createElement("buildCommand");
			gwtBuildCommandElement.appendChild(nameElement);
			additionalBuildcommandsElement.appendChild(gwtBuildCommandElement);
		}

		// Add GWT projectnature
		Element additionalProjectnaturesElement = XmlUtils.findFirstElement("/project/build/plugins/plugin[artifactId = 'maven-eclipse-plugin']/configuration/additionalProjectnatures", root);
		Assert.notNull(additionalProjectnaturesElement, "additionalProjectnatures element of the maven-eclipse-plugin required");
		String gwtProjectnatureName = "com.google.gwt.eclipse.core.gwtNature";
		Element gwtProjectnatureElement = XmlUtils.findFirstElement("projectnature[name = '" + gwtProjectnatureName + "']", additionalProjectnaturesElement);
		if (gwtProjectnatureElement == null) {
			gwtProjectnatureElement = new XmlElementBuilder("projectnature", document).setText(gwtProjectnatureName).build();
			additionalProjectnaturesElement.appendChild(gwtProjectnatureElement);
		}

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), false);
	}

	private void updateBuildOutputDirectory() {
		String pom = projectOperations.getPathResolver().getIdentifier(Path.ROOT, "pom.xml");
		Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		Element root = document.getDocumentElement();

		String outputDirectoryText = "${project.build.directory}/${project.build.finalName}/WEB-INF/classes";
		Element outputDirectoryElement = XmlUtils.findFirstElement("/project/build/outputDirectory", root);
		if (outputDirectoryElement != null) {
			if (!outputDirectoryElement.getTextContent().equals(outputDirectoryText)) {
				outputDirectoryElement.setTextContent(outputDirectoryText);
			}
		} else {
			outputDirectoryElement = new XmlElementBuilder("outputDirectory", document).setText(outputDirectoryText).build();
			Element buildElement = XmlUtils.findRequiredElement("/project/build", root);
			buildElement.appendChild(outputDirectoryElement);
		}

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), false);
	}

	private void updateRepositories(Element configuration) {
		List<Repository> repositories = new ArrayList<Repository>();

		List<Element> gwtRepositories = XmlUtils.findElements("/configuration/gwt/repositories/repository", configuration);
		for (Element repositoryElement : gwtRepositories) {
			repositories.add(new Repository(repositoryElement));
		}
		projectOperations.addRepositories(repositories);

		repositories.clear();
		List<Element> gwtPluginRepositories = XmlUtils.findElements("/configuration/gwt/pluginRepositories/pluginRepository", configuration);
		for (Element repositoryElement : gwtPluginRepositories) {
			repositories.add(new Repository(repositoryElement));
		}
		projectOperations.addPluginRepositories(repositories);
	}

	private void updateDependencies(Element configuration) {
		List<Dependency> dependencies = new ArrayList<Dependency>();
		List<Element> gwtDependencies = XmlUtils.findElements("/configuration/gwt/dependencies/dependency", configuration);
		for (Element dependencyElement : gwtDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}
		projectOperations.addDependencies(dependencies);
	}

	private void updateWebXml() {
		String webXmlpath = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml");
		Document webXml = XmlUtils.readXml(fileManager.getInputStream(webXmlpath));
		Element root = webXml.getDocumentElement();

		WebXmlUtils.addServlet("requestFactory", "com.google.gwt.requestfactory.server.RequestFactoryServlet", "/gwtRequest", null, webXml, null);
		if (projectOperations.getProjectMetadata().isGaeEnabled()) {
			WebXmlUtils.addFilter("GaeAuthFilter", GwtPath.SERVER_GAE.packageName(projectOperations.getProjectMetadata()) + ".GaeAuthFilter", "/gwtRequest/*", webXml, "This filter makes GAE authentication services visible to a RequestFactory client.");
			String displayName = "Redirect to the login page if needed before showing any html pages";
			WebXmlUtils.WebResourceCollection webResourceCollection = new WebXmlUtils.WebResourceCollection("Login required", null, Collections.singletonList("*.html"), new ArrayList<String>());
			ArrayList<String> roleNames = new ArrayList<String>();
			roleNames.add("*");
			String userDataConstraint = null;
			WebXmlUtils.addSecurityConstraint(displayName, Collections.singletonList(webResourceCollection), roleNames, userDataConstraint, webXml, null);
		} else {
			Element filter = XmlUtils.findFirstElement("/web-app/filter[filter-name = 'GaeAuthFilter']", root);
			if (filter != null) {
				filter.getParentNode().removeChild(filter);
			}
			Element filterMapping = XmlUtils.findFirstElement("/web-app/filter-mapping[filter-name = 'GaeAuthFilter']", root);
			if (filterMapping != null) {
				filterMapping.getParentNode().removeChild(filterMapping);
			}
			Element securityConstraint = XmlUtils.findFirstElement("security-constraint", root);
			if (securityConstraint != null) {
				securityConstraint.getParentNode().removeChild(securityConstraint);
			}
		}

		removeIfFound("/web-app/error-page", root);
		
		fileManager.createOrUpdateTextFileIfRequired(webXmlpath, XmlUtils.nodeToString(webXml), false);
	}

	private void updatePersistenceXml() {
		String persistencePath = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml");
		Document persistenceXml = XmlUtils.readXml(fileManager.getInputStream(persistencePath));
		Element root = persistenceXml.getDocumentElement();

		for (Element persistenceUnitElement : XmlUtils.findElements("persistence-unit", root)) {
			Element provider = XmlUtils.findFirstElement("provider", persistenceUnitElement);
			if (provider != null && "org.datanucleus.store.appengine.jpa.DatastorePersistenceProvider".equals(provider.getTextContent()) && !projectOperations.getProjectMetadata().isGaeEnabled()) {
				persistenceUnitElement.getParentNode().removeChild(persistenceUnitElement);
				fileManager.createOrUpdateTextFileIfRequired(persistencePath, XmlUtils.nodeToString(persistenceXml), false);
				break;
			}
		}
	}

	private void updateGaeHelper(boolean isGaeEnabled) {
		String sourceAntPath = "module/client/scaffold/gae/GaeHelper-template.java";
		String segmentPackage = "client.scaffold.gae";
		String targetDirectory = projectOperations.getProjectMetadata().getPathResolver().getIdentifier(Path.SRC_MAIN_JAVA, projectOperations.getProjectMetadata().getTopLevelPackage().getFullyQualifiedPackageName().replace('.', File.separatorChar) + File.separator + "client" + File.separator + "scaffold" + File.separator + "gae");
		updateFile(sourceAntPath, targetDirectory, segmentPackage, true, isGaeEnabled);
	}

	private void copyDirectoryContents() {
		for (GwtPath path : GwtPath.values()) {
			copyDirectoryContents(path);
		}
	}

	private void copyDirectoryContents(GwtPath gwtPath) {
		String sourceAntPath = gwtPath.getSourceAntPath();
		if (sourceAntPath.contains("gae") && !projectOperations.getProjectMetadata().isGaeEnabled()) {
			return;
		}
		String targetDirectory = gwtPath.canonicalFileSystemPath(projectOperations.getProjectMetadata());
		updateFile(sourceAntPath, targetDirectory, gwtPath.segmentPackage(), false, projectOperations.getProjectMetadata().isGaeEnabled());
	}

	private void updateFile(String sourceAntPath, String targetDirectory, String segmentPackage, boolean overwrite, boolean isGaeEnabled) {
		if (!targetDirectory.endsWith(File.separator)) {
			targetDirectory += File.separator;
		}
		if (!fileManager.exists(targetDirectory)) {
			fileManager.createDirectory(targetDirectory);
		}

		String path = TemplateUtils.getTemplatePath(getClass(), sourceAntPath);
		Set<URL> urls = UrlFindingUtils.findMatchingClasspathResources(context.getBundleContext(), path);
		Assert.notNull(urls, "Could not search bundles for resources for Ant Path '" + path + "'");

		for (URL url : urls) {
			String fileName = url.getPath().substring(url.getPath().lastIndexOf('/') + 1);
			fileName = fileName.replace("-template", "");
			String targetFilename = targetDirectory + fileName;
			try {
				if (fileManager.exists(targetFilename) && !overwrite) {
					continue;
				}
				if (targetFilename.endsWith("png")) {
					FileCopyUtils.copy(url.openStream(), fileManager.createFile(targetFilename).getOutputStream());
				} else {
					// Read template and insert the user's package
					String input = FileCopyUtils.copyToString(new InputStreamReader(url.openStream()));

					String topLevelPackage = projectOperations.getProjectMetadata().getTopLevelPackage().getFullyQualifiedPackageName();
					input = input.replace("__TOP_LEVEL_PACKAGE__", topLevelPackage);
					input = input.replace("__SEGMENT_PACKAGE__", segmentPackage);
					input = input.replace("__PROJECT_NAME__", projectOperations.getProjectMetadata().getProjectName());

					if (isGaeEnabled) {
						input = input.replace("__GAE_IMPORT__", "import " + topLevelPackage + ".client.scaffold.gae.*;\n");
						input = input.replace("__GAE_HOOKUP__", getGaeHookup());
						input = input.replace("__GAE_REQUEST_TRANSPORT__", ", new GaeAuthRequestTransport(eventBus)");
					} else {
						input = input.replace("__GAE_IMPORT__", "");
						input = input.replace("__GAE_HOOKUP__", "");
						input = input.replace("__GAE_REQUEST_TRANSPORT__", "");
					}

					// Output the file for the user
					fileManager.createOrUpdateTextFileIfRequired(targetFilename, input, true);
				}
			} catch (IOException e) {
				throw new IllegalStateException("Unable to create '" + targetFilename + "'", e);
			}
		}
	}

	private void updateBuildPlugins(boolean isGaeEnabled) {
		Element configuration = XmlUtils.getConfiguration(getClass());
		String xPath = "/configuration/" + (isGaeEnabled ? "gae" : "gwt") + "/plugins/plugin";
		Element pluginElement = XmlUtils.findFirstElement(xPath, configuration);
		Assert.notNull(pluginElement, "gwt-maven-plugin required");
		Plugin defaultPlugin = new Plugin(pluginElement);
		for (Plugin plugin : projectOperations.getProjectMetadata().getBuildPlugins()) {
			if ("gwt-maven-plugin".equals(plugin.getArtifactId())) {
				if (defaultPlugin.equals(plugin)) {
					return;
				}
			}
		}

		projectOperations.updateBuildPlugin(defaultPlugin);
	}

	private void removeIfFound(String xpath, Element webXmlRoot) {
		for (Element toRemove : XmlUtils.findElements(xpath, webXmlRoot)) {
			if (toRemove != null) {
				toRemove.getParentNode().removeChild(toRemove);
				toRemove = null;
			}
		}
	}

	private CharSequence getGaeHookup() {
		StringBuilder builder = new StringBuilder("// AppEngine user authentication\n\n");
		builder.append("new GaeLoginWidgetDriver(requestFactory).setWidget(shell.getLoginWidget());\n\n");
		builder.append("new ReloadOnAuthenticationFailure().register(eventBus);\n\n");
		return builder.toString();
	}
}
