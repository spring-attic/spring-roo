package org.springframework.roo.addon.gwt;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
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
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Repository;
import org.springframework.roo.support.osgi.OSGiUtils;
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
public class GwtOperationsImpl implements GwtOperations {
	
	// Fields
	@Reference protected FileManager fileManager;
	@Reference protected GwtTypeService gwtTypeService;
	@Reference protected WebMvcOperations mvcOperations;
	@Reference protected ProjectOperations projectOperations;
	@Reference protected TypeLocationService typeLocationService;
	@Reference protected TypeManagementService typeManagementService;

	private ComponentContext context;
	private Boolean wasGaeEnabled;

	protected void activate(ComponentContext context) {
		this.context = context;
	}

	public boolean isSetupAvailable() {
		String persistencePath = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml");
		return projectOperations.isProjectAvailable() && new File(persistencePath).exists();
	}

	public boolean isGwtEnabled() {
		return projectOperations.isProjectAvailable() && projectOperations.getProjectMetadata().isGwtEnabled();
	}

	public void proxyAll(JavaPackage proxyPackage) {
		for (ClassOrInterfaceTypeDetails entity : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_JPA_ENTITY, RooJavaType.ROO_ENTITY)) {
			createProxy(entity, proxyPackage);
		}
	}

	public void proxyType(JavaPackage proxyPackage, JavaType type) {
		ClassOrInterfaceTypeDetails typeDetails = typeLocationService.getClassOrInterface(type);
		if (typeDetails != null) {
			createProxy(typeDetails, proxyPackage);
		}
	}

	public void requestAll(JavaPackage proxyPackage) {
		for (ClassOrInterfaceTypeDetails entity : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_JPA_ENTITY, RooJavaType.ROO_ENTITY)) {
			createRequest(entity, proxyPackage);
		}
	}

	public void requestType(JavaPackage requestPackage, JavaType type) {
		ClassOrInterfaceTypeDetails typeDetails = typeLocationService.getClassOrInterface(type);
		if (typeDetails != null) {
			createRequest(typeDetails, requestPackage);
		}
	}

	public void proxyAndRequestAll(JavaPackage proxyAndRequestPackage) {
		proxyAll(proxyAndRequestPackage);
		requestAll(proxyAndRequestPackage);
	}

	public void proxyAndRequestType(JavaPackage proxyAndRequestPackage, JavaType type) {
		proxyType(proxyAndRequestPackage, type);
		requestType(proxyAndRequestPackage, type);
	}

	public void scaffoldAll() {
		updateScaffoldBoilerPlate();
		for (ClassOrInterfaceTypeDetails proxy : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_GWT_PROXY)) {
			ClassOrInterfaceTypeDetails request = gwtTypeService.lookupRequestFromProxy(proxy);
			if (request == null) {
				throw new IllegalStateException("In order to scaffold and entity must have a request");
			}
			AnnotationMetadata annotationMetadata = GwtUtils.getFirstAnnotation(proxy, RooJavaType.ROO_GWT_PROXY);
			if (annotationMetadata != null) {
				ClassOrInterfaceTypeDetailsBuilder proxyBuilder = new ClassOrInterfaceTypeDetailsBuilder(proxy);
				AnnotationMetadataBuilder annotationMetadataBuilder = new AnnotationMetadataBuilder(annotationMetadata);
				annotationMetadataBuilder.addBooleanAttribute("scaffold", true);
				proxyBuilder.updateTypeAnnotation(annotationMetadataBuilder);
				typeManagementService.createOrUpdateTypeOnDisk(proxyBuilder.build());
			}
		}
	}

	public void scaffoldType(JavaType type) {
		ClassOrInterfaceTypeDetails entity = typeLocationService.getClassOrInterface(type);
		if (entity != null) {
			ClassOrInterfaceTypeDetails proxy = gwtTypeService.lookupProxyFromEntity(entity);
			ClassOrInterfaceTypeDetails request = gwtTypeService.lookupRequestFromEntity(entity);
			if (proxy == null || request == null) {
				throw new IllegalStateException("In order to scaffold and entity must have an associated proxy and request");
			}
			updateScaffoldBoilerPlate();
			createScaffold(proxy);
		}
	}

	public void setup() {
		// Install web pieces if not already installed
		if (!fileManager.exists(projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/web.xml"))) {
			mvcOperations.installAllWebMvcArtifacts();
		}

		String sourceAntPath = "setup/*";
		if (sourceAntPath.contains("gae") && !projectOperations.getProjectMetadata().isGaeEnabled()) {
			return;
		}
		String targetDirectory = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_JAVA, projectOperations.getProjectMetadata().getTopLevelPackage().getFullyQualifiedPackageName().replace('.', File.separatorChar));
		updateFile(sourceAntPath, targetDirectory, "", false);

		sourceAntPath = "setup/client/*";
		if (sourceAntPath.contains("gae") && !projectOperations.getProjectMetadata().isGaeEnabled()) {
			return;
		}
		updateFile(sourceAntPath, targetDirectory + "/client", "", false);

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

		updateBuildPlugins(projectOperations.getProjectMetadata().isGaeEnabled());
	}

	private void createProxy(ClassOrInterfaceTypeDetails entity, JavaPackage destinationPackage) {
		ClassOrInterfaceTypeDetails existingProxy = gwtTypeService.lookupProxyFromEntity(entity);
		if (existingProxy != null) {
			return;
		}
		JavaType proxyName = new JavaType(destinationPackage.getFullyQualifiedPackageName()  + "." + entity.getName().getSimpleTypeName() + "Proxy");
		ClassOrInterfaceTypeDetailsBuilder builder = new ClassOrInterfaceTypeDetailsBuilder(PhysicalTypeIdentifier.createIdentifier(proxyName));
		builder.setName(proxyName);
		builder.setExtendsTypes(Collections.singletonList(GwtUtils.ENTITY_PROXY));
		builder.setPhysicalTypeCategory(PhysicalTypeCategory.INTERFACE);
		builder.setModifier(Modifier.PUBLIC);
		List<AnnotationAttributeValue<?>> attributeValues = new ArrayList<AnnotationAttributeValue<?>>();
		StringAttributeValue stringAttributeValue = new StringAttributeValue(new JavaSymbolName("value"), entity.getName().getFullyQualifiedTypeName());
		attributeValues.add(stringAttributeValue);
		builder.updateTypeAnnotation(new AnnotationMetadataBuilder(GwtUtils.PROXY_FOR_NAME, attributeValues));
		attributeValues = new ArrayList<AnnotationAttributeValue<?>>();
		List<StringAttributeValue> readOnlyValues = new ArrayList<StringAttributeValue>();
		readOnlyValues.add(new StringAttributeValue(new JavaSymbolName("value"), "version"));
		readOnlyValues.add(new StringAttributeValue(new JavaSymbolName("value"), "id"));
		ArrayAttributeValue<StringAttributeValue> readOnlyAttribute = new ArrayAttributeValue<StringAttributeValue>(new JavaSymbolName("readOnly"), readOnlyValues);
		attributeValues.add(readOnlyAttribute);
		builder.updateTypeAnnotation(new AnnotationMetadataBuilder(RooJavaType.ROO_GWT_PROXY, attributeValues));
		typeManagementService.createOrUpdateTypeOnDisk(builder.build());
	}

	private void createRequest(ClassOrInterfaceTypeDetails entity, JavaPackage destinationPackage) {
		ClassOrInterfaceTypeDetails existingProxy = gwtTypeService.lookupRequestFromEntity(entity);
		if (existingProxy != null) {
			return;
		}
		JavaType proxyName = new JavaType(destinationPackage.getFullyQualifiedPackageName()  + "." + entity.getName().getSimpleTypeName() + "Request");
		ClassOrInterfaceTypeDetailsBuilder builder = new ClassOrInterfaceTypeDetailsBuilder(PhysicalTypeIdentifier.createIdentifier(proxyName));
		builder.setName(proxyName);
		builder.setExtendsTypes(Collections.singletonList(GwtUtils.REQUEST_CONTEXT));
		builder.setPhysicalTypeCategory(PhysicalTypeCategory.INTERFACE);
		builder.setModifier(Modifier.PUBLIC);
		List<AnnotationAttributeValue<?>> attributeValues = new ArrayList<AnnotationAttributeValue<?>>();
		StringAttributeValue stringAttributeValue = new StringAttributeValue(new JavaSymbolName("value"), entity.getName().getFullyQualifiedTypeName());
		attributeValues.add(stringAttributeValue);
		builder.updateTypeAnnotation(new AnnotationMetadataBuilder(GwtUtils.SERVICE_NAME, attributeValues));
		attributeValues = new ArrayList<AnnotationAttributeValue<?>>();
		List<StringAttributeValue> toExclude = new ArrayList<StringAttributeValue>();
		toExclude.add(new StringAttributeValue(new JavaSymbolName("value"), "entityManager"));
		toExclude.add(new StringAttributeValue(new JavaSymbolName("value"), "toString"));
		toExclude.add(new StringAttributeValue(new JavaSymbolName("value"), "merge"));
		toExclude.add(new StringAttributeValue(new JavaSymbolName("value"), "flush"));
		toExclude.add(new StringAttributeValue(new JavaSymbolName("value"), "clear"));
		ArrayAttributeValue<StringAttributeValue> exclude = new ArrayAttributeValue<StringAttributeValue>(new JavaSymbolName("exclude"), toExclude);
		attributeValues.add(exclude);
		builder.updateTypeAnnotation(new AnnotationMetadataBuilder(RooJavaType.ROO_GWT_REQUEST, attributeValues));
		typeManagementService.createOrUpdateTypeOnDisk(builder.build());
	}

	private void createScaffold(ClassOrInterfaceTypeDetails proxy) {
		AnnotationMetadata annotationMetadata = GwtUtils.getFirstAnnotation(proxy, RooJavaType.ROO_GWT_PROXY);
		if (annotationMetadata != null) {
			ClassOrInterfaceTypeDetailsBuilder proxyBuilder = new ClassOrInterfaceTypeDetailsBuilder(proxy);
			AnnotationMetadataBuilder annotationMetadataBuilder = new AnnotationMetadataBuilder(annotationMetadata);
			annotationMetadataBuilder.addBooleanAttribute("scaffold", true);
			for (AnnotationMetadataBuilder existingAnnotation : proxyBuilder.getAnnotations()) {
				if (existingAnnotation.getAnnotationType().equals(annotationMetadata.getAnnotationType())) {
					proxyBuilder.getAnnotations().remove(existingAnnotation);
					proxyBuilder.getAnnotations().add(annotationMetadataBuilder);
					break;
				}
			}
			typeManagementService.createOrUpdateTypeOnDisk(proxyBuilder.build());
		}
	}

	private void updateScaffoldBoilerPlate() {
		String targetDirectory = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_JAVA, projectOperations.getProjectMetadata().getTopLevelPackage().getFullyQualifiedPackageName().replace('.', File.separatorChar));
		deleteUntouchedSetupFiles("setup/*", targetDirectory);
		deleteUntouchedSetupFiles("setup/client/*", targetDirectory + "/client");
		copyDirectoryContents();
		updateGaeHelper();
	}

	public void updateGaeConfiguration() {
		boolean isGaeEnabled = projectOperations.getProjectMetadata().isGaeEnabled();
		boolean hasGaeStateChanged = wasGaeEnabled == null || isGaeEnabled != wasGaeEnabled;
		if (projectOperations.getProjectMetadata().isGwtEnabled() && hasGaeStateChanged) {
			wasGaeEnabled = isGaeEnabled;

			// Update the GaeHelper type
			updateGaeHelper();

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
		Element additionalBuildCommandsElement = XmlUtils.findFirstElement("/project/build/plugins/plugin[artifactId = 'maven-eclipse-plugin']/configuration/additionalBuildcommands", root);
		Assert.notNull(additionalBuildCommandsElement, "additionalBuildCommands element of the maven-eclipse-plugin required");
		String gwtBuildCommandName = "com.google.gwt.eclipse.core.gwtProjectValidator";
		Element gwtBuildCommandElement = XmlUtils.findFirstElement("buildCommand[name = '" + gwtBuildCommandName + "']", additionalBuildCommandsElement);
		if (gwtBuildCommandElement == null) {
			Element nameElement = document.createElement("name");
			nameElement.setTextContent(gwtBuildCommandName);
			gwtBuildCommandElement = document.createElement("buildCommand");
			gwtBuildCommandElement.appendChild(nameElement);
			additionalBuildCommandsElement.appendChild(gwtBuildCommandElement);
		}

		// Add GWT projectnature
		Element additionalProjectNaturesElement = XmlUtils.findFirstElement("/project/build/plugins/plugin[artifactId = 'maven-eclipse-plugin']/configuration/additionalProjectnatures", root);
		Assert.notNull(additionalProjectNaturesElement, "additionalProjectnatures element of the maven-eclipse-plugin required");
		String gwtProjectNatureName = "com.google.gwt.eclipse.core.gwtNature";
		Element gwtProjectNatureElement = XmlUtils.findFirstElement("projectnature[name = '" + gwtProjectNatureName + "']", additionalProjectNaturesElement);
		if (gwtProjectNatureElement == null) {
			gwtProjectNatureElement = new XmlElementBuilder("projectnature", document).setText(gwtProjectNatureName).build();
			additionalProjectNaturesElement.appendChild(gwtProjectNatureElement);
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

		WebXmlUtils.addServlet("requestFactory", "com.google.web.bindery.requestfactory.server.RequestFactoryServlet", "/gwtRequest", null, webXml, null);
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

	private void updateGaeHelper() {
		String sourceAntPath = "module/client/scaffold/gae/GaeHelper-template.java";
		String segmentPackage = "client.scaffold.gae";
		String targetDirectory = projectOperations.getProjectMetadata().getPathResolver().getIdentifier(Path.SRC_MAIN_JAVA, projectOperations.getProjectMetadata().getTopLevelPackage().getFullyQualifiedPackageName().replace('.', File.separatorChar) + File.separator + "client" + File.separator + "scaffold" + File.separator + "gae");
		updateFile(sourceAntPath, targetDirectory, segmentPackage, true);
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
		updateFile(sourceAntPath, targetDirectory, gwtPath.segmentPackage(), false);
	}

	private void deleteUntouchedSetupFiles(String sourceAntPath, String targetDirectory) {
		if (!targetDirectory.endsWith(File.separator)) {
			targetDirectory += File.separator;
		}
		if (!fileManager.exists(targetDirectory)) {
			fileManager.createDirectory(targetDirectory);
		}

		String path = TemplateUtils.getTemplatePath(getClass(), sourceAntPath);
		final Iterable<URL> uris = OSGiUtils.findEntriesByPattern(context.getBundleContext(), path);
		Assert.notNull(uris, "Could not search bundles for resources for Ant Path '" + path + "'");

		for (final URL url : uris) {
			String fileName = url.getPath().substring(url.getPath().lastIndexOf('/') + 1);
			fileName = fileName.replace("-template", "");
			String targetFilename = targetDirectory + fileName;
			if (!fileManager.exists(targetFilename)) {
				continue;
			}
			try {
				String input = FileCopyUtils.copyToString(new InputStreamReader(url.openStream()));
				input = processTemplate(input, null);
				String existing = FileCopyUtils.copyToString(new File(targetFilename));
				if (existing.equals(input)) {
					fileManager.delete(targetFilename);
				}
			} catch (IOException ignored) {}
		}
	}

	private void updateFile(String sourceAntPath, String targetDirectory, String segmentPackage, boolean overwrite) {
		if (!targetDirectory.endsWith(File.separator)) {
			targetDirectory += File.separator;
		}
		if (!fileManager.exists(targetDirectory)) {
			fileManager.createDirectory(targetDirectory);
		}

		String path = TemplateUtils.getTemplatePath(getClass(), sourceAntPath);
		final Iterable<URL> urls = OSGiUtils.findEntriesByPattern(context.getBundleContext(), path);
		Assert.notNull(urls, "Could not search bundles for resources for Ant Path '" + path + "'");

		for (final URL url : urls) {
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

					input = processTemplate(input, segmentPackage);

					// Output the file for the user
					fileManager.createOrUpdateTextFileIfRequired(targetFilename, input, true);
				}
			} catch (IOException e) {
				throw new IllegalStateException("Unable to create '" + targetFilename + "'", e);
			}
		}
	}

	private String processTemplate(String input, String segmentPackage) {
		if (segmentPackage == null) {
			segmentPackage = "";
		}
		String topLevelPackage = projectOperations.getProjectMetadata().getTopLevelPackage().getFullyQualifiedPackageName();
		input = input.replace("__TOP_LEVEL_PACKAGE__", topLevelPackage);
		input = input.replace("__SEGMENT_PACKAGE__", segmentPackage);
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
		return input;
	}

	private void updateBuildPlugins(boolean isGaeEnabled) {
		Element configuration = XmlUtils.getConfiguration(getClass());
		String xPath = "/configuration/" + (isGaeEnabled ? "gae" : "gwt") + "/plugins/plugin";
		Element pluginElement = XmlUtils.findFirstElement(xPath, configuration);
		Assert.notNull(pluginElement, "gwt-maven-plugin required");
		final Plugin defaultPlugin = new Plugin(pluginElement);
		for (Plugin plugin : projectOperations.getProjectMetadata().getBuildPlugins()) {
			if ("gwt-maven-plugin".equals(plugin.getArtifactId()) && defaultPlugin.equals(plugin)) {
				// The GWT Maven plugin is already in the POM with the correct configuration
				return;
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
