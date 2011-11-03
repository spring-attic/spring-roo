package org.springframework.roo.addon.gwt;

import static org.springframework.roo.model.RooJavaType.ROO_GWT_MIRRORED_FROM;
import static org.springframework.roo.model.RooJavaType.ROO_GWT_PROXY;
import static org.springframework.roo.model.RooJavaType.ROO_GWT_REQUEST;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ACTIVE_RECORD;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ENTITY;

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
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Repository;
import org.springframework.roo.support.osgi.OSGiUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.FileUtils;
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

	// Constants
	private static final String GWT_BUILD_COMMAND = "com.google.gwt.eclipse.core.gwtProjectValidator";
	private static final String GWT_PROJECT_NATURE = "com.google.gwt.eclipse.core.gwtNature";
	private static final String MAVEN_ECLIPSE_PLUGIN = "/project/build/plugins/plugin[artifactId = 'maven-eclipse-plugin']";
	private static final String OUTPUT_DIRECTORY = "${project.build.directory}/${project.build.finalName}/WEB-INF/classes";

	// Fields
	@Reference protected FileManager fileManager;
	@Reference protected GwtTemplateService gwtTemplateService;
	@Reference protected GwtTypeService gwtTypeService;
	@Reference protected MetadataService metadataService;
	@Reference protected WebMvcOperations webMvcOperations;
	@Reference protected PersistenceMemberLocator persistenceMemberLocator;
	@Reference protected ProjectOperations projectOperations;
	@Reference protected TypeLocationService typeLocationService;
	@Reference protected TypeManagementService typeManagementService;

	private ComponentContext context;
	private Boolean wasGaeEnabled;

	protected void activate(final ComponentContext context) {
		this.context = context;
	}

	public boolean isSetupAvailable() {
		return !isGwtEnabled() && fileManager.exists(projectOperations.getPathResolver().getFocusedIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml"));
	}

	public boolean isGwtEnabled() {
		return projectOperations.isFocusedProjectAvailable() && projectOperations.isGwtEnabled(projectOperations.getFocusedModuleName());
	}

	public boolean isGaeEnabled() {
		return projectOperations.isFocusedProjectAvailable() && projectOperations.isGaeEnabled(projectOperations.getFocusedModuleName());
	}

	public void setup() {
		// Install web pieces if not already installed
		if (!fileManager.exists(projectOperations.getPathResolver().getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml"))) {
			webMvcOperations.installAllWebMvcArtifacts();
		}

		String topPackageName = projectOperations.getTopLevelPackage(projectOperations.getFocusedModuleName()).getFullyQualifiedPackageName();
		Set<FileDetails> gwtConfigs = fileManager.findMatchingAntPath(projectOperations.getPathResolver().getFocusedRoot(Path.SRC_MAIN_JAVA) + File.separatorChar + topPackageName.replace('.', File.separatorChar) + File.separator + "*.gwt.xml");
		boolean gwtAlreadySetup = !gwtConfigs.isEmpty();

		if (!gwtAlreadySetup) {
			String sourceAntPath = "setup/*";
			String targetDirectory = projectOperations.getPathResolver().getFocusedIdentifier(Path.SRC_MAIN_JAVA, topPackageName.replace('.', File.separatorChar));
			updateFile(sourceAntPath, targetDirectory, "", false);

			sourceAntPath = "setup/client/*";
			updateFile(sourceAntPath, targetDirectory + "/client", "", false);
		}

		for (ClassOrInterfaceTypeDetails proxyOrRequest : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(ROO_GWT_MIRRORED_FROM)) {
			ClassOrInterfaceTypeDetailsBuilder builder = new ClassOrInterfaceTypeDetailsBuilder(proxyOrRequest);
			if (proxyOrRequest.extendsType(GwtUtils.ENTITY_PROXY) || proxyOrRequest.extendsType(GwtUtils.OLD_ENTITY_PROXY)) {
				AnnotationMetadata annotationMetadata = MemberFindingUtils.getAnnotationOfType(proxyOrRequest.getAnnotations(), ROO_GWT_MIRRORED_FROM);
				if (annotationMetadata != null) {
					AnnotationMetadataBuilder annotationMetadataBuilder = new AnnotationMetadataBuilder(annotationMetadata);
					annotationMetadataBuilder.setAnnotationType(ROO_GWT_PROXY);
					builder.removeAnnotation(ROO_GWT_MIRRORED_FROM);
					builder.addAnnotation(annotationMetadataBuilder);
					typeManagementService.createOrUpdateTypeOnDisk(builder.build());
				}
			} else if (proxyOrRequest.extendsType(GwtUtils.REQUEST_CONTEXT) || proxyOrRequest.extendsType(GwtUtils.OLD_REQUEST_CONTEXT)) {
				AnnotationMetadata annotationMetadata = MemberFindingUtils.getAnnotationOfType(proxyOrRequest.getAnnotations(), ROO_GWT_MIRRORED_FROM);
				if (annotationMetadata != null) {
					AnnotationMetadataBuilder annotationMetadataBuilder = new AnnotationMetadataBuilder(annotationMetadata);
					annotationMetadataBuilder.setAnnotationType(ROO_GWT_REQUEST);
					builder.removeAnnotation(ROO_GWT_MIRRORED_FROM);
					builder.addAnnotation(annotationMetadataBuilder);
					typeManagementService.createOrUpdateTypeOnDisk(builder.build());
				}
			}
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

		// Update web.xml
		updateWebXml();

		// Update gwt-maven-plugin and others
		updateBuildPlugins(projectOperations.isGaeEnabled(projectOperations.getFocusedModuleName()));
	}

	public void proxyAll(final JavaPackage proxyPackage) {
		for (ClassOrInterfaceTypeDetails entity : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(ROO_JPA_ENTITY, ROO_JPA_ACTIVE_RECORD)) {
			createProxy(entity, proxyPackage);
		}
		copyDirectoryContents(GwtPath.LOCATOR);
	}

	public void proxyType(final JavaPackage proxyPackage, final JavaType type) {
		ClassOrInterfaceTypeDetails entity = typeLocationService.getTypeDetails(type);
		if (entity != null) {
			createProxy(entity, proxyPackage);
		}
		copyDirectoryContents(GwtPath.LOCATOR);
	}

	public void requestAll(final JavaPackage proxyPackage) {
		for (ClassOrInterfaceTypeDetails entity : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(ROO_JPA_ENTITY, ROO_JPA_ACTIVE_RECORD)) {
			createRequest(entity, proxyPackage);
		}
	}

	public void requestType(final JavaPackage requestPackage, final JavaType type) {
		ClassOrInterfaceTypeDetails entity = typeLocationService.getTypeDetails(type);
		if (entity != null) {
			createRequest(entity, requestPackage);
		}
	}

	public void proxyAndRequestAll(final JavaPackage proxyAndRequestPackage) {
		proxyAll(proxyAndRequestPackage);
		requestAll(proxyAndRequestPackage);
	}

	public void proxyAndRequestType(final JavaPackage proxyAndRequestPackage, final JavaType type) {
		proxyType(proxyAndRequestPackage, type);
		requestType(proxyAndRequestPackage, type);
	}

	public void scaffoldAll(final JavaPackage proxyPackage, final JavaPackage requestPackage) {
		updateScaffoldBoilerPlate();
		proxyAll(proxyPackage);
		requestAll(requestPackage);
		for (ClassOrInterfaceTypeDetails proxy : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(ROO_GWT_PROXY)) {
			ClassOrInterfaceTypeDetails request = gwtTypeService.lookupRequestFromProxy(proxy);
			if (request == null) {
				throw new IllegalStateException("In order to scaffold, an entity must have a request");
			}
			createScaffold(proxy);
		}
	}

	public void scaffoldType(final JavaPackage proxyPackage, final JavaPackage requestPackage, final JavaType type) {
		proxyType(proxyPackage, type);
		requestType(requestPackage, type);
		ClassOrInterfaceTypeDetails entity = typeLocationService.getTypeDetails(type);
		if (entity != null) {
			ClassOrInterfaceTypeDetails proxy = gwtTypeService.lookupProxyFromEntity(entity);
			ClassOrInterfaceTypeDetails request = gwtTypeService.lookupRequestFromEntity(entity);
			if (proxy == null || request == null) {
				throw new IllegalStateException("In order to scaffold, an entity must have an associated proxy and request");
			}
			updateScaffoldBoilerPlate();
			createScaffold(proxy);
		}
	}

	public void updateGaeConfiguration() {
		boolean isGaeEnabled = projectOperations.isGaeEnabled(projectOperations.getFocusedModuleName());
		boolean hasGaeStateChanged = wasGaeEnabled == null || isGaeEnabled != wasGaeEnabled;
		if (!projectOperations.isGwtEnabled(projectOperations.getFocusedModuleName()) || !hasGaeStateChanged) {
			return;
		}
		
		wasGaeEnabled = isGaeEnabled;

		// Update the GaeHelper type
		updateGaeHelper();

		gwtTypeService.buildType(GwtType.APP_REQUEST_FACTORY, gwtTemplateService.getStaticTemplateTypeDetails(GwtType.APP_REQUEST_FACTORY, projectOperations.getFocusedProjectMetadata()), projectOperations.getFocusedModuleName());

		// Ensure the gwt-maven-plugin appropriate to a GAE enabled or disabled environment is updated
		updateBuildPlugins(isGaeEnabled);

		// If there is a class that could possibly import from the appengine sdk, denoted here as having Gae in the type name,
		// then we need to add the appengine-api-1.0-sdk dependency to the pom.xml file
		String rootPath = projectOperations.getPathResolver().getFocusedRoot(Path.ROOT);
		Set<FileDetails> files = fileManager.findMatchingAntPath(rootPath + "/**/*Gae*.java");
		if (!files.isEmpty()) {
			Element configuration = XmlUtils.getConfiguration(getClass());
			Element gaeDependency = XmlUtils.findFirstElement("/configuration/gae/dependencies/dependency", configuration);
			projectOperations.addDependency(projectOperations.getFocusedModuleName(), new Dependency(gaeDependency));
		}

		// Copy across any missing files, only if GAE state has changed and is now enabled
		if (isGaeEnabled) {
			copyDirectoryContents();
		}
	}

	private void addPackageToGwtXml(String packageName) {
		String gwtConfig = gwtTypeService.getGwtModuleXml(projectOperations.getFocusedModuleName());
		gwtConfig = FileUtils.removeTrailingSeparator(gwtConfig).substring(0, gwtConfig.lastIndexOf(File.separator));
		String moduleRoot = projectOperations.getPathResolver().getFocusedRoot(Path.SRC_MAIN_JAVA);
		String topLevelPackage = gwtConfig.replaceAll(FileUtils.ensureTrailingSeparator(moduleRoot), "").replaceAll(File.separator, ".");
		String sourcePath = packageName.replaceAll(topLevelPackage + ".", "");
		gwtTypeService.addSourcePath(sourcePath, projectOperations.getFocusedModuleName());
	}

	private void createProxy(final ClassOrInterfaceTypeDetails entity, final JavaPackage destinationPackage) {
		ClassOrInterfaceTypeDetails existingProxy = gwtTypeService.lookupProxyFromEntity(entity);
		if (existingProxy != null) {
			return;
		}
		JavaType proxyName = new JavaType(destinationPackage.getFullyQualifiedPackageName() + "." + entity.getName().getSimpleTypeName() + "Proxy");
		ClassOrInterfaceTypeDetailsBuilder builder = new ClassOrInterfaceTypeDetailsBuilder(PhysicalTypeIdentifier.createIdentifier(proxyName));
		builder.setName(proxyName);
		builder.setExtendsTypes(Collections.singletonList(GwtUtils.ENTITY_PROXY));
		builder.setPhysicalTypeCategory(PhysicalTypeCategory.INTERFACE);
		builder.setModifier(Modifier.PUBLIC);
		List<AnnotationAttributeValue<?>> attributeValues = new ArrayList<AnnotationAttributeValue<?>>();
		StringAttributeValue stringAttributeValue = new StringAttributeValue(new JavaSymbolName("value"), entity.getName().getFullyQualifiedTypeName());
		attributeValues.add(stringAttributeValue);
		String locator = projectOperations.getTopLevelPackage(projectOperations.getFocusedModuleName()) + ".server.locator." + entity.getName().getSimpleTypeName() + "Locator";
		StringAttributeValue locatorAttributeValue = new StringAttributeValue(new JavaSymbolName("locator"), locator);
		attributeValues.add(locatorAttributeValue);
		builder.updateTypeAnnotation(new AnnotationMetadataBuilder(GwtUtils.PROXY_FOR_NAME, attributeValues));
		attributeValues.remove(locatorAttributeValue);
		List<StringAttributeValue> readOnlyValues = new ArrayList<StringAttributeValue>();
		FieldMetadata versionField = persistenceMemberLocator.getVersionField(entity.getName());
		if (versionField != null) {
			readOnlyValues.add(new StringAttributeValue(new JavaSymbolName("value"), versionField.getFieldName().getSymbolName()));
		}
		List<FieldMetadata> idFields = persistenceMemberLocator.getIdentifierFields(entity.getName());
		if (idFields != null && !idFields.isEmpty()) {
			readOnlyValues.add(new StringAttributeValue(new JavaSymbolName("value"), idFields.get(0).getFieldName().getSymbolName()));
		}
		ArrayAttributeValue<StringAttributeValue> readOnlyAttribute = new ArrayAttributeValue<StringAttributeValue>(new JavaSymbolName("readOnly"), readOnlyValues);
		attributeValues.add(readOnlyAttribute);
		builder.updateTypeAnnotation(new AnnotationMetadataBuilder(ROO_GWT_PROXY, attributeValues));
		typeManagementService.createOrUpdateTypeOnDisk(builder.build());
		addPackageToGwtXml(destinationPackage.getFullyQualifiedPackageName());
	}

	private void createRequest(final ClassOrInterfaceTypeDetails entity, final JavaPackage destinationPackage) {
		ClassOrInterfaceTypeDetails existingProxy = gwtTypeService.lookupRequestFromEntity(entity);
		if (existingProxy != null) {
			return;
		}
		JavaType proxyName = new JavaType(destinationPackage.getFullyQualifiedPackageName() + "." + entity.getName().getSimpleTypeName() + "Request");
		ClassOrInterfaceTypeDetailsBuilder builder = new ClassOrInterfaceTypeDetailsBuilder(PhysicalTypeIdentifier.createIdentifier(proxyName));
		builder.setName(proxyName);
		builder.setExtendsTypes(Collections.singletonList(GwtUtils.REQUEST_CONTEXT));
		builder.setPhysicalTypeCategory(PhysicalTypeCategory.INTERFACE);
		builder.setModifier(Modifier.PUBLIC);
		List<AnnotationAttributeValue<?>> attributeValues = new ArrayList<AnnotationAttributeValue<?>>();
		StringAttributeValue stringAttributeValue = new StringAttributeValue(new JavaSymbolName("value"), entity.getName().getFullyQualifiedTypeName());
		attributeValues.add(stringAttributeValue);
		builder.updateTypeAnnotation(new AnnotationMetadataBuilder(GwtUtils.SERVICE_NAME, attributeValues));
		List<StringAttributeValue> toExclude = new ArrayList<StringAttributeValue>();
		toExclude.add(new StringAttributeValue(new JavaSymbolName("value"), "entityManager"));
		toExclude.add(new StringAttributeValue(new JavaSymbolName("value"), "toString"));
		toExclude.add(new StringAttributeValue(new JavaSymbolName("value"), "merge"));
		toExclude.add(new StringAttributeValue(new JavaSymbolName("value"), "flush"));
		toExclude.add(new StringAttributeValue(new JavaSymbolName("value"), "clear"));
		ArrayAttributeValue<StringAttributeValue> exclude = new ArrayAttributeValue<StringAttributeValue>(new JavaSymbolName("exclude"), toExclude);
		attributeValues.add(exclude);
		builder.updateTypeAnnotation(new AnnotationMetadataBuilder(ROO_GWT_REQUEST, attributeValues));
		typeManagementService.createOrUpdateTypeOnDisk(builder.build());
		addPackageToGwtXml(destinationPackage.getFullyQualifiedPackageName());
	}

	private void createScaffold(final ClassOrInterfaceTypeDetails proxy) {
		AnnotationMetadata annotationMetadata = GwtUtils.getFirstAnnotation(proxy, ROO_GWT_PROXY);
		if (annotationMetadata != null) {
			AnnotationAttributeValue<Boolean> booleanAttributeValue = annotationMetadata.getAttribute("scaffold");
			if (booleanAttributeValue == null || !booleanAttributeValue.getValue()) {
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
	}

	private void updateScaffoldBoilerPlate() {
		String targetDirectory = projectOperations.getPathResolver().getFocusedIdentifier(Path.SRC_MAIN_JAVA, projectOperations.getTopLevelPackage(projectOperations.getFocusedModuleName()).getFullyQualifiedPackageName().replace('.', File.separatorChar));
		deleteUntouchedSetupFiles("setup/*", targetDirectory);
		deleteUntouchedSetupFiles("setup/client/*", targetDirectory + "/client");
		copyDirectoryContents();
		updateGaeHelper();
	}

	/**
	 * Updates the Eclipse plugin in the POM with the necessary GWT details
	 */
	private void updateEclipsePlugin() {
		// Load the POM
		final String pom = projectOperations.getPathResolver().getFocusedIdentifier(Path.ROOT, "pom.xml");
		final Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		final Element root = document.getDocumentElement();

		// Add the GWT "buildCommand"
		final Element additionalBuildCommandsElement = XmlUtils.findFirstElement(MAVEN_ECLIPSE_PLUGIN + "/configuration/additionalBuildcommands", root);
		Assert.notNull(additionalBuildCommandsElement, "additionalBuildcommands element of the maven-eclipse-plugin required");
		Element gwtBuildCommandElement = XmlUtils.findFirstElement("buildCommand[name = '" + GWT_BUILD_COMMAND + "']", additionalBuildCommandsElement);
		if (gwtBuildCommandElement == null) {
			gwtBuildCommandElement = DomUtils.createChildElement("buildCommand", additionalBuildCommandsElement, document);
			final Element nameElement = DomUtils.createChildElement("name", gwtBuildCommandElement, document);
			nameElement.setTextContent(GWT_BUILD_COMMAND);
		}

		// Add the GWT "projectnature"
		Element additionalProjectNaturesElement = XmlUtils.findFirstElement(MAVEN_ECLIPSE_PLUGIN + "/configuration/additionalProjectnatures", root);
		Assert.notNull(additionalProjectNaturesElement, "additionalProjectnatures element of the maven-eclipse-plugin required");
		Element gwtProjectNatureElement = XmlUtils.findFirstElement("projectnature[name = '" + GWT_PROJECT_NATURE + "']", additionalProjectNaturesElement);
		if (gwtProjectNatureElement == null) {
			gwtProjectNatureElement = new XmlElementBuilder("projectnature", document).setText(GWT_PROJECT_NATURE).build();
			additionalProjectNaturesElement.appendChild(gwtProjectNatureElement);
		}

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), false);
	}

	/**
	 * Sets the POM's output directory to {@value #OUTPUT_DIRECTORY}, if it's not already set to something else.
	 */
	private void updateBuildOutputDirectory() {
		// Read the POM
		final String pom = projectOperations.getPathResolver().getFocusedIdentifier(Path.ROOT, "pom.xml");
		final Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		final Element root = document.getDocumentElement();

		Element outputDirectoryElement = XmlUtils.findFirstElement("/project/build/outputDirectory", root);
		if (outputDirectoryElement == null) {
			// Create it
			final Element buildElement = XmlUtils.findRequiredElement("/project/build", root);
			outputDirectoryElement = DomUtils.createChildElement("outputDirectory", buildElement, document);
		}
		outputDirectoryElement.setTextContent(OUTPUT_DIRECTORY);

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), false);
	}

	private void updateRepositories(final Element configuration) {
		List<Repository> repositories = new ArrayList<Repository>();

		List<Element> gwtRepositories = XmlUtils.findElements("/configuration/gwt/repositories/repository", configuration);
		for (Element repositoryElement : gwtRepositories) {
			repositories.add(new Repository(repositoryElement));
		}
		projectOperations.addRepositories(projectOperations.getFocusedModuleName(), repositories);

		repositories.clear();
		List<Element> gwtPluginRepositories = XmlUtils.findElements("/configuration/gwt/pluginRepositories/pluginRepository", configuration);
		for (Element repositoryElement : gwtPluginRepositories) {
			repositories.add(new Repository(repositoryElement));
		}
		projectOperations.addPluginRepositories(projectOperations.getFocusedModuleName(), repositories);
	}

	private void updateDependencies(final Element configuration) {
		List<Dependency> dependencies = new ArrayList<Dependency>();
		List<Element> gwtDependencies = XmlUtils.findElements("/configuration/gwt/dependencies/dependency", configuration);
		for (Element dependencyElement : gwtDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}
		projectOperations.removeDependencies(projectOperations.getFocusedModuleName(), dependencies);
		metadataService.evict(ProjectMetadata.getProjectIdentifier(projectOperations.getFocusedModuleName()));
		projectOperations.addDependencies(projectOperations.getFocusedModuleName(), dependencies);
	}

	private void updateWebXml() {
		String webXmlpath = projectOperations.getPathResolver().getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml");
		Document webXml = XmlUtils.readXml(fileManager.getInputStream(webXmlpath));
		Element root = webXml.getDocumentElement();

		WebXmlUtils.addServlet("requestFactory", projectOperations.getTopLevelPackage(projectOperations.getFocusedModuleName()) + ".server.CustomRequestFactoryServlet", "/gwtRequest", null, webXml, null);
		if (projectOperations.isGaeEnabled(projectOperations.getFocusedModuleName())) {
			WebXmlUtils.addFilter("GaeAuthFilter", GwtPath.SERVER_GAE.packageName(projectOperations.getTopLevelPackage(projectOperations.getFocusedModuleName())) + ".GaeAuthFilter", "/gwtRequest/*", webXml, "This filter makes GAE authentication services visible to a RequestFactory client.");
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

	private void updateGaeHelper() {
		String sourceAntPath = "module/client/scaffold/gae/GaeHelper-template.java";
		String segmentPackage = "client.scaffold.gae";
		String targetDirectory = projectOperations.getPathResolver().getFocusedIdentifier(Path.SRC_MAIN_JAVA, projectOperations.getTopLevelPackage(projectOperations.getFocusedModuleName()).getFullyQualifiedPackageName().replace('.', File.separatorChar) + File.separator + "client" + File.separator + "scaffold" + File.separator + "gae");
		updateFile(sourceAntPath, targetDirectory, segmentPackage, true);
	}

	private void copyDirectoryContents() {
		for (GwtPath path : GwtPath.values()) {
			copyDirectoryContents(path);
		}
	}

	private void copyDirectoryContents(final GwtPath gwtPath) {
		String sourceAntPath = gwtPath.getSourceAntPath();
		if (sourceAntPath.contains("gae") && !projectOperations.isGaeEnabled(projectOperations.getFocusedModuleName())) {
			return;
		}
		String targetDirectory = gwtPath.canonicalFileSystemPath(projectOperations);
		updateFile(sourceAntPath, targetDirectory, gwtPath.segmentPackage(), false);
	}

	private void deleteUntouchedSetupFiles(final String sourceAntPath, String targetDirectory) {
		if (!targetDirectory.endsWith(File.separator)) {
			targetDirectory += File.separator;
		}
		if (!fileManager.exists(targetDirectory)) {
			fileManager.createDirectory(targetDirectory);
		}

		String path = FileUtils.getPath(getClass(), sourceAntPath);
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
					// new File(targetFilename).delete();
					fileManager.delete(targetFilename);
				}
			} catch (IOException ignored) {
			}
		}
	}

	private void updateFile(final String sourceAntPath, String targetDirectory, final String segmentPackage, final boolean overwrite) {
		if (!targetDirectory.endsWith(File.separator)) {
			targetDirectory += File.separator;
		}
		if (!fileManager.exists(targetDirectory)) {
			fileManager.createDirectory(targetDirectory);
		}

		String path = FileUtils.getPath(getClass(), sourceAntPath);
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
		String topLevelPackage = projectOperations.getTopLevelPackage(projectOperations.getFocusedModuleName()).getFullyQualifiedPackageName();
		input = input.replace("__TOP_LEVEL_PACKAGE__", topLevelPackage);
		input = input.replace("__SEGMENT_PACKAGE__", segmentPackage);
		input = input.replace("__PROJECT_NAME__", projectOperations.getProjectName(projectOperations.getFocusedModuleName()));

		if (projectOperations.isGaeEnabled(projectOperations.getFocusedModuleName())) {
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

	private void updateBuildPlugins(final boolean isGaeEnabled) {
		// Update the POM
		final String xPathExpression = "/configuration/" + (isGaeEnabled ? "gae" : "gwt") + "/plugins/plugin";
		final List<Element> pluginElements = XmlUtils.findElements(xPathExpression, XmlUtils.getConfiguration(getClass()));
		for (Element pluginElement : pluginElements) {
			final Plugin defaultPlugin = new Plugin(pluginElement);
			for (Plugin plugin : projectOperations.getFocusedModule().getBuildPlugins()) {
				if ("gwt-maven-plugin".equals(plugin.getArtifactId()) ) {
					projectOperations.removeBuildPluginImmediately(projectOperations.getFocusedModuleName(), defaultPlugin);
					break;
				}
			}
			projectOperations.addBuildPlugin(projectOperations.getFocusedModuleName(), defaultPlugin);
		}
	}

	private void removeIfFound(final String xpath, final Element webXmlRoot) {
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
