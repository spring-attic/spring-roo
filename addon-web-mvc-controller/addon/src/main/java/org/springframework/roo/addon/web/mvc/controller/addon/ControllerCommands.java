package org.springframework.roo.addon.web.mvc.controller.addon;

import static org.springframework.roo.shell.OptionContexts.APPLICATION_FEATURE;
import static org.springframework.roo.shell.OptionContexts.APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
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
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
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


/**
 * This class provides necessary commands to be able to include Spring MVC on generated
 * project and generate new controllers.
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

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  /**
   * This indicator checks if --module parameter should be visible or not.
   * 
   * If exists more than one module that match with the properties of ModuleFeature APPLICATION,
   * --module parameter should be mandatory.
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
   * If focused module doesn't match with the properties of ModuleFeature APPLICATION,
   * --module parameter should be mandatory.
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
   * If a valid project has been generated and Spring MVC has not been installed yet, 
   * this command will be available.
   * 
   * @return
   */
  @CliAvailabilityIndicator(value = "web mvc setup")
  public boolean isSetupAvailable() {
    return getControllerOperations().isSetupAvailable();
  }

  /**
   * This method provides the Command definition to be able to include
   * Spring MVC on generated project.
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
   * If --controller parameter has been specified, --all parameter will not be visible
   * to prevent conflicts.
   * 
   * @return
   */
  @CliOptionVisibilityIndicator(
      params = "all",
      command = "web mvc controller",
      help = "--all parameter is not be visible if --controller parameter has been specified before.")
  public boolean isAllParameterVisible(ShellContext context) {
    if (context.getParameters().containsKey("controller")) {
      return false;
    }
    return true;
  }

  /**
   * This indicator says if --package parameter should be visible or not
   *
   * If --all parameter has not been specified, --package parameter will not be visible
   * to prevent conflicts.
   * 
   * @return
   */
  @CliOptionVisibilityIndicator(
      params = "package",
      command = "web mvc controller",
      help = "--package parameter is not be visible if --all parameter has not been specified before.")
  public boolean isPackageParameterVisible(ShellContext context) {
    if (context.getParameters().containsKey("all")) {
      return true;
    }
    return false;
  }

  /**
   * This indicator says if --package parameter should be mandatory or not
   *
   * If --all parameter has been specified, --package parameter will be mandatory.
   * 
   * @return
   */
  @CliOptionMandatoryIndicator(params = "package", command = "web mvc controller")
  public boolean isPackageParameterMandatory(ShellContext context) {
    if (context.getParameters().containsKey("all")) {
      return true;
    }
    return false;
  }

  /**
   * This indicator says if --controller parameter should be visible or not
   *
   * If --all parameter has been specified, --controller parameter will not be visible
   * to prevent conflicts.
   * 
   * @return
   */
  @CliOptionVisibilityIndicator(
      params = "controller",
      command = "web mvc controller",
      help = "--controller parameter is not be visible if --all parameter has been specified before.")
  public boolean isClassParameterVisible(ShellContext context) {
    if (context.getParameters().containsKey("all")) {
      return false;
    }
    return true;
  }


  /**
   * This indicator says if --entity parameter should be visible or not
   *
   * If --controller parameter has not been specified, --entity parameter will not be visible
   * If --controller parameter has been specified with an existing controller, --entity parameter will
   * not be visible
   * If --controller parameter has been specified with new controller to be generated, --entity parameter 
   * will be visible
   * 
   * @return
   */
  @CliOptionVisibilityIndicator(
      params = "entity",
      command = "web mvc controller",
      help = "--entity parameter will be visible if --controller parameter has been specified with new controller class")
  public boolean isEntityParameterVisible(ShellContext context) {
    Map<String, String> parameters = context.getParameters();
    if (parameters.containsKey("controller")) {
      String specifiedClass = parameters.get("controller");
      JavaType controller =
          getJavaTypeConverter().convertFromText(specifiedClass, JavaType.class, "");
      ClassOrInterfaceTypeDetails controllerDetails =
          getTypeLocationService().getTypeDetails(controller);
      if (controllerDetails != null
          && controllerDetails.getAnnotation(RooJavaType.ROO_CONTROLLER) != null) {
        return false;
      }
      return true;
    }
    return false;
  }

  /**
   * This indicator says if --entity parameter should be mandatory or not
   *
   * If --controller parameter has not been specified, --entity parameter will be optional
   * If --controller parameter has been specified with an existing controller, --entity parameter will
   * be optional
   * If --controller parameter has been specified with new controller to be generated, --entity parameter 
   * will be mandatory
   * 
   * @return
   */
  @CliOptionMandatoryIndicator(params = "entity", command = "web mvc controller")
  public boolean isEntityParameterMandatory(ShellContext context) {
    Map<String, String> parameters = context.getParameters();
    if (parameters.containsKey("controller")) {
      String specifiedClass = parameters.get("controller");
      JavaType controller =
          getJavaTypeConverter().convertFromText(specifiedClass, JavaType.class, "");
      ClassOrInterfaceTypeDetails controllerDetails =
          getTypeLocationService().getTypeDetails(controller);
      if (controllerDetails != null
          && controllerDetails.getAnnotation(RooJavaType.ROO_CONTROLLER) != null) {
        return false;
      }
      return true;
    }
    return false;
  }

  /**
   * This indicator says if --service parameter should be visible or not
   *
   * If --entity parameter has not been specified, --service parameter will not be visible
   * to preserve order.
   * 
   * @return
   */
  @CliOptionVisibilityIndicator(
      params = "service",
      command = "web mvc controller",
      help = "--service parameter is not be visible if --entity parameter has not been specified before.")
  public boolean isServiceParameterVisible(ShellContext context) {
    if (context.getParameters().containsKey("entity")) {
      return true;
    }
    return false;
  }

  /**
   * This indicator says if --service parameter should be mandatory or not
   *
   * If --entity parameter has been specified, --service parameter will be mandatory
   * 
   * @return
   */
  @CliOptionMandatoryIndicator(params = "service", command = "web mvc controller")
  public boolean isServiceParameterMandatory(ShellContext context) {
    if (context.getParameters().containsKey("entity")
        && getProjectOperations().isMultimoduleProject()) {
      return true;
    }
    return false;
  }

  /**
   * This indicator says if --path parameter should be visible or not
   *
   * If --entity parameter has not been specified, --path parameter will not be visible
   * to preserve order.
   * 
   * @return
   */
  @CliOptionVisibilityIndicator(
      params = "path",
      command = "web mvc controller",
      help = "--path parameter is not be visible if --entity parameter has not been specified before.")
  public boolean isPathParameterVisible(ShellContext context) {
    if (context.getParameters().containsKey("entity")) {
      return true;
    }
    return false;
  }

  /**
   * This indicator says if --path parameter should be mandatory or not
   *
   * If --entity parameter has not been specified, --path parameter will be optional
   * If --entity parameter has specified and exists yet some controller that manages that 
   * entity, --path parameter will be mandatory
   * If --entity parameter has specified and doesn't exist other controller that manages that entity,
   * --path parameter will be optional.
   * 
   * @return
   */
  @CliOptionMandatoryIndicator(params = "path", command = "web mvc controller")
  public boolean isPathParameterMandatory(ShellContext context) {
    Map<String, String> parameters = context.getParameters();
    if (parameters.containsKey("entity")) {
      // Getting specified entity
      String specifiedEntity = parameters.get("entity");
      JavaType entity = getJavaTypeConverter().convertFromText(specifiedEntity, JavaType.class, "");

      // Check if some controller has the specified entity
      Set<ClassOrInterfaceTypeDetails> controllers =
          getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
              RooJavaType.ROO_CONTROLLER);

      for (ClassOrInterfaceTypeDetails controller : controllers) {
        AnnotationMetadata controllerAnnotation =
            controller.getAnnotation(RooJavaType.ROO_CONTROLLER);
        if (entity.equals(controllerAnnotation.getAttribute("entity").getValue())) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * This indicator says if --responseType parameter should be visible or not
   *
   * If --controller parameter has not been specified, --responseType parameter will not be visible
   * to preserve order.
   * 
   * @return
   */
  @CliOptionVisibilityIndicator(
      params = "responseType",
      command = "web mvc controller",
      help = "--responseType parameter is not be visible if --controller parameter has not been specified before.")
  public boolean isResponseTypeParameterVisible(ShellContext context) {
    if (context.getParameters().containsKey("controller")
        || context.getParameters().containsKey("all")) {
      return true;
    }
    return false;
  }

  /**
   * This indicator returns all possible values for --responseType parameter.
   * 
   * Depends of the specified --controller, responseTypes will be filtered to provide only that
   * responseTypes that doesn't exists on current controller. Also, only installed response types
   * will be provided.
   * 
   * @param context
   * @return
   */
  @CliOptionAutocompleteIndicator(param = "responseType", command = "web mvc controller",
      help = "--responseType parameter should be completed with the provided response types.")
  public List<String> getAllResponseTypeValues(ShellContext context) {
    // Getting all installed services that implements ControllerMVCResponseService
    Map<String, ControllerMVCResponseService> installedResponseTypes =
        getInstalledControllerMVCResponseTypes();

    // Getting the specified controller
    String controllerParam = context.getParameters().get("controller");
    JavaType specifiedController = null;
    if (controllerParam != null) {
      specifiedController =
          getJavaTypeConverter().convertFromText(controllerParam, JavaType.class, "");
    }

    // Generating all possible values
    List<String> responseTypes = new ArrayList<String>();

    for (Entry<String, ControllerMVCResponseService> responseType : installedResponseTypes
        .entrySet()) {
      // If specified controller doesn't have this response type installed. Add to responseTypes
      // possible values
      if (specifiedController == null) {
        responseTypes.add(responseType.getKey());
      } else if (!responseType.getValue().hasResponseType(specifiedController)) {
        responseTypes.add(responseType.getKey());
      }
    }

    return responseTypes;
  }

  /**
   * This indicator says if --formattersPackage parameter should be visible or not
   *
   * If --controller parameter or --all parameter has been specified, --formattersPackage parameter will be visible
   * to preserve order.
   * 
   * @return
   */
  @CliOptionVisibilityIndicator(
      params = "formattersPackage",
      command = "web mvc controller",
      help = "--formattersPackage parameter is not be visible if --controller or --all parameter has not been specified before.")
  public boolean isFormattersPackageParameterVisible(ShellContext context) {
    Map<String, String> parameters = context.getParameters();
    if (parameters.containsKey("controller")) {

      String specifiedClass = parameters.get("controller");
      JavaType controller =
          getJavaTypeConverter().convertFromText(specifiedClass, JavaType.class, "");
      ClassOrInterfaceTypeDetails controllerDetails =
          getTypeLocationService().getTypeDetails(controller);
      if (controllerDetails != null
          && controllerDetails.getAnnotation(RooJavaType.ROO_CONTROLLER) != null) {
        return false;
      }

      return true;

    } else if (parameters.containsKey("all")) {
      return true;
    }
    return false;
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
   * Find entities in project and returns a list with their fully qualified names.
   * 
   * @param shellContext
   * @return List<String> with available entity full qualified names.
   */
  @CliOptionAutocompleteIndicator(command = "web mvc controller", param = "entity",
      help = "Option entity must have an existing entity value. Please, assign it a right value.")
  public List<String> returnEntityValues(ShellContext shellContext) {

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

  /**
   * Find controllers in project and returns a list with their fully qualified names.
   * 
   * @param shellContext
   * @return List<String> with available controllers full qualified names.
   */
  @CliOptionAutocompleteIndicator(
      command = "web mvc controller",
      param = "controller",
      help = "Option controller must have an existing controller value. Please, assign it a right value.",
      validate = false, includeSpaceOnFinish = false)
  public List<String> returnControllerValues(ShellContext shellContext) {

    // Get current value of class
    String currentText = shellContext.getParameters().get("controller");

    // Create results to return
    List<String> results = new ArrayList<String>();

    // Add all modules to completions list
    Collection<String> modules = getProjectOperations().getModuleNames();
    for (String module : modules) {
      if (StringUtils.isNotBlank(module)
          && !module.equals(getProjectOperations().getFocusedModule().getModuleName())) {
        results.add(module.concat(LogicalPath.MODULE_PATH_SEPARATOR).concat("~."));
      }
    }

    // Get controller full qualified names
    Set<ClassOrInterfaceTypeDetails> controllers =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_CONTROLLER);
    for (ClassOrInterfaceTypeDetails controller : controllers) {
      String name = replaceTopLevelPackageString(controller, currentText);
      if (!results.contains(name)) {
        results.add(name);
      }
    }

    // Always add base package
    results.add("~.");

    return results;
  }

  /**
   * Find services in project and returns a list with their fully qualified names.
   * 
   * @param shellContext
   * @return List<String> with available entity full qualified names.
   */
  @CliOptionAutocompleteIndicator(command = "web mvc controller", param = "service",
      help = "Option service must have an existing service value. Please, assign it a right value.")
  public List<String> returnServiceValues(ShellContext shellContext) {

    // Get current value of class
    String currentText = shellContext.getParameters().get("service");

    // Getting provided entity
    String entity = shellContext.getParameters().get("entity");
    JavaType entityType = getJavaTypeConverter().convertFromText(entity, JavaType.class, "");

    // Create results to return
    List<String> results = new ArrayList<String>();

    // Get service full qualified names
    Set<ClassOrInterfaceTypeDetails> services =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_SERVICE);
    for (ClassOrInterfaceTypeDetails service : services) {

      // Autocomplete with services related to specified entity
      AnnotationAttributeValue<JavaType> entityAttr =
          service.getAnnotation(RooJavaType.ROO_SERVICE).getAttribute("entity");

      if (entityAttr != null && entityAttr.getValue().equals(entityType)) {
        String name = replaceTopLevelPackageString(service, currentText);
        if (!results.contains(name)) {
          results.add(name);
        }
      }
    }

    return results;
  }

  /**
   * This method provides the Command definition to be able to generate
   * new Controllers on current project.
   * 
   * @param all
   * @param package
   * @param controller
   * @param entity
   * @param service
   * @param path
   * @param responseType
   * @param formattersPackage
   */
  @CliCommand(value = "web mvc controller",
      help = "Generates new @RooController inside current project")
  public void addController(
      @CliOption(
          key = "all",
          mandatory = false,
          specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false",
          help = "Indicates if developer wants to generate controllers for every entity of current project ") boolean all,
      @CliOption(
          key = "package",
          mandatory = true,
          optionContext = APPLICATION_FEATURE,
          help = "This param will be mandatory if --all parameter has been specified. Indicates which package should be used to include generated controllers") JavaPackage controllersPackage,
      @CliOption(
          key = "controller",
          mandatory = false,
          help = "Indicates the new controller class to generate. Also, you can indicates an existing controller to update it.") JavaType controller,
      @CliOption(
          key = "entity",
          mandatory = true,
          help = "This param will be mandatory if --controller parameter has been specified with a new controller to generate. Indicates the entity that new controller will be manage.") JavaType entity,
      @CliOption(
          key = "service",
          mandatory = true,
          help = "This param will be mandatory if --entity parameter has been specified. Indicates the service that new controller will use to access to negotiation layer.") JavaType service,
      @CliOption(
          key = "path",
          mandatory = true,
          help = "Indicates @ResquestMapping to be used on this controller. Is not necessary to specify '/'. Spring Roo shell will include it automatically.") String path,
      @CliOption(
          key = "responseType",
          mandatory = false,
          unspecifiedDefaultValue = "JSON",
          specifiedDefaultValue = "JSON",
          help = "Indicates the responseType to be used by generated controller. Depending of the selected responseType, generated methods and views will vary.") String responseType,
      @CliOption(
          key = "formattersPackage",
          mandatory = false,
          help = "Indicates project package where formatters should be generated. By default they will be generated inside the same controllers package.") JavaPackage formattersPackage) {

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
      getControllerOperations().createControllerForAllEntities(controllersPackage,
          responseTypeServices.get(responseType), formattersPackage);
    } else if (controllerExists(controller)) {
      getControllerOperations()
          .updateController(controller, responseTypeServices.get(responseType));
    } else {
      getControllerOperations().createController(controller, entity, service, path,
          responseTypeServices.get(responseType), formattersPackage);
    }
  }

  /**
   * Checks if provided controller already exists.
   * 
   * @param controller JavaType representing provided controller
   * @return <code>true</code> if provided controller already exists
   */
  private boolean controllerExists(JavaType controller) {
    Validate.notNull(controller,
        "ERROR: Controller class is required to be able to generate or update new controller");

    Set<JavaType> controllers =
        getTypeLocationService().findTypesWithAnnotation(RooJavaType.ROO_CONTROLLER);
    for (JavaType controllerType : controllers) {
      if (controllerType.equals(controller)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Replaces a JavaType fullyQualifiedName for a shorter name using '~' for TopLevelPackage
   * 
   * @param cid ClassOrInterfaceTypeDetails of a JavaType
   * @param currentText String current text for option value
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
   * This method gets all implementations of ServerProvider interface to be able
   * to locate all availbale appServers
   * 
   * @return Map with appServer identifier and the ServerProvider implementation
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
   * This method gets all implementations of ControllerMVCResponseService interface to be able
   * to locate all installed ControllerMVCResponseService
   * 
   * @return Map with responseTypes identifier and the ControllerMVCResponseService implementation
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
   * This method obtains JavaType converter to be able to obtain JavaType 
   * from strings
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
