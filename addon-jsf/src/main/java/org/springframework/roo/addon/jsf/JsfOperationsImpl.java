package org.springframework.roo.addon.jsf;

import static org.springframework.roo.model.RooJavaType.ROO_JSF_CONVERTER;
import static org.springframework.roo.model.RooJavaType.ROO_JSF_MANAGED_BEAN;

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
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.operations.AbstractOperations;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.ProjectType;
import org.springframework.roo.project.Repository;
import org.springframework.roo.project.maven.Pom;
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
public class JsfOperationsImpl extends AbstractOperations implements
        JsfOperations {

    // Constants
    private static final String JSF_IMPLEMENTATION_XPATH = "/configuration/jsf-implementations/jsf-implementation";
    private static final String JSF_LIBRARY_XPATH = "/configuration/jsf-libraries/jsf-library";
    private static final String DEPENDENCY_XPATH = "/dependencies/dependency";
    private static final String REPOSITORY_XPATH = "/repositories/repository";
    private static final String MYFACES_LISTENER = "org.apache.myfaces.webapp.StartupServletContextListener";
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
        LogicalPath webAppPath = LogicalPath.getInstance(Path.SRC_MAIN_WEBAPP,
                moduleName);
        return fileManager.exists(pathResolver.getIdentifier(webAppPath,
                "WEB-INF/faces-config.xml"))
                || fileManager.exists(pathResolver.getIdentifier(webAppPath,
                        "templates/layout.xhtml"));
    }

    public boolean isJsfInstallationPossible() {
        return projectOperations.isFocusedProjectAvailable()
                && !projectOperations
                        .isFeatureInstalledInFocusedModule(FeatureNames.MVC);
    }

    public boolean isScaffoldOrMediaAdditionAvailable() {
        return isInstalledInModule(projectOperations.getFocusedModuleName())
                && fileManager.exists(getWebXmlFile());
    }

    public void setup(final JsfImplementation jsfImplementation,
            final JsfLibrary jsfLibrary, final Theme theme) {
        updateConfiguration(jsfImplementation, jsfLibrary);
        createOrUpdateWebXml(jsfImplementation, theme);

        final LogicalPath webappPath = Path.SRC_MAIN_WEBAPP
                .getModulePathId(projectOperations.getFocusedModuleName());
        copyDirectoryContents("index.html",
                pathResolver.getIdentifier(webappPath, ""), false);
        copyDirectoryContents("viewExpired.xhtml",
                pathResolver.getIdentifier(webappPath, ""), false);
        copyDirectoryContents("resources/images/*.*",
                pathResolver.getIdentifier(webappPath, "resources/images"),
                false);
        copyDirectoryContents("resources/css/*.css",
                pathResolver.getIdentifier(webappPath, "resources/css"), false);
        copyDirectoryContents("resources/js/*.js",
                pathResolver.getIdentifier(webappPath, "resources/js"), false);
        copyDirectoryContents("templates/*.xhtml",
                pathResolver.getIdentifier(webappPath, "templates"), false);
        copyDirectoryContents("pages/main.xhtml",
                pathResolver.getIdentifier(webappPath, "pages"), false);

        projectOperations.updateProjectType(
                projectOperations.getFocusedModuleName(), ProjectType.WAR);

        fileManager.scan();
    }

    public void generateAll(final JavaPackage destinationPackage) {
        Assert.notNull(destinationPackage, "Destination package required");

        // Create JSF managed bean for each entity
        generateManagedBeans(destinationPackage);
    }

    public void createManagedBean(final JavaType managedBean,
            final JavaType entity, String beanName, final boolean includeOnMenu) {
        final JavaPackage managedBeanPackage = managedBean.getPackage();
        installFacesConfig(managedBeanPackage);
        installI18n(managedBeanPackage);
        installBean("ApplicationBean-template.java", managedBeanPackage);

        final String managedBeanTypeName = managedBeanPackage
                .getFullyQualifiedPackageName();
        final JavaPackage utilPackage = new JavaPackage(managedBeanTypeName
                + ".util");
        installBean("LocaleBean-template.java", utilPackage);
        installBean(
                "ViewExpiredExceptionExceptionHandlerFactory-template.java",
                utilPackage);
        installBean("ViewExpiredExceptionExceptionHandler-template.java",
                utilPackage);

        if (fileManager.exists(typeLocationService
                .getPhysicalTypeCanonicalPath(managedBean,
                        pathResolver.getFocusedPath(Path.SRC_MAIN_JAVA)))) {
            // Type exists already - nothing to do
            return;
        }

        ClassOrInterfaceTypeDetails entityTypeDetails = typeLocationService
                .getTypeDetails(entity);
        Assert.notNull(entityTypeDetails, "The type '" + entity
                + "' could not be resolved");

        PluralMetadata pluralMetadata = (PluralMetadata) metadataService
                .get(PluralMetadata.createIdentifier(entity,
                        PhysicalTypeIdentifier.getPath(entityTypeDetails
                                .getDeclaredByMetadataId())));
        Assert.notNull(pluralMetadata, "The plural for type '" + entity
                + "' could not be resolved");

        // Create type annotation for new managed bean
        AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(
                ROO_JSF_MANAGED_BEAN);
        annotationBuilder.addClassAttribute("entity", entity);

        if (StringUtils.isBlank(beanName)) {
            beanName = StringUtils
                    .uncapitalize(managedBean.getSimpleTypeName());
        }
        annotationBuilder.addStringAttribute("beanName", beanName);

        if (!includeOnMenu) {
            annotationBuilder.addBooleanAttribute("includeOnMenu",
                    includeOnMenu);
        }

        LogicalPath managedBeanPath = pathResolver
                .getFocusedPath(Path.SRC_MAIN_JAVA);
        String resourceIdentifier = typeLocationService
                .getPhysicalTypeCanonicalPath(managedBean, managedBeanPath);
        String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(
                managedBean, pathResolver.getPath(resourceIdentifier));
        ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                declaredByMetadataId, Modifier.PUBLIC, managedBean,
                PhysicalTypeCategory.CLASS);
        cidBuilder.addAnnotation(annotationBuilder);
        typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());

        shell.flash(Level.FINE,
                "Created " + managedBean.getFullyQualifiedTypeName(),
                JsfOperationsImpl.class.getName());
        shell.flash(Level.FINE, "", JsfOperationsImpl.class.getName());

        copyEntityTypePage(entity, beanName, pluralMetadata.getPlural());

        // Create a javax.faces.convert.Converter class for the entity
        createConverter(new JavaPackage(managedBeanTypeName + ".converter"),
                entity);
    }

    public void addMediaSuurce(final String url, MediaPlayer mediaPlayer) {
        Assert.isTrue(StringUtils.hasText(url), "Media source url required");

        String mainPage = projectOperations.getPathResolver()
                .getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "pages/main.xhtml");
        Document document = XmlUtils.readXml(fileManager
                .getInputStream(mainPage));
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
            mediaElement = new XmlElementBuilder("p:media", document)
                    .addAttribute("value", url).build();
        }
        else {
            mediaElement = new XmlElementBuilder("p:media", document)
                    .addAttribute("value", url)
                    .addAttribute("player",
                            StringUtils.toLowerCase(mediaPlayer.name()))
                    .build();
        }
        paraElement.appendChild(mediaElement);
        element.appendChild(paraElement);

        fileManager.createOrUpdateTextFileIfRequired(mainPage,
                XmlUtils.nodeToString(document), false);
    }

    private void generateManagedBeans(final JavaPackage destinationPackage) {
        for (final ClassOrInterfaceTypeDetails cid : typeLocationService
                .findClassesOrInterfaceDetailsWithTag(CustomDataKeys.PERSISTENT_TYPE)) {
            if (Modifier.isAbstract(cid.getModifier())) {
                continue;
            }

            final JavaType entity = cid.getName();
            final LogicalPath path = PhysicalTypeIdentifier.getPath(cid
                    .getDeclaredByMetadataId());

            // Check to see if this persistent type has a JSF metadata listening
            // to it
            final String downstreamJsfMetadataId = JsfManagedBeanMetadata
                    .createIdentifier(entity, path);
            if (metadataDependencyRegistry.getDownstream(
                    cid.getDeclaredByMetadataId()).contains(
                    downstreamJsfMetadataId)) {
                // There is already a JSF managed bean for this entity
                continue;
            }

            // To get here, there is no listening managed bean, so add one
            final JavaType managedBean = new JavaType(
                    destinationPackage.getFullyQualifiedPackageName() + "."
                            + entity.getSimpleTypeName() + "Bean");
            final String beanName = StringUtils.uncapitalize(managedBean
                    .getSimpleTypeName());
            createManagedBean(managedBean, entity, beanName, true);
        }
    }

    private void installI18n(final JavaPackage destinationPackage) {
        String packagePath = destinationPackage.getFullyQualifiedPackageName()
                .replace('.', File.separatorChar);
        String i18nDirectory = projectOperations.getPathResolver()
                .getFocusedIdentifier(Path.SRC_MAIN_RESOURCES,
                        packagePath + "/i18n");
        copyDirectoryContents("i18n/*.properties", i18nDirectory, false);
    }

    private void copyEntityTypePage(final JavaType entity,
            final String beanName, final String plural) {
        String domainTypeFile = projectOperations.getPathResolver()
                .getFocusedIdentifier(
                        Path.SRC_MAIN_WEBAPP,
                        "pages/"
                                + StringUtils.uncapitalize(entity
                                        .getSimpleTypeName()) + ".xhtml");
        try {
            InputStream inputStream = FileUtils.getInputStream(getClass(),
                    "pages/content-template.xhtml");
            String input = FileCopyUtils.copyToString(new InputStreamReader(
                    inputStream));
            input = input.replace("__BEAN_NAME__", beanName);
            input = input
                    .replace("__DOMAIN_TYPE__", entity.getSimpleTypeName());
            input = input.replace("__LC_DOMAIN_TYPE__", JavaSymbolName
                    .getReservedWordSafeName(entity).getSymbolName());
            input = input.replace("__DOMAIN_TYPE_PLURAL__", plural);

            fileManager.createOrUpdateTextFileIfRequired(domainTypeFile, input,
                    false);
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to create '"
                    + domainTypeFile + "'", e);
        }
    }

    private void createConverter(final JavaPackage javaPackage,
            final JavaType entity) {
        // Create type annotation for new converter class
        JavaType converterType = new JavaType(
                javaPackage.getFullyQualifiedPackageName() + "."
                        + entity.getSimpleTypeName() + "Converter");
        AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(
                ROO_JSF_CONVERTER);
        annotationBuilder.addClassAttribute("entity", entity);
        String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(
                converterType, pathResolver.getFocusedPath(Path.SRC_MAIN_JAVA));
        ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                declaredByMetadataId, Modifier.PUBLIC, converterType,
                PhysicalTypeCategory.CLASS);
        cidBuilder.addAnnotation(annotationBuilder);

        typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());

        shell.flash(Level.FINE,
                "Created " + converterType.getFullyQualifiedTypeName(),
                JsfOperationsImpl.class.getName());
        shell.flash(Level.FINE, "", JsfOperationsImpl.class.getName());
    }

    private String getWebXmlFile() {
        return projectOperations.getPathResolver().getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml");
    }

    private void createOrUpdateWebXml(
            final JsfImplementation jsfImplementation, final Theme theme) {
        String webXmlPath = getWebXmlFile();

        final Document document;
        if (fileManager.exists(webXmlPath)) {
            document = XmlUtils.readXml(fileManager.getInputStream(webXmlPath));
        }
        else {
            document = getDocumentTemplate("WEB-INF/web-template.xml");
            String projectName = projectOperations.getFocusedModule()
                    .getDisplayName();
            WebXmlUtils.setDisplayName(projectName, document, null);
            WebXmlUtils.setDescription("Roo generated " + projectName
                    + " application", document, null);
        }

        if (jsfImplementation != null) {
            addOrRemoveMyFacesListener(jsfImplementation, document);
        }
        if (theme != null) {
            changePrimeFacesTheme(theme, document);
        }

        fileManager.createOrUpdateTextFileIfRequired(webXmlPath,
                XmlUtils.nodeToString(document), false);
    }

    private void addOrRemoveMyFacesListener(
            final JsfImplementation jsfImplementation, final Document document) {
        Assert.notNull(jsfImplementation, "JSF implementation required");
        Assert.notNull(document, "web.xml document required");

        final Element root = document.getDocumentElement();
        final Element webAppElement = XmlUtils.findFirstElement("/web-app",
                root);
        Element listenerElement = XmlUtils.findFirstElement(
                "listener[listener-class = '" + MYFACES_LISTENER + "']",
                webAppElement);
        switch (jsfImplementation) {
        case ORACLE_MOJARRA:
            if (listenerElement != null) {
                webAppElement.removeChild(listenerElement);
                DomUtils.removeTextNodes(webAppElement);
            }
            break;
        case APACHE_MYFACES:
            if (listenerElement == null) {
                WebXmlUtils.addListener(MYFACES_LISTENER, document, "");
                DomUtils.removeTextNodes(webAppElement);
            }
            break;
        }
    }

    private void changePrimeFacesTheme(final Theme theme,
            final Document document) {
        Assert.notNull(theme, "Theme required");
        Assert.notNull(document, "web.xml document required");

        // Add theme to the pom if not already there
        String themeName = StringUtils.toLowerCase(theme.name().replace("_",
                "-"));
        projectOperations.addDependency(
                projectOperations.getFocusedModuleName(),
                "org.primefaces.themes", themeName, PRIMEFACES_THEMES_VERSION);

        // Update the web.xml primefaces.THEME content-param
        Element root = document.getDocumentElement();

        Element contextParamElement = XmlUtils
                .findFirstElement(
                        "/web-app/context-param[param-name = 'primefaces.THEME']",
                        root);
        Assert.notNull(contextParamElement,
                "The web.xml primefaces.THEME context param element required");
        Element paramValueElement = XmlUtils.findFirstElement("param-value",
                contextParamElement);
        Assert.notNull(paramValueElement,
                "primefaces.THEME param-value element required");
        paramValueElement.setTextContent(themeName);
    }

    private void installFacesConfig(final JavaPackage destinationPackage) {
        Assert.isTrue(projectOperations.isFocusedProjectAvailable(),
                "Project metadata required");
        if (hasFacesConfig()) {
            return;
        }

        InputStream inputStream = null;
        try {
            inputStream = FileUtils.getInputStream(getClass(),
                    "WEB-INF/faces-config-template.xml");
            String input = FileCopyUtils.copyToString(new InputStreamReader(
                    inputStream));
            input = input.replace("__PACKAGE__",
                    destinationPackage.getFullyQualifiedPackageName());
            fileManager.createOrUpdateTextFileIfRequired(getFacesConfigFile(),
                    input, false);
        }
        catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to create 'faces.config.xml'", e);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private boolean hasFacesConfig() {
        return fileManager.exists(getFacesConfigFile());
    }

    private String getFacesConfigFile() {
        return projectOperations.getPathResolver().getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, "WEB-INF/faces-config.xml");
    }

    private void updateConfiguration(JsfImplementation jsfImplementation,
            JsfLibrary jsfLibrary) {
        // Update pom.xml with JSF/Primefaces dependencies and repositories
        final Element configuration = XmlUtils.getConfiguration(getClass());

        if (jsfImplementation == null) {
            // JSF implementation was not specified by user so first query POM
            // to determine if there is an existing JSF dependency and use it,
            // otherwise default to Oracle Mojarra
            jsfImplementation = getExistingOrDefaultJsfImplementation(configuration);
        }

        if (jsfLibrary == null) {
            // JSF component libraru was not specified by user so first query
            // POM to determine if there is an existing JSF dependency and use
            // it, otherwise default to PrimeFaces
            jsfLibrary = getExistingOrDefaultJsfLibrary(configuration);
        }

        updateDependencies(configuration, jsfImplementation, jsfLibrary);
        updateRepositories(configuration, jsfImplementation, jsfLibrary);
    }

    private JsfImplementation getExistingOrDefaultJsfImplementation(
            final Element configuration) {
        final Pom pom = projectOperations
                .getPomFromModuleName(projectOperations.getFocusedModuleName());
        JsfImplementation existingJsfImplementation = null;
        for (JsfImplementation value : JsfImplementation.values()) {
            final Element jsfDependencyElement = XmlUtils.findFirstElement(
                    JSF_IMPLEMENTATION_XPATH + "[@id = '" + value.name() + "']"
                            + DEPENDENCY_XPATH, configuration);
            if (jsfDependencyElement != null
                    && pom.isDependencyRegistered(new Dependency(
                            jsfDependencyElement))) {
                existingJsfImplementation = value;
                break;
            }
        }
        return existingJsfImplementation == null ? JsfImplementation.ORACLE_MOJARRA
                : existingJsfImplementation;
    }

    private JsfLibrary getExistingOrDefaultJsfLibrary(
            final Element configuration) {
        final Pom pom = projectOperations
                .getPomFromModuleName(projectOperations.getFocusedModuleName());
        JsfLibrary existingJsfImplementation = null;
        for (JsfLibrary value : JsfLibrary.values()) {
            final Element jsfDependencyElement = XmlUtils.findFirstElement(
                    JSF_LIBRARY_XPATH + "[@id = '" + value.name() + "']"
                            + DEPENDENCY_XPATH, configuration);
            if (jsfDependencyElement != null
                    && pom.isDependencyRegistered(new Dependency(
                            jsfDependencyElement))) {
                existingJsfImplementation = value;
                break;
            }
        }
        return existingJsfImplementation == null ? JsfLibrary.PRIMEFACES
                : existingJsfImplementation;
    }

    private List<Dependency> getDependencies(final String xPathExpression,
            final Element configuration) {
        final List<Dependency> dependencies = new ArrayList<Dependency>();
        for (Element dependencyElement : XmlUtils.findElements(xPathExpression
                + DEPENDENCY_XPATH, configuration)) {
            dependencies.add(new Dependency(dependencyElement));
        }
        return dependencies;
    }

    private void updateDependencies(final Element configuration,
            final JsfImplementation jsfImplementation,
            final JsfLibrary jsfLibrary) {
        final List<Dependency> requiredDependencyElements = new ArrayList<Dependency>();

        final List<Element> jsfImplementationDependencyElements = XmlUtils
                .findElements(jsfImplementation.getConfigPrefix()
                        + DEPENDENCY_XPATH, configuration);
        for (Element dependencyElement : jsfImplementationDependencyElements) {
            requiredDependencyElements.add(new Dependency(dependencyElement));
        }

        final List<Element> jsfLibraryDependencyElements = XmlUtils
                .findElements(jsfLibrary.getConfigPrefix() + DEPENDENCY_XPATH,
                        configuration);
        for (Element dependencyElement : jsfLibraryDependencyElements) {
            requiredDependencyElements.add(new Dependency(dependencyElement));
        }

        final List<Element> jsfDependencyElements = XmlUtils.findElements(
                "/configuration/jsf" + DEPENDENCY_XPATH, configuration);
        for (Element dependencyElement : jsfDependencyElements) {
            requiredDependencyElements.add(new Dependency(dependencyElement));
        }

        // Remove redundant dependencies
        final List<Dependency> redundantDependencyElements = new ArrayList<Dependency>();

        final List<JsfImplementation> unwantedJsfImplementations = getUnwantedJsfImplementations(jsfImplementation);
        if (!unwantedJsfImplementations.isEmpty()) {
            redundantDependencyElements.addAll(getDependencies(
                    getJsfImplementationXPath(unwantedJsfImplementations),
                    configuration));
        }

        final List<JsfLibrary> unwantedJsfLibraries = getUnwantedJsfLibraries(jsfLibrary);
        if (!unwantedJsfLibraries.isEmpty()) {
            redundantDependencyElements.addAll(getDependencies(
                    getJsfLibraryXPath(unwantedJsfLibraries), configuration));
        }

        // Don't remove any we actually need
        redundantDependencyElements.removeAll(requiredDependencyElements);

        // Update the POM
        projectOperations.addDependencies(
                projectOperations.getFocusedModuleName(),
                requiredDependencyElements);
        projectOperations.removeDependencies(
                projectOperations.getFocusedModuleName(),
                redundantDependencyElements);
    }

    private void updateRepositories(final Element configuration,
            final JsfImplementation jsfImplementation,
            final JsfLibrary jsfLibrary) {
        List<Repository> repositories = new ArrayList<Repository>();

        List<Element> jsfRepositoryElements = XmlUtils.findElements(
                jsfImplementation.getConfigPrefix() + REPOSITORY_XPATH,
                configuration);
        for (Element repositoryElement : jsfRepositoryElements) {
            repositories.add(new Repository(repositoryElement));
        }

        List<Element> jsfLibraryRepositoryElements = XmlUtils.findElements(
                jsfLibrary.getConfigPrefix() + REPOSITORY_XPATH, configuration);
        for (Element repositoryElement : jsfLibraryRepositoryElements) {
            repositories.add(new Repository(repositoryElement));
        }

        projectOperations.addRepositories(
                projectOperations.getFocusedModuleName(), repositories);
    }

    private List<JsfImplementation> getUnwantedJsfImplementations(
            final JsfImplementation jsfImplementation) {
        final List<JsfImplementation> unwantedJsfImplementations = new ArrayList<JsfImplementation>(
                Arrays.asList(JsfImplementation.values()));
        unwantedJsfImplementations.remove(jsfImplementation);
        return unwantedJsfImplementations;
    }

    private List<JsfLibrary> getUnwantedJsfLibraries(final JsfLibrary jsfLibrary) {
        final List<JsfLibrary> unwantedJsfLibraries = new ArrayList<JsfLibrary>(
                Arrays.asList(JsfLibrary.values()));
        unwantedJsfLibraries.remove(jsfLibrary);
        return unwantedJsfLibraries;
    }

    private void installBean(final String templateName,
            final JavaPackage destinationPackage) {
        String beanName = templateName.substring(0,
                templateName.indexOf("-template"));
        JavaType javaType = new JavaType(
                destinationPackage.getFullyQualifiedPackageName() + "."
                        + beanName);
        String physicalTypeIdentifier = PhysicalTypeIdentifier
                .createIdentifier(javaType,
                        pathResolver.getFocusedPath(Path.SRC_MAIN_JAVA));
        String physicalPath = typeLocationService
                .getPhysicalTypeCanonicalPath(physicalTypeIdentifier);
        if (fileManager.exists(physicalPath)) {
            return;
        }

        InputStream inputStream = null;
        try {
            inputStream = FileUtils.getInputStream(getClass(), templateName);
            String input = FileCopyUtils.copyToString(new InputStreamReader(
                    inputStream));
            input = input.replace("__PACKAGE__",
                    destinationPackage.getFullyQualifiedPackageName());
            fileManager.createOrUpdateTextFileIfRequired(physicalPath, input,
                    false);

            shell.flash(Level.FINE,
                    "Created " + javaType.getFullyQualifiedTypeName(),
                    JsfOperationsImpl.class.getName());
            shell.flash(Level.FINE, "", JsfOperationsImpl.class.getName());
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to create '" + physicalPath
                    + "'", e);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private String getJsfImplementationXPath(
            final List<JsfImplementation> jsfImplementations) {
        StringBuilder builder = new StringBuilder(JSF_IMPLEMENTATION_XPATH)
                .append("[");
        for (int i = 0; i < jsfImplementations.size(); i++) {
            if (i > 0) {
                builder.append(" or ");
            }
            builder.append("@id = '");
            builder.append(jsfImplementations.get(i).name());
            builder.append("'");
        }
        builder.append("]");
        return builder.toString();
    }

    private String getJsfLibraryXPath(final List<JsfLibrary> jsfLibraries) {
        StringBuilder builder = new StringBuilder(JSF_LIBRARY_XPATH)
                .append("[");
        for (int i = 0; i < jsfLibraries.size(); i++) {
            if (i > 0) {
                builder.append(" or ");
            }
            builder.append("@id = '");
            builder.append(jsfLibraries.get(i).name());
            builder.append("'");
        }
        builder.append("]");
        return builder.toString();
    }
}
