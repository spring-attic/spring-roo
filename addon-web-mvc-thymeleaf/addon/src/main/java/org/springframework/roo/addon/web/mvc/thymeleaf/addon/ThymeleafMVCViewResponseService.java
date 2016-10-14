package org.springframework.roo.addon.web.mvc.thymeleaf.addon;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.addon.web.mvc.controller.addon.responses.ControllerMVCResponseService;
import org.springframework.roo.addon.web.mvc.i18n.I18nOperations;
import org.springframework.roo.addon.web.mvc.i18n.languages.EnglishLanguage;
import org.springframework.roo.addon.web.mvc.views.MVCViewGenerationService;
import org.springframework.roo.addon.web.mvc.views.ViewContext;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.operations.AbstractOperations;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Property;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Implementation of ControllerMVCResponseService that provides
 * Thymeleaf Response types.
 * 
 * With this implementation, Spring Roo will be able to provide Thymeleaf response
 * types during controller generations.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class ThymeleafMVCViewResponseService extends AbstractOperations implements
    ControllerMVCResponseService {

  private static Logger LOGGER = HandlerUtils.getLogger(ThymeleafMVCViewResponseService.class);
  private static final String RESPONSE_TYPE = "THYMELEAF";

  private final Dependency starterThymeleafDependency = new Dependency("org.springframework.boot",
      "spring-boot-starter-thymeleaf", null);
  private final Dependency layoutThymeleafDependency = new Dependency("nz.net.ultraq.thymeleaf",
      "thymeleaf-layout-dialect", null);

  private ProjectOperations projectOperations;
  private TypeLocationService typeLocationService;
  private TypeManagementService typeManagementService;
  private PathResolver pathResolver;
  private FileManager fileManager;
  private MVCViewGenerationService viewGenerationService;
  private I18nOperations i18nOperations;

  /**
   * This operation returns the Feature name. In this case,
   * the Feature name is the same as the response type.
   * 
   * @return String with TYHMELEAF as Feature name
   */
  @Override
  public String getName() {
    return getResponseType();
  }

  /**
   * This operation checks if this feature is installed in module.
   * THYMELEAF is installed in module if thymeleaf dependencies has been installed before.
   * 
   * @return true if thymeleaf dependencies has been installed, if not return false.
   */
  @Override
  public boolean isInstalledInModule(String moduleName) {
    // THYMELEAF is installed if Spring MVC has been installed and Thymeleaf
    // dependencies has been installed.
    return getProjectOperations().isFeatureInstalled(FeatureNames.MVC)
        && hasThymeleafDependencies(moduleName);
  }

  /**
   * This operation returns the THYMELEAF response type.
   * 
   * @return String with THYMELEAF as response type
   */
  @Override
  public String getResponseType() {
    return RESPONSE_TYPE;
  }

  /**
   * This operation returns the annotation type @RooThymeleaf
   * 
   * @return JavaType with the THYMELEAF annotation type
   */
  @Override
  public JavaType getAnnotation() {
    // Generating @RooThymeleaf annotation
    return RooJavaType.ROO_THYMELEAF;
  }

  /**
   * This operation annotates a controller with the THYMELEAF annotation
   * 
   * @param controller JavaType with the controller to be annotated.
   */
  @Override
  public void annotate(JavaType controller) {

    Validate.notNull(controller, "ERROR: You must provide a valid controller");

    ClassOrInterfaceTypeDetails controllerDetails =
        getTypeLocationService().getTypeDetails(controller);

    // Check if provided controller exists on current project
    Validate.notNull(controllerDetails, "ERROR: You must provide an existing controller");

    // Check if provided controller has been annotated with @RooController
    Validate.notNull(controllerDetails.getAnnotation(RooJavaType.ROO_CONTROLLER),
        "ERROR: You must provide a controller annotated with @RooController");

    // Add Thymeleaf annotation
    ClassOrInterfaceTypeDetailsBuilder typeBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(controllerDetails);
    typeBuilder.addAnnotation(new AnnotationMetadataBuilder(getAnnotation()));

    // Write changes on provided controller
    getTypeManagementService().createOrUpdateTypeOnDisk(typeBuilder.build());

  }

  /**
   * This operation will check if some controller has the @RooThymeleaf annotation
   * 
   * @param controller JavaType with controller to check
   * @return true if provided controller has the THYMELEAF responseType.
   *        If not, return false.
   */
  @Override
  public boolean hasResponseType(JavaType controller) {
    Validate.notNull(controller, "ERROR: You must provide a valid controller");

    ClassOrInterfaceTypeDetails controllerDetails =
        getTypeLocationService().getTypeDetails(controller);

    if (controllerDetails == null) {
      return false;
    }

    return controllerDetails.getAnnotation(getAnnotation()) != null;
  }

  @Override
  public void install(Pom module) {
    // Is necessary to install thymeleaf dependencies
    addThymeleafDependencies(module);
    // Is necessary to generate the main controller
    addMainController(module);
    // Thymeleaf needs Datatables component to list
    // data, so is necessary to install Datatables resources
    addThymeleafDatatablesResources(module);
    // Is necessary to copy static resources
    copyStaticResources(module);

    // Add all available WebJar containing required static resources
    addWebJars(module);

    // Delegate on view generation to create view elements
    ViewContext ctx = new ViewContext();
    ctx.setProjectName(getProjectOperations().getProjectName(""));
    ctx.setVersion(getProjectOperations().getPomFromModuleName("").getVersion());

    getViewGenerationService().addIndexView(module.getModuleName(), ctx);
    getViewGenerationService().addErrorView(module.getModuleName(), ctx);
    getViewGenerationService().addDefaultLayout(module.getModuleName(), ctx);
    getViewGenerationService().addDefaultLayoutNoMenu(module.getModuleName(), ctx);
    getViewGenerationService().addFooter(module.getModuleName(), ctx);
    getViewGenerationService().addHeader(module.getModuleName(), ctx);
    getViewGenerationService().addMenu(module.getModuleName(), ctx);
    getViewGenerationService().addModal(module.getModuleName(), ctx);
    getViewGenerationService().addSession(module.getModuleName(), ctx);
    getViewGenerationService().addSessionLinks(module.getModuleName(), ctx);
    getViewGenerationService().addLanguages(module.getModuleName(), ctx);
    getViewGenerationService().addAccessibilityView(module.getModuleName(), ctx);
    getViewGenerationService().addDefaultLayoutNoMenu(module.getModuleName(), ctx);

    // Add i18n support for english language
    getI18nOperations().installI18n(new EnglishLanguage(), module);
  }

  /**
   * This operation returns the annotation type that identifies the main 
   * controller generated by THYMELEAF response type.
   * 
   * @return JavaType with the annotation managed by the implementation. 
   * This method never returns null.
   * 
   */
  @Override
  public JavaType getMainControllerAnnotation() {
    // Provides @RooThymeleafMainController
    return RooJavaType.ROO_THYMELEAF_MAIN_CONTROLLER;
  }

  /**
   * This operation returns the main controller generated by THYMELEAF response type.
   * 
   * 
   * @return JavaType with the main controller generated by the implementation.
   * Maybe returns null if main controller has not been generated yet.
   */
  @Override
  public JavaType getMainController() {
    Set<ClassOrInterfaceTypeDetails> mainController =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            getMainControllerAnnotation());
    if (mainController != null && !mainController.isEmpty()) {
      Iterator<ClassOrInterfaceTypeDetails> it = mainController.iterator();
      while (it.hasNext()) {
        return it.next().getType();
      }
    }
    return null;
  }

  /**
   * This method adds necessary thymeleaf dependencies to 
   * generated project
   * 
   * @param module
   */
  private void addThymeleafDependencies(Pom module) {

    // Add Thymeleaf starter
    getProjectOperations().addDependency(module.getModuleName(), starterThymeleafDependency);

    // Add Thymeleaf layout dialect
    getProjectOperations().addDependency(module.getModuleName(), layoutThymeleafDependency);

    // TODO: Add Thymeleaf tests dependencies 

  }

  /**
   * This method add new MainController.java class annotated
   * with @RooThymeleafMainController
   * 
   * This controller will be used to return index page.
   * 
   * @param module
   */
  private void addMainController(Pom module) {

    // Check if already exists other main controller annotated with @RooThymeleafMainController
    if (getMainController() != null) {
      return;
    }

    // If not, define new JavaType
    JavaType mainController =
        new JavaType(String.format("%s.web.MainController", getProjectOperations()
            .getTopLevelPackage(module.getModuleName())), module.getModuleName());

    // Check that new JavaType doesn't exists
    ClassOrInterfaceTypeDetails mainControllerDetails =
        getTypeLocationService().getTypeDetails(mainController);

    if (mainControllerDetails != null) {
      AnnotationMetadata mainControllerThymeleafAnnotation =
          mainControllerDetails.getAnnotation(getMainControllerAnnotation());
      // Maybe, this controller already exists
      if (mainControllerThymeleafAnnotation != null) {
        return;
      } else {
        throw new RuntimeException(
            "ERROR: You are trying to generate more than one MainController.");
      }
    }

    ClassOrInterfaceTypeDetailsBuilder cidBuilder = null;
    final LogicalPath controllerPath =
        getPathResolver().getPath(mainController.getModule(), Path.SRC_MAIN_JAVA);
    final String resourceIdentifier =
        getTypeLocationService().getPhysicalTypeCanonicalPath(mainController, controllerPath);
    final String declaredByMetadataId =
        PhysicalTypeIdentifier.createIdentifier(mainController,
            getPathResolver().getPath(resourceIdentifier));

    List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
    annotations.add(new AnnotationMetadataBuilder(getMainControllerAnnotation()));

    cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC,
            mainController, PhysicalTypeCategory.CLASS);
    cidBuilder.setAnnotations(annotations);

    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());
  }

  /**
   * This method copy and generate all necessary resources to be able
   * to use THYMELEAF.
   * 
   * @param module
   */
  private void copyStaticResources(Pom module) {
    LogicalPath resourcesPath =
        LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, module.getModuleName());

    // copy all necessary styles inside SRC_MAIN_RESOURCES/static/public/css
    copyDirectoryContents("static/css/*.css",
        getPathResolver().getIdentifier(resourcesPath, "/static/public/css"), true);

    // copy all necessary fonts inside SRC_MAIN_RESOURCES/static/public/fonts
    copyDirectoryContents("static/fonts/*.eot",
        getPathResolver().getIdentifier(resourcesPath, "/static/public/fonts"), true);
    copyDirectoryContents("static/fonts/*.svg",
        getPathResolver().getIdentifier(resourcesPath, "/static/public/fonts"), true);
    copyDirectoryContents("static/fonts/*.ttf",
        getPathResolver().getIdentifier(resourcesPath, "/static/public/fonts"), true);
    copyDirectoryContents("static/fonts/*.woff",
        getPathResolver().getIdentifier(resourcesPath, "/static/public/fonts"), true);
    copyDirectoryContents("static/fonts/*.woff2",
        getPathResolver().getIdentifier(resourcesPath, "/static/public/fonts"), true);

    // copy all necessary images inside SRC_MAIN_RESOURCES/static/public/img
    copyDirectoryContents("static/img/*.ico",
        getPathResolver().getIdentifier(resourcesPath, "/static/public/img"), false);
    copyDirectoryContents("static/img/*.jpg",
        getPathResolver().getIdentifier(resourcesPath, "/static/public/img"), false);
    copyDirectoryContents("static/img/*.png",
        getPathResolver().getIdentifier(resourcesPath, "/static/public/img"), false);
    copyDirectoryContents("static/img/*.gif",
        getPathResolver().getIdentifier(resourcesPath, "/static/public/img"), false);

    // copy all necessary scripts inside SRC_MAIN_RESOURCES/static/public/js
    copyDirectoryContents("static/js/*.js",
        getPathResolver().getIdentifier(resourcesPath, "/static/public/js"), true);

    // copy all necessary scripts inside SRC_MAIN_RESOURCES/templates/fragments/js
    copyDirectoryContents("templates/fragments/js/*.html",
        pathResolver.getIdentifier(resourcesPath, "/templates/fragments/js"), true);
  }

  /**
   * This method install Datatables resources and Datatables support for thymeleaf 
   * implementation
   * 
   * This is necessary because THYMELEAF response type uses Datatables components to
   * list results on generated views.
   * 
   * @param module
   */
  private void addThymeleafDatatablesResources(Pom module) {

    // Add DatatablesData.java class
    addDatatablesDataClass(module);

    // Add DatatablesPageableHandlerMethodArgumentResolver.java class
    addDatatablesPageableHandlerMethodArgumentResolverClass(module);

    // Add DatatablesPageable.java class
    addDatatablesPageableClass(module);

    // Add DatatablesSortHandlerMethodArgumentResolver.java class
    addDatatablesSortHandlerMethodArgumentResolverClass(module);

    // Add DatatablesSort.java class
    addDatatablesSortClass(module);

    // Add WebMVCThymeleafUIConfiguration config class
    addWebMVCThymeleafUIConfigurationClass(module);
  }

  /**
   * This method adds new DatatablesSortHandlerMethodArgumentResolver.java class
   * annotated with @RooThymeleafDatatablesSortHandler
   * 
   * @param module
   */
  private void addDatatablesSortHandlerMethodArgumentResolverClass(Pom module) {
    // First of all, check if already exists a @RooThymeleafDatatablesSortHandler
    // class on current project
    Set<JavaType> sortHandlerClasses =
        getTypeLocationService().findTypesWithAnnotation(
            RooJavaType.ROO_THYMELEAF_DATATABLES_SORT_HANDLER);

    if (!sortHandlerClasses.isEmpty()) {
      return;
    }

    JavaPackage modulePackage = getProjectOperations().getTopLevelPackage(module.getModuleName());

    final JavaType javaType =
        new JavaType(String.format("%s.datatables.DatatablesSortHandlerMethodArgumentResolver",
            modulePackage), module.getModuleName());
    final String physicalPath =
        getPathResolver().getCanonicalPath(javaType.getModule(), Path.SRC_MAIN_JAVA, javaType);


    // Including DatatablesSortHandlerMethodArgumentResolver class
    InputStream inputStream = null;
    try {
      // Use defined template
      inputStream =
          FileUtils.getInputStream(getClass(),
              "DatatablesSortHandlerMethodArgumentResolver-template._java");
      String input = IOUtils.toString(inputStream);
      // Replacing package
      input = input.replace("__PACKAGE__", javaType.getPackage().getFullyQualifiedPackageName());

      // Creating DatatablesSortHandlerMethodArgumentResolver class
      getFileManager().createOrUpdateTextFileIfRequired(physicalPath, input, true);
    } catch (final IOException e) {
      throw new IllegalStateException(String.format("Unable to create '%s'", physicalPath), e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  /**
   * This method adds new DatatablesSort.java class
   * annotated with @RooThymeleafDatatablesSort
   * 
   * @param module
   */
  private void addDatatablesSortClass(Pom module) {
    // First of all, check if already exists a @RooThymeleafDatatablesSort
    // class on current project
    Set<JavaType> sortClasses =
        getTypeLocationService().findTypesWithAnnotation(RooJavaType.ROO_THYMELEAF_DATATABLES_SORT);

    if (!sortClasses.isEmpty()) {
      return;
    }

    JavaPackage modulePackage = getProjectOperations().getTopLevelPackage(module.getModuleName());

    final JavaType javaType =
        new JavaType(String.format("%s.datatables.DatatablesSort", modulePackage),
            module.getModuleName());
    final String physicalPath =
        getPathResolver().getCanonicalPath(javaType.getModule(), Path.SRC_MAIN_JAVA, javaType);


    // Including DatatablesSort class
    InputStream inputStream = null;
    try {
      // Use defined template
      inputStream = FileUtils.getInputStream(getClass(), "DatatablesSort-template._java");
      String input = IOUtils.toString(inputStream);
      // Replacing package
      input = input.replace("__PACKAGE__", javaType.getPackage().getFullyQualifiedPackageName());

      // Creating DatatablesSort class
      getFileManager().createOrUpdateTextFileIfRequired(physicalPath, input, true);
    } catch (final IOException e) {
      throw new IllegalStateException(String.format("Unable to create '%s'", physicalPath), e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  /**
   * This method adds new DatatablesPageableHandlerMethodArgumentResolver.java class
   * annotated with @RooThymeleafDatatablesPageableHandler
   * 
   * @param module
   */
  private void addDatatablesPageableHandlerMethodArgumentResolverClass(Pom module) {
    // First of all, check if already exists a @RooThymeleafDatatablesPageableHandler
    // class on current project
    Set<JavaType> pageableHandlerClasses =
        getTypeLocationService().findTypesWithAnnotation(
            RooJavaType.ROO_THYMELEAF_DATATABLES_PAGEABLE_HANDLER);

    if (!pageableHandlerClasses.isEmpty()) {
      return;
    }

    JavaPackage modulePackage = getProjectOperations().getTopLevelPackage(module.getModuleName());

    final JavaType javaType =
        new JavaType(String.format("%s.datatables.DatatablesPageableHandlerMethodArgumentResolver",
            modulePackage), module.getModuleName());
    final String physicalPath =
        getPathResolver().getCanonicalPath(javaType.getModule(), Path.SRC_MAIN_JAVA, javaType);


    // Including DatatablesPageableHandlerMethodArgumentResolver class
    InputStream inputStream = null;
    try {
      // Use defined template
      inputStream =
          FileUtils.getInputStream(getClass(),
              "DatatablesPageableHandlerMethodArgumentResolver-template._java");
      String input = IOUtils.toString(inputStream);
      // Replacing package
      input = input.replace("__PACKAGE__", javaType.getPackage().getFullyQualifiedPackageName());

      // Creating DatatablesPageableHandlerMethodArgumentResolver class
      getFileManager().createOrUpdateTextFileIfRequired(physicalPath, input, true);
    } catch (final IOException e) {
      throw new IllegalStateException(String.format("Unable to create '%s'", physicalPath), e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  /**
   * This method adds new DatatablesPageable.java class
   * annotated with @RooThymeleafDatatablesPageable
   * 
   * @param module
   */
  private void addDatatablesPageableClass(Pom module) {
    // First of all, check if already exists a @RooThymeleafDatatablesPageable
    // class on current project
    Set<JavaType> pageableClasses =
        getTypeLocationService().findTypesWithAnnotation(
            RooJavaType.ROO_THYMELEAF_DATATABLES_PAGEABLE);

    if (!pageableClasses.isEmpty()) {
      return;
    }

    JavaPackage modulePackage = getProjectOperations().getTopLevelPackage(module.getModuleName());

    final JavaType javaType =
        new JavaType(String.format("%s.datatables.DatatablesPageable", modulePackage),
            module.getModuleName());
    final String physicalPath =
        getPathResolver().getCanonicalPath(javaType.getModule(), Path.SRC_MAIN_JAVA, javaType);


    // Including DatatablesPageable class
    InputStream inputStream = null;
    try {
      // Use defined template
      inputStream = FileUtils.getInputStream(getClass(), "DatatablesPageable-template._java");
      String input = IOUtils.toString(inputStream);
      // Replacing package
      input = input.replace("__PACKAGE__", javaType.getPackage().getFullyQualifiedPackageName());

      // Creating DatatablesPageable class
      getFileManager().createOrUpdateTextFileIfRequired(physicalPath, input, true);
    } catch (final IOException e) {
      throw new IllegalStateException(String.format("Unable to create '%s'", physicalPath), e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  /**
   * This method adds new DatatablesData.java annotated with @RooDatatablesData.
   * 
   * This class will be used on Thymeleaf Controllers to return DatatablesData object.
   * 
   * @param module
   */
  private void addDatatablesDataClass(Pom module) {
    // First of all, check if already exists a @RooThymeleafDatatablesData
    // class on current project
    Set<JavaType> datatablesDataClasses =
        getTypeLocationService().findTypesWithAnnotation(RooJavaType.ROO_THYMELEAF_DATATABLES_DATA);

    if (!datatablesDataClasses.isEmpty()) {
      return;
    }

    JavaPackage modulePackage = getProjectOperations().getTopLevelPackage(module.getModuleName());

    final JavaType javaType =
        new JavaType(String.format("%s.datatables.DatatablesData", modulePackage),
            module.getModuleName());
    final String physicalPath =
        getPathResolver().getCanonicalPath(javaType.getModule(), Path.SRC_MAIN_JAVA, javaType);


    // Including DatatablesData class
    InputStream inputStream = null;
    try {
      // Use defined template
      inputStream = FileUtils.getInputStream(getClass(), "DatatablesData-template._java");
      String input = IOUtils.toString(inputStream);
      // Replacing package
      input = input.replace("__PACKAGE__", javaType.getPackage().getFullyQualifiedPackageName());

      // Creating DatatablesData class
      getFileManager().createOrUpdateTextFileIfRequired(physicalPath, input, true);
    } catch (final IOException e) {
      throw new IllegalStateException(String.format("Unable to create '%s'", physicalPath), e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  /**
   * This method adds new WebMVCThymeleafUIConfiguration.java class inside .config
   * package of generated project
   * 
   * @param module
   */
  private void addWebMVCThymeleafUIConfigurationClass(Pom module) {

    // Check if already exists other main controller annotated with @RooWebMvcThymeleafUIConfiguration
    Set<ClassOrInterfaceTypeDetails> webMvcUIConfiguration =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_WEB_MVC_THYMELEAF_UI_CONFIGURATION);
    if (webMvcUIConfiguration != null && !webMvcUIConfiguration.isEmpty()) {
      return;
    }

    // If not, define new JavaType
    JavaType thymeleafUiConfig =
        new JavaType(String.format("%s.config.WebMvcThymeleafUIConfiguration",
            getProjectOperations().getTopLevelPackage(module.getModuleName())),
            module.getModuleName());

    // Check that new JavaType doesn't exists
    ClassOrInterfaceTypeDetails thymeleafUiConfigDetails =
        getTypeLocationService().getTypeDetails(thymeleafUiConfig);

    if (thymeleafUiConfigDetails != null) {
      AnnotationMetadata webMvcUIConfigAnnotation =
          thymeleafUiConfigDetails.getAnnotation(getMainControllerAnnotation());
      // Maybe, this config class already exists
      if (webMvcUIConfigAnnotation != null) {
        return;
      } else {
        throw new RuntimeException(
            "ERROR: You are trying to generate more than one WebMvcThymeleafUIConfiguration.");
      }
    }

    ClassOrInterfaceTypeDetailsBuilder cidBuilder = null;
    final LogicalPath path =
        getPathResolver().getPath(thymeleafUiConfig.getModule(), Path.SRC_MAIN_JAVA);
    final String resourceIdentifier =
        getTypeLocationService().getPhysicalTypeCanonicalPath(thymeleafUiConfig, path);
    final String declaredByMetadataId =
        PhysicalTypeIdentifier.createIdentifier(thymeleafUiConfig,
            getPathResolver().getPath(resourceIdentifier));

    List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
    annotations.add(new AnnotationMetadataBuilder(
        RooJavaType.ROO_WEB_MVC_THYMELEAF_UI_CONFIGURATION));

    cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC,
            thymeleafUiConfig, PhysicalTypeCategory.CLASS);
    cidBuilder.setAnnotations(annotations);

    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());
  }

  /**
   * Configure WebJar dependencies in required pom's.
   * 
   * @param module the module where Thymeleaf is going to be installed
   */
  public void addWebJars(Pom module) {
    String rootModuleName = "";

    // Create lists of dependencies with and without version
    List<Dependency> dependenciesWithVersion = new ArrayList<Dependency>();
    List<Dependency> dependenciesUnversioned = new ArrayList<Dependency>();

    //Add Bootstrap WebJar
    getProjectOperations().addProperty(rootModuleName, new Property("bootstrap.version", "3.3.6"));
    dependenciesWithVersion.add(new Dependency("org.webjars.bower", "bootstrap",
        "${bootstrap.version}"));
    dependenciesUnversioned.add(new Dependency("org.webjars.bower", "bootstrap", null));

    // Add Datatables and Datatables related WebJars
    getProjectOperations().addProperty(rootModuleName,
        new Property("datatables.version", "1.10.11"));
    dependenciesWithVersion.add(new Dependency("org.webjars.bower", "datatables",
        "${datatables.version}"));
    dependenciesUnversioned.add(new Dependency("org.webjars.bower", "datatables", null));

    getProjectOperations().addProperty(rootModuleName,
        new Property("datatables-bs.version", "1.10.11"));
    dependenciesWithVersion.add(new Dependency("org.webjars.bower", "datatables.net-bs",
        "${datatables-bs.version}"));
    dependenciesUnversioned.add(new Dependency("org.webjars.bower", "datatables.net-bs", null));

    getProjectOperations().addProperty(rootModuleName,
        new Property("datatables-buttons.version", "1.1.2"));
    dependenciesWithVersion.add(new Dependency("org.webjars.bower", "datatables.net-buttons",
        "${datatables-buttons.version}"));
    dependenciesUnversioned
        .add(new Dependency("org.webjars.bower", "datatables.net-buttons", null));

    getProjectOperations().addProperty(rootModuleName,
        new Property("datatables-buttons-bs.version", "1.1.2"));
    dependenciesWithVersion.add(new Dependency("org.webjars.bower", "datatables.net-buttons-bs",
        "${datatables-buttons-bs.version}"));
    dependenciesUnversioned.add(new Dependency("org.webjars.bower", "datatables.net-buttons-bs",
        null));

    getProjectOperations().addProperty(rootModuleName,
        new Property("datatables-responsive.version", "2.0.2"));
    dependenciesWithVersion.add(new Dependency("org.webjars.bower", "datatables.net-responsive",
        "${datatables-responsive.version}"));
    dependenciesUnversioned.add(new Dependency("org.webjars.bower", "datatables.net-responsive",
        null));

    getProjectOperations().addProperty(rootModuleName,
        new Property("datatables-responsive-bs.version", "2.0.2"));
    dependenciesWithVersion.add(new Dependency("org.webjars.bower", "datatables.net-responsive-bs",
        "${datatables-responsive-bs.version}"));
    dependenciesUnversioned.add(new Dependency("org.webjars.bower", "datatables.net-responsive-bs",
        null));

    getProjectOperations().addProperty(rootModuleName,
        new Property("datatables-select.version", "1.1.2"));
    dependenciesWithVersion.add(new Dependency("org.webjars.bower", "datatables.net-select",
        "${datatables-select.version}"));
    dependenciesUnversioned.add(new Dependency("org.webjars.bower", "datatables.net-select", null));

    getProjectOperations().addProperty(rootModuleName,
        new Property("datatables-select-bs.version", "1.1.2"));
    dependenciesWithVersion.add(new Dependency("org.webjars.bower", "datatables.net-select-bs",
        "${datatables-select-bs.version}"));
    dependenciesUnversioned.add(new Dependency("org.webjars.bower", "datatables.net-select-bs",
        null));

    // Add DatetimePicker WebJar
    getProjectOperations().addProperty(rootModuleName,
        new Property("datetimepicker.version", "2.5.4"));
    dependenciesWithVersion.add(new Dependency("org.webjars.bower", "datetimepicker",
        "${datetimepicker.version}"));
    dependenciesUnversioned.add(new Dependency("org.webjars.bower", "datetimepicker", null));

    // Add FontAwesome WebJar
    getProjectOperations()
        .addProperty(rootModuleName, new Property("fontawesome.version", "4.6.2"));
    dependenciesWithVersion.add(new Dependency("org.webjars.bower", "font-awesome",
        "${fontawesome.version}"));
    dependenciesUnversioned.add(new Dependency("org.webjars.bower", "font-awesome", null));

    // Add jQuery WebJar
    getProjectOperations().addProperty(rootModuleName, new Property("jquery.version", "1.12.3"));
    dependenciesWithVersion.add(new Dependency("org.webjars.bower", "jquery", "${jquery.version}"));
    dependenciesUnversioned.add(new Dependency("org.webjars.bower", "jquery", null));

    // Add jQuery InputMask WebJar
    getProjectOperations().addProperty(rootModuleName,
        new Property("jquery-inputmask.version", "3.3.1"));
    dependenciesWithVersion.add(new Dependency("org.webjars", "jquery.inputmask",
        "${jquery-inputmask.version}"));
    dependenciesUnversioned.add(new Dependency("org.webjars", "jquery.inputmask", null));

    // Add jQuery InputMask WebJar
    getProjectOperations().addProperty(rootModuleName,
        new Property("jquery-validation.version", "1.15.0"));
    dependenciesWithVersion.add(new Dependency("org.webjars.bower", "jquery-validation",
        "${jquery-validation.version}"));
    dependenciesUnversioned.add(new Dependency("org.webjars.bower", "jquery-validation", null));

    // Add MomentJS WebJar
    getProjectOperations().addProperty(rootModuleName, new Property("momentjs.version", "2.13.0"));
    dependenciesWithVersion.add(new Dependency("org.webjars.bower", "momentjs",
        "${momentjs.version}"));
    dependenciesUnversioned.add(new Dependency("org.webjars.bower", "momentjs", null));

    // Add Select2 WebJar
    getProjectOperations().addProperty(rootModuleName, new Property("select2.version", "4.0.2"));
    dependenciesWithVersion
        .add(new Dependency("org.webjars.bower", "select2", "${select2.version}"));
    dependenciesUnversioned.add(new Dependency("org.webjars.bower", "select2", null));

    // Add Select2 Bootstrap Theme WebJar
    getProjectOperations().addProperty(rootModuleName,
        new Property("select2-bootstrap-theme.version", "0.1.0-beta.7"));
    dependenciesWithVersion.add(new Dependency("org.webjars.bower", "select2-bootstrap-theme",
        "${select2-bootstrap-theme.version}"));
    dependenciesUnversioned
        .add(new Dependency("org.webjars.bower", "select2-bootstrap-theme", null));

    if (projectOperations.isMultimoduleProject()) {

      // If multimodule project, add dependencies with version to dependencyManagement
      getProjectOperations().addDependenciesToDependencyManagement(rootModuleName,
          dependenciesWithVersion);

      // Add dependencies without version to specified module
      getProjectOperations().addDependencies(module.getModuleName(), dependenciesUnversioned);
    } else {

      // Add dependencies with version to the only module
      getProjectOperations().addDependencies(module.getModuleName(), dependenciesWithVersion);
    }
  }

  /**
   * This method checks if THYMELEAF dependencies has been installed before
   * 
   * @param module
   * @return
   */
  private boolean hasThymeleafDependencies(String moduleName) {
    if (!getProjectOperations().getPomFromModuleName(moduleName)
        .getDependenciesExcludingVersion(starterThymeleafDependency).isEmpty()
        && !getProjectOperations().getPomFromModuleName(moduleName)
            .getDependenciesExcludingVersion(layoutThymeleafDependency).isEmpty()) {
      return true;
    }

    return false;
  }

  // Getting OSGi services

  public ProjectOperations getProjectOperations() {
    if (projectOperations == null) {
      // Get all Services implement ProjectOperations interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ProjectOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          projectOperations = (ProjectOperations) this.context.getService(ref);
          return projectOperations;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ProjectOperations on ThymeleafMVCViewResponseService.");
        return null;
      }
    } else {
      return projectOperations;
    }
  }

  public TypeLocationService getTypeLocationService() {
    if (typeLocationService == null) {
      // Get all Services implement TypeLocationService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(TypeLocationService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          typeLocationService = (TypeLocationService) this.context.getService(ref);
          return typeLocationService;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TypeLocationService on ThymeleafMVCViewResponseService.");
        return null;
      }
    } else {
      return typeLocationService;
    }
  }

  public TypeManagementService getTypeManagementService() {
    if (typeManagementService == null) {
      // Get all Services implement TypeManagementService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(TypeManagementService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          typeManagementService = (TypeManagementService) this.context.getService(ref);
          return typeManagementService;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TypeManagementService on ThymeleafMVCViewResponseService.");
        return null;
      }
    } else {
      return typeManagementService;
    }
  }

  public PathResolver getPathResolver() {
    if (pathResolver == null) {
      // Get all Services implement PathResolver interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(PathResolver.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          pathResolver = (PathResolver) this.context.getService(ref);
          return pathResolver;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load PathResolver on ThymeleafMVCViewResponseService.");
        return null;
      }
    } else {
      return pathResolver;
    }
  }

  public FileManager getFileManager() {
    if (fileManager == null) {
      // Get all Services implement FileManager interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(FileManager.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          fileManager = (FileManager) this.context.getService(ref);
          return fileManager;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load FileManager on ThymeleafMVCViewResponseService.");
        return null;
      }
    } else {
      return fileManager;
    }
  }

  public MVCViewGenerationService getViewGenerationService() {
    if (viewGenerationService == null) {
      // Get all Services implement MVCViewGenerationService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(MVCViewGenerationService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          MVCViewGenerationService service =
              (MVCViewGenerationService) this.context.getService(ref);
          if (service.getName().equals(getName())) {
            viewGenerationService = service;
            return viewGenerationService;
          }
        }
        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load MVCViewGenerationService on ThymeleafMVCViewResponseService.");
        return null;
      }
    } else {
      return viewGenerationService;
    }
  }

  public I18nOperations getI18nOperations() {
    if (i18nOperations == null) {
      // Get all Services implement ProjectOperations interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(I18nOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          i18nOperations = (I18nOperations) this.context.getService(ref);
          return i18nOperations;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ProjectOperations on ThymeleafMvcViewResponseService.");
        return null;
      }
    } else {
      return i18nOperations;
    }
  }
}
