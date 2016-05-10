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
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.backup.BackupOperations;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.addon.web.mvc.views.i18n.I18n;
import org.springframework.roo.addon.web.mvc.views.i18n.I18nSupport;
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
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Property;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Implementation of {@link JspOperations}.
 * 
 * @author Stefan Schmidt
 * @author Jeremy Grelle
 * @author Juan Carlos García
 * @since 1.0
 */
@Component
@Service
public class JspOperationsImpl extends AbstractOperations implements JspOperations {

  private static final Logger LOGGER = HandlerUtils.getLogger(JspOperationsImpl.class);

  private BackupOperations backupOperations;
  private I18nSupport i18nSupport;
  private MenuOperations menuOperations;
  private PathResolver pathResolver;
  private ProjectOperations projectOperations;
  private PropFileOperations propFileOperations;
  private TilesOperations tilesOperations;
  private TypeManagementService typeManagementService;

  protected void activate(final ComponentContext cContext) {
    this.context = cContext.getBundleContext();
  }

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
  static ImmutablePair<String, String> getFolderAndMapping(final String preferredMapping,
      final JavaType controller) {
    if (StringUtils.isNotBlank(preferredMapping)) {
      String folderName = StringUtils.removeStart(preferredMapping, "/");
      folderName = StringUtils.removeEnd(folderName, "**");
      folderName = StringUtils.removeEnd(folderName, "/");

      String mapping = (preferredMapping.startsWith("/") ? "" : "/") + preferredMapping;
      mapping = StringUtils.removeEnd(mapping, "/");
      mapping = mapping + (mapping.endsWith("/**") ? "" : "/**");

      return new ImmutablePair<String, String>(folderName, mapping);
    }

    // Use sensible defaults
    final String typeNameLower =
        StringUtils.removeEnd(controller.getSimpleTypeName(), "Controller").toLowerCase();
    return new ImmutablePair<String, String>(typeNameLower, "/" + typeNameLower + "/**");
  }

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
  public void createManualController(final JavaType controller, final String preferredMapping,
      final LogicalPath webappPath) {
    Validate.notNull(controller, "Controller Java Type required");

    // Create annotation @RequestMapping("/myobject/**")
    final ImmutablePair<String, String> folderAndMapping =
        getFolderAndMapping(preferredMapping, controller);
    final String folderName = folderAndMapping.getKey();

    final String resourceIdentifier =
        getPathResolver().getFocusedCanonicalPath(Path.SRC_MAIN_JAVA, controller);
    final String declaredByMetadataId =
        PhysicalTypeIdentifier.createIdentifier(controller, getProjectOperations()
            .getPathResolver().getPath(resourceIdentifier));
    final List<MethodMetadataBuilder> methods = new ArrayList<MethodMetadataBuilder>();

    // Add HTTP post method
    methods.add(getHttpPostMethod(declaredByMetadataId));

    // Add index method
    methods.add(getIndexMethod(folderName, declaredByMetadataId));

    // Create Type definition
    final List<AnnotationMetadataBuilder> typeAnnotations =
        new ArrayList<AnnotationMetadataBuilder>();

    final List<AnnotationAttributeValue<?>> requestMappingAttributes =
        new ArrayList<AnnotationAttributeValue<?>>();
    requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"),
        folderAndMapping.getValue()));
    final AnnotationMetadataBuilder requestMapping =
        new AnnotationMetadataBuilder(REQUEST_MAPPING, requestMappingAttributes);
    typeAnnotations.add(requestMapping);

    // Create annotation @Controller
    final List<AnnotationAttributeValue<?>> controllerAttributes =
        new ArrayList<AnnotationAttributeValue<?>>();
    final AnnotationMetadataBuilder controllerAnnotation =
        new AnnotationMetadataBuilder(CONTROLLER, controllerAttributes);
    typeAnnotations.add(controllerAnnotation);

    final ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, controller,
            PhysicalTypeCategory.CLASS);
    cidBuilder.setAnnotations(typeAnnotations);
    cidBuilder.setDeclaredMethods(methods);
    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());

    installView(folderName, "/index",
        new JavaSymbolName(controller.getSimpleTypeName()).getReadableSymbolName() + " View",
        "Controller", null, false, webappPath);
  }

  private MethodMetadataBuilder getHttpPostMethod(final String declaredByMetadataId) {
    final List<AnnotationMetadataBuilder> postMethodAnnotations =
        new ArrayList<AnnotationMetadataBuilder>();
    final List<AnnotationAttributeValue<?>> postMethodAttributes =
        new ArrayList<AnnotationAttributeValue<?>>();
    postMethodAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"), new EnumDetails(
        REQUEST_METHOD, new JavaSymbolName("POST"))));
    postMethodAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "{id}"));
    postMethodAnnotations.add(new AnnotationMetadataBuilder(REQUEST_MAPPING, postMethodAttributes));

    final List<AnnotatedJavaType> postParamTypes = new ArrayList<AnnotatedJavaType>();
    final AnnotationMetadataBuilder idParamAnnotation =
        new AnnotationMetadataBuilder(PATH_VARIABLE);
    postParamTypes.add(new AnnotatedJavaType(new JavaType("java.lang.Long"), idParamAnnotation
        .build()));
    postParamTypes.add(new AnnotatedJavaType(MODEL_MAP));
    postParamTypes.add(new AnnotatedJavaType(HTTP_SERVLET_REQUEST));
    postParamTypes.add(new AnnotatedJavaType(HTTP_SERVLET_RESPONSE));

    final List<JavaSymbolName> postParamNames = new ArrayList<JavaSymbolName>();
    postParamNames.add(new JavaSymbolName("id"));
    postParamNames.add(new JavaSymbolName("modelMap"));
    postParamNames.add(new JavaSymbolName("request"));
    postParamNames.add(new JavaSymbolName("response"));

    final MethodMetadataBuilder postMethodBuilder =
        new MethodMetadataBuilder(declaredByMetadataId, Modifier.PUBLIC,
            new JavaSymbolName("post"), JavaType.VOID_PRIMITIVE, postParamTypes, postParamNames,
            new InvocableMemberBodyBuilder());
    postMethodBuilder.setAnnotations(postMethodAnnotations);
    return postMethodBuilder;
  }

  private MethodMetadataBuilder getIndexMethod(final String folderName,
      final String declaredByMetadataId) {
    final List<AnnotationMetadataBuilder> indexMethodAnnotations =
        new ArrayList<AnnotationMetadataBuilder>();
    indexMethodAnnotations.add(new AnnotationMetadataBuilder(REQUEST_MAPPING,
        new ArrayList<AnnotationAttributeValue<?>>()));
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    bodyBuilder.appendFormalLine("return \"" + folderName + "/index\";");
    final MethodMetadataBuilder indexMethodBuilder =
        new MethodMetadataBuilder(declaredByMetadataId, Modifier.PUBLIC,
            new JavaSymbolName("index"), JavaType.STRING, new ArrayList<AnnotatedJavaType>(),
            new ArrayList<JavaSymbolName>(), bodyBuilder);
    indexMethodBuilder.setAnnotations(indexMethodAnnotations);
    return indexMethodBuilder;
  }

  public void installCommonViewArtefacts() {
    installCommonViewArtefacts(getProjectOperations().getFocusedModuleName());
  }

  public void installCommonViewArtefacts(final String moduleName) {
    Validate.isTrue(isProjectAvailable(), "Project metadata required");
    final LogicalPath webappPath = Path.SRC_MAIN_WEBAPP.getModulePathId(moduleName);
    // Install servers maven plugin
    installMavenPlugins(moduleName);

    // Install tiles config
    updateConfiguration();

    // Install styles
    copyDirectoryContents("images/*.*", getPathResolver().getIdentifier(webappPath, "images"),
        false);

    // Install styles
    copyDirectoryContents("styles/*.css", getPathResolver().getIdentifier(webappPath, "styles"),
        false);
    copyDirectoryContents("styles/*.properties",
        getPathResolver().getIdentifier(webappPath, "WEB-INF/classes"), false);

    // Install layout
    copyDirectoryContents("tiles/default.jspx",
        getPathResolver().getIdentifier(webappPath, "WEB-INF/layouts/"), false);
    copyDirectoryContents("tiles/layouts.xml",
        getPathResolver().getIdentifier(webappPath, "WEB-INF/layouts/"), false);
    copyDirectoryContents("tiles/header.jspx",
        getPathResolver().getIdentifier(webappPath, "WEB-INF/views/"), false);
    copyDirectoryContents("tiles/menu.jspx",
        getPathResolver().getIdentifier(webappPath, "WEB-INF/views/"), false);
    copyDirectoryContents("tiles/footer.jspx",
        getPathResolver().getIdentifier(webappPath, "WEB-INF/views/"), false);
    copyDirectoryContents("tiles/views.xml",
        getPathResolver().getIdentifier(webappPath, "WEB-INF/views/"), false);

    // Install common view files
    copyDirectoryContents("*.jspx", getPathResolver().getIdentifier(webappPath, "WEB-INF/views/"),
        false);

    // Install tags
    copyDirectoryContents("tags/form/*.tagx",
        getPathResolver().getIdentifier(webappPath, "WEB-INF/tags/form"), false);
    copyDirectoryContents("tags/form/fields/*.tagx",
        getPathResolver().getIdentifier(webappPath, "WEB-INF/tags/form/fields"), false);
    copyDirectoryContents("tags/menu/*.tagx",
        getPathResolver().getIdentifier(webappPath, "WEB-INF/tags/menu"), false);
    copyDirectoryContents("tags/util/*.tagx",
        getPathResolver().getIdentifier(webappPath, "WEB-INF/tags/util"), false);

    // TODO: Rebuild with new views system
    // Install default language 'en'
//    installI18n(getI18nSupport().getLanguage(Locale.ENGLISH), webappPath);
//    final String i18nDirectory =
//        getPathResolver().getIdentifier(webappPath, "WEB-INF/i18n/application.properties");
//    if (!fileManager.exists(i18nDirectory)) {
//      try {
//        final String projectName =
//            getProjectOperations().getProjectName(getProjectOperations().getFocusedModuleName());
//        fileManager.createFile(getPathResolver().getIdentifier(webappPath,
//            "WEB-INF/i18n/application.properties"));
//        /*getPropFileOperations()
//                .addPropertyIfNotExists(webappPath,
//                        "WEB-INF/i18n/application.properties",
//                        "application_name",
//                        projectName.substring(0, 1).toUpperCase()
//                                + projectName.substring(1), true);*/
//      } catch (final Exception e) {
//        throw new IllegalStateException(
//            "Encountered an error during copying of resources for MVC JSP addon.", e);
//      }
//    }
  }

  private void installMavenPlugins(String moduleName) {
    final Element configuration = XmlUtils.getConfiguration(getClass());

    // Add properties
    List<Element> properties = XmlUtils.findElements("/configuration/properties/*", configuration);
    for (Element property : properties) {
      getProjectOperations().addProperty(moduleName, new Property(property));
    }

    // Add Plugins
    List<Element> elements = XmlUtils.findElements("/configuration/plugins/plugin", configuration);
    for (Element element : elements) {
      Plugin plugin = new Plugin(element);
      getProjectOperations().addBuildPlugin(moduleName, plugin);
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
  private void installView(final JavaSymbolName viewName, final String folderName,
      final String title, final String category, final boolean registerStaticController,
      final LogicalPath webappPath) {
    // Probe if common web artifacts exist, and install them if needed
    if (!fileManager.exists(getPathResolver().getIdentifier(webappPath,
        "WEB-INF/layouts/default.jspx"))) {
      installCommonViewArtefacts(webappPath.getModule());
    }

    final String lcViewName = viewName.getSymbolName().toLowerCase();

    // Update the application-specific resource bundle (i.e. default
    // translation)
    final String messageCode =
        "label" + folderName.replace("/", "_").toLowerCase() + "_" + lcViewName;
    /*getPropFileOperations()
            .addPropertyIfNotExists(
                    getPathResolver().getFocusedPath(Path.SRC_MAIN_WEBAPP),
                    "WEB-INF/i18n/application.properties", messageCode,
                    title, true);*/

    // Add the menu item
    final String relativeUrl = folderName + "/" + lcViewName;
    getMenuOperations().addMenuItem(new JavaSymbolName(category),
        new JavaSymbolName(folderName.replace("/", "_").toLowerCase() + lcViewName + "_id"), title,
        "global_generic", relativeUrl, null, webappPath);

    // Add the view definition
    getTilesOperations().addViewDefinition(folderName.toLowerCase(),
        getPathResolver().getFocusedPath(Path.SRC_MAIN_WEBAPP), relativeUrl,
        TilesOperations.DEFAULT_TEMPLATE,
        "/WEB-INF/views" + folderName.toLowerCase() + "/" + lcViewName + ".jspx");

    if (registerStaticController) {
      // Update the Spring MVC config file
      registerStaticSpringMvcController(relativeUrl, webappPath);
    }
  }

  private void installView(final String path, final String viewName, final String title,
      final String category, Document document, final boolean registerStaticController,
      final LogicalPath webappPath) {
    Validate.notBlank(path, "Path required");
    Validate.notBlank(viewName, "View name required");
    Validate.notBlank(title, "Title required");

    final String cleanedPath = cleanPath(path);
    final String cleanedViewName = cleanViewName(viewName);
    final String lcViewName = cleanedViewName.toLowerCase();

    if (document == null) {
      try {
        document = getDocumentTemplate("index-template.jspx");
        XmlUtils.findRequiredElement("/div/message", document.getDocumentElement()).setAttribute(
            "code", "label" + cleanedPath.replace("/", "_").toLowerCase() + "_" + lcViewName);
      } catch (final Exception e) {
        throw new IllegalStateException(
            "Encountered an error during copying of resources for controller class.", e);
      }
    }

    final String viewFile =
        getPathResolver().getFocusedIdentifier(Path.SRC_MAIN_WEBAPP,
            "WEB-INF/views" + cleanedPath.toLowerCase() + "/" + lcViewName + ".jspx");
    fileManager.createOrUpdateTextFileIfRequired(viewFile, XmlUtils.nodeToString(document), false);

    installView(new JavaSymbolName(lcViewName), cleanedPath, title, category,
        registerStaticController, webappPath);
  }

  public void installView(final String path, final String viewName, final String title,
      final String category, final Document document, final LogicalPath webappPath) {
    installView(path, viewName, title, category, document, true, webappPath);
  }

  public void installView(final String path, final String viewName, final String title,
      final String category, final LogicalPath webappPath) {
    installView(path, viewName, title, category, null, true, webappPath);
  }

  public boolean isInstallLanguageCommandAvailable() {
    return isProjectAvailable()
        && fileManager.exists(getPathResolver().getFocusedIdentifier(Path.SRC_MAIN_WEBAPP,
            "WEB-INF/views/footer.jspx"));
  }

  private boolean isProjectAvailable() {
    return getProjectOperations().isFocusedProjectAvailable();
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
    final String mvcConfig =
        getProjectOperations().getPathResolver().getIdentifier(webappPath,
            "WEB-INF/spring/webmvc-config.xml");
    if (fileManager.exists(mvcConfig)) {
      final Document document = XmlUtils.readXml(fileManager.getInputStream(mvcConfig));
      final String prefixedUrl = "/" + relativeUrl;
      if (XmlUtils.findFirstElement("/beans/view-controller[@path='" + prefixedUrl + "']",
          document.getDocumentElement()) == null) {
        final Element sibling =
            XmlUtils.findFirstElement("/beans/view-controller", document.getDocumentElement());
        final Element view =
            new XmlElementBuilder("mvc:view-controller", document)
                .addAttribute("path", prefixedUrl).build();
        if (sibling != null) {
          sibling.getParentNode().insertBefore(view, sibling);
        } else {
          document.getDocumentElement().appendChild(view);
        }
        fileManager.createOrUpdateTextFileIfRequired(mvcConfig, XmlUtils.nodeToString(document),
            false);
      }
    }
  }

  /**
   * Adds Tiles Maven dependencies and updates the MVC config to include Tiles
   * view support
   */
  private void updateConfiguration() {
    // Add tiles dependencies to pom
    final Element configuration = XmlUtils.getRootElement(getClass(), "tiles/configuration.xml");

    final List<Dependency> dependencies = new ArrayList<Dependency>();
    final List<Element> springDependencies =
        XmlUtils.findElements("/configuration/tiles/dependencies/dependency", configuration);
    for (final Element dependencyElement : springDependencies) {
      dependencies.add(new Dependency(dependencyElement));
    }
    getProjectOperations().addDependencies(getProjectOperations().getFocusedModuleName(),
        dependencies);

    // Add config to MVC app context
    final String mvcConfig =
        getPathResolver().getFocusedIdentifier(SRC_MAIN_WEBAPP, "WEB-INF/spring/webmvc-config.xml");
    final Document mvcConfigDocument = XmlUtils.readXml(fileManager.getInputStream(mvcConfig));
    final Element beans = mvcConfigDocument.getDocumentElement();

    if (XmlUtils.findFirstElement("/beans/bean[@id = 'tilesViewResolver']", beans) != null
        || XmlUtils.findFirstElement("/beans/bean[@id = 'tilesConfigurer']", beans) != null) {
      return; // Tiles is already configured, nothing to do
    }

    final Document configDoc = getDocumentTemplate("tiles/tiles-mvc-config-template.xml");
    final Element configElement = configDoc.getDocumentElement();
    final List<Element> tilesConfig = XmlUtils.findElements("/config/bean", configElement);
    for (final Element bean : tilesConfig) {
      final Node importedBean = mvcConfigDocument.importNode(bean, true);
      beans.appendChild(importedBean);
    }
    fileManager.createOrUpdateTextFileIfRequired(mvcConfig,
        XmlUtils.nodeToString(mvcConfigDocument), true);
  }

  public void updateTags(final boolean backup, final LogicalPath webappPath) {
    if (backup) {
      getBackupOperations().backup();
    }

    // Update tags
    copyDirectoryContents("tags/form/*.tagx",
        getPathResolver().getIdentifier(webappPath, "WEB-INF/tags/form"), true);
    copyDirectoryContents("tags/form/fields/*.tagx",
        getPathResolver().getIdentifier(webappPath, "WEB-INF/tags/form/fields"), true);
    copyDirectoryContents("tags/menu/*.tagx",
        getPathResolver().getIdentifier(webappPath, "WEB-INF/tags/menu"), true);
    copyDirectoryContents("tags/util/*.tagx",
        getPathResolver().getIdentifier(webappPath, "WEB-INF/tags/util"), true);
  }

  public BackupOperations getBackupOperations() {
    if (backupOperations == null) {
      // Get all Services implement BackupOperations interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(BackupOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          backupOperations = (BackupOperations) context.getService(ref);
          return backupOperations;
        }
        return null;
      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load BackupOperations on JspOperationsImpl.");
        return null;
      }
    } else {
      return backupOperations;
    }
  }

  public MenuOperations getMenuOperations() {
    if (menuOperations == null) {
      // Get all Services implement MenuOperations interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(MenuOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          menuOperations = (MenuOperations) context.getService(ref);
          return menuOperations;
        }
        return null;
      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load MenuOperations on JspOperationsImpl.");
        return null;
      }
    } else {
      return menuOperations;
    }
  }

  public PathResolver getPathResolver() {
    if (pathResolver == null) {
      // Get all Services implement PathResolver interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(PathResolver.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          pathResolver = (PathResolver) context.getService(ref);
          return pathResolver;
        }
        return null;
      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load PathResolver on JspOperationsImpl.");
        return null;
      }
    } else {
      return pathResolver;
    }
  }

  public ProjectOperations getProjectOperations() {
    if (projectOperations == null) {
      // Get all Services implement ProjectOperations interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(ProjectOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          projectOperations = (ProjectOperations) context.getService(ref);
          return projectOperations;
        }
        return null;
      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ProjectOperations on JspOperationsImpl.");
        return null;
      }
    } else {
      return projectOperations;
    }
  }

  public PropFileOperations getPropFileOperations() {
    if (propFileOperations == null) {
      // Get all Services implement PropFileOperations interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(PropFileOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          propFileOperations = (PropFileOperations) context.getService(ref);
          return propFileOperations;
        }
        return null;
      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load PropFileOperations on JspOperationsImpl.");
        return null;
      }
    } else {
      return propFileOperations;
    }
  }

  public TilesOperations getTilesOperations() {
    if (tilesOperations == null) {
      // Get all Services implement TilesOperations interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(TilesOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          tilesOperations = (TilesOperations) context.getService(ref);
          return tilesOperations;
        }
        return null;
      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TilesOperations on JspOperationsImpl.");
        return null;
      }
    } else {
      return tilesOperations;
    }
  }

  public TypeManagementService getTypeManagementService() {
    if (typeManagementService == null) {
      // Get all Services implement TypeManagementService interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(TypeManagementService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          typeManagementService = (TypeManagementService) context.getService(ref);
          return typeManagementService;
        }
        return null;
      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TypeManagementService on JspOperationsImpl.");
        return null;
      }
    } else {
      return typeManagementService;
    }
  }
  
  public I18nSupport getI18nSupport() {
    if (i18nSupport == null) {
      // Get all Services implement I18nSupport interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(I18nSupport.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          i18nSupport = (I18nSupport) context.getService(ref);
          return i18nSupport;
        }
        return null;
      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load I18nSupport on JspOperationsImpl.");
        return null;
      }
    } else {
      return i18nSupport;
    }
  }
}
