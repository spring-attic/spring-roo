package org.springframework.roo.addon.gwt;

import static org.springframework.roo.addon.gwt.GwtJavaType.ENTITY_PROXY;
import static org.springframework.roo.addon.gwt.GwtJavaType.OLD_ENTITY_PROXY;
import static org.springframework.roo.addon.gwt.GwtJavaType.OLD_REQUEST_CONTEXT;
import static org.springframework.roo.addon.gwt.GwtJavaType.PROXY_FOR_NAME;
import static org.springframework.roo.addon.gwt.GwtJavaType.REQUEST_CONTEXT;
import static org.springframework.roo.addon.gwt.GwtJavaType.SERVICE_NAME;
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
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Repository;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.osgi.OSGiUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.CollectionUtils;
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
	private static final JavaSymbolName VALUE = new JavaSymbolName("value");
	private static final String GWT_BUILD_COMMAND = "com.google.gwt.eclipse.core.gwtProjectValidator";
	private static final String GWT_PROJECT_NATURE = "com.google.gwt.eclipse.core.gwtNature";
	private static final String MAVEN_ECLIPSE_PLUGIN = "/project/build/plugins/plugin[artifactId = 'maven-eclipse-plugin']";
	private static final String OUTPUT_DIRECTORY = "${project.build.directory}/${project.build.finalName}/WEB-INF/classes";
	private static final String[] REQUEST_METHODS_EXCLUDED_BY_DEFAULT = { "clear", "entityManager", "equals", "flush", "hashCode", "merge", "toString" };

	// Fields
	@Reference protected FileManager fileManager;
	@Reference protected GwtTemplateService gwtTemplateService;
	@Reference protected GwtTypeService gwtTypeService;
	@Reference protected MetadataService metadataService;
	@Reference protected PersistenceMemberLocator persistenceMemberLocator;
	@Reference protected ProjectOperations projectOperations;
	@Reference protected TypeLocationService typeLocationService;
	@Reference protected TypeManagementService typeManagementService;
	@Reference protected WebMvcOperations webMvcOperations;

	private ComponentContext context;
	private Boolean wasGaeEnabled;

	protected void activate(final ComponentContext context) {
		this.context = context;
	}

	public String getName() {
		return FeatureNames.GWT;
	}

	public boolean isInstalledInModule(final String moduleName) {
		final Pom pom = projectOperations.getPomFromModuleName(moduleName);
		if (pom == null) {
			return false;
		}
		for (final Plugin buildPlugin : pom.getBuildPlugins()) {
			if ("gwt-maven-plugin".equals(buildPlugin.getArtifactId())) {
				return true;
			}
		}
		return false;
	}

	public boolean isGwtInstallationPossible() {
		return projectOperations.isFocusedProjectAvailable() && !projectOperations.isFeatureInstalledInFocusedModule(FeatureNames.JSF);
	}

	public boolean isScaffoldAvailable() {
		return isGwtInstallationPossible() && isInstalledInModule(projectOperations.getFocusedModuleName());
	}

	public void setup() {
		// Install web pieces if not already installed
		if (!fileManager.exists(projectOperations.getPathResolver().getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml"))) {
			webMvcOperations.installAllWebMvcArtifacts();
		}

		final String topPackageName = projectOperations.getTopLevelPackage(projectOperations.getFocusedModuleName()).getFullyQualifiedPackageName();
		final Set<FileDetails> gwtConfigs = fileManager.findMatchingAntPath(projectOperations.getPathResolver().getFocusedRoot(Path.SRC_MAIN_JAVA) + File.separatorChar + topPackageName.replace('.', File.separatorChar) + File.separator + "*.gwt.xml");
		final boolean gwtAlreadySetup = !gwtConfigs.isEmpty();

		if (!gwtAlreadySetup) {
			String sourceAntPath = "setup/*";
			final String targetDirectory = projectOperations.getPathResolver().getFocusedIdentifier(Path.SRC_MAIN_JAVA, topPackageName.replace('.', File.separatorChar));
			updateFile(sourceAntPath, targetDirectory, "", false);

			sourceAntPath = "setup/client/*";
			updateFile(sourceAntPath, targetDirectory + "/client", "", false);
		}

		for (final ClassOrInterfaceTypeDetails proxyOrRequest : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(ROO_GWT_MIRRORED_FROM)) {
			final ClassOrInterfaceTypeDetailsBuilder builder = new ClassOrInterfaceTypeDetailsBuilder(proxyOrRequest);
			if (proxyOrRequest.extendsType(ENTITY_PROXY) || proxyOrRequest.extendsType(OLD_ENTITY_PROXY)) {
				final AnnotationMetadata annotationMetadata = MemberFindingUtils.getAnnotationOfType(proxyOrRequest.getAnnotations(), ROO_GWT_MIRRORED_FROM);
				if (annotationMetadata != null) {
					final AnnotationMetadataBuilder annotationMetadataBuilder = new AnnotationMetadataBuilder(annotationMetadata);
					annotationMetadataBuilder.setAnnotationType(ROO_GWT_PROXY);
					builder.removeAnnotation(ROO_GWT_MIRRORED_FROM);
					builder.addAnnotation(annotationMetadataBuilder);
					typeManagementService.createOrUpdateTypeOnDisk(builder.build());
				}
			} else if (proxyOrRequest.extendsType(REQUEST_CONTEXT) || proxyOrRequest.extendsType(OLD_REQUEST_CONTEXT)) {
				final AnnotationMetadata annotationMetadata = MemberFindingUtils.getAnnotationOfType(proxyOrRequest.getAnnotations(), ROO_GWT_MIRRORED_FROM);
				if (annotationMetadata != null) {
					final AnnotationMetadataBuilder annotationMetadataBuilder = new AnnotationMetadataBuilder(annotationMetadata);
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

		final Element configuration = XmlUtils.getConfiguration(getClass());

		// Add POM repositories
		updateRepositories(configuration);

		// Add dependencies
		updateDependencies(configuration);

		// Update web.xml
		updateWebXml();

		// Update gwt-maven-plugin and others
		updateBuildPlugins(projectOperations.isFeatureInstalledInFocusedModule(FeatureNames.GAE));
	}

	public void proxyAll(final JavaPackage proxyPackage) {
		for (final ClassOrInterfaceTypeDetails entity : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(ROO_JPA_ENTITY, ROO_JPA_ACTIVE_RECORD)) {
			createProxy(entity, proxyPackage);
		}
		copyDirectoryContents(GwtPath.LOCATOR);
	}

	public void proxyType(final JavaPackage proxyPackage, final JavaType type) {
		final ClassOrInterfaceTypeDetails entity = typeLocationService.getTypeDetails(type);
		if (entity != null) {
			createProxy(entity, proxyPackage);
		}
		copyDirectoryContents(GwtPath.LOCATOR);
	}

	public void requestAll(final JavaPackage proxyPackage) {
		for (final ClassOrInterfaceTypeDetails entity : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(ROO_JPA_ENTITY, ROO_JPA_ACTIVE_RECORD)) {
			createRequestInterface(entity, proxyPackage);
		}
	}

	public void requestType(final JavaPackage requestPackage, final JavaType type) {
		final ClassOrInterfaceTypeDetails entity = typeLocationService.getTypeDetails(type);
		if (entity != null) {
			createRequestInterface(entity, requestPackage);
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
		for (final ClassOrInterfaceTypeDetails proxy : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(ROO_GWT_PROXY)) {
			final ClassOrInterfaceTypeDetails request = gwtTypeService.lookupRequestFromProxy(proxy);
			if (request == null) {
				throw new IllegalStateException("In order to scaffold, an entity must have a request");
			}
			createScaffold(proxy);
		}
	}

	public void scaffoldType(final JavaPackage proxyPackage, final JavaPackage requestPackage, final JavaType type) {
		proxyType(proxyPackage, type);
		requestType(requestPackage, type);
		final ClassOrInterfaceTypeDetails entity = typeLocationService.getTypeDetails(type);
		if (entity != null && !Modifier.isAbstract(entity.getModifier())) {
			final ClassOrInterfaceTypeDetails proxy = gwtTypeService.lookupProxyFromEntity(entity);
			final ClassOrInterfaceTypeDetails request = gwtTypeService.lookupRequestFromEntity(entity);
			if (proxy == null || request == null) {
				throw new IllegalStateException("In order to scaffold, an entity must have an associated proxy and request");
			}
			updateScaffoldBoilerPlate();
			createScaffold(proxy);
		}
	}

	public void updateGaeConfiguration() {
		final boolean isGaeEnabled = projectOperations.isFeatureInstalledInFocusedModule(FeatureNames.GAE);
		final boolean hasGaeStateChanged = wasGaeEnabled == null || isGaeEnabled != wasGaeEnabled;
		if (!isInstalledInModule(projectOperations.getFocusedModuleName()) || !hasGaeStateChanged) {
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
		final String rootPath = projectOperations.getPathResolver().getFocusedRoot(Path.ROOT);
		final Set<FileDetails> files = fileManager.findMatchingAntPath(rootPath + "/**/*Gae*.java");
		if (!files.isEmpty()) {
			final Element configuration = XmlUtils.getConfiguration(getClass());
			final Element gaeDependency = XmlUtils.findFirstElement("/configuration/gae/dependencies/dependency", configuration);
			projectOperations.addDependency(projectOperations.getFocusedModuleName(), new Dependency(gaeDependency));
		}

		// Copy across any missing files, only if GAE state has changed and is now enabled
		if (isGaeEnabled) {
			copyDirectoryContents();
		}
	}

	private void addPackageToGwtXml(final String packageName) {
		String gwtConfig = gwtTypeService.getGwtModuleXml(projectOperations.getFocusedModuleName());
		gwtConfig = FileUtils.removeTrailingSeparator(gwtConfig).substring(0, gwtConfig.lastIndexOf(File.separator));
		final String moduleRoot = projectOperations.getPathResolver().getFocusedRoot(Path.SRC_MAIN_JAVA);
		final String topLevelPackage = gwtConfig.replace(FileUtils.ensureTrailingSeparator(moduleRoot), "").replace(File.separator, ".");
		final String sourcePath = packageName.replace(topLevelPackage + ".", "");
		gwtTypeService.addSourcePath(sourcePath, projectOperations.getFocusedModuleName());
	}

	private void createProxy(final ClassOrInterfaceTypeDetails entity, final JavaPackage destinationPackage) {
		final ClassOrInterfaceTypeDetails existingProxy = gwtTypeService.lookupProxyFromEntity(entity);
		if (existingProxy != null || Modifier.isAbstract(entity.getModifier())) {
			return;
		}
		
		final JavaType proxyType = new JavaType(destinationPackage.getFullyQualifiedPackageName() + "." + entity.getName().getSimpleTypeName() + "Proxy");
		final String focusedModule = projectOperations.getFocusedModuleName();
		final LogicalPath proxyLogicalPath = LogicalPath.getInstance(Path.SRC_MAIN_JAVA, focusedModule);
		final ClassOrInterfaceTypeDetailsBuilder builder = new ClassOrInterfaceTypeDetailsBuilder(PhysicalTypeIdentifier.createIdentifier(proxyType, proxyLogicalPath));
		builder.setName(proxyType);
		builder.setExtendsTypes(Collections.singletonList(ENTITY_PROXY));
		builder.setPhysicalTypeCategory(PhysicalTypeCategory.INTERFACE);
		builder.setModifier(Modifier.PUBLIC);
		final List<AnnotationAttributeValue<?>> attributeValues = new ArrayList<AnnotationAttributeValue<?>>();
		final StringAttributeValue stringAttributeValue = new StringAttributeValue(VALUE, entity.getName().getFullyQualifiedTypeName());
		attributeValues.add(stringAttributeValue);
		final String locator = projectOperations.getTopLevelPackage(focusedModule) + ".server.locator." + entity.getName().getSimpleTypeName() + "Locator";
		final StringAttributeValue locatorAttributeValue = new StringAttributeValue(new JavaSymbolName("locator"), locator);
		attributeValues.add(locatorAttributeValue);
		builder.updateTypeAnnotation(new AnnotationMetadataBuilder(PROXY_FOR_NAME, attributeValues));
		attributeValues.remove(locatorAttributeValue);
		final List<StringAttributeValue> readOnlyValues = new ArrayList<StringAttributeValue>();
		final FieldMetadata versionField = persistenceMemberLocator.getVersionField(entity.getName());
		if (versionField != null) {
			readOnlyValues.add(new StringAttributeValue(VALUE, versionField.getFieldName().getSymbolName()));
		}
		final List<FieldMetadata> idFields = persistenceMemberLocator.getIdentifierFields(entity.getName());
		if (!CollectionUtils.isEmpty(idFields)) {
			readOnlyValues.add(new StringAttributeValue(VALUE, idFields.get(0).getFieldName().getSymbolName()));
		}
		final ArrayAttributeValue<StringAttributeValue> readOnlyAttribute = new ArrayAttributeValue<StringAttributeValue>(new JavaSymbolName("readOnly"), readOnlyValues);
		attributeValues.add(readOnlyAttribute);
		builder.updateTypeAnnotation(new AnnotationMetadataBuilder(ROO_GWT_PROXY, attributeValues));
		typeManagementService.createOrUpdateTypeOnDisk(builder.build());
		addPackageToGwtXml(destinationPackage.getFullyQualifiedPackageName());
	}
	
	private void createRequestInterface(final ClassOrInterfaceTypeDetails entity, final JavaPackage destinationPackage) {
		final ClassOrInterfaceTypeDetails existingProxy = gwtTypeService.lookupRequestFromEntity(entity);
		if (existingProxy != null || Modifier.isAbstract(entity.getModifier())) {
			return;
		}
		final JavaType requestType = new JavaType(destinationPackage.getFullyQualifiedPackageName() + "." + entity.getType().getSimpleTypeName() + "Request");
		final LogicalPath focusedSrcMainJava = LogicalPath.getInstance(Path.SRC_MAIN_JAVA, projectOperations.getFocusedModuleName());
		final ClassOrInterfaceTypeDetailsBuilder builder = new ClassOrInterfaceTypeDetailsBuilder(PhysicalTypeIdentifier.createIdentifier(requestType, focusedSrcMainJava));
		builder.setName(requestType);
		builder.setExtendsTypes(Collections.singletonList(REQUEST_CONTEXT));
		builder.setPhysicalTypeCategory(PhysicalTypeCategory.INTERFACE);
		builder.setModifier(Modifier.PUBLIC);
		annotateRequestInterface(entity, builder);
		typeManagementService.createOrUpdateTypeOnDisk(builder.build());
		addPackageToGwtXml(destinationPackage.getFullyQualifiedPackageName());
	}

	private void annotateRequestInterface(final ClassOrInterfaceTypeDetails entity, final ClassOrInterfaceTypeDetailsBuilder builder) {
		final List<AnnotationAttributeValue<?>> attributeValues = new ArrayList<AnnotationAttributeValue<?>>();
		
		// @ServiceName annotation
		final StringAttributeValue stringAttributeValue = new StringAttributeValue(VALUE, entity.getType().getFullyQualifiedTypeName());
		attributeValues.add(stringAttributeValue);
		builder.updateTypeAnnotation(new AnnotationMetadataBuilder(SERVICE_NAME, attributeValues));
		
		// @RooGwtRequest annotation
		final List<StringAttributeValue> toExclude = new ArrayList<StringAttributeValue>();
		for (final String excludedMethod : REQUEST_METHODS_EXCLUDED_BY_DEFAULT) {
			toExclude.add(new StringAttributeValue(VALUE, excludedMethod));
		}
		final ArrayAttributeValue<StringAttributeValue> exclude = new ArrayAttributeValue<StringAttributeValue>(new JavaSymbolName("exclude"), toExclude);
		attributeValues.add(exclude);
		builder.updateTypeAnnotation(new AnnotationMetadataBuilder(ROO_GWT_REQUEST, attributeValues));
	}

	private void createScaffold(final ClassOrInterfaceTypeDetails proxy) {
		final AnnotationMetadata annotationMetadata = GwtUtils.getFirstAnnotation(proxy, ROO_GWT_PROXY);
		if (annotationMetadata != null) {
			final AnnotationAttributeValue<Boolean> booleanAttributeValue = annotationMetadata.getAttribute("scaffold");
			if (booleanAttributeValue == null || !booleanAttributeValue.getValue()) {
				final ClassOrInterfaceTypeDetailsBuilder proxyBuilder = new ClassOrInterfaceTypeDetailsBuilder(proxy);
				final AnnotationMetadataBuilder annotationMetadataBuilder = new AnnotationMetadataBuilder(annotationMetadata);
				annotationMetadataBuilder.addBooleanAttribute("scaffold", true);
				for (final AnnotationMetadataBuilder existingAnnotation : proxyBuilder.getAnnotations()) {
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
		final String targetDirectory = projectOperations.getPathResolver().getFocusedIdentifier(Path.SRC_MAIN_JAVA, projectOperations.getTopLevelPackage(projectOperations.getFocusedModuleName()).getFullyQualifiedPackageName().replace('.', File.separatorChar));
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
		final String pom = getPomPath();
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
		final Element additionalProjectNaturesElement = XmlUtils.findFirstElement(MAVEN_ECLIPSE_PLUGIN + "/configuration/additionalProjectnatures", root);
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
		final String pom = getPomPath();
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

	private String getPomPath() {
		return projectOperations.getPathResolver().getFocusedIdentifier(Path.ROOT, "pom.xml");
	}

	private void updateRepositories(final Element configuration) {
		final List<Repository> repositories = new ArrayList<Repository>();

		final List<Element> gwtRepositories = XmlUtils.findElements("/configuration/gwt/repositories/repository", configuration);
		for (final Element repositoryElement : gwtRepositories) {
			repositories.add(new Repository(repositoryElement));
		}
		projectOperations.addRepositories(projectOperations.getFocusedModuleName(), repositories);

		repositories.clear();
		final List<Element> gwtPluginRepositories = XmlUtils.findElements("/configuration/gwt/pluginRepositories/pluginRepository", configuration);
		for (final Element repositoryElement : gwtPluginRepositories) {
			repositories.add(new Repository(repositoryElement));
		}
		projectOperations.addPluginRepositories(projectOperations.getFocusedModuleName(), repositories);
	}

	private void updateDependencies(final Element configuration) {
		final List<Dependency> dependencies = new ArrayList<Dependency>();
		final List<Element> gwtDependencies = XmlUtils.findElements("/configuration/gwt/dependencies/dependency", configuration);
		for (final Element dependencyElement : gwtDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}
		projectOperations.removeDependencies(projectOperations.getFocusedModuleName(), dependencies);
		metadataService.evict(ProjectMetadata.getProjectIdentifier(projectOperations.getFocusedModuleName()));
		projectOperations.addDependencies(projectOperations.getFocusedModuleName(), dependencies);
	}

	private void updateWebXml() {
		final String webXmlpath = projectOperations.getPathResolver().getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml");
		final Document webXml = XmlUtils.readXml(fileManager.getInputStream(webXmlpath));
		final Element root = webXml.getDocumentElement();

		WebXmlUtils.addServlet("requestFactory", projectOperations.getTopLevelPackage(projectOperations.getFocusedModuleName()) + ".server.CustomRequestFactoryServlet", "/gwtRequest", null, webXml, null);
		if (projectOperations.isFeatureInstalledInFocusedModule(FeatureNames.GAE)) {
			WebXmlUtils.addFilter("GaeAuthFilter", GwtPath.SERVER_GAE.packageName(projectOperations.getTopLevelPackage(projectOperations.getFocusedModuleName())) + ".GaeAuthFilter", "/gwtRequest/*", webXml, "This filter makes GAE authentication services visible to a RequestFactory client.");
			final String displayName = "Redirect to the login page if needed before showing any html pages";
			final WebXmlUtils.WebResourceCollection webResourceCollection = new WebXmlUtils.WebResourceCollection("Login required", null, Collections.singletonList("*.html"), new ArrayList<String>());
			final ArrayList<String> roleNames = new ArrayList<String>();
			roleNames.add("*");
			final String userDataConstraint = null;
			WebXmlUtils.addSecurityConstraint(displayName, Collections.singletonList(webResourceCollection), roleNames, userDataConstraint, webXml, null);
		} else {
			final Element filter = XmlUtils.findFirstElement("/web-app/filter[filter-name = 'GaeAuthFilter']", root);
			if (filter != null) {
				filter.getParentNode().removeChild(filter);
			}
			final Element filterMapping = XmlUtils.findFirstElement("/web-app/filter-mapping[filter-name = 'GaeAuthFilter']", root);
			if (filterMapping != null) {
				filterMapping.getParentNode().removeChild(filterMapping);
			}
			final Element securityConstraint = XmlUtils.findFirstElement("security-constraint", root);
			if (securityConstraint != null) {
				securityConstraint.getParentNode().removeChild(securityConstraint);
			}
		}

		removeIfFound("/web-app/error-page", root);

		fileManager.createOrUpdateTextFileIfRequired(webXmlpath, XmlUtils.nodeToString(webXml), false);
	}

	private void updateGaeHelper() {
		final String sourceAntPath = "module/client/scaffold/gae/GaeHelper-template.java";
		final String segmentPackage = "client.scaffold.gae";
		final String targetDirectory = projectOperations.getPathResolver().getFocusedIdentifier(Path.SRC_MAIN_JAVA, projectOperations.getTopLevelPackage(projectOperations.getFocusedModuleName()).getFullyQualifiedPackageName().replace('.', File.separatorChar) + File.separator + "client" + File.separator + "scaffold" + File.separator + "gae");
		updateFile(sourceAntPath, targetDirectory, segmentPackage, true);
	}

	private void copyDirectoryContents() {
		for (final GwtPath path : GwtPath.values()) {
			copyDirectoryContents(path);
		}
	}

	private void copyDirectoryContents(final GwtPath gwtPath) {
		final String sourceAntPath = gwtPath.getSourceAntPath();
		if (sourceAntPath.contains("gae") && !projectOperations.isFeatureInstalledInFocusedModule(FeatureNames.GAE)) {
			return;
		}
		final String targetDirectory = gwtPath == GwtPath.WEB ? projectOperations.getPathResolver().getFocusedRoot(Path.SRC_MAIN_WEBAPP) : projectOperations.getPathResolver().getFocusedIdentifier(Path.SRC_MAIN_JAVA, gwtPath.getPackagePath(projectOperations.getFocusedTopLevelPackage()));
		updateFile(sourceAntPath, targetDirectory, gwtPath.segmentPackage(), false);
	}

	private void deleteUntouchedSetupFiles(final String sourceAntPath, String targetDirectory) {
		if (!targetDirectory.endsWith(File.separator)) {
			targetDirectory += File.separator;
		}
		if (!fileManager.exists(targetDirectory)) {
			fileManager.createDirectory(targetDirectory);
		}

		final String path = FileUtils.getPath(getClass(), sourceAntPath);
		final Iterable<URL> uris = OSGiUtils.findEntriesByPattern(context.getBundleContext(), path);
		Assert.notNull(uris, "Could not search bundles for resources for Ant Path '" + path + "'");

		for (final URL url : uris) {
			String fileName = url.getPath().substring(url.getPath().lastIndexOf('/') + 1);
			fileName = fileName.replace("-template", "");
			final String targetFilename = targetDirectory + fileName;
			if (!fileManager.exists(targetFilename)) {
				continue;
			}
			try {
				String input = FileCopyUtils.copyToString(new InputStreamReader(url.openStream()));
				input = processTemplate(input, null);
				final String existing = FileCopyUtils.copyToString(new File(targetFilename));
				if (existing.equals(input)) {
					// new File(targetFilename).delete();
					fileManager.delete(targetFilename);
				}
			} catch (final IOException ignored) {
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

		final String path = FileUtils.getPath(getClass(), sourceAntPath);
		final Iterable<URL> urls = OSGiUtils.findEntriesByPattern(context.getBundleContext(), path);
		Assert.notNull(urls, "Could not search bundles for resources for Ant Path '" + path + "'");

		for (final URL url : urls) {
			String fileName = url.getPath().substring(url.getPath().lastIndexOf('/') + 1);
			fileName = fileName.replace("-template", "");
			final String targetFilename = targetDirectory + fileName;
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
			} catch (final IOException e) {
				throw new IllegalStateException("Unable to create '" + targetFilename + "'", e);
			}
		}
	}

	private String processTemplate(String input, String segmentPackage) {
		if (segmentPackage == null) {
			segmentPackage = "";
		}
		final String topLevelPackage = projectOperations.getTopLevelPackage(projectOperations.getFocusedModuleName()).getFullyQualifiedPackageName();
		input = input.replace("__TOP_LEVEL_PACKAGE__", topLevelPackage);
		input = input.replace("__SEGMENT_PACKAGE__", segmentPackage);
		input = input.replace("__PROJECT_NAME__", projectOperations.getProjectName(projectOperations.getFocusedModuleName()));

		if (projectOperations.isFeatureInstalledInFocusedModule(FeatureNames.GAE)) {
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
		for (final Element pluginElement : pluginElements) {
			final Plugin defaultPlugin = new Plugin(pluginElement);
			for (final Plugin plugin : projectOperations.getFocusedModule().getBuildPlugins()) {
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
		final StringBuilder builder = new StringBuilder("// AppEngine user authentication\n\n");
		builder.append("new GaeLoginWidgetDriver(requestFactory).setWidget(shell.getLoginWidget());\n\n");
		builder.append("new ReloadOnAuthenticationFailure().register(eventBus);\n\n");
		return builder.toString();
	}
}
