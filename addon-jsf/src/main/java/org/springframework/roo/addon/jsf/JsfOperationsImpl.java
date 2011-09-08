package org.springframework.roo.addon.jsf;

import static org.springframework.roo.model.RooJavaType.ROO_JSF_CONVERTER;
import static org.springframework.roo.model.RooJavaType.ROO_JSF_MANAGED_BEAN;
import static org.springframework.roo.model.RooJavaType.ROO_UPLOADED_FILE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
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
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.ProjectType;
import org.springframework.roo.project.Repository;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.IOUtils;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.WebXmlUtils;
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
	
	// Fields
	@Reference private MetadataDependencyRegistry dependencyRegistry;
	@Reference private MetadataService metadataService;
	@Reference private ProjectOperations projectOperations;
	@Reference private TypeLocationService typeLocationService;
	@Reference private TypeManagementService typeManagementService;
	@Reference private Shell shell;

	public boolean isSetupAvailable() {
		return projectOperations.isProjectAvailable() && !hasWebXml() && !hasFacesConfig();
	}

	public boolean isScaffoldAvailable() {
		return hasWebXml();
	}

	public void setup(JsfImplementation jsfImplementation) {
		if (jsfImplementation == null) {
			jsfImplementation = JsfImplementation.ORACLE_MOJARRA;
		}

		changeJsfImplementation(jsfImplementation);
		copyWebXml();

		PathResolver pathResolver = projectOperations.getPathResolver();
		copyDirectoryContents("index.html", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/"), false);
		copyDirectoryContents("viewExpired.xhtml", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/"), false);
		copyDirectoryContents("resources/images/*.*", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/resources/images"), false);
		copyDirectoryContents("resources/css/*.css", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/resources/css"), false);
		copyDirectoryContents("resources/js/*.js", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/resources/js"), false);
		copyDirectoryContents("templates/*.xhtml", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/templates"), false);
		copyDirectoryContents("pages/main.xhtml", pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/pages"), false);
		
		projectOperations.updateProjectType(ProjectType.WAR);

		fileManager.scan();
	}

	public void changeJsfImplementation(JsfImplementation jsfImplementation) {
		updateConfiguration(jsfImplementation);
	}

	public void generateAll(JavaPackage destinationPackage) {
		Assert.notNull(destinationPackage, "Destination package required");
		
		// Create JSF managed bean for each entity
		generateManagedBeans(destinationPackage);
	}

	public void createManagedBean(JavaType managedBean, JavaType entity, boolean includeOnMenu, boolean createConverter) {
		installFacesConfig(managedBean.getPackage());
		installI18n(managedBean.getPackage());
		installBean("ApplicationBean-template.java", managedBean.getPackage());
		installBean("LocaleBean-template.java", managedBean.getPackage());
		installBean("ViewExpiredExceptionExceptionHandlerFactory-template.java", managedBean.getPackage());
		installBean("ViewExpiredExceptionExceptionHandler-template.java", managedBean.getPackage());

		if (fileManager.exists(typeLocationService.getPhysicalTypeCanonicalPath(managedBean, Path.SRC_MAIN_JAVA))) {
			// Type exists already - nothing to do
			return; 
		}
		
		PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(PluralMetadata.createIdentifier(entity, Path.SRC_MAIN_JAVA));
		if (pluralMetadata == null) {
			return;
		}

		// Create type annotation for new managed bean
		AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(ROO_JSF_MANAGED_BEAN);
		annotationBuilder.addClassAttribute("entity", entity);
		if (!includeOnMenu) {
			annotationBuilder.addBooleanAttribute("includeOnMenu", includeOnMenu);
		}
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(managedBean, Path.SRC_MAIN_JAVA);
		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, managedBean, PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.addAnnotation(annotationBuilder);

		typeManagementService.createOrUpdateTypeOnDisk(typeDetailsBuilder.build());

		shell.flash(Level.FINE, "Created " + managedBean.getFullyQualifiedTypeName(), JsfOperationsImpl.class.getName());
		shell.flash(Level.FINE, "", JsfOperationsImpl.class.getName());

		copyEntityTypePage(entity, pluralMetadata.getPlural());

		if (createConverter) {
			// Create a javax.faces.convert.Converter class for the entity
			createConverter(managedBean.getPackage(), entity);
		}
	}

	public void changeTheme(Theme theme) {
		Assert.notNull(theme, "Theme required");
		
		// Add theme to the pom if not already there
		String themeName = StringUtils.toLowerCase(theme.name().replace("_", "-"));
		projectOperations.addDependency("org.primefaces.themes", themeName, "1.0.1");
		
		// Update the web.xml primefaces.THEME content-param
		String webXmlPath = getWebXmlFile();
		Document document = XmlUtils.readXml(fileManager.getInputStream(webXmlPath));
		Element root = document.getDocumentElement();
		
		Element contextParamElement = XmlUtils.findFirstElement("/web-app/context-param[param-name = 'primefaces.THEME']", root);
		Assert.notNull(contextParamElement, "The web.xml primefaces.THEME context param element required");
		Element paramValueElement =  XmlUtils.findFirstElement("param-value", contextParamElement);
		Assert.notNull(paramValueElement, "primefaces.THEME param-value element required");
		paramValueElement.setTextContent(themeName);

		fileManager.createOrUpdateTextFileIfRequired(webXmlPath, XmlUtils.nodeToString(document), false);
	}

	public void addFileUploadField(JavaSymbolName fieldName, JavaType typeName, String fileName, String contentType, String column, Boolean notNull, boolean permitReservedWords) {
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(typeName, Path.SRC_MAIN_JAVA);
		JavaType fieldType = JavaType.BYTE_ARRAY_PRIMITIVE;
		FieldDetails fieldDetails = new FieldDetails(physicalTypeIdentifier, fieldType, fieldName);

		AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(ROO_UPLOADED_FILE);
		if (StringUtils.hasText(fileName)) {
			annotationBuilder.addStringAttribute("fileName", fileName);
		}
		if (StringUtils.hasText(contentType)) {
			annotationBuilder.addStringAttribute("contentType", contentType);
		}
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(annotationBuilder);
		annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.persistence.Lob")));
		
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

	private void generateManagedBeans(JavaPackage destinationPackage) {
		for (ClassOrInterfaceTypeDetails cid : typeLocationService.findClassesOrInterfaceDetailsWithTag(PersistenceCustomDataKeys.PERSISTENT_TYPE)) {
			if (Modifier.isAbstract(cid.getModifier())) {
				continue;
			}
			
			JavaType entity = cid.getName();
			Path path = PhysicalTypeIdentifier.getPath(cid.getDeclaredByMetadataId());
			
			// Check to see if this persistent type has a JSF metadata listening to it
			String downstreamJsfMetadataId = JsfManagedBeanMetadata.createIdentifier(entity, path);
			if (dependencyRegistry.getDownstream(cid.getDeclaredByMetadataId()).contains(downstreamJsfMetadataId)) {
				// There is already a JSF managed bean for this entity
				continue;
			}
			
			// To get here, there is no listening managed bean, so add one
			JavaType managedBean = new JavaType(destinationPackage.getFullyQualifiedPackageName() + "." + entity.getSimpleTypeName() + "Bean");
			createManagedBean(managedBean, entity, true, true);
		}
	}

	private void installI18n(JavaPackage destinationPackage) {
		String packagePath = destinationPackage.getFullyQualifiedPackageName().replace('.', File.separatorChar);
		String i18nDirectory = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, packagePath + "/i18n");
		copyDirectoryContents("i18n/*.properties", i18nDirectory, false);
	}
	
	private void copyEntityTypePage(JavaType entity, String plural) {
		String domainTypeFile = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "/pages/" + StringUtils.uncapitalize(entity.getSimpleTypeName()) + ".xhtml");
		InputStream inputStream = null;
		try {
			inputStream = TemplateUtils.getTemplate(getClass(), "pages/content-template.xhtml");
			String input = FileCopyUtils.copyToString(new InputStreamReader(inputStream));
			input = input.replace("__DOMAIN_TYPE__", entity.getSimpleTypeName());
			input = input.replace("__LC_DOMAIN_TYPE__", JavaSymbolName.getReservedWordSafeName(entity).getSymbolName());
			input = input.replace("__DOMAIN_TYPE_PLURAL__", plural);
			input = input.replace("__LC_DOMAIN_TYPE_PLURAL__", StringUtils.uncapitalize(plural));

			fileManager.createOrUpdateTextFileIfRequired(domainTypeFile, input, false);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to create '" + domainTypeFile + "'", e);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

	private void createConverter(JavaPackage javaPackage, JavaType entity) {
		// Create type annotation for new converter class
		JavaType converterType = new JavaType(javaPackage.getFullyQualifiedPackageName() + "." + entity.getSimpleTypeName() + "Converter");
		AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(ROO_JSF_CONVERTER);
		annotationBuilder.addClassAttribute("entity", entity);
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(converterType, Path.SRC_MAIN_JAVA);
		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, converterType, PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.addAnnotation(annotationBuilder);

		typeManagementService.createOrUpdateTypeOnDisk(typeDetailsBuilder.build());

		shell.flash(Level.FINE, "Created " + converterType.getFullyQualifiedTypeName(), JsfOperationsImpl.class.getName());
		shell.flash(Level.FINE, "", JsfOperationsImpl.class.getName());
	}

	private boolean hasWebXml() {
		return fileManager.exists(getWebXmlFile());
	}
	
	private String getWebXmlFile() {
		return projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/web.xml");
	}

	private void copyWebXml() {
		Assert.isTrue(projectOperations.isProjectAvailable(), "Project metadata required");
		if (hasWebXml()) {
			return;
		}

		Document document = getDocumentTemplate("WEB-INF/web-template.xml");
		String projectName = projectOperations.getProjectMetadata().getProjectName();
		WebXmlUtils.setDisplayName(projectName, document, null);
		WebXmlUtils.setDescription("Roo generated " + projectName + " application", document, null);
		
		fileManager.createOrUpdateTextFileIfRequired(getWebXmlFile(), XmlUtils.nodeToString(document), false);
	}

	private void installFacesConfig(JavaPackage destinationPackage) {
		Assert.isTrue(projectOperations.isProjectAvailable(), "Project metadata required");
		if (hasFacesConfig()) {
			return;
		}

		InputStream inputStream = null;
		try {
			inputStream = TemplateUtils.getTemplate(getClass(), "WEB-INF/faces-config-template.xml");
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
		return projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/faces-config.xml");
	}

	private void updateConfiguration(JsfImplementation jsfImplementation) {
		Element configuration = XmlUtils.getConfiguration(getClass());

		// Remove unnecessary artifacts not specific to current JSF implementation
		cleanup(configuration, jsfImplementation);

		// Update pom.xml with JSF/Primefaces dependencies and repositories
		updateDependencies(configuration, jsfImplementation);
		updateRepositories(configuration, jsfImplementation);
	}

	private void cleanup(Element configuration, JsfImplementation jsfImplementation) {
		List<JsfImplementation> jsfImplementations = new ArrayList<JsfImplementation>();
		for (JsfImplementation implementation : JsfImplementation.values()) {
			if (implementation != jsfImplementation) {
				jsfImplementations.add(implementation);
			}
		}
		
		String implementationXPath = getImplementationXPath(jsfImplementations);
		projectOperations.removeDependencies(getDependencies(implementationXPath, configuration));
	}
	
	private List<Dependency> getDependencies(String xPathExpression, Element configuration) {
		List<Dependency> dependencies = new ArrayList<Dependency>();
		for (Element dependencyElement : XmlUtils.findElements(xPathExpression + "/dependencies/dependency", configuration)) {
			dependencies.add(new Dependency(dependencyElement));
		}
		return dependencies;
	}

	private void updateDependencies(Element configuration, JsfImplementation jsfImplementation) {
		List<Dependency> dependencies = new ArrayList<Dependency>();
		List<Element> jsfImplementationDependencies = XmlUtils.findElements(getImplementationXPath(jsfImplementation) +"/dependencies/dependency", configuration);
		for (Element dependencyElement : jsfImplementationDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}
		
		List<Element> jsfLibraryDependencies = XmlUtils.findElements(PRIMEFACES_XPATH + "/dependencies/dependency", configuration);
		for (Element dependencyElement : jsfLibraryDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}
		
		List<Element> jsfDependencies = XmlUtils.findElements("/configuration/jsf/dependencies/dependency", configuration);
		for (Element dependencyElement : jsfDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}

		projectOperations.addDependencies(dependencies);
	}

	private void updateRepositories(Element configuration, JsfImplementation jsfImplementation) {
		List<Repository> repositories = new ArrayList<Repository>();

		List<Element> jsfRepositories = XmlUtils.findElements(getImplementationXPath(jsfImplementation) +"/repositories/repository", configuration);
		for (Element repositoryElement : jsfRepositories) {
			repositories.add(new Repository(repositoryElement));
		}

		List<Element> jsfLibraryRepositories = XmlUtils.findElements(PRIMEFACES_XPATH + "/repositories/repository", configuration);
		for (Element repositoryElement : jsfLibraryRepositories) {
			repositories.add(new Repository(repositoryElement));
		}

		projectOperations.addRepositories(repositories);
	}

	private void installBean(final String templateName, final JavaPackage destinationPackage) {
		String beanName = templateName.substring(0, templateName.indexOf("-template"));
		JavaType javaType = new JavaType(destinationPackage.getFullyQualifiedPackageName() + "." + beanName);
		String physicalPath = typeLocationService.getPhysicalTypeCanonicalPath(javaType, Path.SRC_MAIN_JAVA);
		if (fileManager.exists(physicalPath)) {
			return;
		}
		try {
			InputStream template = TemplateUtils.getTemplate(getClass(), templateName);
			String input = FileCopyUtils.copyToString(new InputStreamReader(template));
			input = input.replace("__PACKAGE__", destinationPackage.getFullyQualifiedPackageName());
			fileManager.createOrUpdateTextFileIfRequired(physicalPath, input, false);
			
			shell.flash(Level.FINE, "Created " + javaType.getFullyQualifiedTypeName(), JsfOperationsImpl.class.getName());
			shell.flash(Level.FINE, "", JsfOperationsImpl.class.getName());
		} catch (IOException e) {
			throw new IllegalStateException("Unable to create '" + physicalPath + "'", e);
		}
	}
	
	private String getImplementationXPath(List<JsfImplementation> jsfImplementations) {
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
	
	private String getImplementationXPath(JsfImplementation jsfImplementation) {
		return "/configuration/jsf-implementations/jsf-implementation[@id = '" + jsfImplementation.name() + "']";
	}
}