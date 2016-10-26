package org.springframework.roo.addon.web.mvc.controller.addon;

import static java.lang.reflect.Modifier.PUBLIC;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.layers.service.addon.ServiceMetadata;
import org.springframework.roo.addon.plural.addon.PluralService;
import org.springframework.roo.addon.web.mvc.controller.addon.responses.ControllerMVCResponseService;
import org.springframework.roo.addon.web.mvc.controller.addon.servers.ServerProvider;
import org.springframework.roo.addon.web.mvc.controller.annotations.ControllerType;
import org.springframework.roo.addon.web.mvc.controller.annotations.config.RooWebMvcJSONConfiguration;
import org.springframework.roo.application.config.ApplicationConfigService;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.converters.PomConverter;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.RooEnumDetails;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.model.SpringletsJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Property;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;
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

  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();

  private Map<String, ControllerMVCResponseService> responseTypes =
      new HashMap<String, ControllerMVCResponseService>();

  private static final Property SPRINGLETS_VERSION_PROPERTY = new Property("springlets.version",
      "1.0.0.BUILD-SNAPSHOT");
  private static final Dependency SPRINGLETS_WEB_STARTER = new Dependency("io.springlets",
      "springlets-boot-starter-web", "${springlets.version}");

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
    serviceInstaceManager.activate(this.context);
  }

  /**
   * This operation will check if setup operation is available
   *
   * @return true if setup operation is available. false if not.
   */
  @Override
  public boolean isSetupAvailable() {
    return getProjectOperations().isFocusedProjectAvailable();
  }

  /**
   * This operation will setup Spring MVC on generated project.
   *
   * @param module
   *            Pom module where Spring MVC should be included
   * @param appServer
   *            Server where application should be deployed
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

    // Include Springlets Starter project dependencies and properties
    getProjectOperations().addProperty("", SPRINGLETS_VERSION_PROPERTY);

    getProjectOperations().addDependency(module.getModuleName(), SPRINGLETS_WEB_STARTER);


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
    createClassFromTemplate(module, "ExceptionHandlerAdvice-template._java",
        "ExceptionHandlerAdvice", "http.converter.json");

    // Adding spring.jackson.serialization.indent-output property
    getApplicationConfigService().addProperty(module.getModuleName(),
        "spring.jackson.serialization.indent-output", "true", "dev", true);

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
    // First of all, check if already exists a
    // @RooThymeleafGlobalSearchHandler
    // class on current project
    Set<JavaType> globalSearchHandlerClasses =
        getTypeLocationService().findTypesWithAnnotation(RooJavaType.ROO_GLOBAL_SEARCH_HANDLER);

    if (!globalSearchHandlerClasses.isEmpty()) {
      return;
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
      input =
          input.replace("__GLOBAL_SEARCH__",
              SpringletsJavaType.SPRINGLETS_GLOBAL_SEARCH.getFullyQualifiedTypeName());

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
   * This operation will check if add detail controllers operation is
   * available
   *
   * @return true if add detail controller operation is available. false if
   *         not.
   */
  @Override
  public boolean isAddDetailControllerAvailable() {
    return getProjectOperations().isFeatureInstalled(FeatureNames.MVC);
  }

  /**
   * This operation will check if the operation publish services methods is
   * available
   *
   * @return true if publish services methods is available. false if not.
   */
  @Override
  public boolean isPublishOperationsAvailable() {
    return getProjectOperations().isFeatureInstalled(FeatureNames.MVC);
  }

  /**
   * Create WebMvcJSONConfiguration.java class and adds it
   * {@link RooWebMvcJSONConfiguration} annotation
   *
   * @param module
   *            the Pom where configuration classes should be installed
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
   * This operation will generate or update a controller for every class
   * annotated with @RooJpaEntity
   *
   * @param responseType
   *            View provider to use
   * @param controllerPackage
   *            Package where is situated the controller
   * @param pathPrefix
   *            Prefix to use in RequestMapping
   */
  @Override
  public void createOrUpdateControllerForAllEntities(ControllerMVCResponseService responseType,
      JavaPackage controllerPackage, String pathPrefix) {

    // Getting all entities annotated with @RooJpaEntity
    Set<ClassOrInterfaceTypeDetails> entities =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_JPA_ENTITY);
    for (ClassOrInterfaceTypeDetails entity : entities) {
      if (!entity.isAbstract()) {
        createOrUpdateControllerForEntity(entity.getType(), responseType, controllerPackage,
            pathPrefix);
      }
    }

  }

  @Override
  public void createOrUpdateControllerForEntity(JavaType entity,
      ControllerMVCResponseService responseType, JavaPackage controllerPackage, String pathPrefix) {

    // Getting entity details to obtain information about it
    ClassOrInterfaceTypeDetails entityDetails = getTypeLocationService().getTypeDetails(entity);
    AnnotationMetadata entityAnnotation = entityDetails.getAnnotation(RooJavaType.ROO_JPA_ENTITY);
    if (entityAnnotation == null) {
      LOGGER
          .log(
              Level.INFO,
              String
                  .format(
                      "ERROR: The provided class %s is not a valid entity. It should be annotated with @RooEntity",
                      entity.getSimpleTypeName()));
      return;
    }

    // Getting related service
    JavaType service = null;
    Set<ClassOrInterfaceTypeDetails> services =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_SERVICE);
    Iterator<ClassOrInterfaceTypeDetails> itServices = services.iterator();

    while (itServices.hasNext()) {
      ClassOrInterfaceTypeDetails existingService = itServices.next();
      AnnotationAttributeValue<Object> entityAttr =
          existingService.getAnnotation(RooJavaType.ROO_SERVICE).getAttribute("entity");
      if (entityAttr != null && entityAttr.getValue().equals(entity)) {
        service = existingService.getType();
      }
    }

    if (service == null) {
      // Is necessary at least one service to generate controller
      LOGGER.log(Level.INFO, String.format(
          "ERROR: You must generate a service to '%s' entity before to generate a new controller.",
          entity.getFullyQualifiedTypeName()));
      return;
    }

    Set<ClassOrInterfaceTypeDetails> controllers =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_CONTROLLER);

    Iterator<ClassOrInterfaceTypeDetails> itControllers = controllers.iterator();

    while (itControllers.hasNext()) {
      ClassOrInterfaceTypeDetails existingController = itControllers.next();
      AnnotationAttributeValue<Object> entityAttr =
          existingController.getAnnotation(RooJavaType.ROO_CONTROLLER).getAttribute("entity");
      AnnotationAttributeValue<String> pathPrefixAttr =
          existingController.getAnnotation(RooJavaType.ROO_CONTROLLER).getAttribute("pathPrefix");
      AnnotationAttributeValue<Object> typeAttribute =
          existingController.getAnnotation(RooJavaType.ROO_CONTROLLER).getAttribute("type");
      ControllerType existingControllerType =
          ControllerType.getControllerType(((EnumDetails) typeAttribute.getValue()).getField()
              .getSymbolName());
      String pathPrefixAttrValue = "";
      if (pathPrefixAttr != null) {
        pathPrefixAttrValue = pathPrefixAttr.getValue();
      }
      if (entityAttr != null
          && entityAttr.getValue().equals(entity)
          && pathPrefixAttrValue.equals(pathPrefix)
          && existingController.getAnnotation(responseType.getAnnotation()) != null
          && (existingControllerType.equals(ControllerType
              .getControllerType(RooEnumDetails.CONTROLLER_TYPE_COLLECTION.getField()
                  .getSymbolName())) || existingControllerType.equals(ControllerType
              .getControllerType(RooEnumDetails.CONTROLLER_TYPE_ITEM.getField().getSymbolName())))) {
        LOGGER.log(Level.INFO, String.format(
            "ERROR: Already exists a controller associated to entity '%s' with the "
                + "pathPrefix '%s' for this responseType. Specify different one "
                + "using --pathPrefix or --responseType parameter.", entity.getSimpleTypeName(),
            pathPrefix));
        return;
      }
    }

    // Check controllersPackage value
    if (controllerPackage == null) {
      controllerPackage = getDefaultControllerPackage();
      if (controllerPackage == null) {
        return;
      }
    }

    Iterator<ClassOrInterfaceTypeDetails> it = controllers.iterator();

    while (it.hasNext()) {
      ClassOrInterfaceTypeDetails existingController = it.next();

      // Only check controllers from the specified package and its
      // ControllerType
      if (existingController.getType().getPackage().equals(controllerPackage)) {

        // Getting entity attribute
        AnnotationAttributeValue<Object> entityAttr =
            existingController.getAnnotation(RooJavaType.ROO_CONTROLLER).getAttribute("entity");

        // Get controller type
        AnnotationAttributeValue<Object> typeAttribute =
            existingController.getAnnotation(RooJavaType.ROO_CONTROLLER).getAttribute("type");
        ControllerType existingControllerType =
            ControllerType.getControllerType(((EnumDetails) typeAttribute.getValue()).getField()
                .getSymbolName());
        if (entityAttr != null
            && entityAttr.getValue().equals(entity)
            && (existingControllerType.equals(ControllerType
                .getControllerType(RooEnumDetails.CONTROLLER_TYPE_COLLECTION.getField()
                    .getSymbolName())) || existingControllerType.equals(ControllerType
                .getControllerType(RooEnumDetails.CONTROLLER_TYPE_ITEM.getField().getSymbolName())))) {

          // Exists a controller for the same entity in the same
          // provided package.
          // Let's check if also have the same responseType.
          if (existingController.getAnnotation(responseType.getAnnotation()) != null) {
            LOGGER.log(Level.INFO, String.format(
                "ERROR: Already exists a controller associated to entity '%s' in '%s' "
                    + "package with response type '%s'. If you want to update the existing "
                    + "controller, provide a different value in --responseType parameter.",
                entity.getSimpleTypeName(), controllerPackage.getFullyQualifiedPackageName(),
                responseType.getName()));
            return;
          } else {
            // If the controller exists but the specified
            // responseType has not been
            // applied to it yet, is time to update the controller
            // to include
            // a new responseType
            updateControllerWithResponseType(existingController.getType(), responseType);
          }
        }
      }
    }

    // Generate Collection controller JavaType
    JavaType collectionController =
        new JavaType(String.format("%s.%sCollectionController",
            controllerPackage.getFullyQualifiedPackageName(),
            StringUtils.capitalize(getPluralService().getPlural(entity))),
            controllerPackage.getModule());

    ClassOrInterfaceTypeDetails collectionControllerDetails =
        getTypeLocationService().getTypeDetails(collectionController);
    if (collectionControllerDetails == null) {
      List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
      annotations.add(getRooControllerAnnotation(entity, pathPrefix, ControllerType.COLLECTION));

      // Add responseType annotation. Don't use responseTypeService
      // annotate to
      // prevent multiple
      // updates of the .java file. Annotate operation will be used during
      // controller update.
      annotations.add(new AnnotationMetadataBuilder(responseType.getAnnotation()));

      final LogicalPath controllerPath =
          getPathResolver().getPath(collectionController.getModule(), Path.SRC_MAIN_JAVA);
      final String resourceIdentifier =
          getTypeLocationService().getPhysicalTypeCanonicalPath(collectionController,
              controllerPath);
      final String declaredByMetadataId =
          PhysicalTypeIdentifier.createIdentifier(collectionController,
              getPathResolver().getPath(resourceIdentifier));

      ClassOrInterfaceTypeDetailsBuilder cidBuilder =
          new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC,
              collectionController, PhysicalTypeCategory.CLASS);
      cidBuilder.setAnnotations(annotations);

      getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());
    }

    // Same operation to itemController

    // Generate Item Controller JavaType
    JavaType itemController =
        new JavaType(String.format("%s.%sItemController",
            controllerPackage.getFullyQualifiedPackageName(),
            StringUtils.capitalize(getPluralService().getPlural(entity))),
            controllerPackage.getModule());

    ClassOrInterfaceTypeDetails itemControllerDetails =
        getTypeLocationService().getTypeDetails(itemController);
    if (itemControllerDetails == null) {
      List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
      annotations = new ArrayList<AnnotationMetadataBuilder>();
      annotations.add(getRooControllerAnnotation(entity, pathPrefix, ControllerType.ITEM));

      // Add responseType annotation. Don't use responseTypeService
      // annotate to
      // prevent multiple
      // updates of the .java file. Annotate operation will be used during
      // controller update.
      annotations.add(new AnnotationMetadataBuilder(responseType.getAnnotation()));

      final LogicalPath controllerPathItem =
          getPathResolver().getPath(itemController.getModule(), Path.SRC_MAIN_JAVA);
      final String resourceIdentifierItem =
          getTypeLocationService().getPhysicalTypeCanonicalPath(itemController, controllerPathItem);
      final String declaredByMetadataIdItem =
          PhysicalTypeIdentifier.createIdentifier(itemController,
              getPathResolver().getPath(resourceIdentifierItem));
      ClassOrInterfaceTypeDetailsBuilder cidBuilder =
          new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataIdItem, Modifier.PUBLIC,
              itemController, PhysicalTypeCategory.CLASS);
      cidBuilder.setAnnotations(annotations);

      getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());
    }

    // Check multimodule project
    if (getProjectOperations().isMultimoduleProject()) {
      getProjectOperations().addModuleDependency(collectionController.getModule(),
          service.getModule());
      getProjectOperations().addModuleDependency(itemController.getModule(), service.getModule());

    }

  }

  public void updateControllerWithResponseType(JavaType controller,
      ControllerMVCResponseService responseType) {
    // Delegates on the provided responseType to annotate the controller.
    responseType.annotate(controller);
  }

  /**
   * Method that returns @RooController annotation
   *
   * @param entity
   *            Entity over which create the controller
   * @param pathPrefix
   *            Prefix to use in RequestMapping
   * @param controllerType
   *            Indicates the controller type
   * @return
   */
  private AnnotationMetadataBuilder getRooControllerAnnotation(final JavaType entity,
      final String pathPrefix, final ControllerType controllerType) {
    final List<AnnotationAttributeValue<?>> rooControllerAttributes =
        new ArrayList<AnnotationAttributeValue<?>>();
    rooControllerAttributes.add(new ClassAttributeValue(new JavaSymbolName("entity"), entity));
    if (StringUtils.isNotEmpty(pathPrefix)) {
      rooControllerAttributes.add(new StringAttributeValue(new JavaSymbolName("pathPrefix"),
          pathPrefix));
    }
    rooControllerAttributes.add(new EnumAttributeValue(new JavaSymbolName("type"), new EnumDetails(
        RooJavaType.ROO_ENUM_CONTROLLER_TYPE, new JavaSymbolName(controllerType.name()))));
    return new AnnotationMetadataBuilder(RooJavaType.ROO_CONTROLLER, rooControllerAttributes);
  }

  /**
   * Method that returns @RooDetail annotation
   *
   * @param relationField
   *            Field that set the relationship
   * @return
   */
  private AnnotationMetadataBuilder getRooDetailAnnotation(final String relationField) {
    AnnotationMetadataBuilder annotationDetail =
        new AnnotationMetadataBuilder(RooJavaType.ROO_DETAIL);
    annotationDetail.addStringAttribute("relationField", relationField);
    return annotationDetail;
  }

  /**
   * Creates a class from a template
   *
   * @param module
   *            the Pom related to modeule where the class should be created
   * @param templateName
   *            the String with the template name
   * @param className
   *            the String with the class name to create
   * @param packageLastElement
   *            the String (optional) with the last element of the package,
   *            which will be appended to module artifactId. If null, package
   *            will be module artifactId
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

  @Override
  public void createOrUpdateDetailControllersForAllEntities(
      ControllerMVCResponseService responseType, JavaPackage controllerPackage) {

    // Getting all entities annotated with @RooJpaEntity
    Set<ClassOrInterfaceTypeDetails> entities =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_JPA_ENTITY);
    for (ClassOrInterfaceTypeDetails entity : entities) {
      if (!entity.isAbstract()) {
        createOrUpdateDetailControllerForEntity(entity.getType(), "", responseType,
            controllerPackage);
      }
    }

  }

  @Override
  public void createOrUpdateDetailControllerForEntity(JavaType entity, String relationField,
      ControllerMVCResponseService responseType, JavaPackage controllerPackage) {

    // Getting entity details to obtain information about it
    ClassOrInterfaceTypeDetails entityDetails = getTypeLocationService().getTypeDetails(entity);
    AnnotationMetadata entityAnnotation = entityDetails.getAnnotation(RooJavaType.ROO_JPA_ENTITY);
    if (entityAnnotation == null) {
      LOGGER
          .log(
              Level.INFO,
              String
                  .format(
                      "ERROR: The provided class %s is not a valid entity. It should be annotated with @RooJpaEntity",
                      entity.getSimpleTypeName()));
      return;
    }

    // Check controllersPackage value
    if (controllerPackage == null) {
      controllerPackage = getDefaultControllerPackage();
      if (controllerPackage == null) {
        return;
      }
    }

    Set<ClassOrInterfaceTypeDetails> controllers =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_CONTROLLER);
    Iterator<ClassOrInterfaceTypeDetails> itControllers = controllers.iterator();

    boolean existsBasicControllers = false;
    String pathPrefixController = "";

    while (itControllers.hasNext()) {
      ClassOrInterfaceTypeDetails existingController = itControllers.next();

      if (existingController.getType().getPackage().equals(controllerPackage)) {

        AnnotationAttributeValue<Object> entityAttr =
            existingController.getAnnotation(RooJavaType.ROO_CONTROLLER).getAttribute("entity");
        AnnotationAttributeValue<Object> typeAttr =
            existingController.getAnnotation(RooJavaType.ROO_CONTROLLER).getAttribute("type");
        ControllerType existingControllerType =
            ControllerType.getControllerType(((EnumDetails) typeAttr.getValue()).getField()
                .getSymbolName());
        AnnotationAttributeValue<String> pathPrefixAttr =
            existingController.getAnnotation(RooJavaType.ROO_CONTROLLER).getAttribute("pathPrefix");

        AnnotationMetadata responseTypeAnnotation =
            existingController.getAnnotation(responseType.getAnnotation());
        if (entityAttr != null
            && entityAttr.getValue().equals(entity)
            && typeAttr != null
            && responseTypeAnnotation != null
            && (existingControllerType.equals(ControllerType.ITEM) || existingControllerType
                .equals(ControllerType.COLLECTION))) {

          if (pathPrefixAttr != null) {
            pathPrefixController = pathPrefixAttr.getValue();

          }
          existsBasicControllers = true;
          break;
        }
      }
    }

    if (!existsBasicControllers) {
      LOGGER
          .log(
              Level.INFO,
              String
                  .format(
                      "ERROR: Doesn't exist parent controller in the package %s with the response type %s for the entity %s. Please, use 'web mvc controller' command to create them",
                      controllerPackage, responseType.getName(), entity.getSimpleTypeName()));
      return;
    }

    boolean relationFieldIsValid = false;

    MemberDetails memberDetails =
        getMemberDetailsScanner().getMemberDetails(entity.getSimpleTypeName(), entityDetails);
    List<FieldMetadata> fields = memberDetails.getFields();
    List<String> relationFields = new ArrayList<String>();
    Map<String, String> relationFieldObject = new HashMap<String, String>();
    if (relationField != null && StringUtils.isNotEmpty(relationField)) {

      String[] relationFieldParse;
      if (relationField.contains(".")) {
        relationFieldParse = relationField.split("[.]");
      } else {
        relationFieldParse = new String[] {relationField};
      }

      relationFieldIsValid =
          checkRelationField(entityDetails, relationFieldParse, relationFieldObject, 0,
              responseType, controllerPackage, pathPrefixController, entity);

      if (relationFieldIsValid) {
        relationFields.add(relationField);
      } else {
        LOGGER
            .log(
                Level.INFO,
                String
                    .format(
                        "ERROR: the field '%s' can't generate a detail controller because it isn't a 'List' or 'Set' element or it doesn't pertain to entity '%s' or its relationships.",
                        relationField, entity.getSimpleTypeName()));
        return;
      }

    } else {

      // Get all first level related fields

      for (FieldMetadata field : fields) {

        AnnotationMetadata oneToManyAnnotation = field.getAnnotation(JpaJavaType.ONE_TO_MANY);

        if (oneToManyAnnotation != null
            && (field.getFieldType().getFullyQualifiedTypeName()
                .equals(JavaType.LIST.getFullyQualifiedTypeName()) || field.getFieldType()
                .getFullyQualifiedTypeName().equals(JavaType.SET.getFullyQualifiedTypeName()))) {

          relationFields.add(field.getFieldName().getSymbolName());
          relationFieldObject.put(field.getFieldName().getSymbolName(), field.getFieldType()
              .getParameters().get(0).getSimpleTypeName());

        }
      }

      if (relationFields.isEmpty()) {
        LOGGER.log(Level.INFO, String.format(
            "INFO: the entity '%s' hasn't attributes to generate detail controllers.",
            entity.getSimpleTypeName()));
        return;
      }
    }

    Set<ClassOrInterfaceTypeDetails> detailControllers =
        getTypeLocationService()
            .findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_DETAIL);
    Iterator<ClassOrInterfaceTypeDetails> itDetailControllers = detailControllers.iterator();
    while (itDetailControllers.hasNext()) {
      ClassOrInterfaceTypeDetails existingController = itDetailControllers.next();

      if (existingController.getType().getPackage().equals(controllerPackage)) {

        AnnotationAttributeValue<Object> entityAttr =
            existingController.getAnnotation(RooJavaType.ROO_CONTROLLER).getAttribute("entity");
        if (entityAttr != null && entityAttr.getValue().equals(entity)) {
          AnnotationMetadata annotationDetail =
              existingController.getAnnotation(RooJavaType.ROO_DETAIL);
          String relatedFieldController =
              (String) annotationDetail.getAttribute("relationField").getValue();
          List<String> relationFieldsToRemove = new ArrayList<String>();
          for (String field : relationFields) {
            // Check if exists
            if (field.equals(relatedFieldController)) {
              if (existingController.getAnnotation(responseType.getAnnotation()) == null) {
                // Update field
                responseType.annotate(existingController.getType());
                relationFieldsToRemove.add(field);
              } else {
                // Detail controller exists
                relationFieldsToRemove.add(field);
                LOGGER.log(Level.INFO, String.format(
                    "ERROR: the entity '%s' has already a detail controller for field %s.",
                    entity.getSimpleTypeName(), field));
              }
            }
          }
          relationFields.removeAll(relationFieldsToRemove);
        }
      }
    }

    for (String field : relationFields) {

      StringBuffer detailControllerName = new StringBuffer(getPluralService().getPlural(entity));
      detailControllerName.append("Item");
      if (field.contains(".")) {
        String[] splitField = field.split("[.]");
        for (String nameField : splitField) {
          detailControllerName.append(StringUtils.capitalize(nameField)).append("Item");
        }
      } else {
        detailControllerName.append(StringUtils.capitalize(field)).append("Item");
      }
      detailControllerName.append("Controller");

      JavaType detailController =
          new JavaType(String.format("%s.%s", controllerPackage.getFullyQualifiedPackageName(),
              detailControllerName), controllerPackage.getModule());

      ClassOrInterfaceTypeDetails detailControllerDetails =
          getTypeLocationService().getTypeDetails(detailController);
      if (detailControllerDetails != null) {
        LOGGER.log(Level.INFO, String.format(
            "ERROR: Class '%s' already exists inside your generated project.",
            detailController.getFullyQualifiedTypeName()));
      }

      List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
      annotations.add(getRooControllerAnnotation(entity, pathPrefixController,
          ControllerType.DETAIL));
      annotations.add(getRooDetailAnnotation(field));

      // Add responseType annotation. Don't use responseTypeService
      // annotate to
      // prevent multiple
      // updates of the .java file. Annotate operation will be used during
      // controller update.
      annotations.add(new AnnotationMetadataBuilder(responseType.getAnnotation()));

      final LogicalPath detailControllerPathItem =
          getPathResolver().getPath(detailController.getModule(), Path.SRC_MAIN_JAVA);
      final String resourceIdentifierItem =
          getTypeLocationService().getPhysicalTypeCanonicalPath(detailController,
              detailControllerPathItem);
      final String declaredByMetadataId =
          PhysicalTypeIdentifier.createIdentifier(detailController,
              getPathResolver().getPath(resourceIdentifierItem));
      ClassOrInterfaceTypeDetailsBuilder cidBuilder =
          new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC,
              detailController, PhysicalTypeCategory.CLASS);

      cidBuilder.setAnnotations(annotations);

      getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());

      if (getProjectOperations().isMultimoduleProject()) {
        // Getting related service
        JavaType relatedEntityService = null;
        Set<ClassOrInterfaceTypeDetails> services =
            getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
                RooJavaType.ROO_SERVICE);
        Iterator<ClassOrInterfaceTypeDetails> itServices = services.iterator();

        while (itServices.hasNext()) {
          ClassOrInterfaceTypeDetails existingService = itServices.next();
          AnnotationAttributeValue<Object> entityAttr =
              existingService.getAnnotation(RooJavaType.ROO_SERVICE).getAttribute("entity");
          JavaType entityJavaType = (JavaType) entityAttr.getValue();
          String entityField = relationFieldObject.get(field);
          if (entityJavaType.getSimpleTypeName().equals(entityField)) {
            relatedEntityService = existingService.getType();
            break;
          }
        }

        getProjectOperations().addModuleDependency(detailController.getModule(),
            relatedEntityService.getModule());
      }
    }

  }

  /**
   * Find recursively if relation field is valid. Check that the fields are
   * Set or List and check that the parents controllers exists
   *
   * @param entityDetails
   *            Entity to search the current field parameter
   * @param relationField
   *            Array with the related parameter splited
   * @param relationFieldObject
   *            Map to save the name of the entity of the related field
   * @param level
   *            Current level to search
   * @param responseType
   *            Response type of the detail controller
   * @param controllerPackage
   *            Package where create the detail controller
   * @param pathPrefix
   *            Path prefix of the detail controller
   * @param masterEntity
   *            Entity of the detail controller
   *
   * @throws Error
   *             if there aren't parent detail controllers
   * @return If finally the relation field is valid
   */
  private boolean checkRelationField(ClassOrInterfaceTypeDetails entityDetails,
      String[] relationField, Map<String, String> relationFieldObject, int level,
      ControllerMVCResponseService responseType, JavaPackage controllerPackage, String pathPrefix,
      JavaType masterEntity) {
    boolean relationFieldIsValid = false;
    MemberDetails memberDetails =
        getMemberDetailsScanner().getMemberDetails(entityDetails.getType().getSimpleTypeName(),
            entityDetails);
    List<FieldMetadata> fields = memberDetails.getFields();
    for (FieldMetadata entityField : fields) {
      if (entityField.getFieldName().getSymbolName().equals(relationField[level])) {

        AnnotationMetadata oneToManyAnnotation = entityField.getAnnotation(JpaJavaType.ONE_TO_MANY);

        if (oneToManyAnnotation != null
            && (entityField.getFieldType().getFullyQualifiedTypeName()
                .equals(JavaType.LIST.getFullyQualifiedTypeName()) || entityField.getFieldType()
                .getFullyQualifiedTypeName().equals(JavaType.SET.getFullyQualifiedTypeName()))) {
          level++;
          if (relationField.length > level) {

            // check if exists a detail controller (avoid check the
            // master)
            String currentRelationField = relationField[0];
            for (int i = 1; i < level; i++) {
              currentRelationField = currentRelationField.concat(".").concat(relationField[i]);
            }
            boolean detailControllerExists =
                checkDetailControllerExists(masterEntity, responseType, controllerPackage,
                    pathPrefix, currentRelationField);

            Validate.isTrue(detailControllerExists, String.format(
                "ERROR: Doesn't exist detail controller with response type %s for field %s.",
                responseType.getName(), entityField.getFieldName()));

            ClassOrInterfaceTypeDetails entityFieldDetails =
                getTypeLocationService().getTypeDetails(
                    entityField.getFieldType().getParameters().get(0));
            relationFieldIsValid =
                checkRelationField(entityFieldDetails, relationField, relationFieldObject, level,
                    responseType, controllerPackage, pathPrefix, masterEntity);
          } else {
            relationFieldIsValid = true;
            relationFieldObject.put(StringUtils.join(relationField, "."), entityField
                .getFieldType().getParameters().get(0).getSimpleTypeName());
          }
          break;
        }
      }
    }
    return relationFieldIsValid;
  }

  /**
   * Check if detail controller exists for the values entity, responseType,
   * controllerPackage, pathPrefix and relationField provided by parameters
   *
   * @param entity
   *            Detail controller entity
   * @param responseType
   * @param controllerPackage
   * @param pathPrefix
   * @param relationField
   * @return
   */
  private boolean checkDetailControllerExists(JavaType entity,
      ControllerMVCResponseService responseType, JavaPackage controllerPackage, String pathPrefix,
      String relationField) {
    boolean detailControllerExists = false;
    Set<ClassOrInterfaceTypeDetails> detailControllers =
        getTypeLocationService()
            .findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_DETAIL);
    Iterator<ClassOrInterfaceTypeDetails> itDetailControllers = detailControllers.iterator();
    while (itDetailControllers.hasNext()) {
      ClassOrInterfaceTypeDetails existingController = itDetailControllers.next();

      if (existingController.getType().getPackage().equals(controllerPackage)) {
        AnnotationAttributeValue<Object> entityAttr =
            existingController.getAnnotation(RooJavaType.ROO_CONTROLLER).getAttribute("entity");
        AnnotationAttributeValue<Object> typeAttr =
            existingController.getAnnotation(RooJavaType.ROO_CONTROLLER).getAttribute("type");
        ControllerType existingControllerType =
            ControllerType.getControllerType(((EnumDetails) typeAttr.getValue()).getField()
                .getSymbolName());
        AnnotationAttributeValue<String> pathPrefixAttr =
            existingController.getAnnotation(RooJavaType.ROO_CONTROLLER).getAttribute("pathPrefix");
        AnnotationAttributeValue<String> relationFieldAttr =
            existingController.getAnnotation(RooJavaType.ROO_DETAIL).getAttribute("relationField");
        AnnotationMetadata responseTypeAnnotation =
            existingController.getAnnotation(responseType.getAnnotation());
        String pathPrefixController = "";
        if (pathPrefixAttr != null) {
          pathPrefixController = pathPrefixAttr.getValue();
        }
        if (entityAttr != null && entityAttr.getValue().equals(entity) && typeAttr != null
            && responseTypeAnnotation != null
            && existingControllerType.equals(ControllerType.DETAIL)
            && pathPrefix.equals(pathPrefixController) && relationFieldAttr != null
            && relationField.equals(relationFieldAttr.getValue())) {
          detailControllerExists = true;
          break;
        }
      }
    }
    return detailControllerExists;
  }

  /**
   * Get default package to set it to a controller or a detail controller.
   * Search classes with @SpringBootApplication annotation to establish the
   * module and package.
   *
   * @return project's default controller package
   */
  private JavaPackage getDefaultControllerPackage() {
    String module = "";
    String topLevelPackage = "";
    if (getProjectOperations().isMultimoduleProject()) {
      // scan all modules to get that modules that contains a class
      // annotated with @SpringBootApplication
      Set<ClassOrInterfaceTypeDetails> applicationClasses =
          getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
              SpringJavaType.SPRING_BOOT_APPLICATION);

      // Compare the focus module. If it is an 'application' module,
      // set it like controllerPackage
      boolean controllersPackageNotSet = true;
      String focusedModuleName = getProjectOperations().getFocusedModuleName();
      for (ClassOrInterfaceTypeDetails applicationClass : applicationClasses) {
        if (focusedModuleName.equals(applicationClass.getType().getModule())) {
          module = focusedModuleName;
          topLevelPackage = applicationClass.getType().getPackage().getFullyQualifiedPackageName();
          controllersPackageNotSet = false;
          break;
        }
      }

      if (controllersPackageNotSet) {
        // if exists more than one module, show error message
        if (applicationClasses.size() > 1) {
          LOGGER
              .log(
                  Level.INFO,
                  String
                      .format("ERROR: Exists more than one module 'application'. Specify --package parameter to set the indicated"));
          return null;
        } else {
          ClassOrInterfaceTypeDetails applicationClass = applicationClasses.iterator().next();
          module = applicationClass.getType().getModule();
          topLevelPackage = applicationClass.getType().getPackage().getFullyQualifiedPackageName();
        }
      }
    } else {
      topLevelPackage =
          getProjectOperations().getFocusedTopLevelPackage().getFullyQualifiedPackageName();
    }
    String packageStr = topLevelPackage;
    if (StringUtils.isNotEmpty(module)) {
      packageStr = packageStr.concat(".").concat(module);
    }
    return new JavaPackage(packageStr.concat(".web"), module);
  }

  /**
   * Get all the methods that can be published from the service or the controller established by parameter
   *
   * @param currentService Service from which obtain methods
   * @param currentController Controller from which obtain methods
   * @return methods names list
   */
  public List<String> getAllMethodsToPublish(String currentService, String currentController) {

    //Generating all possible values
    List<String> serviceMethodsToPublish = new ArrayList<String>();

    List<ClassOrInterfaceTypeDetails> servicesToPublish =
        new ArrayList<ClassOrInterfaceTypeDetails>();

    if (StringUtils.isEmpty(currentService)) {

      // Get controllers
      Set<ClassOrInterfaceTypeDetails> controllers =
          getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
              RooJavaType.ROO_CONTROLLER);

      for (ClassOrInterfaceTypeDetails controller : controllers) {
        String name = replaceTopLevelPackageString(controller, currentController);
        if (currentController.equals(name)) {

          // Get the entity associated
          AnnotationMetadata controllerAnnotation =
              controller.getAnnotation(RooJavaType.ROO_CONTROLLER);
          JavaType entity = (JavaType) controllerAnnotation.getAttribute("entity").getValue();

          // Search the service related with the entity
          Set<ClassOrInterfaceTypeDetails> services =
              getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
                  RooJavaType.ROO_SERVICE);
          Iterator<ClassOrInterfaceTypeDetails> itServices = services.iterator();
          while (itServices.hasNext()) {
            ClassOrInterfaceTypeDetails existingService = itServices.next();
            AnnotationAttributeValue<Object> entityAttr =
                existingService.getAnnotation(RooJavaType.ROO_SERVICE).getAttribute("entity");
            if (entityAttr != null && entityAttr.getValue().equals(entity)) {
              servicesToPublish.add(existingService);
            }
          }

          break;
        }
      }
    } else {

      // Get the services
      Set<ClassOrInterfaceTypeDetails> services =
          getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
              RooJavaType.ROO_SERVICE);

      for (ClassOrInterfaceTypeDetails service : services) {
        String name = replaceTopLevelPackageString(service, currentService);
        if (currentService.equals(name)) {
          servicesToPublish.add(service);
          break;
        }
      }
    }

    for (ClassOrInterfaceTypeDetails serviceToPublish : servicesToPublish) {

      // Getting service metadata
      ClassOrInterfaceTypeDetails serviceDetails =
          getTypeLocationService().getTypeDetails(serviceToPublish.getType());
      final LogicalPath serviceLogicalPath =
          PhysicalTypeIdentifier.getPath(serviceDetails.getDeclaredByMetadataId());
      final String serviceMetadataKey =
          ServiceMetadata.createIdentifier(serviceDetails.getType(), serviceLogicalPath);
      final ServiceMetadata serviceMetadata =
          (ServiceMetadata) getMetadataService().get(serviceMetadataKey);

      // Get all methods generated by Roo
      List<MethodMetadata> methodsImplementedByRoo = new ArrayList<MethodMetadata>();
      methodsImplementedByRoo.addAll(serviceMetadata.getNotTransactionalDefinedMethods());
      methodsImplementedByRoo.addAll(serviceMetadata.getTransactionalDefinedMethods());


      // Get all methods and compare them with the generated by Roo
      List<MethodMetadata> methods = serviceToPublish.getMethods();
      boolean notGeneratedByRoo;
      for (MethodMetadata method : methods) {
        notGeneratedByRoo = true;
        Iterator<MethodMetadata> iterMethodsImplRoo = methodsImplementedByRoo.iterator();
        while (iterMethodsImplRoo.hasNext() && notGeneratedByRoo) {
          MethodMetadata methodImplementedByRoo = iterMethodsImplRoo.next();

          // If name is equals check the parameters
          if (method.getMethodName().equals(methodImplementedByRoo.getMethodName())) {

            //Check parameters type are equals and in the same order
            if (method.getParameterTypes().size() == methodImplementedByRoo.getParameterTypes()
                .size()) {
              Iterator<AnnotatedJavaType> iterParameterTypesMethodRoo =
                  methodImplementedByRoo.getParameterTypes().iterator();
              boolean allParametersAreEquals = true;
              for (AnnotatedJavaType parameterType : method.getParameterTypes()) {
                AnnotatedJavaType parameterTypeMethodRoo = iterParameterTypesMethodRoo.next();
                if (!parameterType.getJavaType().equals(parameterTypeMethodRoo.getJavaType())) {
                  allParametersAreEquals = false;
                  break;
                }
              }
              if (allParametersAreEquals) {
                notGeneratedByRoo = false;
              }
            }
          }
        }

        // If is not generated by Roo add to list of the elements
        if (notGeneratedByRoo) {
          StringBuffer methodNameBuffer = new StringBuffer("");
          if (StringUtils.isEmpty(currentService)) {
            methodNameBuffer.append(replaceTopLevelPackage(serviceToPublish)).append(".");
          }

          methodNameBuffer.append(method.getMethodName().getSymbolName());
          List<AnnotatedJavaType> parameterTypes = method.getParameterTypes();

          methodNameBuffer = methodNameBuffer.append("(");

          for (int i = 0; i < parameterTypes.size(); i++) {
            String paramType = parameterTypes.get(i).getJavaType().getSimpleTypeName();
            methodNameBuffer = methodNameBuffer.append(paramType).append(",");
          }

          String methodName;
          if (!parameterTypes.isEmpty()) {
            methodName =
                methodNameBuffer.toString().substring(0, methodNameBuffer.toString().length() - 1)
                    .concat(")");
          } else {
            methodName = methodNameBuffer.append(")").toString();
          }

          serviceMethodsToPublish.add(methodName);
        }
      }
    }
    return serviceMethodsToPublish;
  }


  /**
   * Replaces a JavaType fullyQualifiedName for a shorter name using '~' for
   * TopLevelPackage
   *
   * @param cid
   *            ClassOrInterfaceTypeDetails of a JavaType
   * @param currentText
   *            String current text for option value
   * @return the String representing a JavaType with its name shortened
   */
  public String replaceTopLevelPackageString(ClassOrInterfaceTypeDetails cid, String currentText) {
    String javaTypeFullyQualilfiedName = cid.getType().getFullyQualifiedTypeName();
    String javaTypeString = "";
    String topLevelPackageString = "";

    // Add module value to topLevelPackage when necessary
    if (StringUtils.isNotBlank(cid.getType().getModule())
        && !cid.getType().getModule().equals(getProjectOperations().getFocusedModuleName())) {

      // Target module is not focused
      javaTypeString = cid.getType().getModule().concat(LogicalPath.MODULE_PATH_SEPARATOR);
      topLevelPackageString =
          getProjectOperations().getTopLevelPackage(cid.getType().getModule())
              .getFullyQualifiedPackageName();
    } else if (StringUtils.isNotBlank(cid.getType().getModule())
        && cid.getType().getModule().equals(getProjectOperations().getFocusedModuleName())
        && (currentText.startsWith(cid.getType().getModule()) || cid.getType().getModule()
            .startsWith(currentText)) && StringUtils.isNotBlank(currentText)) {

      // Target module is focused but user wrote it
      javaTypeString = cid.getType().getModule().concat(LogicalPath.MODULE_PATH_SEPARATOR);
      topLevelPackageString =
          getProjectOperations().getTopLevelPackage(cid.getType().getModule())
              .getFullyQualifiedPackageName();
    } else {

      // Not multimodule project
      topLevelPackageString =
          getProjectOperations().getFocusedTopLevelPackage().getFullyQualifiedPackageName();
    }

    // Autocomplete with abbreviate or full qualified mode
    String auxString =
        javaTypeString.concat(StringUtils.replace(javaTypeFullyQualilfiedName,
            topLevelPackageString, "~"));
    if ((StringUtils.isBlank(currentText) || auxString.startsWith(currentText))
        && StringUtils.contains(javaTypeFullyQualilfiedName, topLevelPackageString)) {

      // Value is for autocomplete only or user wrote abbreviate value
      javaTypeString = auxString;
    } else {

      // Value could be for autocomplete or for validation
      javaTypeString = String.format("%s%s", javaTypeString, javaTypeFullyQualilfiedName);
    }

    return javaTypeString;
  }


  /**
   * Replaces a JavaType fullyQualifiedName for a shorter name using '~' for
   * TopLevelPackage
   *
   * @param cid
   *            ClassOrInterfaceTypeDetails of a JavaType
   *
   * @return the String representing a JavaType with its name shortened
   */
  public String replaceTopLevelPackage(ClassOrInterfaceTypeDetails cid) {
    String javaTypeFullyQualilfiedName = cid.getType().getFullyQualifiedTypeName();
    String javaTypeString = "";
    String topLevelPackageString = "";

    // Add module value to topLevelPackage when necessary
    if (StringUtils.isNotBlank(cid.getType().getModule())) {

      // Target module is not focused
      javaTypeString = cid.getType().getModule().concat(LogicalPath.MODULE_PATH_SEPARATOR);
      topLevelPackageString =
          getProjectOperations().getTopLevelPackage(cid.getType().getModule())
              .getFullyQualifiedPackageName();
    } else {

      // Not multimodule project
      topLevelPackageString =
          getProjectOperations().getFocusedTopLevelPackage().getFullyQualifiedPackageName();
    }

    return javaTypeString.concat(StringUtils.replace(javaTypeFullyQualilfiedName,
        topLevelPackageString, "~"));
  }


  /**
   * Generate the operations selected in the controller indicated
   *
   * @param controller Controller where the operations will be created
   * @param operations Service operations names that will be created
   */
  public void exportOperation(JavaType controller, List<String> operations) {
    ClassOrInterfaceTypeDetails controllerDetails =
        getTypeLocationService().getTypeDetails(controller);

    // Check if provided controller exists on current project
    Validate.notNull(controllerDetails, "ERROR: You must provide an existing controller");

    // Check if provided controller has been annotated with @RooController
    Validate.notNull(controllerDetails.getAnnotation(RooJavaType.ROO_CONTROLLER),
        "ERROR: You must provide a controller annotated with @RooController");

    // Check parameter operations
    Validate.notEmpty(operations, "INFO: Don't exist operations to publish");

    ClassOrInterfaceTypeDetailsBuilder controllerBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(controllerDetails);

    AnnotationMetadata operationsAnnotation =
        controllerDetails.getAnnotation(RooJavaType.ROO_OPERATIONS);

    // Create an array with new attributes array
    List<StringAttributeValue> operationsToAdd = new ArrayList<StringAttributeValue>();

    if (operationsAnnotation == null) {

      // Add Operations annotation
      AnnotationMetadataBuilder opAnnotation =
          new AnnotationMetadataBuilder(RooJavaType.ROO_OPERATIONS);
      controllerBuilder.addAnnotation(opAnnotation);

      // set operations from command
      for (String operation : operations) {
        operationsToAdd.add(new StringAttributeValue(new JavaSymbolName("value"), operation));
      }

      opAnnotation.addAttribute(new ArrayAttributeValue<StringAttributeValue>(new JavaSymbolName(
          "operations"), operationsToAdd));

      // Write changes on provided controller
      getTypeManagementService().createOrUpdateTypeOnDisk(controllerBuilder.build());

    } else {
      List<String> operationsNames = new ArrayList<String>();
      boolean operationsAdded = false;
      AnnotationAttributeValue<Object> attributeOperations =
          operationsAnnotation.getAttribute("operations");
      if (attributeOperations != null) {
        List<StringAttributeValue> existingOperations =
            (List<StringAttributeValue>) attributeOperations.getValue();
        Iterator<StringAttributeValue> it = existingOperations.iterator();

        // Add existent operations to new attributes array to merge with new ones
        while (it.hasNext()) {
          StringAttributeValue attributeValue = (StringAttributeValue) it.next();
          operationsToAdd.add(attributeValue);
          operationsNames.add(attributeValue.getValue());
        }

        // Add new finders to new attributes array
        for (String operation : operations) {
          if (!operationsNames.contains(operation)) {
            operationsToAdd.add(new StringAttributeValue(new JavaSymbolName("value"), operation));
            operationsAdded = true;
          }
        }

        if (operationsAdded) {
          AnnotationMetadataBuilder opAnnotation =
              new AnnotationMetadataBuilder(operationsAnnotation);
          opAnnotation.addAttribute(new ArrayAttributeValue<StringAttributeValue>(
              new JavaSymbolName("operations"), operationsToAdd));

          controllerBuilder.updateTypeAnnotation(opAnnotation);

          // Write changes on provided controller
          getTypeManagementService().createOrUpdateTypeOnDisk(controllerBuilder.build());
        }
      }
    }
  }

  //Methods to obtain OSGi Services

  public TypeLocationService getTypeLocationService() {
    return serviceInstaceManager.getServiceInstance(this, TypeLocationService.class);
  }

  public ProjectOperations getProjectOperations() {
    return serviceInstaceManager.getServiceInstance(this, ProjectOperations.class);
  }

  public PathResolver getPathResolver() {
    return serviceInstaceManager.getServiceInstance(this, PathResolver.class);
  }

  public FileManager getFileManager() {
    return serviceInstaceManager.getServiceInstance(this, FileManager.class);
  }

  public TypeManagementService getTypeManagementService() {
    return serviceInstaceManager.getServiceInstance(this, TypeManagementService.class);
  }

  public ApplicationConfigService getApplicationConfigService() {
    return serviceInstaceManager.getServiceInstance(this, ApplicationConfigService.class);
  }

  public MemberDetailsScanner getMemberDetailsScanner() {
    return serviceInstaceManager.getServiceInstance(this, MemberDetailsScanner.class);
  }

  private MetadataService getMetadataService() {
    return serviceInstaceManager.getServiceInstance(this, MetadataService.class);
  }

  public Converter<Pom> getPomConverter() {
    return serviceInstaceManager.getServiceInstance(this, PomConverter.class);
  }

  public PluralService getPluralService() {
    return serviceInstaceManager.getServiceInstance(this, PluralService.class);
  }

}
