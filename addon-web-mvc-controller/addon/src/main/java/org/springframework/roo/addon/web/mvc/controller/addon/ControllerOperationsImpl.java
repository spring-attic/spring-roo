package org.springframework.roo.addon.web.mvc.controller.addon;

import static java.lang.reflect.Modifier.PUBLIC;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.jvnet.inflector.Noun;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.addon.web.mvc.controller.addon.responses.ControllerMVCResponseService;
import org.springframework.roo.addon.web.mvc.controller.addon.servers.ServerProvider;
import org.springframework.roo.addon.web.mvc.controller.annotations.config.RooWebMvcJSONConfiguration;
import org.springframework.roo.application.config.ApplicationConfigService;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.ImportMetadata;
import org.springframework.roo.classpath.details.ImportMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.FileUtils;

/**
 * Implementation of {@link ControllerOperations}.
 * 
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @author Paula Navarro
 * @author Sergio Clares
 * @since 1.0
 */
@Component
@Service
public class ControllerOperationsImpl implements ControllerOperations {

  private static final Logger LOGGER = HandlerUtils.getLogger(ControllerOperationsImpl.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private static final JavaSymbolName PATH = new JavaSymbolName("path");

  private Map<String, ControllerMVCResponseService> responseTypes =
      new HashMap<String, ControllerMVCResponseService>();

  private ProjectOperations projectOperations;

  private TypeLocationService typeLocationService;

  private PathResolver pathResolver;

  private FileManager fileManager;

  private TypeManagementService typeManagementService;

  private ApplicationConfigService applicationConfigService;

  private Converter<Pom> pomConverter;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  /**
   * This operation will check if setup operation is available
   * 
   * @return true if setup operation is available. false if not.
   */
  @Override
  public boolean isSetupAvailable() {
    return getProjectOperations().isFocusedProjectAvailable()
        && !getProjectOperations().isFeatureInstalled(FeatureNames.MVC);
  }

  /**
   * This operation will setup Spring MVC on generated project.
   * 
   * @param module Pom module where Spring MVC should be included
   * @param appServer Server where application should be deployed
   */
  @Override
  public void setup(Pom module, ServerProvider appServer) {

    Validate.notNull(appServer, "Application server required");

    // Checks that provided module matches with Application properties
    // modules
    Validate
        .isTrue(
            getTypeLocationService().hasModuleFeature(module, ModuleFeatureName.APPLICATION),
            "ERROR: You are trying to install Spring MVC inside module that doesn't match with APPLICATION modules features.");

    // Add Spring MVC dependency
    getProjectOperations().addDependency(module.getModuleName(),
        new Dependency("org.springframework.boot", "spring-boot-starter-web", null));

    // Add DateTime dependency
    getProjectOperations().addDependency(module.getModuleName(),
        new Dependency("joda-time", "joda-time", null));

    // Create WebMvcConfiguration.java class
    JavaType webMvcConfiguration =
        new JavaType(String.format("%s.config.WebMvcConfiguration", module.getGroupId()),
            module.getModuleName());

    Validate.notNull(webMvcConfiguration.getModule(),
        "ERROR: Module name is required to generate a valid JavaType");

    // Checks if new service interface already exists.
    final String webMvcConfigurationIdentifier =
        getPathResolver().getCanonicalPath(webMvcConfiguration.getModule(), Path.SRC_MAIN_JAVA,
            webMvcConfiguration);

    if (!getFileManager().exists(webMvcConfigurationIdentifier)) {

      // Creating class builder
      final String mid =
          PhysicalTypeIdentifier.createIdentifier(webMvcConfiguration,
              getPathResolver().getPath(webMvcConfigurationIdentifier));
      final ClassOrInterfaceTypeDetailsBuilder typeBuilder =
          new ClassOrInterfaceTypeDetailsBuilder(mid, PUBLIC, webMvcConfiguration,
              PhysicalTypeCategory.CLASS);

      // Generating @RooWebMvcConfiguration annotation
      final AnnotationMetadataBuilder annotationMetadata =
          new AnnotationMetadataBuilder(RooJavaType.ROO_WEB_MVC_CONFIGURATION);
      typeBuilder.addAnnotation(annotationMetadata.build());

      // Write new class disk
      getTypeManagementService().createOrUpdateTypeOnDisk(typeBuilder.build());
    }

    // Create JSON configuration class
    createJsonConfigurationClass(module);

    // Create Roo Validator classes
    createClassFromTemplate(module, "CollectionValidator-template._java", "CollectionValidator",
        "validation");
    createClassFromTemplate(module, "ValidatorAdvice-template._java", "ValidatorAdvice",
        "validation");

    // Create Roo JSON converters
    createClassFromTemplate(module, "BindingErrorException-template._java",
        "BindingErrorException", "http.converter.json");
    createClassFromTemplate(module, "BindingResultSerializer-template._java",
        "BindingResultSerializer", "http.converter.json");
    createClassFromTemplate(module, "ConversionServiceBeanSerializerModifier-template._java",
        "ConversionServiceBeanSerializerModifier", "http.converter.json");
    createClassFromTemplate(module, "ConversionServicePropertySerializer-template._java",
        "ConversionServicePropertySerializer", "http.converter.json");
    createClassFromTemplate(module, "DataBinderBeanDeserializerModifier-template._java",
        "DataBinderBeanDeserializerModifier", "http.converter.json");
    createClassFromTemplate(module, "DataBinderDeserializer-template._java",
        "DataBinderDeserializer", "http.converter.json");
    createClassFromTemplate(module, "FieldErrorSerializer-template._java", "FieldErrorSerializer",
        "http.converter.json");
    createClassFromTemplate(module, "JsonpAdvice-template._java", "JsonpAdvice",
        "http.converter.json");
    createClassFromTemplate(module, "ExceptionHandlerAdvice-template._java",
        "ExceptionHandlerAdvice", "http.converter.json");

    // Adding spring.jackson.serialization.indent-output property
    getApplicationConfigService().addProperty(module.getModuleName(),
        "spring.jackson.serialization.indent-output", "true", "", true);

    // Add GlobalSearchHandlerMethodArgumentResolver.java
    addGlobalSearchHandlerMethodArgumentResolverClass(module);

    // Add server configuration
    appServer.setup(module);
  }

  /**
   * This method adds new GlobalSearchHandlerMethodArgumentResolver.java class
   * annotated with @RooGlobalSearchHandler
   * 
   * @param module
   */
  private void addGlobalSearchHandlerMethodArgumentResolverClass(Pom module) {
    // First of all, check if already exists a @RooThymeleafGlobalSearchHandler
    // class on current project
    Set<JavaType> globalSearchHandlerClasses =
        getTypeLocationService().findTypesWithAnnotation(RooJavaType.ROO_GLOBAL_SEARCH_HANDLER);

    if (!globalSearchHandlerClasses.isEmpty()) {
      return;
    }

    // Getting generated global class
    Set<ClassOrInterfaceTypeDetails> gobalSearchClasses =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_GLOBAL_SEARCH);
    if (gobalSearchClasses.isEmpty()) {
      throw new RuntimeException(
          "ERROR: GlobalSearch.java class doesn't exists or has been deleted.");
    }
    JavaType globalSearchClass = null;
    Iterator<ClassOrInterfaceTypeDetails> it = gobalSearchClasses.iterator();
    while (it.hasNext()) {
      globalSearchClass = it.next().getType();
      break;
    }

    JavaPackage modulePackage = getProjectOperations().getTopLevelPackage(module.getModuleName());

    final JavaType javaType =
        new JavaType(String.format(
            "%s.web.method.support.GlobalSearchHandlerMethodArgumentResolver", modulePackage),
            module.getModuleName());
    final String physicalPath =
        getPathResolver().getCanonicalPath(javaType.getModule(), Path.SRC_MAIN_JAVA, javaType);

    // Including GlobalSearchHandlerMethodArgumentResolver class
    InputStream inputStream = null;
    try {
      // Use defined template
      inputStream =
          FileUtils.getInputStream(getClass(),
              "GlobalSearchHandlerMethodArgumentResolver-template._java");
      String input = IOUtils.toString(inputStream);
      // Replacing package
      input = input.replace("__PACKAGE__", javaType.getPackage().getFullyQualifiedPackageName());
      input = input.replace("__GLOBAL_SEARCH__", globalSearchClass.getFullyQualifiedTypeName());


      // Creating GlobalSearchHandlerMethodArgumentResolver class
      getFileManager().createOrUpdateTextFileIfRequired(physicalPath, input, true);
    } catch (final IOException e) {
      throw new IllegalStateException(String.format("Unable to create '%s'", physicalPath), e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  /**
   * This operation will check if add controllers operation is available
   * 
   * @return true if add controller operation is available. false if not.
   */
  @Override
  public boolean isAddControllerAvailable() {
    return getProjectOperations().isFeatureInstalled(FeatureNames.MVC);
  }

  /**
   * Create WebMvcJSONConfiguration.java class and adds it 
   * {@link RooWebMvcJSONConfiguration} annotation
   * 
   * @param module the Pom where configuration classes should be installed
   */
  private void createJsonConfigurationClass(Pom module) {

    // Create WebMvcJSONConfiguration.java class
    JavaType webMvcJSONConfiguration =
        new JavaType(String.format("%s.config.WebMvcJSONConfiguration", module.getGroupId()),
            module.getModuleName());

    Validate.notNull(webMvcJSONConfiguration.getModule(),
        "ERROR: Module name is required to generate a valid JavaType");

    final String webMvcJSONConfigurationIdentifier =
        getPathResolver().getCanonicalPath(webMvcJSONConfiguration.getModule(), Path.SRC_MAIN_JAVA,
            webMvcJSONConfiguration);

    // Check if file already exists
    if (!getFileManager().exists(webMvcJSONConfigurationIdentifier)) {

      // Creating class builder
      final String mid =
          PhysicalTypeIdentifier.createIdentifier(webMvcJSONConfiguration, getPathResolver()
              .getPath(webMvcJSONConfigurationIdentifier));
      final ClassOrInterfaceTypeDetailsBuilder typeBuilder =
          new ClassOrInterfaceTypeDetailsBuilder(mid, PUBLIC, webMvcJSONConfiguration,
              PhysicalTypeCategory.CLASS);

      // Generating @RooWebMvcConfiguration annotation
      final AnnotationMetadataBuilder annotationMetadata =
          new AnnotationMetadataBuilder(RooJavaType.ROO_WEB_MVC_JSON_CONFIGURATION);
      typeBuilder.addAnnotation(annotationMetadata.build());

      // Write new class disk
      getTypeManagementService().createOrUpdateTypeOnDisk(typeBuilder.build());
    }
  }

  /**
   * This operation will generate a new controller for every class annotated
   * with @RooJpaEntity on current project.
   * 
   * @param controllersPackage
   * @param responseType
   * @param formattersPackage
   */
  @Override
  public void createControllerForAllEntities(JavaPackage controllersPackage,
      ControllerMVCResponseService responseType, JavaPackage formattersPackage) {

    // Getting all entities annotated with @RooJpaEntity
    Set<ClassOrInterfaceTypeDetails> allEntities =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_JPA_ENTITY);

    for (ClassOrInterfaceTypeDetails entity : allEntities) {
      // Check if exists yet some controller that manages this entity
      // and if current entity is not abstract
      JavaType entityController = getControllerForEntity(entity);
      if (entityController == null && !entity.isAbstract()) {
        // Generate controller JavaType
        JavaType controller =
            new JavaType(String.format("%s.%sController",
                controllersPackage.getFullyQualifiedPackageName(),
                Noun.pluralOf(entity.getType().getSimpleTypeName(), Locale.ENGLISH)),
                controllersPackage.getModule());

        // Generate path
        String path = getNotRepeatedControllerPathFromEntity(entity.getType());

        // Getting related service
        JavaType service = getServiceRelatedWithEntity(entity.getType());

        // Delegate on individual create controller method
        createController(controller, entity.getType(), service, path, responseType,
            formattersPackage);
      } else if (entityController != null) {
        // If controller exists, update it with new responseType
        updateController(entityController, responseType);
      }
    }

  }

  /**
   * This operation will generate a new controller with the specified
   * information
   * 
   * @param controller
   * @param entity
   * @param service
   * @param path
   * @param responseType
   * @param formattersPackage
   */
  @Override
  public void createController(JavaType controller, JavaType entity, JavaType service, String path,
      ControllerMVCResponseService responseType, JavaPackage formattersPackage) {
    JavaType serviceImplType = null;

    Validate.notNull(controller,
        "ERROR: Controller class is required to be able to generate new controller");
    Validate.notNull(entity,
        "ERROR: Entity class is required to be able to generate new controller");

    if (getProjectOperations().isMultimoduleProject()) {
      Validate
          .notNull(
              service,
              String
                  .format(
                      "ERROR: Service class related with '%s' entity is required to be able to generate new controller on multimodule projects.",
                      entity.getFullyQualifiedTypeName()));
    } else if (service == null) {
      // Getting related service
      Set<ClassOrInterfaceTypeDetails> services =
          getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
              RooJavaType.ROO_SERVICE);
      Iterator<ClassOrInterfaceTypeDetails> it = services.iterator();

      while (it.hasNext()) {
        ClassOrInterfaceTypeDetails existingService = it.next();
        AnnotationAttributeValue<Object> entityAttr =
            existingService.getAnnotation(RooJavaType.ROO_SERVICE).getAttribute("entity");
        if (entityAttr != null && entityAttr.getValue().equals(entity)) {
          service = existingService.getType();
        }
      }

      if (service == null) {
        // Is necessary at least one service to generate controller
        LOGGER
            .log(
                Level.INFO,
                String
                    .format(
                        "ERROR: You must generate a service to '%s' entity before to generate a new controller.",
                        entity.getFullyQualifiedTypeName()));
        return;
      }
    }

    // Validate that provided responseType exists and is installed
    Validate.isTrue(getInstalledControllerMVCResponseTypes().containsValue(responseType),
        "ERROR: Specified responseType is not valid or is not installed on current project. ");

    // Validate that new controller doesn't exists
    ClassOrInterfaceTypeDetails controllerDetails =
        getTypeLocationService().getTypeDetails(controller);
    if (controllerDetails != null) {
      LOGGER.log(
          Level.SEVERE,
          String.format("ERROR: Specified class '%s' already exists",
              controller.getFullyQualifiedTypeName()));
    }

    // Check if provided module is an application module
    Pom module = getPomConverter().convertFromText(controller.getModule(), Pom.class, "");
    Validate.isTrue(getTypeLocationService()
        .hasModuleFeature(module, ModuleFeatureName.APPLICATION),
        "ERROR: You are trying to generate controller inside a module that doesn't match with "
            + "ModuleFeature.APPLICATION");

    // Check if provided path exists
    if (StringUtils.isBlank(path)) {
      path = getNotRepeatedControllerPathFromEntity(entity);
    } else if (!path.startsWith("/")) {
      path = "/".concat(path);
    }
    Validate.isTrue(!existsPathController(path),
        String.format("ERROR: The provided path %s already exists", path));

    // Check if provided entity exists and it's annotated with @RooJpaEntity
    ClassOrInterfaceTypeDetails entityDetails = getTypeLocationService().getTypeDetails(entity);
    Validate.notNull(
        entityDetails,
        String.format("ERROR: Provided entity '%s' doesn't exists on current project."
            + " Provide an entity class annotated with @RooJpaEntity",
            entity.getFullyQualifiedTypeName()));

    Validate
        .notNull(
            entityDetails.getAnnotation(RooJavaType.ROO_JPA_ENTITY),
            String
                .format(
                    "ERROR: Provided entity '%s' is not a valid class. Provide an entity class annotated with @RooJpaEntity",
                    entity.getFullyQualifiedTypeName()));

    // Check if provided service exists and it's annotated with @RooService
    // and service annotation has reference to provided entity
    ClassOrInterfaceTypeDetails serviceDetails = getTypeLocationService().getTypeDetails(service);
    Validate.notNull(
        serviceDetails,
        String.format("ERROR: Provided service '%s' doesn't exists on current project."
            + " Provide a service class annotated with @RooService",
            service.getFullyQualifiedTypeName()));

    AnnotationMetadata serviceAnnotation = serviceDetails.getAnnotation(RooJavaType.ROO_SERVICE);
    Validate
        .notNull(
            serviceAnnotation,
            String
                .format(
                    "ERROR: Provided service '%s' is not a valid class. Provide a service class annotated with @RooService",
                    service.getFullyQualifiedTypeName()));

    JavaType serviceEntity = (JavaType) serviceAnnotation.getAttribute("entity").getValue();
    Validate.isTrue(entity.equals(serviceEntity), String.format(
        "ERROR: Provided service '%s' is not related with provided entity '%s' class.",
        service.getFullyQualifiedTypeName(), entity.getFullyQualifiedTypeName()));

    // Find service implementation related to service api
    for (ClassOrInterfaceTypeDetails serviceImpl : typeLocationService
        .findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_SERVICE_IMPL)) {
      AnnotationAttributeValue<JavaType> serviceAttr =
          serviceImpl.getAnnotation(RooJavaType.ROO_SERVICE_IMPL).getAttribute("service");

      if (serviceAttr != null && serviceAttr.getValue().equals(service)) {
        serviceImplType = serviceImpl.getType();
        break;
      }
    }

    Validate
        .notNull(
            serviceImplType,
            String
                .format(
                    "ERROR: Provided service '%s' does not have a implementation generated. Use 'service' commands to generate a valid service and then try again.",
                    service.getFullyQualifiedTypeName()));

    ClassOrInterfaceTypeDetailsBuilder cidBuilder = null;
    final LogicalPath controllerPath =
        getPathResolver().getPath(controller.getModule(), Path.SRC_MAIN_JAVA);
    final String resourceIdentifier =
        getTypeLocationService().getPhysicalTypeCanonicalPath(controller, controllerPath);
    final String declaredByMetadataId =
        PhysicalTypeIdentifier.createIdentifier(controller,
            getPathResolver().getPath(resourceIdentifier));

    // Create annotation @RooController(path = "/test", entity = MyEntity.class,
    // service = MyService.class)
    List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
    annotations.add(getRooControllerAnnotation(entity, service, path, PATH));

    // Add responseType annotation. Don't use responseTypeService annotate to
    // prevent multiple
    // updates of the .java file. Annotate operation will be used during
    // controller update.
    annotations.add(new AnnotationMetadataBuilder(responseType.getAnnotation()));

    cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, controller,
            PhysicalTypeCategory.CLASS);
    cidBuilder.setAnnotations(annotations);

    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());

    // Generate formatter if needed
    if (!existsFormatterForEntity(entity)) {
      if (formattersPackage == null) {
        formattersPackage = controller.getPackage();
      }

      // Check if formattersPackage provided package is an application module
      Pom formattersModule =
          getPomConverter().convertFromText(formattersPackage.getModule(), Pom.class, "");
      Validate.isTrue(
          getTypeLocationService()
              .hasModuleFeature(formattersModule, ModuleFeatureName.APPLICATION),
          "ERROR: You are trying to generate formatters inside a module that doesn't match with "
              + "ModuleFeature.APPLICATION");

      addRooFormatter(entity, service, formattersPackage);

      getProjectOperations().addModuleDependency(controller.getModule(),
          formattersPackage.getModule());
    }

    // Add dependencies between modules
    getProjectOperations().addModuleDependency(controller.getModule(), serviceImplType.getModule());
    getProjectOperations().addModuleDependency(controller.getModule(), service.getModule());
  }

  @Override
  public void updateController(JavaType controller, ControllerMVCResponseService responseType) {
    ClassOrInterfaceTypeDetails controllerCid = getTypeLocationService().getTypeDetails(controller);
    if (controllerCid.getAnnotation(responseType.getAnnotation()) == null) {

      // Get controller builder
      ClassOrInterfaceTypeDetailsBuilder controllerCidBuilder =
          new ClassOrInterfaceTypeDetailsBuilder(controllerCid);
      controllerCidBuilder
          .addAnnotation(new AnnotationMetadataBuilder(responseType.getAnnotation()).build());

      // Write changes to disk
      getTypeManagementService().createOrUpdateTypeOnDisk(controllerCidBuilder.build());
    } else {
      LOGGER.info(String.format("No changes are necessary. %s already has %s responseType",
          controller.getSimpleTypeName(), responseType.getName()));
    }
  }

  /**
   * This method checks if exists some formatter for provided entity
   * 
   * @param entity
   * @return
   */
  private boolean existsFormatterForEntity(JavaType entity) {

    // Obtain all formatters
    Set<ClassOrInterfaceTypeDetails> allFormatters =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_FORMATTER);

    for (ClassOrInterfaceTypeDetails formatter : allFormatters) {
      AnnotationMetadata annotation = formatter.getAnnotation(RooJavaType.ROO_FORMATTER);
      AnnotationAttributeValue<JavaType> entityAttribute = annotation.getAttribute("entity");
      if (entityAttribute != null && entity.equals(entityAttribute.getValue())) {
        return true;
      }
    }

    return false;

  }

  /**
   * This method generates new formatter inside generated project. This
   * formatter will be annotated with @RooFormatted and will be related with
   * some existing entity.
   * 
   * @param entity
   * @param service
   * @param formattersPackage
   */
  private void addRooFormatter(JavaType entity, JavaType service, JavaPackage formattersPackage) {

    JavaType formatter =
        new JavaType(String.format("%s.%sFormatter",
            formattersPackage.getFullyQualifiedPackageName(), entity.getSimpleTypeName()),
            formattersPackage.getModule());

    ClassOrInterfaceTypeDetailsBuilder cidBuilder = null;
    final LogicalPath fomatterPath =
        getPathResolver().getPath(formattersPackage.getModule(), Path.SRC_MAIN_JAVA);
    final String resourceIdentifier =
        getTypeLocationService().getPhysicalTypeCanonicalPath(formatter, fomatterPath);
    final String declaredByMetadataId =
        PhysicalTypeIdentifier.createIdentifier(formatter,
            getPathResolver().getPath(resourceIdentifier));

    // Create annotation @RooFormatter(entity = MyEntity.class, service =
    // MyService.class)
    List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
    annotations.add(getRooFormatterAnnotation(entity, service));
    cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, formatter,
            PhysicalTypeCategory.CLASS);
    cidBuilder.setAnnotations(annotations);

    // Adding necessary imports
    List<ImportMetadata> imports = new ArrayList<ImportMetadata>();

    ImportMetadataBuilder conversionServiceImport = new ImportMetadataBuilder(declaredByMetadataId);
    conversionServiceImport.setImportType(SpringJavaType.CONVERSION_SERVICE);
    imports.add(conversionServiceImport.build());

    cidBuilder.addImports(imports);

    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());

    // Add dependencies between module
    getProjectOperations().addModuleDependency(formattersPackage.getModule(), service.getModule());
    getProjectOperations().addModuleDependency(formattersPackage.getModule(), entity.getModule());

  }

  /**
   * This method gets the service class related with provided entity
   * 
   * @param entity
   * @return
   */
  private JavaType getServiceRelatedWithEntity(JavaType entity) {
    Set<ClassOrInterfaceTypeDetails> currentServices =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_SERVICE);

    for (ClassOrInterfaceTypeDetails service : currentServices) {
      JavaType serviceEntity =
          (JavaType) service.getAnnotation(RooJavaType.ROO_SERVICE).getAttribute("entity")
              .getValue();

      if (entity.equals(serviceEntity)) {
        return service.getType();
      }
    }

    return null;
  }

  /**
   * This method generates a valir controller path that doesn't repeat on any
   * controller of current project. This prevent request mapping errors.
   * 
   * @param entity
   * @return
   */
  private String getNotRepeatedControllerPathFromEntity(JavaType entity) {
    // Getting entity plural
    String entityPlural = Noun.pluralOf(entity.getSimpleTypeName().toLowerCase(), Locale.ENGLISH);

    String possiblePath = entityPlural;

    // Check if exists some other controller with this path
    int i = 2;
    while (existsPathController(possiblePath)) {
      possiblePath = entityPlural.concat(Integer.toString(i));
    }
    return "/".concat(possiblePath);
  }

  /**
   * This method checks if exists some controller inside generated project that
   * manages provided entity
   * 
   * @param entity
   * @return
   */
  private JavaType getControllerForEntity(ClassOrInterfaceTypeDetails entity) {
    Set<ClassOrInterfaceTypeDetails> currentControllers =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_CONTROLLER);

    for (ClassOrInterfaceTypeDetails controller : currentControllers) {

      AnnotationAttributeValue<JavaType> entityAttr =
          controller.getAnnotation(RooJavaType.ROO_CONTROLLER).getAttribute("entity");

      // Getting entity
      if (entityAttr != null && entity.getType().equals(entityAttr.getValue())) {
        return controller.getType();
      }
    }

    return null;
  }

  /**
   * Method that returns @RooController annotation
   * 
   * @param entity
   * @param service
   * @param path
   * @param pathName
   * @return
   */
  private AnnotationMetadataBuilder getRooControllerAnnotation(final JavaType entity,
      final JavaType service, final String path, final JavaSymbolName pathName) {
    final List<AnnotationAttributeValue<?>> rooControllerAttributes =
        new ArrayList<AnnotationAttributeValue<?>>();
    rooControllerAttributes.add(new StringAttributeValue(pathName, path));
    rooControllerAttributes.add(new ClassAttributeValue(new JavaSymbolName("entity"), entity));
    rooControllerAttributes.add(new ClassAttributeValue(new JavaSymbolName("service"), service));
    return new AnnotationMetadataBuilder(RooJavaType.ROO_CONTROLLER, rooControllerAttributes);
  }

  /**
   * Method that returns @RooFormatter annotation
   * 
   * @param entity
   * @param service
   * @return
   */
  private AnnotationMetadataBuilder getRooFormatterAnnotation(final JavaType entity,
      final JavaType service) {
    final List<AnnotationAttributeValue<?>> rooFormatterAttributes =
        new ArrayList<AnnotationAttributeValue<?>>();
    rooFormatterAttributes.add(new ClassAttributeValue(new JavaSymbolName("entity"), entity));
    rooFormatterAttributes.add(new ClassAttributeValue(new JavaSymbolName("service"), service));
    return new AnnotationMetadataBuilder(RooJavaType.ROO_FORMATTER, rooFormatterAttributes);
  }

  /**
   * This method checks if exists some other controller with the provided path
   * 
   * @param path
   * @return
   */
  private boolean existsPathController(String path) {
    Set<ClassOrInterfaceTypeDetails> currentControllers =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_CONTROLLER);

    for (ClassOrInterfaceTypeDetails controller : currentControllers) {
      String controllerPath =
          (String) controller.getAnnotation(RooJavaType.ROO_CONTROLLER).getAttribute("path")
              .getValue();

      if ("/".concat(path).equals(controllerPath)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Creates a class from a template
   * 
   * @param module the Pom related to modeule where the class should be created
   * @param templateName the String with the template name
   * @param className the String with the class name to create
   * @param packageLastElement the String (optional) with the last element of the package, which will be appended to module artifactId. If null, package will be module artifactId
   */
  public void createClassFromTemplate(Pom module, String templateName, String className,
      String packageLastElement) {

    // Set package
    String packageName = null;
    if (StringUtils.isNotBlank(packageLastElement)) {
      packageName = String.format("%s.%s", module.getGroupId(), packageLastElement);
    } else {
      packageName = module.getGroupId();
    }

    // Include implementation of Validator from template
    final JavaType type =
        new JavaType(String.format("%s.%s", packageName, className), module.getModuleName());
    Validate.notNull(type.getModule(),
        "ERROR: Module name is required to generate a valid JavaType");
    final String identifier =
        getPathResolver().getCanonicalPath(type.getModule(), Path.SRC_MAIN_JAVA, type);
    InputStream inputStream = null;

    // Check first if file exists
    if (!getFileManager().exists(identifier)) {
      try {

        // Use defined template
        inputStream = FileUtils.getInputStream(getClass(), templateName);
        String input = IOUtils.toString(inputStream);

        // Replacing package
        input = input.replace("__PACKAGE__", packageName);

        // Creating CollectionValidator
        getFileManager().createOrUpdateTextFileIfRequired(identifier, input, true);
      } catch (final IOException e) {
        throw new IllegalStateException(String.format("Unable to create '%s'", identifier), e);
      } finally {
        IOUtils.closeQuietly(inputStream);
      }
    }
  }

  // Methods to obtain OSGi Services

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
        LOGGER.warning("Cannot load TypeLocationService on ControllerOperationsImpl.");
        return null;
      }
    } else {
      return typeLocationService;
    }
  }

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
        LOGGER.warning("Cannot load ProjectOperations on ControllerOperationsImpl.");
        return null;
      }
    } else {
      return projectOperations;
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
        LOGGER.warning("Cannot load PathResolver on ControllerOperationsImpl.");
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
        LOGGER.warning("Cannot load FileManager on ControllerOperationsImpl.");
        return null;
      }
    } else {
      return fileManager;
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
        LOGGER.warning("Cannot load TypeManagementService on ControllerOperationsImpl.");
        return null;
      }
    } else {
      return typeManagementService;
    }
  }

  public ApplicationConfigService getApplicationConfigService() {
    if (applicationConfigService == null) {
      // Get all Services implement ApplicationConfigService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ApplicationConfigService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          applicationConfigService = (ApplicationConfigService) this.context.getService(ref);
          return applicationConfigService;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ApplicationConfigService on ControllerOperationsImpl.");
        return null;
      }
    } else {
      return applicationConfigService;
    }
  }

  /**
   * This method obtains Pom converter to be able to obtain Pom module from
   * strings
   * 
   * @return
   */
  @SuppressWarnings("unchecked")
  public Converter<Pom> getPomConverter() {
    if (pomConverter == null) {
      // Get all Services implement Converter<Pom> interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(Converter.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          Converter<?> converter = (Converter<?>) this.context.getService(ref);
          if (converter.supports(Pom.class, "")) {
            pomConverter = (Converter<Pom>) converter;
            return pomConverter;
          }
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load Converter<Pom> on ControllerOperationsImpl.");
        return null;
      }
    } else {
      return pomConverter;
    }
  }

  /**
   * This method gets all implementations of ControllerMVCResponseService
   * interface to be able to locate all installed ControllerMVCResponseService
   * 
   * @return Map with responseTypes identifier and the
   *         ControllerMVCResponseService implementation
   */
  public Map<String, ControllerMVCResponseService> getInstalledControllerMVCResponseTypes() {
    if (responseTypes.isEmpty()) {
      try {
        ServiceReference<?>[] references =
            this.context
                .getAllServiceReferences(ControllerMVCResponseService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          ControllerMVCResponseService responseTypeService =
              (ControllerMVCResponseService) this.context.getService(ref);
          boolean isInstalled = false;
          for (Pom module : getProjectOperations().getPoms()) {
            if (responseTypeService.isInstalledInModule(module.getModuleName())) {
              isInstalled = true;
              break;
            }
          }
          if (isInstalled) {
            responseTypes.put(responseTypeService.getResponseType(), responseTypeService);
          }
        }
        return responseTypes;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ControllerMVCResponseService on ControllerOperationsImpl.");
        return null;
      }
    } else {
      return responseTypes;
    }
  }

  @Override
  public String getName() {
    return FEATURE_NAME;
  }

  @Override
  public boolean isInstalledInModule(String moduleName) {
    Pom module = getProjectOperations().getPomFromModuleName(moduleName);
    for (JavaType javaType : getTypeLocationService().findTypesWithAnnotation(
        RooJavaType.ROO_WEB_MVC_CONFIGURATION)) {
      if (javaType.getModule().equals(moduleName)
          && module.hasDependencyExcludingVersion(new Dependency("org.springframework.boot",
              "spring-boot-starter-web", null))) {
        return true;
      }
    }
    return false;
  }

}
