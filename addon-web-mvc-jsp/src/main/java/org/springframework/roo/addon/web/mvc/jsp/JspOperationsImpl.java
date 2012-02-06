package org.springframework.roo.addon.web.mvc.jsp;

import static org.springframework.roo.model.SpringJavaType.CONTROLLER;
import static org.springframework.roo.model.SpringJavaType.MODEL_MAP;
import static org.springframework.roo.model.SpringJavaType.PATH_VARIABLE;
import static org.springframework.roo.model.SpringJavaType.REQUEST_MAPPING;
import static org.springframework.roo.model.SpringJavaType.REQUEST_METHOD;
import static org.springframework.roo.project.Path.SRC_MAIN_WEBAPP;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.backup.BackupOperations;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.addon.web.mvc.controller.WebMvcOperations;
import org.springframework.roo.addon.web.mvc.jsp.i18n.I18n;
import org.springframework.roo.addon.web.mvc.jsp.i18n.I18nSupport;
import org.springframework.roo.addon.web.mvc.jsp.menu.MenuOperations;
import org.springframework.roo.addon.web.mvc.jsp.tiles.TilesOperations;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.operations.AbstractOperations;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.osgi.BundleFindingUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.springframework.roo.uaa.UaaRegistrationService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Implementation of {@link JspOperations}.
 * 
 * @author Stefan Schmidt
 * @author Jeremy Grelle
 * @since 1.0
 */
@Component
@Service
public class JspOperationsImpl extends AbstractOperations implements
        JspOperations {

    private static final JavaType HTTP_SERVLET_REQUEST = new JavaType(
            "javax.servlet.http.HttpServletRequest");
    private static final JavaType HTTP_SERVLET_RESPONSE = new JavaType(
            "javax.servlet.http.HttpServletResponse");

    /**
     * Returns the folder name and mapping value for the given preferred maaping
     * 
     * @param preferredMapping (can be blank)
     * @param controller the associated controller type (required)
     * @return a non-<code>null</code> pair
     */
    static ImmutablePair<String, String> getFolderAndMapping(
            final String preferredMapping, final JavaType controller) {
        if (StringUtils.isNotBlank(preferredMapping)) {
            String folderName = StringUtils.removeStart(preferredMapping, "/");
            folderName = StringUtils.removeEnd(folderName, "**");
            folderName = StringUtils.removeEnd(folderName, "/");

            String mapping = (preferredMapping.startsWith("/") ? "" : "/")
                    + preferredMapping;
            mapping = StringUtils.removeEnd(mapping, "/");
            mapping = mapping + (mapping.endsWith("/**") ? "" : "/**");

            return new ImmutablePair<String, String>(folderName, mapping);
        }

        // Use sensible defaults
        final String typeNameLower = StringUtils.removeEnd(
                controller.getSimpleTypeName(), "Controller").toLowerCase();
        return new ImmutablePair<String, String>(typeNameLower, "/"
                + typeNameLower + "/**");
    }

    @Reference private BackupOperations backupOperations;
    @Reference private I18nSupport i18nSupport;
    @Reference private MenuOperations menuOperations;
    @Reference private PathResolver pathResolver;
    @Reference private ProjectOperations projectOperations;
    @Reference private PropFileOperations propFileOperations;
    @Reference private TilesOperations tilesOperations;
    @Reference private TypeManagementService typeManagementService;
    @Reference private UaaRegistrationService uaaRegistrationService;
    @Reference private WebMvcOperations webMvcOperations;

    private String cleanPath(String path) {
        if ("/".equals(path)) {
            return "";
        }
        path = "/" + path;
        if (path.contains(".")) {
            path = path.substring(0, path.indexOf(".") - 1);
        }
        return path;
    }

    private String cleanViewName(String viewName) {
        if (viewName.startsWith("/")) {
            viewName = viewName.substring(1);
        }
        if (viewName.contains(".")) {
            viewName = viewName.substring(0, viewName.indexOf(".") - 1);
        }
        return viewName;
    }

    /**
     * Creates a new Spring MVC controller.
     * <p>
     * Request mappings assigned by this method will always commence with "/"
     * and end with "/**". You may present this prefix and/or this suffix if you
     * wish, although it will automatically be added should it not be provided.
     * 
     * @param controller the controller class to create (required)
     * @param preferredMapping the mapping this controller should adopt
     *            (optional; if unspecified it will be based on the controller
     *            name)
     */
    public void createManualController(final JavaType controller,
            final String preferredMapping, final LogicalPath webappPath) {
        Validate.notNull(controller, "Controller Java Type required");

        // Create annotation @RequestMapping("/myobject/**")
        final ImmutablePair<String, String> folderAndMapping = getFolderAndMapping(
                preferredMapping, controller);
        final String folderName = folderAndMapping.getKey();

        final String resourceIdentifier = pathResolver.getFocusedCanonicalPath(
                Path.SRC_MAIN_JAVA, controller);
        final String declaredByMetadataId = PhysicalTypeIdentifier
                .createIdentifier(controller, projectOperations
                        .getPathResolver().getPath(resourceIdentifier));
        final List<MethodMetadataBuilder> methods = new ArrayList<MethodMetadataBuilder>();

        // Add HTTP post method
        methods.add(getHttpPostMethod(declaredByMetadataId));

        // Add index method
        methods.add(getIndexMethod(folderName, declaredByMetadataId));

        // Create Type definition
        final List<AnnotationMetadataBuilder> typeAnnotations = new ArrayList<AnnotationMetadataBuilder>();

        final List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
        requestMappingAttributes.add(new StringAttributeValue(
                new JavaSymbolName("value"), folderAndMapping.getValue()));
        final AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(
                REQUEST_MAPPING, requestMappingAttributes);
        typeAnnotations.add(requestMapping);

        // Create annotation @Controller
        final List<AnnotationAttributeValue<?>> controllerAttributes = new ArrayList<AnnotationAttributeValue<?>>();
        final AnnotationMetadataBuilder controllerAnnotation = new AnnotationMetadataBuilder(
                CONTROLLER, controllerAttributes);
        typeAnnotations.add(controllerAnnotation);

        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                declaredByMetadataId, Modifier.PUBLIC, controller,
                PhysicalTypeCategory.CLASS);
        cidBuilder.setAnnotations(typeAnnotations);
        cidBuilder.setDeclaredMethods(methods);
        typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());

        installView(
                folderName,
                "/index",
                new JavaSymbolName(controller.getSimpleTypeName())
                        .getReadableSymbolName() + " View", "Controller", null,
                false, webappPath);
    }

    private MethodMetadataBuilder getHttpPostMethod(
            final String declaredByMetadataId) {
        final List<AnnotationMetadataBuilder> postMethodAnnotations = new ArrayList<AnnotationMetadataBuilder>();
        final List<AnnotationAttributeValue<?>> postMethodAttributes = new ArrayList<AnnotationAttributeValue<?>>();
        postMethodAttributes.add(new EnumAttributeValue(new JavaSymbolName(
                "method"), new EnumDetails(REQUEST_METHOD, new JavaSymbolName(
                "POST"))));
        postMethodAttributes.add(new StringAttributeValue(new JavaSymbolName(
                "value"), "{id}"));
        postMethodAnnotations.add(new AnnotationMetadataBuilder(
                REQUEST_MAPPING, postMethodAttributes));

        final List<AnnotatedJavaType> postParamTypes = new ArrayList<AnnotatedJavaType>();
        final AnnotationMetadataBuilder idParamAnnotation = new AnnotationMetadataBuilder(
                PATH_VARIABLE);
        postParamTypes.add(new AnnotatedJavaType(
                new JavaType("java.lang.Long"), idParamAnnotation.build()));
        postParamTypes.add(new AnnotatedJavaType(MODEL_MAP));
        postParamTypes.add(new AnnotatedJavaType(HTTP_SERVLET_REQUEST));
        postParamTypes.add(new AnnotatedJavaType(HTTP_SERVLET_RESPONSE));

        final List<JavaSymbolName> postParamNames = new ArrayList<JavaSymbolName>();
        postParamNames.add(new JavaSymbolName("id"));
        postParamNames.add(new JavaSymbolName("modelMap"));
        postParamNames.add(new JavaSymbolName("request"));
        postParamNames.add(new JavaSymbolName("response"));

        final MethodMetadataBuilder postMethodBuilder = new MethodMetadataBuilder(
                declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName(
                        "post"), JavaType.VOID_PRIMITIVE, postParamTypes,
                postParamNames, new InvocableMemberBodyBuilder());
        postMethodBuilder.setAnnotations(postMethodAnnotations);
        return postMethodBuilder;
    }

    private MethodMetadataBuilder getIndexMethod(final String folderName,
            final String declaredByMetadataId) {
        final List<AnnotationMetadataBuilder> indexMethodAnnotations = new ArrayList<AnnotationMetadataBuilder>();
        indexMethodAnnotations.add(new AnnotationMetadataBuilder(
                REQUEST_MAPPING, new ArrayList<AnnotationAttributeValue<?>>()));
        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("return \"" + folderName + "/index\";");
        final MethodMetadataBuilder indexMethodBuilder = new MethodMetadataBuilder(
                declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName(
                        "index"), JavaType.STRING,
                new ArrayList<AnnotatedJavaType>(),
                new ArrayList<JavaSymbolName>(), bodyBuilder);
        indexMethodBuilder.setAnnotations(indexMethodAnnotations);
        return indexMethodBuilder;
    }

    public String getName() {
        return FeatureNames.MVC;
    }

    public void installCommonViewArtefacts() {
        installCommonViewArtefacts(projectOperations.getFocusedModuleName());
    }

    public void installCommonViewArtefacts(final String moduleName) {
        Validate.isTrue(isProjectAvailable(), "Project metadata required");
        final LogicalPath webappPath = Path.SRC_MAIN_WEBAPP
                .getModulePathId(moduleName);
        if (!isControllerAvailable()) {
            webMvcOperations.installAllWebMvcArtifacts();
        }

        // Install tiles config
        updateConfiguration();

        // Install styles
        copyDirectoryContents("images/*.*",
                pathResolver.getIdentifier(webappPath, "images"), false);

        // Install styles
        copyDirectoryContents("styles/*.css",
                pathResolver.getIdentifier(webappPath, "styles"), false);
        copyDirectoryContents("styles/*.properties",
                pathResolver.getIdentifier(webappPath, "WEB-INF/classes"),
                false);

        // Install layout
        copyDirectoryContents("tiles/default.jspx",
                pathResolver.getIdentifier(webappPath, "WEB-INF/layouts/"),
                false);
        copyDirectoryContents("tiles/layouts.xml",
                pathResolver.getIdentifier(webappPath, "WEB-INF/layouts/"),
                false);
        copyDirectoryContents("tiles/header.jspx",
                pathResolver.getIdentifier(webappPath, "WEB-INF/views/"), false);
        copyDirectoryContents("tiles/menu.jspx",
                pathResolver.getIdentifier(webappPath, "WEB-INF/views/"), false);
        copyDirectoryContents("tiles/footer.jspx",
                pathResolver.getIdentifier(webappPath, "WEB-INF/views/"), false);
        copyDirectoryContents("tiles/views.xml",
                pathResolver.getIdentifier(webappPath, "WEB-INF/views/"), false);

        // Install common view files
        copyDirectoryContents("*.jspx",
                pathResolver.getIdentifier(webappPath, "WEB-INF/views/"), false);

        // Install tags
        copyDirectoryContents("tags/form/*.tagx",
                pathResolver.getIdentifier(webappPath, "WEB-INF/tags/form"),
                false);
        copyDirectoryContents("tags/form/fields/*.tagx",
                pathResolver.getIdentifier(webappPath,
                        "WEB-INF/tags/form/fields"), false);
        copyDirectoryContents("tags/menu/*.tagx",
                pathResolver.getIdentifier(webappPath, "WEB-INF/tags/menu"),
                false);
        copyDirectoryContents("tags/util/*.tagx",
                pathResolver.getIdentifier(webappPath, "WEB-INF/tags/util"),
                false);

        // Install default language 'en'
        installI18n(i18nSupport.getLanguage(Locale.ENGLISH), webappPath);

        final String i18nDirectory = pathResolver.getIdentifier(webappPath,
                "WEB-INF/i18n/application.properties");
        if (!fileManager.exists(i18nDirectory)) {
            try {
                final String projectName = projectOperations
                        .getProjectName(projectOperations
                                .getFocusedModuleName());
                fileManager.createFile(pathResolver.getIdentifier(webappPath,
                        "WEB-INF/i18n/application.properties"));
                propFileOperations
                        .addPropertyIfNotExists(webappPath,
                                "WEB-INF/i18n/application.properties",
                                "application_name",
                                projectName.substring(0, 1).toUpperCase()
                                        + projectName.substring(1), true);
            }
            catch (final Exception e) {
                throw new IllegalStateException(
                        "Encountered an error during copying of resources for MVC JSP addon.",
                        e);
            }
        }
    }

    public void installI18n(final I18n i18n, final LogicalPath webappPath) {
        Validate.notNull(i18n, "Language choice required");

        if (i18n.getLocale() == null) {
            LOGGER.warning("could not parse language choice");
            return;
        }

        final String targetDirectory = pathResolver.getIdentifier(webappPath,
                "");

        // Install message bundle
        String messageBundle = targetDirectory + "/WEB-INF/i18n/messages_"
                + i18n.getLocale().getLanguage() /* + country */+ ".properties";
        // Special case for english locale (default)
        if (i18n.getLocale().equals(Locale.ENGLISH)) {
            messageBundle = targetDirectory
                    + "/WEB-INF/i18n/messages.properties";
        }
        if (!fileManager.exists(messageBundle)) {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                inputStream = i18n.getMessageBundle();
                outputStream = fileManager.createFile(messageBundle)
                        .getOutputStream();
                IOUtils.copy(inputStream, outputStream);
            }
            catch (final Exception e) {
                throw new IllegalStateException(
                        "Encountered an error during copying of message bundle MVC JSP addon.",
                        e);
            }
            finally {
                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(outputStream);
            }
        }

        // Install flag
        final String flagGraphic = targetDirectory + "/images/"
                + i18n.getLocale().getLanguage() /* + country */+ ".png";
        if (!fileManager.exists(flagGraphic)) {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                inputStream = i18n.getFlagGraphic();
                outputStream = fileManager.createFile(flagGraphic)
                        .getOutputStream();
                IOUtils.copy(inputStream, outputStream);
            }
            catch (final Exception e) {
                throw new IllegalStateException(
                        "Encountered an error during copying of flag graphic for MVC JSP addon.",
                        e);
            }
            finally {
                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(outputStream);
            }
        }

        // Setup language definition in languages.jspx
        final String footerFileLocation = targetDirectory
                + "/WEB-INF/views/footer.jspx";
        final Document footer = XmlUtils.readXml(fileManager
                .getInputStream(footerFileLocation));

        if (XmlUtils.findFirstElement(
                "//span[@id='language']/language[@locale='"
                        + i18n.getLocale().getLanguage() + "']",
                footer.getDocumentElement()) == null) {
            final Element span = XmlUtils.findRequiredElement(
                    "//span[@id='language']", footer.getDocumentElement());
            span.appendChild(new XmlElementBuilder("util:language", footer)
                    .addAttribute("locale", i18n.getLocale().getLanguage())
                    .addAttribute("label", i18n.getLanguage()).build());
            fileManager.createOrUpdateTextFileIfRequired(footerFileLocation,
                    XmlUtils.nodeToString(footer), false);
        }

        // Record use of add-on (most languages are implemented via public
        // add-ons)
        final String bundleSymbolicName = BundleFindingUtils
                .findFirstBundleForTypeName(context.getBundleContext(), i18n
                        .getClass().getName());
        if (bundleSymbolicName != null) {
            uaaRegistrationService.registerBundleSymbolicNameUse(
                    bundleSymbolicName, null);
        }
    }

    /**
     * Creates a new Spring MVC static view.
     * 
     * @param viewName the bare logical name of the new view (required, e.g.
     *            "index")
     * @param folderName the folder in which to create the view; must be empty
     *            or start with a slash
     * @param title the title
     * @param category the menu category in which to list the new view
     *            (required)
     * @param registerStaticController whether to register a static controller
     *            in the Spring MVC configuration file
     */
    private void installView(final JavaSymbolName viewName,
            final String folderName, final String title, final String category,
            final boolean registerStaticController, final LogicalPath webappPath) {
        // Probe if common web artifacts exist, and install them if needed
        final PathResolver pathResolver = projectOperations.getPathResolver();
        if (!fileManager.exists(pathResolver.getIdentifier(webappPath,
                "WEB-INF/layouts/default.jspx"))) {
            installCommonViewArtefacts(webappPath.getModule());
        }

        final String lcViewName = viewName.getSymbolName().toLowerCase();

        // Update the application-specific resource bundle (i.e. default
        // translation)
        final String messageCode = "label"
                + folderName.replace("/", "_").toLowerCase() + "_" + lcViewName;
        propFileOperations
                .addPropertyIfNotExists(
                        pathResolver.getFocusedPath(Path.SRC_MAIN_WEBAPP),
                        "WEB-INF/i18n/application.properties", messageCode,
                        title, true);

        // Add the menu item
        final String relativeUrl = folderName + "/" + lcViewName;
        menuOperations.addMenuItem(new JavaSymbolName(category),
                new JavaSymbolName(folderName.replace("/", "_").toLowerCase()
                        + lcViewName + "_id"), title, "global_generic",
                relativeUrl, null, webappPath);

        // Add the view definition
        tilesOperations.addViewDefinition(folderName.toLowerCase(),
                pathResolver.getFocusedPath(Path.SRC_MAIN_WEBAPP), relativeUrl,
                TilesOperations.DEFAULT_TEMPLATE,
                "/WEB-INF/views" + folderName.toLowerCase() + "/" + lcViewName
                        + ".jspx");

        if (registerStaticController) {
            // Update the Spring MVC config file
            registerStaticSpringMvcController(relativeUrl, webappPath);
        }
    }

    private void installView(final String path, final String viewName,
            final String title, final String category, Document document,
            final boolean registerStaticController, final LogicalPath webappPath) {
        Validate.notBlank(path, "Path required");
        Validate.notBlank(viewName, "View name required");
        Validate.notBlank(title, "Title required");

        final String cleanedPath = cleanPath(path);
        final String cleanedViewName = cleanViewName(viewName);
        final String lcViewName = cleanedViewName.toLowerCase();

        if (document == null) {
            try {
                document = getDocumentTemplate("index-template.jspx");
                XmlUtils.findRequiredElement("/div/message",
                        document.getDocumentElement()).setAttribute(
                        "code",
                        "label" + cleanedPath.replace("/", "_").toLowerCase()
                                + "_" + lcViewName);
            }
            catch (final Exception e) {
                throw new IllegalStateException(
                        "Encountered an error during copying of resources for controller class.",
                        e);
            }
        }

        final String viewFile = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP,
                "WEB-INF/views" + cleanedPath.toLowerCase() + "/" + lcViewName
                        + ".jspx");
        fileManager.createOrUpdateTextFileIfRequired(viewFile,
                XmlUtils.nodeToString(document), false);

        installView(new JavaSymbolName(lcViewName), cleanedPath, title,
                category, registerStaticController, webappPath);
    }

    public void installView(final String path, final String viewName,
            final String title, final String category, final Document document,
            final LogicalPath webappPath) {
        installView(path, viewName, title, category, document, true, webappPath);
    }

    public void installView(final String path, final String viewName,
            final String title, final String category,
            final LogicalPath webappPath) {
        installView(path, viewName, title, category, null, true, webappPath);
    }

    public boolean isControllerAvailable() {
        return fileManager.exists(pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, "WEB-INF/views"))
                && !projectOperations
                        .isFeatureInstalledInFocusedModule(FeatureNames.JSF);
    }

    public boolean isInstalledInModule(final String moduleName) {
        final LogicalPath webAppPath = LogicalPath.getInstance(
                Path.SRC_MAIN_WEBAPP, moduleName);
        return fileManager.exists(projectOperations.getPathResolver()
                .getIdentifier(webAppPath, "WEB-INF/spring/webmvc-config.xml"));
    }

    public boolean isInstallLanguageCommandAvailable() {
        return isProjectAvailable()
                && fileManager.exists(pathResolver.getFocusedIdentifier(
                        Path.SRC_MAIN_WEBAPP, "WEB-INF/views/footer.jspx"));
    }

    public boolean isMvcInstallationPossible() {
        return isProjectAvailable()
                && !isControllerAvailable()
                && !projectOperations
                        .isFeatureInstalledInFocusedModule(FeatureNames.JSF);
    }

    private boolean isProjectAvailable() {
        return projectOperations.isFocusedProjectAvailable();
    }

    /**
     * Registers a static Spring MVC controller to handle the given relative
     * URL.
     * 
     * @param relativeUrl the relative URL to handle (required); a leading slash
     *            will be added if required
     */
    private void registerStaticSpringMvcController(final String relativeUrl,
            final LogicalPath webappPath) {
        final String mvcConfig = projectOperations.getPathResolver()
                .getIdentifier(webappPath, "WEB-INF/spring/webmvc-config.xml");
        if (fileManager.exists(mvcConfig)) {
            final Document document = XmlUtils.readXml(fileManager
                    .getInputStream(mvcConfig));
            final String prefixedUrl = "/" + relativeUrl;
            if (XmlUtils.findFirstElement("/beans/view-controller[@path='"
                    + prefixedUrl + "']", document.getDocumentElement()) == null) {
                final Element sibling = XmlUtils
                        .findFirstElement("/beans/view-controller",
                                document.getDocumentElement());
                final Element view = new XmlElementBuilder(
                        "mvc:view-controller", document).addAttribute("path",
                        prefixedUrl).build();
                if (sibling != null) {
                    sibling.getParentNode().insertBefore(view, sibling);
                }
                else {
                    document.getDocumentElement().appendChild(view);
                }
                fileManager.createOrUpdateTextFileIfRequired(mvcConfig,
                        XmlUtils.nodeToString(document), false);
            }
        }
    }

    /**
     * Adds Tiles Maven dependencies and updates the MVC config to include Tiles
     * view support
     */
    private void updateConfiguration() {
        // Add tiles dependencies to pom
        final Element configuration = XmlUtils.getRootElement(getClass(),
                "tiles/configuration.xml");

        final List<Dependency> dependencies = new ArrayList<Dependency>();
        final List<Element> springDependencies = XmlUtils.findElements(
                "/configuration/tiles/dependencies/dependency", configuration);
        for (final Element dependencyElement : springDependencies) {
            dependencies.add(new Dependency(dependencyElement));
        }
        projectOperations.addDependencies(
                projectOperations.getFocusedModuleName(), dependencies);

        // Add config to MVC app context
        final String mvcConfig = pathResolver.getFocusedIdentifier(
                SRC_MAIN_WEBAPP, "WEB-INF/spring/webmvc-config.xml");
        final Document mvcConfigDocument = XmlUtils.readXml(fileManager
                .getInputStream(mvcConfig));
        final Element beans = mvcConfigDocument.getDocumentElement();

        if (XmlUtils.findFirstElement("/beans/bean[@id = 'tilesViewResolver']",
                beans) != null
                || XmlUtils.findFirstElement(
                        "/beans/bean[@id = 'tilesConfigurer']", beans) != null) {
            return; // Tiles is already configured, nothing to do
        }

        final Document configDoc = getDocumentTemplate("tiles/tiles-mvc-config-template.xml");
        final Element configElement = configDoc.getDocumentElement();
        final List<Element> tilesConfig = XmlUtils.findElements("/config/bean",
                configElement);
        for (final Element bean : tilesConfig) {
            final Node importedBean = mvcConfigDocument.importNode(bean, true);
            beans.appendChild(importedBean);
        }
        fileManager.createOrUpdateTextFileIfRequired(mvcConfig,
                XmlUtils.nodeToString(mvcConfigDocument), true);
    }

    public void updateTags(final boolean backup, final LogicalPath webappPath) {
        if (backup) {
            backupOperations.backup();
        }

        // Update tags
        copyDirectoryContents("tags/form/*.tagx",
                pathResolver.getIdentifier(webappPath, "WEB-INF/tags/form"),
                true);
        copyDirectoryContents("tags/form/fields/*.tagx",
                pathResolver.getIdentifier(webappPath,
                        "WEB-INF/tags/form/fields"), true);
        copyDirectoryContents("tags/menu/*.tagx",
                pathResolver.getIdentifier(webappPath, "WEB-INF/tags/menu"),
                true);
        copyDirectoryContents("tags/util/*.tagx",
                pathResolver.getIdentifier(webappPath, "WEB-INF/tags/util"),
                true);
    }
}
