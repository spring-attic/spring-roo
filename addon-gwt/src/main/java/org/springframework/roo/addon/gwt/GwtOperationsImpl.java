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
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Repository;
import org.springframework.roo.project.listeners.PluginListener;
import org.springframework.roo.support.osgi.UrlFindingUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
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
public class GwtOperationsImpl implements GwtOperations, PluginListener {
	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;
	@Reference private WebMvcOperations mvcOperations;
	@Reference private ProjectOperations projectOperations;
	private ComponentContext context;

	protected void activate(ComponentContext context) {
		this.context = context;
		projectOperations.addPluginListener(this);
	}

	protected void deactivate(ComponentContext context) {
		projectOperations.removePluginListener(this);
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

		// Add GWT natures and builder names to maven eclipse plugin
		updateEclipsePlugin();

		// Add outputDirectory to build element of pom
		updateBuildOutputDirectory();

		Element configuration = XmlUtils.getConfiguration(getClass());

		// Add POM repositories
		updateRepositories(configuration);

		// Add dependencies
		updateDependencies(configuration);

		// Add POM plugins
		updateBuildPlugins(configuration);

		// Update web.xml
		updateWebXml();

		// Update persistence.xml
		updatePersistenceXml();

		// Do a "get" for every .java file, thus ensuring the metadata is fired
		PathResolver pathResolver = projectOperations.getPathResolver();
		FileDetails srcRoot = new FileDetails(new File(pathResolver.getRoot(Path.SRC_MAIN_JAVA)), null);
		String antPath = pathResolver.getRoot(Path.SRC_MAIN_JAVA) + File.separatorChar + "**" + File.separatorChar + "*.java";
		for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
			String fullPath = srcRoot.getRelativeSegment(fd.getCanonicalPath());
			fullPath = fullPath.substring(1, fullPath.lastIndexOf(".java")).replace(File.separatorChar, '.'); // Ditch the first / and .java
			JavaType javaType = new JavaType(fullPath);
			String id = GwtMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
			metadataService.get(id);
		}
	}

	public void pluginAdded(Plugin plugin) {
		if (plugin.getArtifactId().equals("gwt-maven-plugin")) {
			// Refresh project metadata first in case the maven-gae-plugin has been added
			metadataService.get(ProjectMetadata.getProjectIdentifier(), true);

			// Copy "static" directories
			for (GwtPath path : GwtPath.values()) {
				copyDirectoryContents(path);
			}
		}
	}

	public void pluginRemoved(Plugin plugin) {
		// Do nothing
	}

	private void updateEclipsePlugin() {
		MutableFile mutableFile = fileManager.updateFile(projectOperations.getPathResolver().getIdentifier(Path.ROOT, "pom.xml"));
		Document pom = XmlUtils.readXml(mutableFile.getInputStream());
		Element root = pom.getDocumentElement();

		boolean hasChanged = false;

		// Add GWT buildCommand
		Element additionalBuildcommandsElement = XmlUtils.findFirstElement("/project/build/plugins/plugin[artifactId = 'maven-eclipse-plugin']/configuration/additionalBuildcommands", root);
		Assert.notNull(additionalBuildcommandsElement, "additionalBuildcommands element of the maven-eclipse-plugin required");
		String gwtBuildCommandName = "com.google.gwt.eclipse.core.gwtProjectValidator";
		Element gwtBuildCommandElement = XmlUtils.findFirstElement("buildCommand[name = '" + gwtBuildCommandName + "']", additionalBuildcommandsElement);
		if (gwtBuildCommandElement == null) {
			Element nameElement = pom.createElement("name");
			nameElement.setTextContent(gwtBuildCommandName);
			gwtBuildCommandElement = pom.createElement("buildCommand");
			gwtBuildCommandElement.appendChild(nameElement);
			additionalBuildcommandsElement.appendChild(gwtBuildCommandElement);
			hasChanged = true;
		}

		// Add GWT projectnature
		Element additionalProjectnaturesElement = XmlUtils.findFirstElement("/project/build/plugins/plugin[artifactId = 'maven-eclipse-plugin']/configuration/additionalProjectnatures", root);
		Assert.notNull(additionalProjectnaturesElement, "additionalProjectnatures element of the maven-eclipse-plugin required");
		String gwtProjectnatureName = "com.google.gwt.eclipse.core.gwtNature";
		Element gwtProjectnatureElement = XmlUtils.findFirstElement("projectnature[name = '" + gwtProjectnatureName + "']", additionalProjectnaturesElement);
		if (gwtProjectnatureElement == null) {
			gwtProjectnatureElement = new XmlElementBuilder("projectnature", pom).setText(gwtProjectnatureName).build();
			additionalProjectnaturesElement.appendChild(gwtProjectnatureElement);
			hasChanged = true;
		}

		if (hasChanged) {
			mutableFile.setDescriptionOfChange("Updated GWT buildCommand and projectnature in maven-eclipse-plugin");
			XmlUtils.writeXml(mutableFile.getOutputStream(), pom);
		}
	}

	private void updateBuildOutputDirectory() {
		MutableFile mutableFile = fileManager.updateFile(projectOperations.getPathResolver().getIdentifier(Path.ROOT, "pom.xml"));
		Document pom = XmlUtils.readXml(mutableFile.getInputStream());
		Element root = pom.getDocumentElement();

		boolean hasChanged = false;

		String outputDirectoryText = "${project.build.directory}/${project.build.finalName}/WEB-INF/classes";
		Element outputDirectoryElement = XmlUtils.findFirstElement("/project/build/outputDirectory", root);
		if (outputDirectoryElement != null) {
			if (!outputDirectoryElement.getTextContent().equals(outputDirectoryText)) {
				outputDirectoryElement.setTextContent(outputDirectoryText);
				hasChanged = true;
			}
		} else {
			outputDirectoryElement = new XmlElementBuilder("outputDirectory", pom).setText(outputDirectoryText).build();
			Element buildElement = XmlUtils.findRequiredElement("/project/build", root);
			buildElement.appendChild(outputDirectoryElement);
			hasChanged = true;
		}

		if (hasChanged) {
			mutableFile.setDescriptionOfChange("Added outputDirectory");
			XmlUtils.writeXml(mutableFile.getOutputStream(), pom);
		}
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

	private void updateBuildPlugins(Element configuration) {
		Set<Plugin> buildPlugins = projectOperations.getProjectMetadata().getBuildPlugins();
		List<Element> pluginElements = XmlUtils.findElements("/configuration/gwt/plugins/plugin", configuration);
		for (Element pluginElement : pluginElements) {
			Plugin plugin = new Plugin(pluginElement);
			if (plugin.getArtifactId().equals("gwt-maven-plugin") && buildPlugins.contains(plugin)) {
				projectOperations.removeBuildPlugin(plugin);
			}
			projectOperations.addBuildPlugin(plugin);
		}
	}

	private void updateWebXml() {
		MutableFile mutableFile = fileManager.updateFile(projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml"));
		Document webXml = XmlUtils.readXml(mutableFile.getInputStream());
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
		mutableFile.setDescriptionOfChange("Managed security filter and security-constraint in web.xml");
		XmlUtils.writeXml(mutableFile.getOutputStream(), webXml);
	}

	private void updatePersistenceXml() {
		MutableFile mutableFile = fileManager.updateFile(projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml"));
		Document persistenceXml = XmlUtils.readXml(mutableFile.getInputStream());
		Element root = persistenceXml.getDocumentElement();

		for (Element persistenceUnitElement : XmlUtils.findElements("persistence-unit", root)) {
			Element provider = XmlUtils.findFirstElement("provider", persistenceUnitElement);
			if (provider != null && "org.datanucleus.store.appengine.jpa.DatastorePersistenceProvider".equals(provider.getTextContent()) && !projectOperations.getProjectMetadata().isGaeEnabled()) {
				persistenceUnitElement.getParentNode().removeChild(persistenceUnitElement);
				XmlUtils.writeXml(mutableFile.getOutputStream(), persistenceXml);
				break;
			}
		}
	}

	private void copyDirectoryContents(GwtPath gwtPath) {
		String sourceAntPath = gwtPath.sourceAntPath();
		String targetDirectory = gwtPath.canonicalFileSystemPath(projectOperations.getProjectMetadata());

		if (!projectOperations.getProjectMetadata().isGaeEnabled() && targetDirectory.contains("/gae")) {
			return;
		}
		if (!targetDirectory.endsWith("/")) {
			targetDirectory += "/";
		}
		if (!fileManager.exists(targetDirectory)) {
			fileManager.createDirectory(targetDirectory);
		}

		String path = TemplateUtils.getTemplatePath(getClass(), sourceAntPath);
		Set<URL> urls = UrlFindingUtils.findMatchingClasspathResources(context.getBundleContext(), path);
		Assert.notNull(urls, "Could not search bundles for resources for Ant Path '" + path + "'");
		for (URL url : urls) {
			String fileName = url.getPath().substring(url.getPath().lastIndexOf("/") + 1);
			fileName = fileName.replace("-template", "");
			String targetFilename = targetDirectory + fileName;

			try {
				boolean exists = fileManager.exists(targetFilename);
				if (targetFilename.endsWith("png")) {
					if (exists) {
						continue;
					}
					FileCopyUtils.copy(url.openStream(), fileManager.createFile(targetFilename).getOutputStream());
				} else {
					// Read template and insert the user's package
					String input = FileCopyUtils.copyToString(new InputStreamReader(url.openStream()));
					if (exists && !input.contains("__GAE_")) {
						continue;
					}

					String topLevelPackage = projectOperations.getProjectMetadata().getTopLevelPackage().getFullyQualifiedPackageName();
					input = input.replace("__TOP_LEVEL_PACKAGE__", topLevelPackage);
					input = input.replace("__SEGMENT_PACKAGE__", gwtPath.segmentPackage());
					input = input.replace("__PROJECT_NAME__", projectOperations.getProjectMetadata().getProjectName());

					if (projectOperations.getProjectMetadata().isGaeEnabled()) {
						input = input.replace("__GAE_IMPORT__", "import " + topLevelPackage + ".client.scaffold.gae.*;\n");
						input = input.replace("__GAE_HOOKUP__", getGaeHookup());
						input = input.replace("__GAE_REQUEST_TRANSPORT__", ", new GaeAuthRequestTransport(eventBus)");
					} else {
						input = input.replace("__GAE_IMPORT__", "");
						input = input.replace("__GAE_HOOKUP__", "");
						input = input.replace("__GAE_REQUEST_TRANSPORT__", "");
					}

					// Output the file for the user
					MutableFile mutableFile = exists ? fileManager.updateFile(targetFilename) : fileManager.createFile(targetFilename);
					FileCopyUtils.copy(input.getBytes(), mutableFile.getOutputStream());
				}
			} catch (IOException e) {
				throw new IllegalStateException("Unable to create '" + targetFilename + "'", e);
			}
		}
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
		StringBuilder builder = new StringBuilder("    // AppEngine user authentication\n\n");
		builder.append("    new GaeLoginWidgetDriver(requestFactory).setWidget(shell.loginWidget);\n\n");
		builder.append("    new ReloadOnAuthenticationFailure().register(eventBus);\n\n");
		return builder.toString();
	}
}
