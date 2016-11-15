package org.springframework.roo.addon.web.mvc.controller.addon;

import static org.springframework.roo.shell.OptionContexts.APPLICATION_FEATURE;
import static org.springframework.roo.shell.OptionContexts.APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.web.mvc.controller.addon.responses.ControllerMVCResponseService;
import org.springframework.roo.addon.web.mvc.controller.addon.servers.ServerProvider;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.operations.ClasspathOperations;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CliOptionAutocompleteIndicator;
import org.springframework.roo.shell.CliOptionMandatoryIndicator;
import org.springframework.roo.shell.CliOptionVisibilityIndicator;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.ShellContext;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

/**
 * This class provides necessary commands to be able to include Spring MVC on
 * generated project and generate new controllers.
 *
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @author Paula Navarro
 * @since 1.0
 */
@Component
@Service
public class ControllerCommands implements CommandMarker {

  private static Logger LOGGER = HandlerUtils.getLogger(ControllerCommands.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private Map<String, ServerProvider> serverProviders = new HashMap<String, ServerProvider>();
  private Map<String, ControllerMVCResponseService> responseTypes =
      new HashMap<String, ControllerMVCResponseService>();

  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
    serviceInstaceManager.activate(this.context);
  }

  /**
   * This indicator checks if --module parameter should be visible or not.
   *
   * If exists more than one module that match with the properties of
   * ModuleFeature APPLICATION, --module parameter should be mandatory.
   *
   * @param shellContext
   * @return
   */
  @CliOptionVisibilityIndicator(command = "web mvc setup", params = {"module"},
      help = "Module parameter is not available if there is only one application module")
  public boolean isModuleVisible(ShellContext shellContext) {
    if (getTypeLocationService().getModuleNames(ModuleFeatureName.APPLICATION).size() > 1) {
      return true;
    }
    return false;
  }

  /**
   * This indicator checks if --module parameter should be mandatory or not.
   *
   * If focused module doesn't match with the properties of ModuleFeature
   * APPLICATION, --module parameter should be mandatory.
   *
   * @param shellContext
   * @return
   */
  @CliOptionMandatoryIndicator(command = "web mvc setup", params = {"module"})
  public boolean isModuleRequired(ShellContext shellContext) {
    Pom module = getProjectOperations().getFocusedModule();
    if (!isModuleVisible(shellContext)
        || getTypeLocationService().hasModuleFeature(module, ModuleFeatureName.APPLICATION)) {
      return false;
    }
    return true;
  }

  /**
   * This indicator returns the servers where the application can be deployed
   *
   * @param context
   * @return
   */
  @CliOptionAutocompleteIndicator(command = "web mvc setup", param = "appServer",
      help = "Only valid application servers are available")
  public List<String> getAllAppServers(ShellContext context) {
    return new ArrayList<String>(getServerProviders().keySet());
  }

  /**
   * This indicator checks if Spring MVC setup is available
   *
   * If a valid project has been generated and Spring MVC has not been
   * installed yet, this command will be available.
   *
   * @return
   */
  @CliAvailabilityIndicator(value = "web mvc setup")
  public boolean isSetupAvailable() {
    return getControllerOperations().isSetupAvailable();
  }

  /**
   * This method provides the Command definition to be able to include Spring
   * MVC on generated project.
   *
   * @param module
   * @param appServer
   */
  @CliCommand(value = "web mvc setup", help = "Includes Spring MVC on generated project")
  public void setup(
      @CliOption(
          key = "module",
          mandatory = true,
          help = "The application module where to install the persistence. This option is available if there is more than "
              + "one application module (mandatory if the focus is not set in application module)",
          unspecifiedDefaultValue = ".", optionContext = APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE) Pom module,
      @CliOption(key = "appServer", mandatory = false,
          help = "The server where deploy the application", unspecifiedDefaultValue = "EMBEDDED") String appServer) {

    if (!getServerProviders().containsKey(appServer)) {
      throw new IllegalArgumentException("ERROR: Invalid server provider");
    }

    getControllerOperations().setup(module, serverProviders.get(appServer));
  }

  /**
   * This indicator says if --all parameter should be visible or not
   *
   * If --entity parameter has been specified, --all parameter will not be
   * visible to prevent conflicts.
   *
   * @return
   */
  @CliOptionVisibilityIndicator(params = "all", command = "web mvc controller",
      help = "--all parameter is not be visible if --entity parameter has been specified before.")
  public boolean isAllParameterVisible(ShellContext context) {
    if (context.getParameters().containsKey("entity")) {
      return false;
    }
    return true;
  }

  /**
   * This indicator says if --entity parameter should be visible or not
   *
   * If --all parameter has been specified, --entity parameter will not be
   * visible to prevent conflicts.
   *
   * @return
   */
  @CliOptionVisibilityIndicator(params = "entity", command = "web mvc controller",
      help = "--entity parameter is not be visible if --all parameter has been specified before.")
  public boolean isEntityParameterVisible(ShellContext context) {
    if (context.getParameters().containsKey("all")) {
      return false;
    }
    return true;
  }

  /**
   * This indicator says if --package, --pathPrefix and --responseType
   * parameters should be visible or not
   *
   * If --all or --entity parameters have not been specified, --package,
   * --pathPrefix and --responseType parameters will not be visible.
   *
   * @return
   */
  @CliOptionVisibilityIndicator(
      params = {"package", "pathPrefix", "responseType"},
      command = "web mvc controller",
      help = "--package, --pathPrefix and --responseType parameters are not be visible if --all parameter or --entity parameter has been specified before.")
  public boolean areParametersVisibles(ShellContext context) {
    if (context.getParameters().containsKey("all") || context.getParameters().containsKey("entity")) {
      return true;
    }
    return false;
  }

  /**
   * Find entities in project and returns a list with their fully qualified
   * names.
   *
   * @param shellContext
   * @return List<String> with available entity full qualified names.
   */
  @CliOptionAutocompleteIndicator(
      command = "web mvc controller",
      param = "entity",
      help = "--entity parameter must be an existing class annotated with @RooEntity. Please, assign a valid one.")
  public List<String> getAllEntities(ShellContext shellContext) {

    // Get current value of class
    String currentText = shellContext.getParameters().get("entity");

    // Create results to return
    List<String> results = new ArrayList<String>();

    // Get entity full qualified names
    Set<ClassOrInterfaceTypeDetails> entities =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_JPA_ENTITY);
    for (ClassOrInterfaceTypeDetails entity : entities) {
      if (!entity.isAbstract()) {
        String name = getClasspathOperations().replaceTopLevelPackageString(entity, currentText);
        if (!results.contains(name)) {
          results.add(name);
        }
      }
    }

    return results;
  }

  /**
   * This indicator returns all possible values for --responseType parameter.
   *
   * Depends of the specified --controller, responseTypes will be filtered to
   * provide only that responseTypes that doesn't exists on current
   * controller. Also, only installed response types will be provided.
   *
   * @param context
   * @return
   */
  @CliOptionAutocompleteIndicator(param = "responseType", command = "web mvc controller",
      help = "--responseType parameter should be completed with the provided response types.")
  public List<String> getAllResponseTypeValues(ShellContext context) {

    // Getting all installed services that implements
    // ControllerMVCResponseService
    Map<String, ControllerMVCResponseService> installedResponseTypes =
        getInstalledControllerMVCResponseTypes();

    // Generating all possible values
    List<String> responseTypes = new ArrayList<String>();

    for (Entry<String, ControllerMVCResponseService> responseType : installedResponseTypes
        .entrySet()) {
      // If specified controller doesn't have this response type
      // installed. Add to responseTypes
      // possible values
      responseTypes.add(responseType.getKey());
    }

    return responseTypes;
  }

  /**
   * This indicator checks if is possible to add new controllers.
   *
   * If a valid project has been generated and Spring MVC has been installed,
   * this command will be available.
   *
   * @return
   */
  @CliAvailabilityIndicator(value = "web mvc controller")
  public boolean isAddControllerAvailable() {
    return getControllerOperations().isAddControllerAvailable();
  }

  /**
   * This method provides the Command definition to be able to generate new
   * Controllers on current project.
   *
   * @param all
   * @param entity
   * @param responseType
   * @param package
   * @param pathPrefix
   */
  @CliCommand(
      value = "web mvc controller",
      help = "Generates new @RooController's inside current project. The controllers should manage specific entities in the project.")
  public void addController(
      @CliOption(
          key = "all",
          mandatory = false,
          specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false",
          help = "Indicates if developer wants to generate controllers for every entity of current project. This param will be visible if 'entity' parameter has not been specified") boolean all,
      @CliOption(
          key = "entity",
          mandatory = false,
          help = "Indicates the entity that new controller will be manage. This param will be visible if 'all' parameter has not been specified") JavaType entity,
      @CliOption(
          key = "responseType",
          mandatory = false,
          unspecifiedDefaultValue = "JSON",
          specifiedDefaultValue = "JSON",
          help = "Indicates the responseType to be used by generated controller. Depending of the selected responseType, generated methods and views will vary. This param will be visible if 'all' or 'entity' parameters have been specified") String responseType,
      @CliOption(
          key = "package",
          mandatory = false,
          optionContext = APPLICATION_FEATURE,
          help = "Indicates which package should be used to include generated controllers. This param will be visible if 'all' or 'entity' parameters have been specified") JavaPackage controllersPackage,
      @CliOption(
          key = "pathPrefix",
          mandatory = false,
          specifiedDefaultValue = "",
          unspecifiedDefaultValue = "",
          help = "Indicates @ResquestMapping prefix to be used on this controller. Is not necessary to specify '/'. Spring Roo shell will include it automatically. This param will be visible if 'all' or 'entity' parameters have been specified") String pathPrefix) {

    // Getting --responseType service
    Map<String, ControllerMVCResponseService> responseTypeServices =
        getInstalledControllerMVCResponseTypes();

    // Validate that provided responseType is a valid provided
    if (!responseTypeServices.containsKey(responseType)) {
      LOGGER
          .log(
              Level.SEVERE,
              "ERROR: Provided responseType is not valid. Use autocomplete feature to obtain valid responseTypes.");
      return;
    }

    pathPrefix = StringUtils.lowerCase(pathPrefix);

    // Check --all parameter
    if (all) {
      getControllerOperations().createOrUpdateControllerForAllEntities(
          responseTypeServices.get(responseType), controllersPackage, pathPrefix);
    } else {
      getControllerOperations().createOrUpdateControllerForEntity(entity,
          responseTypeServices.get(responseType), controllersPackage, pathPrefix);
    }
  }



  /**
   * This method gets all implementations of ServerProvider interface to be
   * able to locate all availbale appServers
   *
   * @return Map with appServer identifier and the ServerProvider
   *         implementation
   */
  public Map<String, ServerProvider> getServerProviders() {
    if (serverProviders.isEmpty()) {
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ServerProvider.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          ServerProvider serverProvider = (ServerProvider) this.context.getService(ref);
          serverProviders.put(serverProvider.getName(), serverProvider);
        }
        return serverProviders;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ServerProviders on ControllerCommands.");
        return null;
      }
    } else {
      return serverProviders;
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
    try {
      ServiceReference<?>[] references =
          this.context.getAllServiceReferences(ControllerMVCResponseService.class.getName(), null);

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
      LOGGER.warning("Cannot load ControllerMVCResponseService on ControllerCommands.");
      return null;
    }
  }

  // Detail commands

  /**
   * This indicator checks if is possible to add new detail controllers.
   *
   * If a valid project has been generated and Spring MVC has been installed,
   * this command will be available.
   *
   * @return
   */
  @CliAvailabilityIndicator(value = "web mvc detail")
  public boolean isAddDetailControllerAvailable() {
    return getControllerOperations().isAddDetailControllerAvailable();
  }

  @CliOptionVisibilityIndicator(params = "all", command = "web mvc detail",
      help = "--all parameter is not be visible if --entity parameter has been specified before.")
  public boolean isAllParameterOfDetailCommandVisible(ShellContext context) {
    if (context.getParameters().containsKey("entity")) {
      return false;
    }
    return true;
  }

  @CliOptionVisibilityIndicator(params = "entity", command = "web mvc detail",
      help = "--entity parameter is not be visible if --all parameter has been specified before.")
  public boolean isEntityParameterOfDetailCommandVisible(ShellContext context) {
    if (context.getParameters().containsKey("all")) {
      return false;
    }
    return true;
  }

  @CliOptionAutocompleteIndicator(
      command = "web mvc detail",
      param = "entity",
      help = "--entity parameter must be an existing class annotated with @RooJpaEntity. Please, assign a valid one.")
  public List<String> getAllEntitiesForDetailCommands(ShellContext shellContext) {

    // Get current value of class
    String currentText = shellContext.getParameters().get("entity");

    // Create results to return
    List<String> results = new ArrayList<String>();

    // Get entity full qualified names
    Set<ClassOrInterfaceTypeDetails> entities =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_JPA_ENTITY);

    for (ClassOrInterfaceTypeDetails entity : entities) {
      String name = getClasspathOperations().replaceTopLevelPackageString(entity, currentText);
      if (!results.contains(name)) {
        results.add(name);
      }
    }

    return results;
  }

  @CliOptionVisibilityIndicator(params = {"field"}, command = "web mvc detail",
      help = "--field parameter is visible if --entity parameter has been specified before.")
  public boolean isFieldParameterVisible(ShellContext context) {
    if (context.getParameters().containsKey("entity")) {
      return true;
    }
    return false;
  }

  @CliOptionAutocompleteIndicator(
      command = "web mvc detail",
      param = "field",
      help = "--field parameter must be an existing @OneToMany field of the specified entity in parameter --entity.",
      includeSpaceOnFinish = false)
  public List<String> getDetailFieldsRelatedToEntity(ShellContext shellContext) {

    // Get current value of class
    String currentEntity = shellContext.getParameters().get("entity");

    // Get current fields in --field value
    String currentFieldValue = shellContext.getParameters().get("field");

    // Check the field value (ex: 'entity.detailentity.')
    String[] splittedCurrentField = null;
    boolean includeChildren = false;
    boolean removedData = false;
    if (currentFieldValue.contains(".")) {
      if (!currentFieldValue.endsWith(".")) {
        currentFieldValue = currentFieldValue.substring(0, currentFieldValue.lastIndexOf("."));
        removedData = true;
      }
      includeChildren = true;
      splittedCurrentField = currentFieldValue.split("[.]");
    } else {
      currentFieldValue = "";
    }

    // Create results to return
    List<String> results = new ArrayList<String>();

    // Get entity full qualified names
    Set<ClassOrInterfaceTypeDetails> entities =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_JPA_ENTITY);

    for (ClassOrInterfaceTypeDetails entity : entities) {
      if (getClasspathOperations().replaceTopLevelPackageString(entity,
          entity.getType().getModule()).equals(currentEntity)) {
        if (includeChildren) {
          // Get the entity where search the fields
          entity = getEntityByDetailField(entity, splittedCurrentField, 0);
        }
        if (removedData) {
          currentFieldValue = currentFieldValue.concat(".");
        }
        results.addAll(getDetailFieldsRelatedToEntity(entity, currentFieldValue));
      }
    }

    return results;
  }

  @CliOptionAutocompleteIndicator(param = "responseType", command = "web mvc detail",
      help = "--responseType parameter should be completed with an installed response type.")
  public List<String> getAllResponseTypeForDetailCommandValues(ShellContext context) {

    // Getting all installed services that implements
    // ControllerMVCResponseService
    Map<String, ControllerMVCResponseService> installedResponseTypes =
        getInstalledControllerMVCResponseTypes();

    // Generating all possible values
    List<String> responseTypes = new ArrayList<String>();

    for (Entry<String, ControllerMVCResponseService> responseType : installedResponseTypes
        .entrySet()) {
      // If specified controller doesn't have this response type
      // installed. Add to responseTypes
      // possible values
      responseTypes.add(responseType.getKey());
    }

    return responseTypes;
  }

  @CliOptionVisibilityIndicator(
      params = {"package", "responseType"},
      command = "web mvc detail",
      help = "--package, --pathPrefix and --responseType parameters are not visible if --all parameter or --entity parameter has been specified before.")
  public boolean areDetailParametersVisibles(ShellContext context) {
    if (context.getParameters().containsKey("all") || context.getParameters().containsKey("entity")) {
      return true;
    }
    return false;
  }

  /**
   * This method provides the Command definition to be able to generate new
   * Controllers on current project.
   *
   * @param all
   * @param entity
   * @param responseType
   * @param package
   * @param pathPrefix
   */
  @CliCommand(
      value = "web mvc detail",
      help = "Generates new @RooController's for relation fields which detail wants to be managed. It must be a @OneToMany field. Generated controllers will have @RooDetail with info about the parent entity")
  public void addDetailController(
      @CliOption(
          key = "all",
          mandatory = false,
          specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false",
          help = "Indicates if developer wants to generate first detail controllers for every entity"
              + " that has a controller of current project. This param will be visible if 'entity' parameter has not been specified") boolean all,
      @CliOption(
          key = "entity",
          mandatory = false,
          help = "Indicates the entity on which the detail controller is generated. This param will be visible if 'all' parameter has not been specified ") JavaType entity,
      @CliOption(
          key = "field",
          mandatory = false,
          specifiedDefaultValue = "",
          unspecifiedDefaultValue = "",
          help = "Indicates the entity's field on which the detail controller is generated. It must be a @OneToMany field. This param will be visible if 'entity' parameter has been specified before. ") String field,
      @CliOption(
          key = "package",
          mandatory = false,
          optionContext = APPLICATION_FEATURE,
          help = "Indicates which package has the controllers on which the detail controllers are generated. This param will be visible if 'all' or 'entity' parameters "
              + "have been specified") JavaPackage controllersPackage,
      @CliOption(
          key = "responseType",
          mandatory = false,
          unspecifiedDefaultValue = "JSON",
          specifiedDefaultValue = "JSON",
          help = "Indicates the responseType to be used by generated controller. Depending of the selected responseType, generated methods and views will vary. This param "
              + "will be visible if 'all' or 'entity' parameters have been specified") String responseType) {

    // Getting --responseType service
    Map<String, ControllerMVCResponseService> responseTypeServices =
        getInstalledControllerMVCResponseTypes();

    // Validate that provided responseType is a valid provided
    if (!responseTypeServices.containsKey(responseType)) {
      LOGGER
          .log(
              Level.SEVERE,
              "ERROR: Provided responseType is not valid. Use autocomplete feature to obtain valid responseTypes.");
      return;
    }

    // Check --all parameter
    if (all) {
      getControllerOperations().createOrUpdateDetailControllersForAllEntities(
          responseTypeServices.get(responseType), controllersPackage);
    } else {
      getControllerOperations().createOrUpdateDetailControllerForEntity(entity, field,
          responseTypeServices.get(responseType), controllersPackage);
    }
  }

  /**
   * Get a entity by field recursively
   *
   * @param paternEntity
   *            Root entity
   * @param field
   *            Field to search the entity
   * @param level
   *            Current recursion level
   * @return
   */
  private ClassOrInterfaceTypeDetails getEntityByDetailField(
      ClassOrInterfaceTypeDetails paternEntity, String field[], int level) {
    ClassOrInterfaceTypeDetails entity = paternEntity;
    MemberDetails entityDetails =
        getMemberDetailsScanner().getMemberDetails(paternEntity.getType().getSimpleTypeName(),
            paternEntity);
    List<FieldMetadata> fields = entityDetails.getFields();
    for (FieldMetadata entityField : fields) {
      if (entityField.getFieldName().getSymbolName().equals(field[level])) {
        entity =
            getTypeLocationService().getTypeDetails(
                entityField.getFieldType().getParameters().get(0));
      }
    }
    level++;
    if (level < field.length) {
      entity = getEntityByDetailField(entity, field, level);
    }

    return entity;
  }

  /**
   * Get a field list that can be selected to do a detail controller.
   *
   * @param entity
   *            Entity on which create the detail controller
   * @param parentFieldName
   *            The parent's field name used to construct the field name
   *            related with the original entity
   * @return the related field list
   */
  private List<String> getDetailFieldsRelatedToEntity(ClassOrInterfaceTypeDetails entity,
      String parentFieldName) {
    List<String> results = new ArrayList<String>();

    MemberDetails entityDetails =
        getMemberDetailsScanner().getMemberDetails(entity.getType().getSimpleTypeName(), entity);
    List<FieldMetadata> fields = entityDetails.getFields();

    for (FieldMetadata field : fields) {
      AnnotationMetadata oneToManyAnnotation = field.getAnnotation(JpaJavaType.ONE_TO_MANY);
      if (oneToManyAnnotation != null
          && (field.getFieldType().getFullyQualifiedTypeName()
              .equals(JavaType.LIST.getFullyQualifiedTypeName()) || field.getFieldType()
              .getFullyQualifiedTypeName().equals(JavaType.SET.getFullyQualifiedTypeName()))) {
        results.add(parentFieldName.concat(field.getFieldName().getSymbolName()));
      }
    }

    return results;
  }

  // operation command
  @CliOptionAutocompleteIndicator(
      param = "controller",
      command = "web mvc operation",
      help = "--controller parameter should be completed with an controller generated previously or with a name that will be used to create a new controller.")
  public List<String> getAllControllerForOperationCommandValues(ShellContext context) {

    // Get the actual controller selected
    String currentText = context.getParameters().get("controller");

    // Create results to return
    List<String> results = new ArrayList<String>();

    // Get controllers full qualified names
    Set<ClassOrInterfaceTypeDetails> controllers =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_CONTROLLER);

    for (ClassOrInterfaceTypeDetails controller : controllers) {
      String name = getClasspathOperations().replaceTopLevelPackageString(controller, currentText);
      if (!results.contains(name)) {
        results.add(name);
      }
    }

    return results;
  }

  @CliOptionAutocompleteIndicator(param = "service", command = "web mvc operation",
      help = "--service parameter should be completed with an service generated previously.")
  public List<String> getAllServiceForOperationCommandValues(ShellContext context) {

    // Get the actual service selected
    String currentText = context.getParameters().get("service");

    // Create results to return
    List<String> results = new ArrayList<String>();

    // Get controllers full qualified names
    Set<ClassOrInterfaceTypeDetails> services =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_SERVICE);

    for (ClassOrInterfaceTypeDetails service : services) {
      String name = getClasspathOperations().replaceTopLevelPackageString(service, currentText);
      if (!results.contains(name)) {
        results.add(name);
      }
    }

    return results;
  }

  @CliOptionAutocompleteIndicator(param = "operation", command = "web mvc operation",
      help = "--operation parameter should be completed with the operations availables to publish.")
  public List<String> getAllOperationsForOperationCommandValues(ShellContext context) {

    // Getting the service
    String currentService = context.getParameters().get("service");

    // Get the controller and try to get the related service
    String currentController = context.getParameters().get("controller");

    return getControllerOperations().getAllMethodsToPublish(currentService, currentController);
  }

  @CliOptionVisibilityIndicator(params = {"all"}, command = "web mvc operation",
      help = "--all parameter isn't visible if --operation parameter has been specified before.")
  public boolean isAllParameterOperationVisible(ShellContext context) {
    if (context.getParameters().containsKey("operation")) {
      return false;
    }
    return true;
  }

  @CliOptionVisibilityIndicator(params = {"operation"}, command = "web mvc operation",
      help = "--operation parameter isn't visible if --all parameter has been specified before.")
  public boolean isOperationParameterOperationVisible(ShellContext context) {
    if (context.getParameters().containsKey("all")) {
      return false;
    }
    return true;
  }

  @CliOptionVisibilityIndicator(params = {"service"}, command = "web mvc operation",
      help = "--service parameter isn't visible if --operation parameter has been specified.")
  public boolean isServiceParameterOperationVisible(ShellContext context) {

    // Check if parameter --service has been specified
    if (context.getParameters().containsKey("service")) {
      return true;
    }

    // Check if controller exists and if parameter --operation has been specified
    if (context.getParameters().containsKey("operation")) {
      return false;
    }
    return true;
  }

  /**
   * This indicator checks if is possible to publish own operations in
   * controllers.
   *
   * If a valid project has been generated and Spring MVC has been installed,
   * this command will be available.
   *
   * @return
   */
  @CliAvailabilityIndicator(value = "web mvc operation")
  public boolean isPublishOperationsAvailable() {
    return getControllerOperations().isPublishOperationsAvailable();
  }

  /**
   * This method provides the Command definition to be able to publish service
   * operations in controllers.
   *
   * @param controller
   * @param service
   * @param operation
   * @param all
   */
  /* TODO: TO BE IMPLEMENTED
     @CliCommand(value = "web mvc operation",
      help = "Update or generates @RooController with service operations")
  public void addOperationInController(

      @CliOption(key = "controller", mandatory = true,
          help = "Indicates the controller where should be published the service methods") JavaType controller,
      @CliOption(
          key = "service",
          mandatory = false,
          help = "This param will be visible if --controller parameter doesn't exists in the application. Indicates the service on which methods should be published in the controller") JavaType service,
      @CliOption(
          key = "operation",
          mandatory = false,
          help = "This param will be visible if --all parameter hasn't been specified. Indicates the operation of the service that should be published in the controller") String operation,
      @CliOption(
          key = "all",
          mandatory = false,
          specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false",
          help = "This param will be visible if --operation parameter hasn't been specified. Indicates if every method of the service should be published in the controller") boolean all) {

    List<String> operations = new ArrayList<String>();

    // Check --all parameter
    if (all) {

      // Getting the service
      String currentService = null;
      String serviceName = "";
      if (service != null) {
        currentService = service.getFullyQualifiedTypeName();

        // Get service name to concatenate it on each method name
        serviceName =
            getControllerOperations().replaceTopLevelPackage(
                getTypeLocationService().getTypeDetails(service));
      }

      // Getting the controller
      String currentController = null;
      if (controller != null) {
        currentController = controller.getFullyQualifiedTypeName();
      }

      List<String> allMethodsToPublish =
          getControllerOperations().getAllMethodsToPublish(currentService, currentController);


      for (String methodToPublish : allMethodsToPublish) {
        if (service != null) {
          methodToPublish = serviceName.concat(".").concat(methodToPublish);
        }
        operations.add(methodToPublish);
      }

      // Get all the methods related with controller or service

    } else {
      if (service != null) {
        // Add service name to operation
        String serviceName =
            getControllerOperations().replaceTopLevelPackage(
                getTypeLocationService().getTypeDetails(service));
        operation = serviceName.concat(".").concat(operation);
      }

      operations.add(operation);
    }

    getControllerOperations().exportOperation(controller, operations);
  }*/

  // Gets OSGi Services

  public ControllerOperations getControllerOperations() {
    return serviceInstaceManager.getServiceInstance(this, ControllerOperations.class);
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

  public TypeLocationService getTypeLocationService() {
    return serviceInstaceManager.getServiceInstance(this, TypeLocationService.class);
  }

  public ClasspathOperations getClasspathOperations() {
    return serviceInstaceManager.getServiceInstance(this, ClasspathOperations.class);
  }

  public MemberDetailsScanner getMemberDetailsScanner() {
    return serviceInstaceManager.getServiceInstance(this, MemberDetailsScanner.class);
  }

}
