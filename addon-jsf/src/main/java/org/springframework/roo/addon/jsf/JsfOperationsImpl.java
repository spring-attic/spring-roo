package org.springframework.roo.addon.jsf;

import static org.springframework.roo.model.JpaJavaType.LOB;
import static org.springframework.roo.model.RooJavaType.ROO_JSF_CONVERTER;
import static org.springframework.roo.model.RooJavaType.ROO_JSF_MANAGED_BEAN;
import static org.springframework.roo.model.RooJavaType.ROO_UPLOADED_FILE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.jsf.managedbean.JsfManagedBeanMetadata;
import org.springframework.roo.addon.jsf.model.UploadedFileContentType;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.operations.AbstractOperations;
import org.springframework.roo.classpath.operations.jsr303.FieldDetails;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.ReservedWords;
import org.springframework.roo.project.ContextualPath;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.ProjectType;
import org.springframework.roo.project.Repository;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.IOUtils;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.WebXmlUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of {@link JsfOperations}.
 *
 * @author Alan Stewart
 * @since 1.2.0
 */
@Component
@Service
public class JsfOperationsImpl extends AbstractOperations implements JsfOperations {

	// Constants
	private static final String PRIMEFACES_XPATH = "/configuration/jsf-libraries/jsf-library[@id = 'PRIMEFACES']";
	private static final String PRIMEFACES_THEMES_VERSION = "1.0.2";

	// Fields
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference private MetadataService metadataService;
	@Reference private PathResolver pathResolver;
	@Reference private ProjectOperations projectOperations;
	@Reference private Shell shell;
	@Reference private TypeLocationService typeLocationService;
	@Reference private TypeManagementService typeManagementService;

	public String getName() {
		return FeatureNames.JSF;
	}

	public boolean isInstalledInModule(String moduleName) {
		ContextualPath webAppPath = ContextualPath.getInstance(Path.SRC_MAIN_WEBAPP, moduleName);
		return fileManager.exists(pathResolver.getIdentifier(webAppPath, "WEB-INF/faces-config.xml")) || fileManager.exists(pathResolver.getIdentifier(webAppPath, "templates/layout.xhtml"));
	}
	
	public boolean isJsfInstallationPossible() {
		return projectOperations.isFocusedProjectAvailable() && !projectOperations.isFeatureInstalledInFocusedModule(FeatureNames.MVC);
	}

	public boolean isScaffoldOrMediaAdditionAvailable() {
		return isInstalledInModule(projectOperations.getFocusedModuleName()) && fileManager.exists(getWebXmlFile());
	}

	public void setup(JsfImplementation jsfImplementation, final Theme theme) {
		if (jsfImplementation == null) {
			jsfImplementation = JsfImplementation.ORACLE_MOJARRA;
		}

		updateConfiguration(jsfImplementation);
		createOrUpdateWebXml(theme);

		PathResolver pathResolver = projectOperations.getPathResolver();
		copyDirectoryContents("index.html", pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, ""), false);
		copyDirectoryContents("viewExpired.xhtml", pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, ""), false);
		copyDirectoryContents("resources/images/*.*", pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "resources/images"), false);
		copyDirectoryContents("resources/css/*.css", pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "resources/css"), false);
		copyDirectoryContents("resources/js/*.js", pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "resources/js"), false);
		copyDirectoryContents("templates/*.xhtml", pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "templates"), false);
		copyDirectoryContents("pages/main.xhtml", pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "pages"), false);

		projectOperations.updateProjectType(projectOperations.getFocusedModuleName(), ProjectType.WAR);

		fileManager.scan();
	}

	public void generateAll(final JavaPackage destinationPackage) {
		Assert.notNull(destinationPackage, "Destination package required");

		// Create JSF managed bean for each entity
		generateManagedBeans(destinationPackage);
	}

	public void createManagedBean(final JavaType managedBean, final JavaType entity, String beanName, final boolean includeOnMenu, final boolean createConverter) {
		installFacesConfig(managedBean.getPackage());
		installI18n(managedBean.getPackage());
		installBean("ApplicationBean-template.java", managedBean.getPackage());
		installBean("LocaleBean-template.java", new JavaPackage(managedBean.getPackage().getFullyQualifiedPackageName() + ".util"));
		installBean("ViewExpiredExceptionExceptionHandlerFactory-template.java", new JavaPackage(managedBean.getPackage().getFullyQualifiedPackageName() + ".util"));
		installBean("ViewExpiredExceptionExceptionHandler-template.java", new JavaPackage(managedBean.getPackage().getFullyQualifiedPackageName() + ".util"));

		if (fileManager.exists(typeLocationService.getPhysicalTypeCanonicalPath(managedBean, pathResolver.getFocusedPath(Path.SRC_MAIN_JAVA)))) {
			// Type exists already - nothing to do
			return;
		}

		ClassOrInterfaceTypeDetails entityTypeDetails = typeLocationService.getTypeDetails(entity);
		Assert.notNull(entityTypeDetails, "The type '" + entity + "' could not be resolved");

		PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(PluralMetadata.createIdentifier(entity, PhysicalTypeIdentifier.getPath(entityTypeDetails.getDeclaredByMetadataId())));
		if (pluralMetadata == null) {
			return;
		}

		// Create type annotation for new managed bean
		AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(ROO_JSF_MANAGED_BEAN);
		annotationBuilder.addClassAttribute("entity", entity);
		
		if (!StringUtils.hasText(beanName)) {
			beanName = StringUtils.uncapitalize(managedBean.getSimpleTypeName());
		}
		annotationBuilder.addStringAttribute("beanName", beanName);
		
		if (!includeOnMenu) {
			annotationBuilder.addBooleanAttribute("includeOnMenu", includeOnMenu);
		}
		
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(managedBean, pathResolver.getFocusedPath(Path.SRC_MAIN_JAVA));
		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, managedBean, PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.addAnnotation(annotationBuilder);

		typeManagementService.createOrUpdateTypeOnDisk(typeDetailsBuilder.build());

		shell.flash(Level.FINE, "Created " + managedBean.getFullyQualifiedTypeName(), JsfOperationsImpl.class.getName());
		shell.flash(Level.FINE, "", JsfOperationsImpl.class.getName());

		copyEntityTypePage(entity, beanName, pluralMetadata.getPlural());

		if (createConverter) {
			// Create a javax.faces.convert.Converter class for the entity
			createConverter(new JavaPackage(managedBean.getPackage().getFullyQualifiedPackageName() + ".converter"), entity);
		}
	}

	public void addFileUploadField(final JavaSymbolName fieldName, final JavaType typeName, final UploadedFileContentType contentType, final Boolean autoUpload, final String column, final Boolean notNull, final boolean permitReservedWords) {
		ClassOrInterfaceTypeDetails entityTypeDetails = typeLocationService.getTypeDetails(typeName);
		Assert.notNull(entityTypeDetails, "The type '" + typeName + "' could not be resolved");
		String physicalTypeIdentifier = entityTypeDetails.getDeclaredByMetadataId();
		FieldDetails fieldDetails = new FieldDetails(physicalTypeIdentifier, JavaType.BYTE_ARRAY_PRIMITIVE, fieldName);

		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(ROO_UPLOADED_FILE);
		annotationBuilder.addStringAttribute("contentType", contentType.getContentType());
		if (autoUpload) {
			annotationBuilder.addBooleanAttribute("autoUpload", autoUpload);
		}
		annotations.add(annotationBuilder);
		annotations.add(new AnnotationMetadataBuilder(LOB));

		fieldDetails.decorateAnnotationsList(annotations);

		if (!permitReservedWords) {
			ReservedWords.verifyReservedWordsNotPresent(fieldDetails.getFieldName());
			if (fieldDetails.getColumn() != null) {
				ReservedWords.verifyReservedWordsNotPresent(fieldDetails.getColumn());
			}
		}

		FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(fieldDetails.getPhysicalTypeIdentifier(), Modifier.PRIVATE, annotations, fieldDetails.getFieldName(), fieldDetails.getFieldType());

		typeManagementService.addField(fieldBuilder.build());
	}

	public void addMediaSuurce(final String url, MediaPlayer mediaPlayer) {
		Assert.isTrue(StringUtils.hasText(url), "Media source url required");

		String mainPage = projectOperations.getPathResolver().getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "pages/main.xhtml");
		Document document = XmlUtils.readXml(fileManager.getInputStream(mainPage));
		final Element root = document.getDocumentElement();
		Element element = DomUtils.findFirstElementByName("p:panel", root);
		if (element == null) {
			return;
		}

		if (mediaPlayer == null) {
			mp: for (MediaPlayer mp : MediaPlayer.values()) {
				for (String mediaType : mp.getMediaTypes()) {
					if (StringUtils.toLowerCase(url).contains(mediaType)) {
						mediaPlayer = mp;
						break mp;
					}
				}
			}
		}

		if (url.contains("youtube")) {
			mediaPlayer = MediaPlayer.FLASH;
		}

		Element paraElement = new XmlElementBuilder("p", document).build();
		Element mediaElement;
		if (mediaPlayer == null) {
			mediaElement = new XmlElementBuilder("p:media", document).addAttribute("value", url).build();
		} else {
			mediaElement = new XmlElementBuilder("p:media", document).addAttribute("value", url).addAttribute("player", StringUtils.toLowerCase(mediaPlayer.name())).build();
		}
		paraElement.appendChild(mediaElement);
		element.appendChild(paraElement);

		fileManager.createOrUpdateTextFileIfRequired(mainPage, XmlUtils.nodeToString(document), false);
	}

	private void generateManagedBeans(final JavaPackage destinationPackage) {
		for (ClassOrInterfaceTypeDetails cid : typeLocationService.findClassesOrInterfaceDetailsWithTag(CustomDataKeys.PERSISTENT_TYPE)) {
			if (Modifier.isAbstract(cid.getModifier())) {
				continue;
			}

			JavaType entity = cid.getName();
			ContextualPath path = PhysicalTypeIdentifier.getPath(cid.getDeclaredByMetadataId());

			// Check to see if this persistent type has a JSF metadata listening to it
			String downstreamJsfMetadataId = JsfManagedBeanMetadata.createIdentifier(entity, path);
			if (metadataDependencyRegistry.getDownstream(cid.getDeclaredByMetadataId()).contains(downstreamJsfMetadataId)) {
				// There is already a JSF managed bean for this entity
				continue;
			}

			// To get here, there is no listening managed bean, so add one
			final JavaType managedBean = new JavaType(destinationPackage.getFullyQualifiedPackageName() + "." + entity.getSimpleTypeName() + "Bean");
			final String beanName = StringUtils.uncapitalize(managedBean.getSimpleTypeName());
			createManagedBean(managedBean, entity, beanName, true, true);
		}
	}

	private void installI18n(final JavaPackage destinationPackage) {
		String packagePath = destinationPackage.getFullyQualifiedPackageName().replace('.', File.separatorChar);
		String i18nDirectory = projectOperations.getPathResolver().getFocusedIdentifier(Path.SRC_MAIN_RESOURCES, packagePath + "/i18n");
		copyDirectoryContents("i18n/*.properties", i18nDirectory, false);
	}

	private void copyEntityTypePage(final JavaType entity, final String beanName, final String plural) {
		String domainTypeFile = projectOperations.getPathResolver().getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "pages/" + StringUtils.uncapitalize(entity.getSimpleTypeName()) + ".xhtml");
		try {
			InputStream inputStream = FileUtils.getInputStream(getClass(), "pages/content-template.xhtml");
			String input = FileCopyUtils.copyToString(new InputStreamReader(inputStream));
			input = input.replace("__BEAN_NAME__", beanName);
			input = input.replace("__DOMAIN_TYPE__", entity.getSimpleTypeName());
			input = input.replace("__LC_DOMAIN_TYPE__", JavaSymbolName.getReservedWordSafeName(entity).getSymbolName());
			input = input.replace("__DOMAIN_TYPE_PLURAL__", plural);

			fileManager.createOrUpdateTextFileIfRequired(domainTypeFile, input, false);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to create '" + domainTypeFile + "'", e);
		}
	}

	private void createConverter(final JavaPackage javaPackage, final JavaType entity) {
		// Create type annotation for new converter class
		JavaType converterType = new JavaType(javaPackage.getFullyQualifiedPackageName() + "." + entity.getSimpleTypeName() + "Converter");
		AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(ROO_JSF_CONVERTER);
		annotationBuilder.addClassAttribute("entity", entity);
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(converterType, pathResolver.getFocusedPath(Path.SRC_MAIN_JAVA));
		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, converterType, PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.addAnnotation(annotationBuilder);

		typeManagementService.createOrUpdateTypeOnDisk(typeDetailsBuilder.build());

		shell.flash(Level.FINE, "Created " + converterType.getFullyQualifiedTypeName(), JsfOperationsImpl.class.getName());
		shell.flash(Level.FINE, "", JsfOperationsImpl.class.getName());
	}

	private String getWebXmlFile() {
		return projectOperations.getPathResolver().getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml");
	}

	private void createOrUpdateWebXml(final Theme theme) {
		String webXmlPath = getWebXmlFile();
		boolean hasWebXml = fileManager.exists(webXmlPath);
		if (hasWebXml && theme == null) {
			return;
		}

		final Document document;
		if (hasWebXml) {
			document = XmlUtils.readXml(fileManager.getInputStream(webXmlPath));
		} else {
			document = getDocumentTemplate("WEB-INF/web-template.xml");
			String projectName = projectOperations.getFocusedModule().getDisplayName();
			WebXmlUtils.setDisplayName(projectName, document, null);
			WebXmlUtils.setDescription("Roo generated " + projectName + " application", document, null);
		}
		if (theme != null) {
			changeTheme(theme, document);
		}

		fileManager.createOrUpdateTextFileIfRequired(getWebXmlFile(), XmlUtils.nodeToString(document), false);
	}

	private void changeTheme(final Theme theme, final Document document) {
		Assert.notNull(theme, "Theme required");
		Assert.notNull(document, "web.xml document required");

		// Add theme to the pom if not already there
		String themeName = StringUtils.toLowerCase(theme.name().replace("_", "-"));
		projectOperations.addDependency(projectOperations.getFocusedModuleName(), "org.primefaces.themes", themeName, PRIMEFACES_THEMES_VERSION);

		// Update the web.xml primefaces.THEME content-param
		Element root = document.getDocumentElement();

		Element contextParamElement = XmlUtils.findFirstElement("/web-app/context-param[param-name = 'primefaces.THEME']", root);
		Assert.notNull(contextParamElement, "The web.xml primefaces.THEME context param element required");
		Element paramValueElement =  XmlUtils.findFirstElement("param-value", contextParamElement);
		Assert.notNull(paramValueElement, "primefaces.THEME param-value element required");
		paramValueElement.setTextContent(themeName);
	}

	private void installFacesConfig(final JavaPackage destinationPackage) {
		Assert.isTrue(projectOperations.isFocusedProjectAvailable(), "Project metadata required");
		if (hasFacesConfig()) {
			return;
		}

		InputStream inputStream = null;
		try {
			inputStream = FileUtils.getInputStream(getClass(), "WEB-INF/faces-config-template.xml");
			String input = FileCopyUtils.copyToString(new InputStreamReader(inputStream));
			input = input.replace("__PACKAGE__", destinationPackage.getFullyQualifiedPackageName());
			fileManager.createOrUpdateTextFileIfRequired(getFacesConfigFile(), input, false);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to create 'faces.config.xml'", e);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

	private boolean hasFacesConfig() {
		return fileManager.exists(getFacesConfigFile());
	}

	private String getFacesConfigFile() {
		return projectOperations.getPathResolver().getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/faces-config.xml");
	}

	private void updateConfiguration(final JsfImplementation jsfImplementation) {
		// Update pom.xml with JSF/Primefaces dependencies and repositories
		Element configuration = XmlUtils.getConfiguration(getClass());
		final String jsfImplementationXPath = getJsfImplementationXPath(getUnwantedJsfImplementations(jsfImplementation));

		updateDependencies(configuration, jsfImplementation, jsfImplementationXPath);
		updateRepositories(configuration, jsfImplementation, jsfImplementationXPath);
	}

	private List<Dependency> getDependencies(final String xPathExpression, final Element configuration) {
		final List<Dependency> dependencies = new ArrayList<Dependency>();
		for (Element dependencyElement : XmlUtils.findElements(xPathExpression + "/dependencies/dependency", configuration)) {
			dependencies.add(new Dependency(dependencyElement));
		}
		return dependencies;
	}

	private void updateDependencies(final Element configuration, final JsfImplementation jsfImplementation, final String jsfImplementationXPath) {
		final List<Dependency> requiredDependencies = new ArrayList<Dependency>();

		final List<Element> jsfImplementationDependencies = XmlUtils.findElements(getJsfImplementationXPath(jsfImplementation) + "/dependencies/dependency", configuration);
		for (Element dependencyElement : jsfImplementationDependencies) {
			requiredDependencies.add(new Dependency(dependencyElement));
		}

		final List<Element> jsfLibraryDependencies = XmlUtils.findElements(PRIMEFACES_XPATH + "/dependencies/dependency", configuration);
		for (Element dependencyElement : jsfLibraryDependencies) {
			requiredDependencies.add(new Dependency(dependencyElement));
		}

		final List<Element> jsfDependencies = XmlUtils.findElements("/configuration/jsf/dependencies/dependency", configuration);
		for (Element dependencyElement : jsfDependencies) {
			requiredDependencies.add(new Dependency(dependencyElement));
		}

		// Remove redundant dependencies
		final List<Dependency> redundantDependencies = new ArrayList<Dependency>();
		redundantDependencies.addAll(getDependencies(jsfImplementationXPath, configuration));
		// Don't remove any we actually need
		redundantDependencies.removeAll(requiredDependencies);

		// Update the POM
		projectOperations.addDependencies(projectOperations.getFocusedModuleName(), requiredDependencies);
		projectOperations.removeDependencies(projectOperations.getFocusedModuleName(), redundantDependencies);
	}

	private void updateRepositories(final Element configuration, final JsfImplementation jsfImplementation, final String jsfImplementationXPath) {
		List<Repository> repositories = new ArrayList<Repository>();

		List<Element> jsfRepositories = XmlUtils.findElements(getJsfImplementationXPath(jsfImplementation) +"/repositories/repository", configuration);
		for (Element repositoryElement : jsfRepositories) {
			repositories.add(new Repository(repositoryElement));
		}

		List<Element> jsfLibraryRepositories = XmlUtils.findElements(PRIMEFACES_XPATH + "/repositories/repository", configuration);
		for (Element repositoryElement : jsfLibraryRepositories) {
			repositories.add(new Repository(repositoryElement));
		}

		projectOperations.addRepositories(projectOperations.getFocusedModuleName(), repositories);
	}

	private List<JsfImplementation> getUnwantedJsfImplementations(final JsfImplementation jsfImplementation) {
		final List<JsfImplementation> unwantedJsfImplementations = new ArrayList<JsfImplementation>(Arrays.asList(JsfImplementation.values()));
		unwantedJsfImplementations.remove(jsfImplementation);
		return unwantedJsfImplementations;
	}

	private void installBean(final String templateName, final JavaPackage destinationPackage) {
		String beanName = templateName.substring(0, templateName.indexOf("-template"));
		JavaType javaType = new JavaType(destinationPackage.getFullyQualifiedPackageName() + "." + beanName);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType, pathResolver.getFocusedPath(Path.SRC_MAIN_JAVA));
		String physicalPath = typeLocationService.getPhysicalTypeCanonicalPath(physicalTypeIdentifier);
		if (fileManager.exists(physicalPath)) {
			return;
		}

		InputStream inputStream = null;
		try {
			inputStream = FileUtils.getInputStream(getClass(), templateName);
			String input = FileCopyUtils.copyToString(new InputStreamReader(inputStream));
			input = input.replace("__PACKAGE__", destinationPackage.getFullyQualifiedPackageName());
			fileManager.createOrUpdateTextFileIfRequired(physicalPath, input, false);

			shell.flash(Level.FINE, "Created " + javaType.getFullyQualifiedTypeName(), JsfOperationsImpl.class.getName());
			shell.flash(Level.FINE, "", JsfOperationsImpl.class.getName());
		} catch (IOException e) {
			throw new IllegalStateException("Unable to create '" + physicalPath + "'", e);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

	private String getJsfImplementationXPath(final List<JsfImplementation> jsfImplementations) {
		StringBuilder builder = new StringBuilder("/configuration/jsf-implementations/jsf-implementation[");
		for (int i = 0, n = jsfImplementations.size(); i < n; i++) {
			builder.append("@id = '");
			builder.append(jsfImplementations.get(i).name());
			builder.append("'");
			if (i < n - 1) {
				builder.append(" or ");
			}
		}
		builder.append("]");
		return builder.toString();
	}

	private String getJsfImplementationXPath(final JsfImplementation jsfImplementation) {
		return "/configuration/jsf-implementations/jsf-implementation[@id = '" + jsfImplementation.name() + "']";
	}
}
