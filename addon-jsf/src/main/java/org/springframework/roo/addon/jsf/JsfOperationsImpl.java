package org.springframework.roo.addon.jsf;

import static org.springframework.roo.model.RooJavaType.ROO_JSF_CONVERTER;
import static org.springframework.roo.model.RooJavaType.ROO_JSF_MANAGED_BEAN;
import static org.springframework.roo.model.RooJavaType.ROO_SERIALIZABLE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
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
import org.springframework.roo.model.RooJavaType;
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
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.FileUtils;
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

    private static final String DEPENDENCY_XPATH = "/dependencies/dependency";
    private static final String JSF_IMPLEMENTATION_XPATH = "/configuration/jsf-implementations/jsf-implementation";
    private static final String JSF_LIBRARY_XPATH = "/configuration/jsf-libraries/jsf-library";
    private static final String MYFACES_LISTENER = "org.apache.myfaces.webapp.StartupServletContextListener";
    private static final String MOJARRA_LISTENER = "com.sun.faces.config.ConfigureListener";
    private static final String PRIMEFACES_THEMES_VERSION = "1.0.10";
    private static final String REPOSITORY_XPATH = "/repositories/repository";

    @Reference private MetadataDependencyRegistry metadataDependencyRegistry;
    @Reference private MetadataService metadataService;
    @Reference private PathResolver pathResolver;
    @Reference private ProjectOperations projectOperations;
    @Reference private Shell shell;
    @Reference private TypeLocationService typeLocationService;
    @Reference private TypeManagementService typeManagementService;

    public void addMediaSuurce(final String url, MediaPlayer mediaPlayer) {
        Validate.notBlank(url, "Media source url required");

        final String mainPage = projectOperations.getPathResolver()
                .getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "pages/main.xhtml");
        final Document document = XmlUtils.readXml(fileManager
                .getInputStream(mainPage));
        final Element root = document.getDocumentElement();
        final Element element = DomUtils
                .findFirstElementByName("p:panel", root);
        if (element == null) {
            return;
        }

        if (mediaPlayer == null) {
            mp: for (final MediaPlayer mp : MediaPlayer.values()) {
                for (final String mediaType : mp.getMediaTypes()) {
                    if (StringUtils.lowerCase(url).contains(mediaType)) {
                        mediaPlayer = mp;
                        break mp;
                    }
                }
            }
        }

        if (url.contains("youtube")) {
            mediaPlayer = MediaPlayer.FLASH;
        }

        final Element paraElement = new XmlElementBuilder("p", document)
                .build();
        Element mediaElement;
        if (mediaPlayer == null) {
            mediaElement = new XmlElementBuilder("p:media", document)
                    .addAttribute("value", url).build();
        }
        else {
            mediaElement = new XmlElementBuilder("p:media", document)
                    .addAttribute("value", url)
                    .addAttribute("player",
                            StringUtils.lowerCase(mediaPlayer.name())).build();
        }
        paraElement.appendChild(mediaElement);
        element.appendChild(paraElement);

        fileManager.createOrUpdateTextFileIfRequired(mainPage,
                XmlUtils.nodeToString(document), false);
    }

    private void addOrRemoveListener(final JsfImplementation jsfImplementation,
            final Document document) {
        Validate.notNull(jsfImplementation, "JSF implementation required");
        Validate.notNull(document, "web.xml document required");

        final Element root = document.getDocumentElement();
        final Element webAppElement = XmlUtils.findFirstElement("/web-app",
                root);
        Element listenerElement;
        switch (jsfImplementation) {
        case ORACLE_MOJARRA:
            listenerElement = XmlUtils.findFirstElement(
                    "listener[listener-class = '" + MYFACES_LISTENER + "']",
                    webAppElement);
            if (listenerElement != null) {
                webAppElement.removeChild(listenerElement);
                DomUtils.removeTextNodes(webAppElement);
            }
            listenerElement = XmlUtils.findFirstElement(
                    "listener[listener-class = '" + MOJARRA_LISTENER + "']",
                    webAppElement);
            if (listenerElement == null) {
                WebXmlUtils.addListener(MOJARRA_LISTENER, document, "");
                DomUtils.removeTextNodes(webAppElement);
            }
            break;
        case APACHE_MYFACES:
            listenerElement = XmlUtils.findFirstElement(
                    "listener[listener-class = '" + MOJARRA_LISTENER + "']",
                    webAppElement);
            if (listenerElement != null) {
                webAppElement.removeChild(listenerElement);
                DomUtils.removeTextNodes(webAppElement);
            }
            listenerElement = XmlUtils.findFirstElement(
                    "listener[listener-class = '" + MYFACES_LISTENER + "']",
                    webAppElement);
            if (listenerElement == null) {
                WebXmlUtils.addListener(MYFACES_LISTENER, document, "");
                DomUtils.removeTextNodes(webAppElement);
            }
            break;
        }
    }

    private void changePrimeFacesTheme(final Theme theme,
            final Document document) {
        Validate.notNull(theme, "Theme required");
        Validate.notNull(document, "web.xml document required");

        // Add theme to the pom if not already there
        final String themeName = StringUtils.lowerCase(theme.name().replace(
                "_", "-"));
        projectOperations.addDependency(
                projectOperations.getFocusedModuleName(),
                "org.primefaces.themes", themeName, PRIMEFACES_THEMES_VERSION);

        // Update the web.xml primefaces.THEME content-param
        final Element root = document.getDocumentElement();

        final Element contextParamElement = XmlUtils
                .findFirstElement(
                        "/web-app/context-param[param-name = 'primefaces.THEME']",
                        root);
        Validate.notNull(contextParamElement,
                "The web.xml primefaces.THEME context param element required");
        final Element paramValueElement = XmlUtils.findFirstElement(
                "param-value", contextParamElement);
        Validate.notNull(paramValueElement,
                "primefaces.THEME param-value element required");
        paramValueElement.setTextContent(themeName);
    }

    private void copyEntityTypePage(final JavaType entity,
            final String beanName, final String plural) {
        final String domainTypeFile = projectOperations.getPathResolver()
                .getFocusedIdentifier(
                        Path.SRC_MAIN_WEBAPP,
                        "pages/"
                                + JavaSymbolName
                                        .getReservedWordSafeName(entity)
                                + ".xhtml");
        InputStream inputStream = null;
        try {
            inputStream = FileUtils.getInputStream(getClass(),
                    "pages/content-template.xhtml");
            String input = IOUtils.toString(inputStream);
            input = input.replace("__BEAN_NAME__", beanName);
            input = input
                    .replace("__DOMAIN_TYPE__", entity.getSimpleTypeName());
            input = input.replace("__LC_DOMAIN_TYPE__", JavaSymbolName
                    .getReservedWordSafeName(entity).getSymbolName());
            input = input.replace("__DOMAIN_TYPE_PLURAL__", plural);

            fileManager.createOrUpdateTextFileIfRequired(domainTypeFile, input,
                    false);
        }
        catch (final IOException e) {
            throw new IllegalStateException("Unable to create '"
                    + domainTypeFile + "'", e);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private void createConverter(final JavaPackage javaPackage,
            final JavaType entity) {
        // Create type annotation for new converter class
        final JavaType converterType = new JavaType(
                javaPackage.getFullyQualifiedPackageName() + "."
                        + entity.getSimpleTypeName() + "Converter");
        final AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(
                ROO_JSF_CONVERTER);
        annotationBuilder.addClassAttribute("entity", entity);
        final String declaredByMetadataId = PhysicalTypeIdentifier
                .createIdentifier(converterType,
                        pathResolver.getFocusedPath(Path.SRC_MAIN_JAVA));
        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                declaredByMetadataId, Modifier.PUBLIC, converterType,
                PhysicalTypeCategory.CLASS);
        cidBuilder.addAnnotation(annotationBuilder);
        cidBuilder.addImplementsType(JsfJavaType.CONVERTER);

        typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());

        shell.flash(Level.FINE,
                "Created " + converterType.getFullyQualifiedTypeName(),
                JsfOperationsImpl.class.getName());
        shell.flash(Level.FINE, "", JsfOperationsImpl.class.getName());
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
        installBean("MessageFactory-template.java", utilPackage);

        if (fileManager.exists(typeLocationService
                .getPhysicalTypeCanonicalPath(managedBean,
                        pathResolver.getFocusedPath(Path.SRC_MAIN_JAVA)))) {
            // Type exists already - nothing to do
            return;
        }

        final ClassOrInterfaceTypeDetails entityTypeDetails = typeLocationService
                .getTypeDetails(entity);
        Validate.notNull(entityTypeDetails,
                "The type '%s' could not be resolved", entity);

        final PluralMetadata pluralMetadata = (PluralMetadata) metadataService
                .get(PluralMetadata.createIdentifier(entity,
                        PhysicalTypeIdentifier.getPath(entityTypeDetails
                                .getDeclaredByMetadataId())));
        Validate.notNull(pluralMetadata,
                "The plural for type '%s' could not be resolved", entity);

        // Create type annotation for new managed bean
        final AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(
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

        final LogicalPath managedBeanPath = pathResolver
                .getFocusedPath(Path.SRC_MAIN_JAVA);
        final String resourceIdentifier = typeLocationService
                .getPhysicalTypeCanonicalPath(managedBean, managedBeanPath);
        final String declaredByMetadataId = PhysicalTypeIdentifier
                .createIdentifier(managedBean,
                        pathResolver.getPath(resourceIdentifier));

        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                declaredByMetadataId, Modifier.PUBLIC, managedBean,
                PhysicalTypeCategory.CLASS);
        cidBuilder
                .addAnnotation(new AnnotationMetadataBuilder(ROO_SERIALIZABLE));
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

    private void createOrUpdateWebXml(
            final JsfImplementation jsfImplementation, final Theme theme) {
        final String webXmlPath = getWebXmlFile();

        final Document document;
        if (fileManager.exists(webXmlPath)) {
            document = XmlUtils.readXml(fileManager.getInputStream(webXmlPath));
        }
        else {
            document = getDocumentTemplate("WEB-INF/web-template.xml");
            final String projectName = projectOperations.getFocusedModule()
                    .getDisplayName();
            WebXmlUtils.setDisplayName(projectName, document, null);
            WebXmlUtils.setDescription("Roo generated " + projectName
                    + " application", document, null);
        }

        if (jsfImplementation != null) {
            addOrRemoveListener(jsfImplementation, document);
        }
        if (theme != null) {
            changePrimeFacesTheme(theme, document);
        }

        fileManager.createOrUpdateTextFileIfRequired(webXmlPath,
                XmlUtils.nodeToString(document), false);
    }

    public void generateAll(final JavaPackage destinationPackage) {
        Validate.notNull(destinationPackage, "Destination package required");

        // Create JSF managed bean for each entity
        generateManagedBeans(destinationPackage);
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

    private List<Dependency> getDependencies(final String xPathExpression,
            final Element configuration) {
        final List<Dependency> dependencies = new ArrayList<Dependency>();
        for (final Element dependencyElement : XmlUtils.findElements(
                xPathExpression + DEPENDENCY_XPATH, configuration)) {
            dependencies.add(new Dependency(dependencyElement));
        }
        return dependencies;
    }

    private JsfImplementation getExistingOrDefaultJsfImplementation(
            final Element configuration) {
        final Pom pom = projectOperations
                .getPomFromModuleName(projectOperations.getFocusedModuleName());
        JsfImplementation existingJsfImplementation = null;
        for (final JsfImplementation value : JsfImplementation.values()) {
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
        for (final JsfLibrary value : JsfLibrary.values()) {
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

    private String getFacesConfigFile() {
        return projectOperations.getPathResolver().getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, "WEB-INF/faces-config.xml");
    }

    private String getJsfImplementationXPath(
            final List<JsfImplementation> jsfImplementations) {
        final StringBuilder builder = new StringBuilder(
                JSF_IMPLEMENTATION_XPATH).append("[");
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
        final StringBuilder builder = new StringBuilder(JSF_LIBRARY_XPATH)
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

    public String getName() {
        return FeatureNames.JSF;
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

    private String getWebXmlFile() {
        return projectOperations.getPathResolver().getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml");
    }

    private boolean hasFacesConfig() {
        return fileManager.exists(getFacesConfigFile());
    }

    private void installBean(final String templateName,
            final JavaPackage destinationPackage) {
        final String beanName = templateName.substring(0,
                templateName.indexOf("-template"));
        final JavaType javaType = new JavaType(
                destinationPackage.getFullyQualifiedPackageName() + "."
                        + beanName);
        final String physicalTypeIdentifier = PhysicalTypeIdentifier
                .createIdentifier(javaType,
                        pathResolver.getFocusedPath(Path.SRC_MAIN_JAVA));
        final String physicalPath = typeLocationService
                .getPhysicalTypeCanonicalPath(physicalTypeIdentifier);
        if (fileManager.exists(physicalPath)) {
            return;
        }

        InputStream inputStream = null;
        try {
            inputStream = FileUtils.getInputStream(getClass(), templateName);
            String input = IOUtils.toString(inputStream);
            input = input.replace("__PACKAGE__",
                    destinationPackage.getFullyQualifiedPackageName());
            fileManager.createOrUpdateTextFileIfRequired(physicalPath, input,
                    false);

            shell.flash(Level.FINE,
                    "Created " + javaType.getFullyQualifiedTypeName(),
                    JsfOperationsImpl.class.getName());
            shell.flash(Level.FINE, "", JsfOperationsImpl.class.getName());
        }
        catch (final IOException e) {
            throw new IllegalStateException("Unable to create '" + physicalPath
                    + "'", e);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private void installFacesConfig(final JavaPackage destinationPackage) {
        Validate.isTrue(projectOperations.isFocusedProjectAvailable(),
                "Project metadata required");
        if (hasFacesConfig()) {
            return;
        }

        InputStream inputStream = null;
        try {
            inputStream = FileUtils.getInputStream(getClass(),
                    "WEB-INF/faces-config-template.xml");
            String input = IOUtils.toString(inputStream);
            input = input.replace("__PACKAGE__",
                    destinationPackage.getFullyQualifiedPackageName());
            fileManager.createOrUpdateTextFileIfRequired(getFacesConfigFile(),
                    input, false);
        }
        catch (final IOException e) {
            throw new IllegalStateException(
                    "Unable to create 'faces.config.xml'", e);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private void installI18n(final JavaPackage destinationPackage) {
        final String packagePath = destinationPackage
                .getFullyQualifiedPackageName()
                .replace('.', File.separatorChar);
        final String i18nDirectory = projectOperations.getPathResolver()
                .getFocusedIdentifier(Path.SRC_MAIN_RESOURCES,
                        packagePath + "/i18n");
        copyDirectoryContents("i18n/*.properties", i18nDirectory, false);
    }

    public boolean isInstalledInModule(final String moduleName) {
        final LogicalPath webAppPath = LogicalPath.getInstance(
                Path.SRC_MAIN_WEBAPP, moduleName);
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

    public void setup(JsfImplementation jsfImplementation,
            final JsfLibrary jsfLibrary, final Theme theme) {
        jsfImplementation = updateConfiguration(jsfImplementation, jsfLibrary);
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

    private JsfImplementation updateConfiguration(
            JsfImplementation jsfImplementation, JsfLibrary jsfLibrary) {
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
        return jsfImplementation;
    }

    private void updateDependencies(final Element configuration,
            final JsfImplementation jsfImplementation,
            final JsfLibrary jsfLibrary) {
        final List<Dependency> requiredDependencyElements = new ArrayList<Dependency>();

        final List<Element> jsfImplementationDependencyElements = XmlUtils
                .findElements(jsfImplementation.getConfigPrefix()
                        + DEPENDENCY_XPATH, configuration);
        for (final Element dependencyElement : jsfImplementationDependencyElements) {
            requiredDependencyElements.add(new Dependency(dependencyElement));
        }

        final List<Element> jsfLibraryDependencyElements = XmlUtils
                .findElements(jsfLibrary.getConfigPrefix() + DEPENDENCY_XPATH,
                        configuration);
        for (final Element dependencyElement : jsfLibraryDependencyElements) {
            requiredDependencyElements.add(new Dependency(dependencyElement));
        }

        final List<Element> jsfDependencyElements = XmlUtils.findElements(
                "/configuration/jsf" + DEPENDENCY_XPATH, configuration);
        for (final Element dependencyElement : jsfDependencyElements) {
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
        final List<Repository> repositories = new ArrayList<Repository>();

        final List<Element> jsfRepositoryElements = XmlUtils.findElements(
                jsfImplementation.getConfigPrefix() + REPOSITORY_XPATH,
                configuration);
        for (final Element repositoryElement : jsfRepositoryElements) {
            repositories.add(new Repository(repositoryElement));
        }

        final List<Element> jsfLibraryRepositoryElements = XmlUtils
                .findElements(jsfLibrary.getConfigPrefix() + REPOSITORY_XPATH,
                        configuration);
        for (final Element repositoryElement : jsfLibraryRepositoryElements) {
            repositories.add(new Repository(repositoryElement));
        }

        projectOperations.addRepositories(
                projectOperations.getFocusedModuleName(), repositories);
    }
}
