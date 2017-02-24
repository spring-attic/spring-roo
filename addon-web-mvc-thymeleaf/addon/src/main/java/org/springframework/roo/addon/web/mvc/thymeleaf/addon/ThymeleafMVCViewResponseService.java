package org.springframework.roo.addon.web.mvc.thymeleaf.addon;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.web.mvc.controller.addon.responses.ControllerMVCResponseService;
import org.springframework.roo.addon.web.mvc.i18n.I18nOperations;
import org.springframework.roo.addon.web.mvc.i18n.languages.EnglishLanguage;
import org.springframework.roo.addon.web.mvc.views.ViewContext;
import org.springframework.roo.application.config.ApplicationConfigService;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.operations.AbstractOperations;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Property;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.osgi.ServiceInstaceManager;
import org.springframework.roo.support.util.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

  private static final String RESPONSE_TYPE = "THYMELEAF";
  private static final String CONTROLLER_NAME_MODIFIER = "Thymeleaf";
  private static final String SPRING_MESSAGES_ENCODING = "spring.messages.encoding";
  private static final String SPRING_MESSAGES_ENCODING_VALUE = "ISO-8859-1";
  private static final String SPRING_THYMELEAF_MODE = "spring.thymeleaf.mode";
  private static final String SPRING_THYMELEAF_MODE_VALUE = "html";

  private static final Property DYNAMIC_JASPER_VERSION_PROPERTY = new Property(
      "dynamicjasper.version", "5.0.11");
  private static final Property DYNAMIC_JASPER_FONTS_VERSION_PROPERTY = new Property(
      "dynamicjasper-fonts.version", "1.0");

  private static final String DYNAMIC_JASPER_VERSION_PROPERTY_NAME = "${dynamicjasper.version}";
  private static final String DYNAMIC_JASPER_FONTS_VERSION_PROPERTY_NAME =
      "${dynamicjasper-fonts.version}";

  private static final Dependency STARTER_THYMELEAF_DEPENDENCY = new Dependency(
      "org.springframework.boot", "spring-boot-starter-thymeleaf", null);
  private static final Dependency LAYOUT_THYMELEAF_DEPENDENCY = new Dependency(
      "nz.net.ultraq.thymeleaf", "thymeleaf-layout-dialect", null);
  private static final Dependency DATA_THYMELEAF_DEPENDENCY = new Dependency(
      "com.github.mxab.thymeleaf.extras", "thymeleaf-extras-data-attribute",
      "${thymeleaf-data-dialect.version}");

  private static final Dependency DYNAMIC_JASPER_DEPENDENCY = new Dependency("ar.com.fdvs",
      "DynamicJasper", DYNAMIC_JASPER_VERSION_PROPERTY_NAME);
  private static final Dependency DYNAMIC_JASPER_CORE_FONTS_DEPENDENCY = new Dependency(
      "ar.com.fdvs", "DynamicJasper-core-fonts", DYNAMIC_JASPER_FONTS_VERSION_PROPERTY_NAME);
  private static final Dependency POI_DEPENDENCY = new Dependency("org.apache.poi", "poi", null);
  private static final Dependency SPRING_CONTEXT_SUPPORT = new Dependency("org.springframework",
      "spring-context-support", null);

  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();

  @Override
  protected void activate(final ComponentContext cContext) {
    super.activate(cContext);
    context = cContext.getBundleContext();
    serviceInstaceManager.activate(context);
  }

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

    // Add JasperReport dependencies
    addJasperReportsDependencies(module);

    // Add JasperReport export classes
    addJasperReportExportClasses(module);

    // Is necessary to generate the main controller
    addMainController(module);

    // Thymeleaf needs Datatables component to list
    // data, so is necessary to install Datatables resources
    addThymeleafDatatablesResources(module);

    // Is necessary to copy static resources
    copyStaticResources(module);

    // Add application properties
    addApplicationProperties(module);

    // Add all available WebJar containing required static resources
    addWebJars(module);

    // Delegate on view generation to create view elements
    ViewContext<ThymeleafMetadata> ctx = new ViewContext<ThymeleafMetadata>();
    ctx.setProjectName(getProjectOperations().getProjectName(""));
    ctx.setVersion(getProjectOperations().getPomFromModuleName("").getVersion());

    getViewGenerationService().addIndexView(module.getModuleName(), ctx);
    getViewGenerationService().addLoginView(module.getModuleName(), ctx);
    getViewGenerationService().addErrorView(module.getModuleName(), ctx);
    getViewGenerationService().addDefaultLayout(module.getModuleName(), ctx);
    getViewGenerationService().addDefaultLayoutNoMenu(module.getModuleName(), ctx);
    getViewGenerationService().addHomeLayout(module.getModuleName(), ctx);
    getViewGenerationService().addDefaultListLayout(module.getModuleName(), ctx);
    getViewGenerationService().addFooter(module.getModuleName(), ctx);
    getViewGenerationService().addHeader(module.getModuleName(), ctx);
    getViewGenerationService().addMenu(module.getModuleName(), ctx);
    getViewGenerationService().addModal(module.getModuleName(), ctx);
    getViewGenerationService().addModalConfirm(module.getModuleName(), ctx);
    getViewGenerationService().addModalConfirmDelete(module.getModuleName(), ctx);
    getViewGenerationService().addModalExportEmptyError(module.getModuleName(), ctx);
    getViewGenerationService().addModalConfirmDeleteBatch(module.getModuleName(), ctx);
    getViewGenerationService().addSessionLinks(module.getModuleName(), ctx);
    getViewGenerationService().addLanguages(module.getModuleName(), ctx);
    getViewGenerationService().addAccessibilityView(module.getModuleName(), ctx);
    getViewGenerationService().addDefaultLayoutNoMenu(module.getModuleName(), ctx);

    // Add i18n support for english language and use it as default
    getI18nOperations().installLanguage(new EnglishLanguage(), true, module);
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
   * This method adds JasperReports dependencies to generated project
   *
   * @param module
   */
  private void addJasperReportsDependencies(Pom module) {
    String moduleName = module.getModuleName();

    // Add Dynamic Jasper dependencies
    getProjectOperations().addDependency(moduleName, DYNAMIC_JASPER_DEPENDENCY);
    getProjectOperations().addDependency(moduleName, DYNAMIC_JASPER_CORE_FONTS_DEPENDENCY);

    // Add Apache POI dependency
    getProjectOperations().addDependency(moduleName, POI_DEPENDENCY);

    // Add Spring Context Support dependency
    getProjectOperations().addDependency(moduleName, SPRING_CONTEXT_SUPPORT);

    // Add version properties
    getProjectOperations().addProperty("", DYNAMIC_JASPER_VERSION_PROPERTY);
    getProjectOperations().addProperty("", DYNAMIC_JASPER_FONTS_VERSION_PROPERTY);
  }

  /**
   * Create the support classes to allow export data to different file 
   * types using JasperReports.
   * 
   * @param module the Pom from module where classes should be created.
   */
  private void addJasperReportExportClasses(Pom module) {

    // Create the interface
    createJasperReportsClassFromTemplate(module, "JasperReportsExporter-template._java",
        "JasperReportsExporter");

    // Create one implementation for each type of data
    createJasperReportsClassFromTemplate(module, "JasperReportsCsvExporter-template._java",
        "JasperReportsCsvExporter");
    createJasperReportsClassFromTemplate(module, "JasperReportsPdfExporter-template._java",
        "JasperReportsPdfExporter");
    createJasperReportsClassFromTemplate(module, "JasperReportsXlsExporter-template._java",
        "JasperReportsXlsExporter");

    // Add ExportingErrorException
    createJasperReportsClassFromTemplate(module, "ExportingErrorException-template._java",
        "ExportingErrorException");
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
   * This method adds two spring properties. One of them specifies HTML as the 
   * template mode to use by Thymeleaf for all existing profiles and the other 
   * one specifies the messages files encoding as ISO-8859-1. 
   * 
   * @param module
   */
  private void addApplicationProperties(Pom module) {
    List<String> applicationProfiles =
        getApplicationConfigService().getApplicationProfiles(module.getModuleName());
    for (String profile : applicationProfiles) {
      getApplicationConfigService().addProperty(module.getModuleName(), SPRING_THYMELEAF_MODE,
          SPRING_THYMELEAF_MODE_VALUE, profile, false);
    }

    getApplicationConfigService().addProperty(module.getModuleName(), SPRING_MESSAGES_ENCODING,
        SPRING_MESSAGES_ENCODING_VALUE, "", false);
  }

  /**
   * This method adds necessary thymeleaf dependencies to
   * generated project
   *
   * @param module
   */
  private void addThymeleafDependencies(Pom module) {

    // Add Thymeleaf starter
    getProjectOperations().addDependency(module.getModuleName(), STARTER_THYMELEAF_DEPENDENCY);

    // Add Thymeleaf layout dialect
    getProjectOperations().addDependency(module.getModuleName(), LAYOUT_THYMELEAF_DEPENDENCY);

    // Add Thymeleaf data dialect
    getProjectOperations().addDependency(module.getModuleName(), DATA_THYMELEAF_DEPENDENCY);

    // ROO-3813: Use Thymeleaf 3.0 instead of the provided version by Spring IO
    // More info about Thymelead 3.0 using Spring Boot here
    // http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto-use-thymeleaf-3
    getProjectOperations().addProperty("", new Property("thymeleaf.version", "3.0.0.RELEASE"));
    getProjectOperations().addProperty("",
        new Property("thymeleaf-layout-dialect.version", "2.0.0"));
    getProjectOperations().addProperty("", new Property("thymeleaf-data-dialect.version", "2.0.1"));
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
    // Add WebMVCThymeleafUIConfiguration config class
    addWebMVCThymeleafUIConfiguration(module);
  }

  /**
   * Configure WebJar dependencies in required pom's.
   *
   * @param module the module where Thymeleaf is going to be installed
   */
  private void addWebJars(Pom module) {
    String rootModuleName = "";

    List<Dependency> dependencies = new ArrayList<Dependency>();

    // Add WebJar locator dependency
    dependencies.add(new Dependency("org.webjars", "webjars-locator", null));

    //Add Bootstrap WebJar
    getProjectOperations().addProperty(rootModuleName, new Property("bootstrap.version", "3.3.6"));
    dependencies.add(new Dependency("org.webjars.bower", "bootstrap", "${bootstrap.version}"));

    // Add Datatables and Datatables related WebJars
    getProjectOperations().addProperty(rootModuleName,
        new Property("datatables.version", "1.10.12"));
    dependencies.add(new Dependency("org.webjars.bower", "datatables", "${datatables.version}"));

    getProjectOperations().addProperty(rootModuleName,
        new Property("datatables-bs.version", "1.10.11"));
    dependencies.add(new Dependency("org.webjars.bower", "datatables.net-bs",
        "${datatables-bs.version}"));

    getProjectOperations().addProperty(rootModuleName,
        new Property("datatables-buttons.version", "1.1.2"));
    dependencies.add(new Dependency("org.webjars.bower", "datatables.net-buttons",
        "${datatables-buttons.version}"));

    getProjectOperations().addProperty(rootModuleName,
        new Property("datatables-buttons-bs.version", "1.1.2"));
    dependencies.add(new Dependency("org.webjars.bower", "datatables.net-buttons-bs",
        "${datatables-buttons-bs.version}"));

    getProjectOperations().addProperty(rootModuleName,
        new Property("datatables-responsive.version", "2.0.2"));
    dependencies.add(new Dependency("org.webjars.bower", "datatables.net-responsive",
        "${datatables-responsive.version}"));

    getProjectOperations().addProperty(rootModuleName,
        new Property("datatables-responsive-bs.version", "2.0.2"));
    dependencies.add(new Dependency("org.webjars.bower", "datatables.net-responsive-bs",
        "${datatables-responsive-bs.version}"));

    getProjectOperations().addProperty(rootModuleName,
        new Property("datatables-select.version", "1.1.2"));
    dependencies.add(new Dependency("org.webjars.bower", "datatables.net-select",
        "${datatables-select.version}"));

    getProjectOperations().addProperty(rootModuleName,
        new Property("datatables-select-bs.version", "1.1.2"));
    dependencies.add(new Dependency("org.webjars.bower", "datatables.net-select-bs",
        "${datatables-select-bs.version}"));

    getProjectOperations().addProperty(rootModuleName,
        new Property("datatables-checkboxes.version", "1.1.2"));

    getProjectOperations().addProperty(rootModuleName,
        new Property("datatables-mark.version", "2.0.0"));


    // Add exclusions to avoid conflicts with Bower dependencies
    List<Dependency> exclusions = new ArrayList<Dependency>();
    exclusions.add(new Dependency("org.webjars.npm", "jquery", null));
    exclusions.add(new Dependency("org.webjars.npm", "datatables.net", null));
    dependencies.add(new Dependency("org.webjars.npm", "jquery-datatables-checkboxes",
        "${datatables-checkboxes.version}", exclusions));

    // Add Datatables mark WebJar
    dependencies.add(new Dependency("org.webjars.bower", "github-com-julmot-datatables-mark-js",
        "${datatables-mark.version}"));

    // Add DatetimePicker WebJar
    getProjectOperations().addProperty(rootModuleName,
        new Property("datetimepicker.version", "2.5.4"));
    dependencies.add(new Dependency("org.webjars.bower", "datetimepicker",
        "${datetimepicker.version}"));

    // Add FontAwesome WebJar
    getProjectOperations()
        .addProperty(rootModuleName, new Property("fontawesome.version", "4.6.2"));
    dependencies.add(new Dependency("org.webjars.bower", "font-awesome", "${fontawesome.version}"));

    // Add jQuery WebJar
    getProjectOperations().addProperty(rootModuleName, new Property("jquery.version", "1.12.3"));
    dependencies.add(new Dependency("org.webjars.bower", "jquery", "${jquery.version}"));

    // Add jQuery InputMask WebJar
    getProjectOperations().addProperty(rootModuleName,
        new Property("jquery-inputmask.version", "3.3.1"));
    // Add exclusions to avoid conflicts with Bower dependencies
    exclusions = new ArrayList<Dependency>();
    exclusions.add(new Dependency("org.webjars", "jquery", null));
    dependencies.add(new Dependency("org.webjars", "jquery.inputmask",
        "${jquery-inputmask.version}", exclusions));

    // Add jQuery InputMask WebJar
    getProjectOperations().addProperty(rootModuleName,
        new Property("jquery-validation.version", "1.15.0"));
    dependencies.add(new Dependency("org.webjars.bower", "jquery-validation",
        "${jquery-validation.version}"));

    // Add MomentJS WebJar
    getProjectOperations().addProperty(rootModuleName, new Property("momentjs.version", "2.13.0"));
    dependencies.add(new Dependency("org.webjars.bower", "momentjs", "${momentjs.version}"));

    // Add Select2 WebJar
    getProjectOperations().addProperty(rootModuleName, new Property("select2.version", "4.0.3"));
    dependencies.add(new Dependency("org.webjars.bower", "select2", "${select2.version}"));

    // Add Select2 Bootstrap Theme WebJar
    getProjectOperations().addProperty(rootModuleName,
        new Property("select2-bootstrap-theme.version", "0.1.0-beta.7"));
    dependencies.add(new Dependency("org.webjars.bower", "select2-bootstrap-theme",
        "${select2-bootstrap-theme.version}"));

    // Add respond WebJar
    getProjectOperations().addProperty(rootModuleName, new Property("respond.version", "1.4.2"));
    dependencies.add(new Dependency("org.webjars", "respond", "${respond.version}"));

    // Add html5shiv WebJar
    getProjectOperations().addProperty(rootModuleName, new Property("html5shiv.version", "3.7.3"));
    dependencies.add(new Dependency("org.webjars", "html5shiv", "${html5shiv.version}"));

    // Add ie10-viewport-bug-workaround WebJar
    getProjectOperations().addProperty(rootModuleName,
        new Property("bootstrap.ie10-viewport-bug-workaround.version", "1.0.3"));
    dependencies.add(new Dependency("org.webjars.bower", "ie10-viewport-bug-workaround",
        "${bootstrap.ie10-viewport-bug-workaround.version}"));

    getProjectOperations().addDependencies(module.getModuleName(), dependencies);

  }

  /**
   * This method adds new WebMVCThymeleafUIConfiguration.java class inside .config
   * package of generated project
   *
   * @param module
   */
  private void addWebMVCThymeleafUIConfiguration(Pom module) {

    // Obtain the class annotated with @RooWebMvcConfiguration
    Set<ClassOrInterfaceTypeDetails> webMvcConfigurationSet =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_WEB_MVC_CONFIGURATION);
    if (webMvcConfigurationSet == null || webMvcConfigurationSet.isEmpty()) {
      throw new RuntimeException(String.format(
          "ERROR: Can't found configuration class annotated with @%s.",
          RooJavaType.ROO_WEB_MVC_CONFIGURATION));
    }

    ClassOrInterfaceTypeDetails webMvcConfiguration = webMvcConfigurationSet.iterator().next();

    // Prevent to include the @RooWebMvcThymeleafUIConfiguration more than once
    if (webMvcConfiguration.getAnnotation(RooJavaType.ROO_WEB_MVC_THYMELEAF_UI_CONFIGURATION) == null) {
      AnnotationMetadataBuilder thymeleaftConfigurationAnnotation =
          new AnnotationMetadataBuilder(RooJavaType.ROO_WEB_MVC_THYMELEAF_UI_CONFIGURATION);

      ClassOrInterfaceTypeDetailsBuilder cidBuilder =
          new ClassOrInterfaceTypeDetailsBuilder(webMvcConfiguration);;

      cidBuilder.addAnnotation(thymeleaftConfigurationAnnotation);

      getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());
    }

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
        getPathResolver().getIdentifier(resourcesPath, "/templates/fragments/js"), true);
    copyDirectoryContents("templates/fragments/js/*.js",
        getPathResolver().getIdentifier(resourcesPath, "/templates/fragments/js"), true);

    // copy default JasperReports template
    copyDirectoryContents("templates/reports/*.jrxml",
        getPathResolver().getIdentifier(resourcesPath, "/templates/reports"), true);
  }

  /**
   * Creates a class from a template, replacing its package.
   *
   * @param module
   *            the Pom related to module where the class should be created
   * @param templateName
   *            the String with the template name
   * @param className
   *            the String with the class name to create
   */
  public void createJasperReportsClassFromTemplate(Pom module, String templateName, String className) {

    // Set package
    String packageName =
        getProjectOperations().getTopLevelPackage(module.getModuleName())
            .getFullyQualifiedPackageName().concat(".web.reports");

    // Include implementation of Validator from template
    final JavaType type =
        new JavaType(String.format("%s.%s", packageName, className), module.getModuleName());
    Validate.notNull(type.getModule(),
        "ERROR: Module name is required to generate a valid JavaType");
    final String identifier =
        getPathResolver().getCanonicalPath(type.getModule(), Path.SRC_MAIN_JAVA, type);
    InputStream inputStream = null;

    // Check first if file exists
    if (!this.fileManager.exists(identifier)) {
      try {

        // Use defined template
        inputStream = FileUtils.getInputStream(getClass(), templateName);
        String input = IOUtils.toString(inputStream);

        // Replacing package
        input = input.replace("__PACKAGE__", packageName);

        // Creating CollectionValidator
        this.fileManager.createOrUpdateTextFileIfRequired(identifier, input, true);
      } catch (final IOException e) {
        throw new IllegalStateException(String.format("Unable to create '%s'", identifier), e);
      } finally {
        IOUtils.closeQuietly(inputStream);
      }
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
        .getDependenciesExcludingVersion(STARTER_THYMELEAF_DEPENDENCY).isEmpty()
        && !getProjectOperations().getPomFromModuleName(moduleName)
            .getDependenciesExcludingVersion(LAYOUT_THYMELEAF_DEPENDENCY).isEmpty()) {
      return true;
    }

    return false;
  }

  // Getting OSGi services

  private ApplicationConfigService getApplicationConfigService() {
    return serviceInstaceManager.getServiceInstance(this, ApplicationConfigService.class);
  }

  private ProjectOperations getProjectOperations() {
    return serviceInstaceManager.getServiceInstance(this, ProjectOperations.class);
  }

  private TypeLocationService getTypeLocationService() {
    return serviceInstaceManager.getServiceInstance(this, TypeLocationService.class);
  }

  private TypeManagementService getTypeManagementService() {
    return serviceInstaceManager.getServiceInstance(this, TypeManagementService.class);
  }

  private PathResolver getPathResolver() {
    return serviceInstaceManager.getServiceInstance(this, PathResolver.class);
  }

  private ThymeleafViewGeneratorService getViewGenerationService() {
    return serviceInstaceManager.getServiceInstance(this, ThymeleafViewGeneratorService.class);
  }

  public I18nOperations getI18nOperations() {
    return serviceInstaceManager.getServiceInstance(this, I18nOperations.class);
  }

  @Override
  public String getControllerNameModifier() {
    return CONTROLLER_NAME_MODIFIER;
  }

  @Override
  public boolean requiresJsonDeserializer() {
    return true;
  }

  @Override
  public boolean requiresJsonMixin() {
    return true;
  }

  @Override
  public boolean providesViews() {
    // Thymeleaf provider uses HTML to represent the information
    return true;
  }
}
