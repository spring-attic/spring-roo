package org.springframework.roo.addon.web.mvc.controller.addon;

import static java.lang.reflect.Modifier.PUBLIC;
import static org.springframework.roo.model.RooJavaType.ROO_WEB_MVC_CONFIGURATION;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.jvnet.inflector.Noun;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.web.mvc.controller.addon.servers.ServerProvider;
import org.springframework.roo.application.config.ApplicationConfigService;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.ImportMetadata;
import org.springframework.roo.classpath.details.ImportMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
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

/**
 * Implementation of {@link ControllerOperations}.
 * 
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @author Paula Navarro
 * @since 1.0
 */
@Component
@Service
public class ControllerOperationsImpl implements ControllerOperations {

  private static final Logger LOGGER = HandlerUtils.getLogger(ControllerOperationsImpl.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private static final JavaSymbolName PATH = new JavaSymbolName("path");

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
          new AnnotationMetadataBuilder(ROO_WEB_MVC_CONFIGURATION);
      typeBuilder.addAnnotation(annotationMetadata.build());

      // Write new class disk
      getTypeManagementService().createOrUpdateTypeOnDisk(typeBuilder.build());

    }

    // Adding spring.jackson.serialization.indent_output property
    getApplicationConfigService().addProperty(module.getModuleName(),
        "spring.jackson.serialization.indent_output", "true", "", true);

    // Add server configuration
    appServer.setup(module);
  }

  /**
   * This operation will check if add controllers operation is 
   * available
   * 
   * @return true if add controller operation is available. 
   * false if not.
   */
  @Override
  public boolean isAddControllerAvailable() {
    return getProjectOperations().isFeatureInstalled(FeatureNames.MVC);
  }

  /**
   * This operation will generate a new controller for every class annotated
   * with @RooJpaEntity on current project.
   * 
   * @param controllersPackage
   * @param responseType
   * @param formattersPackage
   * @param module
   */
  @Override
  public void createControllerForAllEntities(JavaPackage controllersPackage, String responseType,
      JavaPackage formattersPackage, Pom module) {

    // Getting all entities annotated with @RooJpaEntity
    Set<ClassOrInterfaceTypeDetails> allEntities =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_JPA_ENTITY);

    for (ClassOrInterfaceTypeDetails entity : allEntities) {
      // Check if exists yet some controller that manages this entity 
      // and if current entity is not abstract
      if (!existsControllerForEntity(entity) && !entity.isAbstract()) {
        // Generate controller JavaType
        JavaType controller =
            new JavaType(String.format("%s.%sController", controllersPackage
                .getFullyQualifiedPackageName(), entity.getType().getSimpleTypeName()),
                module.getModuleName());

        // Generate path
        String path = getNotRepeatedControllerPathFromEntity(entity.getType());

        // Getting related service
        JavaType service = getServiceRelatedWithEntity(entity.getType());

        // Delegate on individual create controller method
        createController(controller, entity.getType(), service, path, responseType,
            formattersPackage);
      }

    }

  }

  /**
   * This operation will generate a new controller with the specified information 
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
      String responseType, JavaPackage formattersPackage) {
    Validate.notNull(controller,
        "ERROR: Controller class is required to be able to generate new controller");
    Validate.notNull(entity,
        "ERROR: Entity class is required to be able to generate new controller");
    Validate.notNull(service,
        "ERROR: Service class is required to be able to generate new controller.");

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

    ClassOrInterfaceTypeDetailsBuilder cidBuilder = null;
    final LogicalPath controllerPath = getPathResolver().getFocusedPath(Path.SRC_MAIN_JAVA);
    final String resourceIdentifier =
        getTypeLocationService().getPhysicalTypeCanonicalPath(controller, controllerPath);
    final String declaredByMetadataId =
        PhysicalTypeIdentifier.createIdentifier(controller,
            getPathResolver().getPath(resourceIdentifier));

    // Create annotation @RooController(path = "/test", entity = MyEntity.class, service = MyService.class)
    List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
    annotations.add(getRooControllerAnnotation(entity, service, path, PATH));
    cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, controller,
            PhysicalTypeCategory.CLASS);
    cidBuilder.setAnnotations(annotations);

    // Adding constructor
    cidBuilder.addConstructor(getControllerConstructor(declaredByMetadataId, service));

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
    }
  }

  /**
   * This method checks if exists some formatter for
   * provided entity
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
   * This method generates new formatter inside generated project. 
   * 
   * This formatter will be annotated with @RooFormatted and will be related
   * with some existing entity.
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
    final LogicalPath fomatterPath = getPathResolver().getFocusedPath(Path.SRC_MAIN_JAVA);
    final String resourceIdentifier =
        getTypeLocationService().getPhysicalTypeCanonicalPath(formatter, fomatterPath);
    final String declaredByMetadataId =
        PhysicalTypeIdentifier.createIdentifier(formatter,
            getPathResolver().getPath(resourceIdentifier));

    // Create annotation @RooFormatter(entity = MyEntity.class, service = MyService.class)
    List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
    annotations.add(getRooFormatterAnnotation(entity, service));
    cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, formatter,
            PhysicalTypeCategory.CLASS);
    cidBuilder.setAnnotations(annotations);

    // Adding constructor
    cidBuilder.addConstructor(getFormatterConstructor(declaredByMetadataId, service));

    // Adding necessary imports
    List<ImportMetadata> imports = new ArrayList<ImportMetadata>();

    ImportMetadataBuilder conversionServiceImport = new ImportMetadataBuilder(declaredByMetadataId);
    conversionServiceImport.setImportType(SpringJavaType.CONVERSION_SERVICE);
    imports.add(conversionServiceImport.build());

    cidBuilder.addImports(imports);

    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());

  }

  /**
   * This method returns the formatter constructor
   * @return
   */
  private ConstructorMetadata getFormatterConstructor(String declaredByMetadataId, JavaType service) {

    FieldMetadata serviceField = getServiceField(declaredByMetadataId, service);
    FieldMetadata conversionServiceField = getConversionServiceField(declaredByMetadataId);

    ConstructorMetadataBuilder constructor = new ConstructorMetadataBuilder(declaredByMetadataId);
    constructor.addParameter(serviceField.getFieldName().getSymbolName(),
        serviceField.getFieldType());
    constructor.addParameter(conversionServiceField.getFieldName().getSymbolName(),
        conversionServiceField.getFieldType());

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // this.serviceField = serviceField;
    bodyBuilder.appendFormalLine(String.format("this.%s = %s;", serviceField.getFieldName(),
        serviceField.getFieldName()));

    // this.conversionServiceField = conversionServiceField;
    bodyBuilder.appendFormalLine(String.format("this.%s = %s;",
        conversionServiceField.getFieldName(), conversionServiceField.getFieldName()));

    constructor.setBodyBuilder(bodyBuilder);

    return constructor.build();
  }

  /**
   * This method returns conversionSevice field included on controller
   * 
   * @param declaredByMetadataId
   * 
   * @return
   */
  public FieldMetadata getConversionServiceField(String declaredByMetadataId) {

    return new FieldMetadataBuilder(declaredByMetadataId, Modifier.PUBLIC,
        new ArrayList<AnnotationMetadataBuilder>(), new JavaSymbolName("conversionService"),
        SpringJavaType.CONVERSION_SERVICE).build();
  }

  /**
   * This method returns service field included on controller
   * 
   * @param declaredByMetadataId
   * @param service
   * 
   * @return
   */
  public FieldMetadata getServiceField(String declaredByMetadataId, JavaType service) {

    // Generating service field name
    String fieldName =
        new JavaSymbolName(service.getSimpleTypeName()).getSymbolNameUnCapitalisedFirstLetter();

    return new FieldMetadataBuilder(declaredByMetadataId, Modifier.PUBLIC,
        new ArrayList<AnnotationMetadataBuilder>(), new JavaSymbolName(fieldName), service).build();
  }

  /**
   * This method returns the controller constructor 
   * 
   * @param declaredByMetadataId
   * @param service
   * @return
   */
  public ConstructorMetadataBuilder getControllerConstructor(String declaredByMetadataId,
      JavaType service) {

    // Generating service field name
    String serviceFieldName =
        service.getSimpleTypeName().substring(0, 1).toLowerCase()
            .concat(service.getSimpleTypeName().substring(1));


    // Generating constructor
    ConstructorMetadataBuilder constructor = new ConstructorMetadataBuilder(declaredByMetadataId);
    constructor.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.AUTOWIRED));
    constructor.addParameter(serviceFieldName, service);

    // Adding body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    bodyBuilder
        .appendFormalLine(String.format("this.%s = %s;", serviceFieldName, serviceFieldName));
    constructor.setBodyBuilder(bodyBuilder);

    return constructor;
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
   * This method generates a valir controller path that doesn't repeat on any controller of
   * current project. This prevent request mapping errors. 
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
  private boolean existsControllerForEntity(ClassOrInterfaceTypeDetails entity) {
    Set<ClassOrInterfaceTypeDetails> currentControllers =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_CONTROLLER);

    for (ClassOrInterfaceTypeDetails controller : currentControllers) {
      JavaType controllerEntity =
          (JavaType) controller.getAnnotation(RooJavaType.ROO_CONTROLLER).getAttribute("entity")
              .getValue();

      if (entity.getType().equals(controllerEntity)) {
        return true;
      }
    }

    return false;
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
   * This method obtains Pom converter to be able to obtain Pom module 
   * from strings
   * 
   * @return
   */
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


  @Override
  public String getName() {
    return FEATURE_NAME;
  }

  @Override
  public boolean isInstalledInModule(String moduleName) {
    Pom module = getProjectOperations().getPomFromModuleName(moduleName);
    for (JavaType javaType : getTypeLocationService().findTypesWithAnnotation(
        ROO_WEB_MVC_CONFIGURATION)) {
      if (javaType.getModule().equals(moduleName)
          && module.hasDependencyExcludingVersion(new Dependency("org.springframework.boot",
              "spring-boot-starter-web", null))) {
        return true;
      }
    }
    return false;
  }
}
