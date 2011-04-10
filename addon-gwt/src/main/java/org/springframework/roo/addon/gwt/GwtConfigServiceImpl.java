package org.springframework.roo.addon.gwt;

import java.io.IOException;
import java.io.InputStream;
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
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.osgi.UrlFindingUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.WebXmlUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Implementation of {@link GwtConfigServiceImpl}.
 * 
 * @author James Tyrrell
 * @since 1.1.2
 */
@Component 
@Service 
public class GwtConfigServiceImpl implements GwtConfigService {
	@Reference private FileManager fileManager;
	@Reference private ProjectOperations projectOperations;
	private ComponentContext context;

	protected void activate(ComponentContext context) {
		this.context = context;
	}

	// TODO: I think this still needs some work (why do I need to add custom xml manipulation logic here..?) - JT
	public void updateConfiguration(boolean initialSetup) {
		// if (!isGwtProject() && !initialSetup) {
		if (!initialSetup) {
			return;
		}

		String pom = projectOperations.getPathResolver().getIdentifier(Path.ROOT, "pom.xml");
		Assert.isTrue(fileManager.exists(pom), "pom.xml not found; cannot continue");

		MutableFile mutablePom = fileManager.updateFile(pom);
		Document pomDoc = getXmlDocument(pom);

		// Add GWT natures and builder names to maven eclipse plugin
		if (updateMavenEclipsePlugin(pomDoc) || updateDataNulcueusPlugin(pomDoc)) {
			XmlUtils.writeXml(mutablePom.getOutputStream(), pomDoc);
		}

		Element configuration = XmlUtils.getConfiguration(getClass());

		// Add dependencies
		updateDependencies(configuration);

		// Add POM plugins
		updatePlugins(configuration);

		// Update web.xml
		updateWebXml();

		// Update persistence.xml
		updatePersistenceXml();

		// Copy "static" directories
		for (GwtPath path : GwtPath.values()) {
			copyDirectoryContents(path);
		}
	}

//	private boolean isGwtProject() {
//		if (gwtProject != null) {
//			return gwtProject;
//		}
//
//		String pom = projectOperations.getPathResolver().getIdentifier(Path.ROOT, "pom.xml");
//		Assert.isTrue(fileManager.exists(pom), "pom.xml not found; cannot continue");
//
//		Document pomDoc = getXmlDocument(pom);
//		Element pomRoot = (Element) pomDoc.getFirstChild();
//		List<Element> pluginElements = XmlUtils.findElements("/project/build/plugins/plugin", pomRoot);
//		gwtProject = false;
//		for (Element pluginElement : pluginElements) {
//			Plugin plugin = new Plugin(pluginElement);
//			if ("maven-eclipse-plugin".equals(plugin.getArtifactId()) && "org.apache.maven.plugins".equals(plugin.getGroupId())) {
//				gwtProject = true;
//				break;
//			}
//		}
//		return gwtProject;
//	}

	private void removeIfFound(String xpath, Element webXmlRoot) {
		for (Element toRemove : XmlUtils.findElements(xpath, webXmlRoot)) {
			if (toRemove != null) {
				toRemove.getParentNode().removeChild(toRemove);
				toRemove = null;
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
			if (!fileManager.exists(targetFilename)) {
				try {
					if (targetFilename.endsWith("png")) {
						FileCopyUtils.copy(url.openStream(), fileManager.createFile(targetFilename).getOutputStream());
					} else {
						// Read template and insert the user's package
						String input = FileCopyUtils.copyToString(new InputStreamReader(url.openStream()));
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
						MutableFile mutableFile = fileManager.createFile(targetFilename);
						FileCopyUtils.copy(input.getBytes(), mutableFile.getOutputStream());
					}
				} catch (IOException ioe) {
					throw new IllegalStateException("Unable to create '" + targetFilename + "'", ioe);
				}
			}
		}
	}

	private CharSequence getGaeHookup() {
		StringBuilder builder = new StringBuilder("    // AppEngine user authentication\n\n");
		builder.append("    new GaeLoginWidgetDriver(requestFactory).setWidget(shell.loginWidget);\n\n");
		builder.append("    new ReloadOnAuthenticationFailure().register(eventBus);\n\n");
		return builder.toString();
	}

	private void updatePersistenceXml() {
		String persistenceXml = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml");
		if (!fileManager.exists(persistenceXml)) {
			return;
		}

		MutableFile mutablePersistenceXml = fileManager.updateFile(persistenceXml);
		Document persistenceXmlDoc = getXmlDocument(persistenceXml);

		Element persistenceXmlDocRoot = (Element) persistenceXmlDoc.getFirstChild();
		List<Element> persistenceUnitElements = XmlUtils.findElements("persistence-unit", persistenceXmlDocRoot);

		for (Element persistenceUnitElement : persistenceUnitElements) {
			Element provider = XmlUtils.findFirstElement("provider", persistenceUnitElement);
			if (provider != null && "org.datanucleus.store.appengine.jpa.DatastorePersistenceProvider".equals(provider.getTextContent()) && !projectOperations.getProjectMetadata().isGaeEnabled()) {
				persistenceUnitElement.getParentNode().removeChild(persistenceUnitElement);
				XmlUtils.writeXml(mutablePersistenceXml.getOutputStream(), persistenceXmlDoc);
				break;
			}
		}
	}

	private Document getXmlDocument(String xmlFile) {
		MutableFile mutableXml;
		InputStream is = null;
		Document xmlDoc;
		try {
			mutableXml = fileManager.updateFile(xmlFile);
			is = mutableXml.getInputStream();
			xmlDoc = XmlUtils.getDocumentBuilder().parse(mutableXml.getInputStream());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException ignored) {}
			}
		}
		return xmlDoc;
	}

	private boolean updateDataNulcueusPlugin(Document pomXmlDoc) {
		Element pomRoot = (Element) pomXmlDoc.getFirstChild();
		List<Element> pluginElements = XmlUtils.findElements("/project/build/plugins/plugin", pomRoot);
		for (Element pluginElement : pluginElements) {
			Plugin plugin = new Plugin(pluginElement);
			if ("maven-datanucleus-plugin".equals(plugin.getArtifactId()) && "org.datanucleus".equals(plugin.getGroupId())) {
				Element configElement = plugin.getConfiguration().getConfiguration();
				boolean mappingExclusionPresent = false;
				for (int i = 0; i < configElement.getChildNodes().getLength(); i++) {
					Node childNode = configElement.getChildNodes().item(i);
					if (childNode.getNodeName().equals("mappingExcludes")) {
						if (childNode.getTextContent().equals("**/GaeAuthFilter.class")) {
							mappingExclusionPresent = true;
						}
					}
				}
				if (!mappingExclusionPresent) {
					configElement.appendChild(new XmlElementBuilder("mappingExcludes", pomXmlDoc).setText("**/GaeAuthFilter.class").build());
					return true;
				}
			}
		}
		return false;
	}

	private void updateWebXml() {
		String webXml = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml");
		Assert.isTrue(fileManager.exists(webXml), "web.xml not found; cannot continue");

		MutableFile mutableWebXml = fileManager.updateFile(webXml);
		Document webXmlDoc = getXmlDocument(webXml);
		Element webXmlRoot = webXmlDoc.getDocumentElement();

		WebXmlUtils.addServlet("requestFactory", "com.google.gwt.requestfactory.server.RequestFactoryServlet", "/gwtRequest", null, webXmlDoc, null);
		if (projectOperations.getProjectMetadata().isGaeEnabled()) {
			WebXmlUtils.addFilter("GaeAuthFilter", GwtPath.SERVER_GAE.packageName(projectOperations.getProjectMetadata()) + ".GaeAuthFilter", "/gwtRequest/*", webXmlDoc, "This filter makes GAE authentication services visible to a RequestFactory client.");
			String displayName = "Redirect to the login page if needed before showing any html pages";
			WebXmlUtils.WebResourceCollection webResourceCollection = new WebXmlUtils.WebResourceCollection("Login required", null, Collections.singletonList("*.html"), new ArrayList<String>());
			ArrayList<String> roleNames = new ArrayList<String>();
			roleNames.add("*");
			String userDataConstraint = null;
			WebXmlUtils.addSecurityConstraint(displayName, Collections.singletonList(webResourceCollection), roleNames, userDataConstraint, webXmlDoc, null);
		} else {
			Element filter = XmlUtils.findFirstElement("/web-app/filter[filter-name = 'GaeAuthFilter']", webXmlDoc.getDocumentElement());
			if (filter != null) {
				filter.getParentNode().removeChild(filter);
			}
			Element filterMapping = XmlUtils.findFirstElement("/web-app/filter-mapping[filter-name = 'GaeAuthFilter']", webXmlDoc.getDocumentElement());
			if (filterMapping != null) {
				filterMapping.getParentNode().removeChild(filterMapping);
			}
			Element securityConstraint = XmlUtils.findFirstElement("security-constraint", webXmlDoc.getDocumentElement());
			if (securityConstraint != null) {
				securityConstraint.getParentNode().removeChild(securityConstraint);
			}
		}

		removeIfFound("/web-app/error-page", webXmlRoot);
		XmlUtils.writeXml(mutableWebXml.getOutputStream(), webXmlDoc);
	}

	private void updatePlugins(Element configuration) {
		boolean removePlugin = false;
		for (Plugin existingBuildPlugin : projectOperations.getProjectMetadata().getBuildPlugins()) {
			if (existingBuildPlugin.getArtifactId().equals("gwt-maven-plugin")) {
				Element pluginConfiguration = existingBuildPlugin.getConfiguration().getConfiguration();
				Element serverElement = XmlUtils.findFirstElement("server", pluginConfiguration);
				if (serverElement == null) {
					removePlugin = projectOperations.getProjectMetadata().isGaeEnabled();
					break;
				}
				if ("com.google.appengine.tools.development.gwt.AppEngineLauncher".equals(serverElement.getTextContent())) {
					removePlugin = !projectOperations.getProjectMetadata().isGaeEnabled();
				}
				break;
			}
		}

		List<Element> plugins = XmlUtils.findElements(projectOperations.getProjectMetadata().isGaeEnabled() ? "/configuration/gwt/gae-plugins/plugin" : "/configuration/gwt/plugins/plugin", configuration);
		for (Element pluginElement : plugins) {
			Plugin plugin = new Plugin(pluginElement);
			if (removePlugin) {
				projectOperations.removeBuildPlugin(plugin);
			}
			projectOperations.addBuildPlugin(plugin);
		}
	}

	private void updateDependencies(Element configuration) {
		List<Dependency> dependencies = new ArrayList<Dependency>();

		List<Element> gwtDependencies = XmlUtils.findElements("/configuration/gwt/dependencies/dependency", configuration);
		for (Element dependencyElement : gwtDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}

		if (projectOperations.getProjectMetadata().isGaeEnabled()) {
			// Add GAE SDK specific JARs using systemPath to make AppEngineLauncher happy
			List<Element> gaeDependencies = XmlUtils.findElements("/configuration/gwt/gae-dependencies/dependency", configuration);
			for (Element dependencyElement : gaeDependencies) {
				dependencies.add(new Dependency(dependencyElement));
			}
		}

		projectOperations.addDependencies(dependencies);
	}

	private boolean updateMavenEclipsePlugin(Document pomDoc) {
		Element pomRoot = (Element) pomDoc.getFirstChild();
		boolean hasChanged = false;
		List<Element> pluginElements = XmlUtils.findElements("/project/build/plugins/plugin", pomRoot);
		for (Element pluginElement : pluginElements) {
			Plugin plugin = new Plugin(pluginElement);
			if ("maven-eclipse-plugin".equals(plugin.getArtifactId()) && "org.apache.maven.plugins".equals(plugin.getGroupId())) {
				Element ctx = XmlUtils.findRequiredElement("configuration/additionalBuildcommands", pluginElement);
				boolean gwtProjectValidatorCommandPresent = false;
				for (Element buildCommand : XmlUtils.findElements("buildCommand", ctx)) {
					Element buildCommandName = XmlUtils.findFirstElementByName("name", buildCommand);
					if (buildCommandName.getTextContent().equals("com.google.gwt.eclipse.core.gwtProjectValidator")) {
						gwtProjectValidatorCommandPresent = true;
						break;
					}
				}

				// Add in the builder configuration
				if (!gwtProjectValidatorCommandPresent) {
					Element newEntry = new XmlElementBuilder("buildCommand", pomDoc).addChild(new XmlElementBuilder("name", pomDoc).setText("com.google.gwt.eclipse.core.gwtProjectValidator").build()).build();
					ctx.appendChild(newEntry);
					hasChanged = true;
				}

				ctx = XmlUtils.findRequiredElement("configuration/additionalProjectnatures", pluginElement);
				boolean gwtNaturePresent = false;
				boolean gaeNaturePresent = false;
				for (Element natureElement : XmlUtils.findElements("projectnature", ctx)) {
					if (natureElement.getTextContent().equals("com.google.appengine.eclipse.core.gaeNature")) {
						gaeNaturePresent = true;
					} else if (natureElement.getTextContent().equals("com.google.gwt.eclipse.core.gwtNature")) {
						gwtNaturePresent = true;
					}
				}

				// Add in the additional nature
				if (!gwtNaturePresent) {
					Element newEntry = new XmlElementBuilder("projectnature", pomDoc).setText("com.google.gwt.eclipse.core.gwtNature").build();
					ctx.appendChild(newEntry);
					hasChanged = true;
				}

				// If gae plugin configured, add gaeNature
				if (projectOperations.getProjectMetadata().isGaeEnabled() && !gaeNaturePresent) {
					Element newEntry = new XmlElementBuilder("projectnature", pomDoc).setText("com.google.appengine.eclipse.core.gaeNature").build();
					ctx.appendChild(newEntry);
					hasChanged = true;
				}
			}
		}

		// Fix output directory
		Element outputDirectory = XmlUtils.findFirstElement("/project/build/outputDirectory", pomRoot);
		if (outputDirectory != null) {
			if (!outputDirectory.getTextContent().equals("${project.build.directory}/${project.build.finalName}/WEB-INF/classes")) {
				outputDirectory.setTextContent("${project.build.directory}/${project.build.finalName}/WEB-INF/classes");
				hasChanged = true;
			}
		} else {
			Element newEntry = new XmlElementBuilder("outputDirectory", pomDoc).setText("${project.build.directory}/${project.build.finalName}/WEB-INF/classes").build();
			Element ctx = XmlUtils.findRequiredElement("/project/build", pomRoot);
			ctx.appendChild(newEntry);
			hasChanged = true;
		}
		return hasChanged;
	}
}
