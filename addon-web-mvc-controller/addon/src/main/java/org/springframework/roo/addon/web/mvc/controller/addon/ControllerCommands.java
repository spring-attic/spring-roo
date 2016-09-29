package org.springframework.roo.addon.web.mvc.controller.addon;

import static org.springframework.roo.shell.OptionContexts.APPLICATION_FEATURE;
import static org.springframework.roo.shell.OptionContexts.APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
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
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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

  private ControllerOperations controllerOperations;
  private ProjectOperations projectOperations;
  private TypeLocationService typeLocationService;
  private Converter<JavaType> javaTypeConverter;
  @Reference
  private MemberDetailsScanner memberDetailsScanner;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
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
      @CliOption(key = "module", mandatory = true,
          help = "The application module where to install the persistence",
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
        String name = replaceTopLevelPackageString(entity, currentText);
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
  @CliCommand(value = "web mvc controller",
      help = "Generates new @RooController inside current project")
  public void addController(
      @CliOption(
          key = "all",
          mandatory = false,
          specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false",
          help = "This param will be visible if --entity parameter hasn't been specified. Indicates if developer wants to generate controllers for every entity of current project ") boolean all,
      @CliOption(
          key = "entity",
          mandatory = false,
          help = "This param will be visible if --all parameter hasn't been specified. Indicates the entity that new controller will be manage.") JavaType entity,
      @CliOption(
          key = "responseType",
          mandatory = false,
          unspecifiedDefaultValue = "JSON",
          specifiedDefaultValue = "JSON",
          help = "This param will be visible if --all or --entity parameters have been specified. Indicates the responseType to be used by generated controller. Depending of the selected responseType, generated methods and views will vary.") String responseType,
      @CliOption(
          key = "package",
          mandatory = false,
          optionContext = APPLICATION_FEATURE,
          help = "This param will be visible if --all or --entity parameters have been specified. Indicates which package should be used to include generated controllers") JavaPackage controllersPackage,
      @CliOption(
          key = "pathPrefix",
          mandatory = false,
          specifiedDefaultValue = "",
          unspecifiedDefaultValue = "",
          help = "This param will be visible if --all or --entity parameters have been specified. Indicates @ResquestMapping prefix to be used on this controller. Is not necessary to specify '/'. Spring Roo shell will include it automatically.") String pathPrefix) {

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
   * Replaces a JavaType fullyQualifiedName for a shorter name using '~' for
   * TopLevelPackage
   *
   * @param cid
   *            ClassOrInterfaceTypeDetails of a JavaType
   * @param currentText
   *            String current text for option value
   * @return the String representing a JavaType with its name shortened
   */
  private String replaceTopLevelPackageString(ClassOrInterfaceTypeDetails cid, String currentText) {
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
   * This method provides the Command definition to be able to generate new
   * Controllers on current project.
   *
   * @param all
   * @param entity
   * @param responseType
   * @param package
   * @param pathPrefix
   */

  @CliCommand(value = "web mvc detail",
      help = "Generates new @RooController inside current project")
  public void addDetailController(
      @CliOption(
          key = "all",
          mandatory = false,
          specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false",
          help = "This param will be visible if --entity parameter hasn't been specified. Indicates if developer wants to generate first detail controllers for every entity that has a controller of current project") boolean all,
      @CliOption(
          key = "entity",
          mandatory = false,
          help = "This param will be visible if --all parameter hasn't been specified. Indicates the entity on which the detail controller is generated.") JavaType entity,
      @CliOption(
          key = "field",
          mandatory = false,
          specifiedDefaultValue = "",
          unspecifiedDefaultValue = "",
          help = "This param will be visible if --all or --entity parameters have been specified. Indicates the entity's field on which the detail controller is generated.") String field,
      @CliOption(
          key = "package",
          mandatory = false,
          optionContext = APPLICATION_FEATURE,
          help = "This param will be visible if --all or --entity parameters have been specified. Indicates which package has the controllers on which the detail controllers are generated.") JavaPackage controllersPackage,
      @CliOption(
          key = "responseType",
          mandatory = false,
          unspecifiedDefaultValue = "JSON",
          specifiedDefaultValue = "JSON",
          help = "This param will be visible if --all or --entity parameters have been specified. Indicates the responseType to be used by generated controller. Depending of the selected responseType, generated methods and views will vary.") String responseType) {

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
      String name = replaceTopLevelPackageString(entity, currentText);
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
      help = "--field parameter must be an existing @OneToMany field of the specified entity in parameter --entity.")
  public List<String> getDetailFieldsRelatedToEntity(ShellContext shellContext) {

    // Get current value of class
    String currentEntity = shellContext.getParameters().get("entity");

    // Create results to return
    List<String> results = new ArrayList<String>();

    // Get entity full qualified names
    Set<ClassOrInterfaceTypeDetails> entities =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_JPA_ENTITY);
    for (ClassOrInterfaceTypeDetails entity : entities) {
      if (replaceTopLevelPackageString(entity, entity.getType().getModule()).equals(currentEntity)) {
        results.addAll(getDetailFieldsRelatedToEntity(entity, "", entity.getType()
            .getSimpleTypeName()));
      }
    }

    return results;
  }

  /**
   * Get a field list recursively that can be selected to do a detail controller.
   *
   * @param entity Entity on which create the detail controller
   * @param parentFieldName The parent's field name used to construct the field name related with the original entity
   * @param entityDetailController The root entity name of the detail controllers
   * @return the related field list
   */
  private List<String> getDetailFieldsRelatedToEntity(ClassOrInterfaceTypeDetails entity,
      String parentFieldName, String entityDetailController) {
    List<String> results = new ArrayList<String>();

    MemberDetails entityDetails =
        memberDetailsScanner.getMemberDetails(entity.getType().getSimpleTypeName(), entity);
    List<FieldMetadata> fields = entityDetails.getFields();

    for (FieldMetadata field : fields) {
      boolean detailNotWasCreated = true;
      AnnotationMetadata oneToManyAnnotation = field.getAnnotation(JpaJavaType.ONE_TO_MANY);
      if (oneToManyAnnotation != null
          && (field.getFieldType().getFullyQualifiedTypeName()
              .equals(JavaType.LIST.getFullyQualifiedTypeName()) || field.getFieldType()
              .getFullyQualifiedTypeName().equals(JavaType.SET.getFullyQualifiedTypeName()))) {

        // Get all controllers with ROO_DETAIL annotation
        Set<ClassOrInterfaceTypeDetails> listRooDetailController =
            getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
                RooJavaType.ROO_DETAIL);
        for (ClassOrInterfaceTypeDetails detailController : listRooDetailController) {
          // Get entity value
          AnnotationMetadata annotationRooController =
              detailController.getAnnotation(RooJavaType.ROO_CONTROLLER);
          JavaType entityAnnotationController =
              (JavaType) annotationRooController.getAttribute("entity").getValue();
          // Get relation field value
          String relationField =
              (String) detailController.getAnnotation(RooJavaType.ROO_DETAIL)
                  .getAttribute("relationField").getValue();
          // if entity and relation field are the same, the detail controller of this field was created
          if (entityDetailController.equals(entityAnnotationController.getSimpleTypeName())
              && parentFieldName.concat(field.getFieldName().getSymbolName()).equals(relationField)) {
            ClassOrInterfaceTypeDetails entityField =
                getTypeLocationService()
                    .getTypeDetails(field.getFieldType().getParameters().get(0));
            // This detail has been created, check if it has more levels
            // and offer them including.
            results.addAll(getDetailFieldsRelatedToEntity(entityField,
                parentFieldName.concat(field.getFieldName().getSymbolName()).concat("."),
                entityDetailController));
            detailNotWasCreated = false;
            break;
          }
        }
        // If the detail controller was not created, add field to the result list.
        if (detailNotWasCreated) {
          results.add(parentFieldName.concat(field.getFieldName().getSymbolName()));
        }

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

  // Gets OSGi Services

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
        LOGGER.warning("Cannot load TypeLocationService on ControllerCommands.");
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
        LOGGER.warning("Cannot load ProjectOperations on ControllerCommands.");
        return null;
      }
    } else {
      return projectOperations;
    }
  }

  public ControllerOperations getControllerOperations() {
    if (controllerOperations == null) {
      // Get all Services implement ControllerOperations interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ControllerOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          controllerOperations = (ControllerOperations) this.context.getService(ref);
          return controllerOperations;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ControllerOperations on ControllerCommands.");
        return null;
      }
    } else {
      return controllerOperations;
    }
  }

  /**
   * This method obtains JavaType converter to be able to obtain JavaType from
   * strings
   *
   * @return
   */
  public Converter<JavaType> getJavaTypeConverter() {
    if (javaTypeConverter == null) {
      // Get all Services implement Converter<JavaType> interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(Converter.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          Converter<?> converter = (Converter<?>) this.context.getService(ref);
          if (converter.supports(JavaType.class, "")) {
            javaTypeConverter = (Converter<JavaType>) converter;
            return javaTypeConverter;
          }
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load Converter<JavaType> on ControllerCommands.");
        return null;
      }
    } else {
      return javaTypeConverter;
    }
  }

}
