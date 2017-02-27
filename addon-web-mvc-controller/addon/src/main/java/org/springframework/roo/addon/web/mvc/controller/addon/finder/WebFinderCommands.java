package org.springframework.roo.addon.web.mvc.controller.addon.finder;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.layers.repository.jpa.addon.RepositoryJpaLocator;
import org.springframework.roo.addon.web.mvc.controller.addon.responses.ControllerMVCResponseService;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
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
import org.springframework.roo.support.osgi.ServiceInstaceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Commands which provide finder functionality through Spring MVC controllers.
 *
 * @author Stefan Schmidt
 * @author Paula Navarro
 * @author Sergio Clares
 * @since 1.2.0
 */
@Component
@Service
public class WebFinderCommands implements CommandMarker {

  private static Logger LOGGER = HandlerUtils.getLogger(WebFinderCommands.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  @Reference
  private WebFinderOperations webFinderOperations;

  @Reference
  private RepositoryJpaLocator repositoryJpaLocator;

  private Map<String, ControllerMVCResponseService> responseTypes =
      new HashMap<String, ControllerMVCResponseService>();
  private Converter<JavaType> javaTypeConverter;

  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
    serviceInstaceManager.activate(this.context);
  }

  @CliAvailabilityIndicator({"web mvc finder"})
  public boolean isCommandAvailable() {
    return webFinderOperations.isWebFinderInstallationPossible();
  }

  /**
   * This indicator says if --entity parameter should be visible or not
   *
   * If --all parameter has been specified, --entity parameter will not be visible
   * to prevent conflicts.
   *
   * @return
   */
  @CliOptionVisibilityIndicator(params = "entity", command = "web mvc finder",
      help = "--entity parameter is not visible if --all parameter has been specified before.")
  public boolean isEntityParameterVisible(ShellContext context) {
    if (context.getParameters().containsKey("all")) {
      return false;
    }
    return true;
  }

  @CliOptionAutocompleteIndicator(param = "entity", command = "web mvc finder",
      help = "--entity parameter should be completed with classes annotated with @RooJpaEntity.")
  public List<String> getEntityValues(ShellContext context) {

    // Get current value of class
    String currentText = context.getParameters().get("entity");

    // Create results to return
    List<String> results = new ArrayList<String>();
    for (JavaType entity : getTypeLocationService().findTypesWithAnnotation(
        RooJavaType.ROO_JPA_ENTITY)) {

      ClassOrInterfaceTypeDetails repository = repositoryJpaLocator.getFirstRepository(entity);
      if (repository == null) {
        continue;
      }
      AnnotationMetadata repositoryAnnotation =
          repository.getAnnotation(RooJavaType.ROO_REPOSITORY_JPA);
      if (repositoryAnnotation.getAttribute("finders") == null) {
        continue;
      }
      String name = replaceTopLevelPackageString(entity, currentText);

      if (!results.contains(name)) {
        results.add(name);
      }
    }

    if (results.isEmpty()) {
      results.add("");
    }

    return results;
  }

  /**
   * This indicator says if --all parameter should be visible or not
   *
   * If --entity parameter has been specified, --all parameter will not be visible
   * to prevent conflicts.
   *
   * @return
   */
  @CliOptionVisibilityIndicator(params = "all", command = "web mvc finder",
      help = "--all parameter is not be visible if --entity parameter has been specified before.")
  public boolean isAllParameterVisible(ShellContext context) {
    if (context.getParameters().containsKey("entity")) {
      return false;
    }
    return true;
  }


  /**
   * This indicator says if --queryMethod parameter should be visible or not
   *
   * If --entity parameter has been specified, --queryMethod parameter will be visible.
   *
   * @return
   */
  @CliOptionVisibilityIndicator(
      params = "queryMethod",
      command = "web mvc finder",
      help = "--queryMethod parameter is not visible if --entity parameter hasn't been specified before.")
  public boolean isQueryMethodParameterVisible(ShellContext context) {
    if (context.getParameters().containsKey("entity")) {
      return true;
    }
    return false;
  }


  @CliOptionAutocompleteIndicator(param = "queryMethod", command = "web mvc finder",
      help = "--queryMethod parameter should be completed with related repository finders.")
  public List<String> getAllQueryMethodValues(ShellContext context) {
    List<String> finders = new ArrayList<String>();

    if (context.getParameters().containsKey("entity")) {
      // Extract entity
      String providedEntity = context.getParameters().get("entity");

      // Getting the JavaType converter
      if (getJavaTypeConverter() != null && StringUtils.isNotBlank(providedEntity)) {
        JavaType entity =
            getJavaTypeConverter().convertFromText(providedEntity, JavaType.class, "");

        // Get finders
        finders = getFinders(entity, null);
      }

    }

    if (finders.isEmpty()) {
      finders.add("");
    }
    return finders;
  }

  /**
   * This indicator says if --responseType parameter should be visible or not
   *
   * If --all or --finder parameter have not been specified, --responseType parameter will not be visible
   * to preserve order.
   *
   * @return
   */
  @CliOptionVisibilityIndicator(
      params = "package",
      command = "web mvc finder",
      help = "--package parameter is not be visible if --all or --entity parameters have not been specified before.")
  public boolean isPackageParameterVisible(ShellContext context) {
    if (context.getParameters().containsKey("entity") || context.getParameters().containsKey("all")) {
      return true;
    }
    return false;
  }

  /**
   * This indicator says if --package parameter should be visible or not. If project has more
   * than one 'application' modules (which contain one @SpringBootApplication), package will
   * be mandatory.
   *
   * @param shellContext
   * @return
   */
  @CliOptionMandatoryIndicator(params = "package", command = "web mvc finder")
  public boolean isPackageRequired(ShellContext shellContext) {
    if (getTypeLocationService().getModuleNames(ModuleFeatureName.APPLICATION).size() <= 1) {
      return false;
    }
    return true;
  }

  /**
   * This indicator says if --responseType parameter should be visible or not
   *
   * If --all or --entity parameter have not been specified, --responseType parameter will not be visible
   * to preserve order.
   *
   * @return
   */
  @CliOptionVisibilityIndicator(
      params = "responseType",
      command = "web mvc finder",
      help = "--responseType parameter is not be visible if --all or --entity parameters have not been specified before.")
  public boolean isResponseTypeParameterVisible(ShellContext context) {
    if (context.getParameters().containsKey("entity") || context.getParameters().containsKey("all")) {
      return true;
    }
    return false;
  }

  /**
   * This indicator returns all possible values for --responseType parameter.
   *
   * Depends of the specified --controller, responseTypes will be filtered to provide only that
   * responseTypes that exists on current controller. Also, only installed response types
   * will be provided.
   *
   * @param context
   * @return
   */
  @CliOptionAutocompleteIndicator(param = "responseType", command = "web mvc finder",
      help = "--responseType parameter should be completed with the provided response types.")
  public List<String> getAllResponseTypeValues(ShellContext context) {

    // Generating all possible values
    List<String> responseTypes = new ArrayList<String>();

    // Getting all installed services that implements ControllerMVCResponseService
    Map<String, ControllerMVCResponseService> installedResponseTypes =
        getInstalledControllerMVCResponseTypes();

    for (Entry<String, ControllerMVCResponseService> responseType : installedResponseTypes
        .entrySet()) {

      // Add installed response type
      responseTypes.add(responseType.getKey());
    }

    if (responseTypes.isEmpty()) {
      responseTypes.add("");
    }

    return responseTypes;
  }

  /**
   * This indicator says if --pathPrefix parameter should be visible or not
   *
   * If --all or --entity parameter have not been specified, --pathPrefix parameter will not be visible
   * to preserve order.
   *
   * @return
   */
  @CliOptionVisibilityIndicator(
      params = "pathPrefix",
      command = "web mvc finder",
      help = "--pathPrefix parameter is not be visible if --all or --entity parameters have not been specified before.")
  public boolean isPathPrefixParameterVisible(ShellContext context) {
    if (context.getParameters().containsKey("entity") || context.getParameters().containsKey("all")) {
      return true;
    }
    return false;
  }

  /**
   * This method provides the Command definition to be able to add
   * new finder on controllers.
   *
   * @param controller
   * @param all
   * @param finder
   * @param responseType
   */
  @CliCommand(
      value = "web mvc finder",
      help = "Publishes existing finders to web layer, generating controllers and additional views for "
          + "them. It adds `@RooWebFinder` annotation to MVC controller type.")
  public void addController(
      @CliOption(
          key = "entity",
          mandatory = false,
          help = "The entity owning the finders that should be published. When working on a single module "
              + "project, simply specify the name of the entity. If you consider it necessary, you can "
              + "also specify the package. Ex.: `--class ~.domain.MyEntity` (where `~` is the base "
              + "package). When working with multiple modules, you should specify the name of the entity "
              + "and the module where it is. Ex.: `--class model:~.domain.MyEntity`. If the module is not "
              + "specified, it is assumed that the entity is in the module which has the focus. "
              + "Possible values are: any of the entities in the project. "
              + "This option is mandatory if `--all` is not specified. Otherwise, using `--all` "
              + "will cause the parameter `--entity` won't be available.") JavaType entity,
      @CliOption(
          key = "all",
          mandatory = false,
          specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false",
          help = "Indicates if developer wants to publish in web layer all finders from all entities in "
              + "project. "
              + "This option is mandatory if `--entity` is not specified. Otherwise, using `--entity` "
              + "will cause the parameter `--all` won't be available. "
              + "Default if option present: `true`; default if option not present: `false`.") boolean all,
      @CliOption(key = "queryMethod", mandatory = false,
          help = "Indicates the name of the finder to add to web layer. "
              + "Possible values are: any of the finder names created for the entity, included in "
              + "`@RooJpaRepository` of the `--entity` associated repository. "
              + "This option is available only when `--entity` has been specified.") String queryMethod,
      @CliOption(
          key = "responseType",
          mandatory = false,
          help = "Indicates the responseType to be used by generated finder controllers. Depending on "
              + "the selected responseType, generated methods and views will vary. "
              + "Possible values are: `JSON` plus any response type installed with `web mvc view setup` "
              + "command. "
              + "This option is only available if `--all` or `--entity` parameters have been specified. "
              + "Default: `JSON`.") String responseType,
      @CliOption(
          key = "package",
          mandatory = true,
          unspecifiedDefaultValue = "~.web",
          help = "Indicates the Java package where the finder controllers should be generated. In"
              + " multi-module project you should specify the module name before the package name. "
              + "Ex.: `--package application:org.springframework.roo.web` but, if module name is not "
              + "present, the Roo Shell focused module will be used. "
              + "This option is available only if `--all` or `--entity` option has been specified. "
              + "Default value if not present: `~.web` package, or 'application:~.web' if multi-module "
              + "project.") JavaPackage controllerPackage,
      @CliOption(
          key = "pathPrefix",
          mandatory = false,
          unspecifiedDefaultValue = "",
          specifiedDefaultValue = "",
          help = "Indicates the default path value for accesing finder resources in controller, used for "
              + "this controller `@RequestMapping` excluding first '/'. "
              + "This option is available only if `--all` or `--entity` option has been specified.") String pathPrefix) {

    // Getting --responseType service
    Map<String, ControllerMVCResponseService> responseTypeServices =
        getInstalledControllerMVCResponseTypes();

    // Validate that provided responseType is a valid provided
    ControllerMVCResponseService controllerResponseType = null;
    if (responseType != null) {
      if (!responseTypeServices.containsKey(responseType)) {
        LOGGER.log(Level.SEVERE,
            "ERROR: Provided responseType is not valid. Use autocomplete feature "
                + "to obtain valid responseTypes.");
        return;
      } else {
        controllerResponseType = responseTypeServices.get(responseType);
      }
    } else {
      controllerResponseType = responseTypeServices.get("JSON");
    }

    // Execute finder operation
    if (!all) {

      // Create queryMethods list
      List<String> queryMethods = new ArrayList<String>();
      if (queryMethod != null) {
        queryMethods.add(queryMethod);
      } else {
        queryMethods = getFinders(entity, controllerResponseType);
      }
      webFinderOperations.createOrUpdateSearchControllerForEntity(entity, queryMethods,
          controllerResponseType, controllerPackage, pathPrefix);
    } else {
      webFinderOperations.createOrUpdateSearchControllerForAllEntities(controllerResponseType,
          controllerPackage, pathPrefix);
    }
  }

  /**
   * Replaces a JavaType fullyQualifiedName for a shorter name using '~' for TopLevelPackage
   *
   * @param cid ClassOrInterfaceTypeDetails of a JavaType
   * @param currentText String current text for option value
   * @return the String representing a JavaType with its name shortened
   */
  private String replaceTopLevelPackageString(JavaType type, String currentText) {
    String javaTypeFullyQualilfiedName = type.getFullyQualifiedTypeName();
    String javaTypeString = "";
    String topLevelPackageString = "";

    // Add module value to topLevelPackage when necessary
    if (StringUtils.isNotBlank(type.getModule())
        && !type.getModule().equals(getProjectOperations().getFocusedModuleName())) {

      // Target module is not focused
      javaTypeString = type.getModule().concat(LogicalPath.MODULE_PATH_SEPARATOR);
      topLevelPackageString =
          getProjectOperations().getTopLevelPackage(type.getModule())
              .getFullyQualifiedPackageName();
    } else if (StringUtils.isNotBlank(type.getModule())
        && type.getModule().equals(getProjectOperations().getFocusedModuleName())
        && (currentText.startsWith(type.getModule()) || type.getModule().startsWith(currentText))
        && StringUtils.isNotBlank(currentText)) {

      // Target module is focused but user wrote it
      javaTypeString = type.getModule().concat(LogicalPath.MODULE_PATH_SEPARATOR);
      topLevelPackageString =
          getProjectOperations().getTopLevelPackage(type.getModule())
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
   * Get all finder names associated to an entity
   *
   * @param entity the JavaType representing the entity whose finder names should
   *            be returned.
   * @param controllerResponseType (can be null)
   * @return a List<String> with the finder names.
   */
  public List<String> getFinders(JavaType entity,
      ControllerMVCResponseService controllerResponseType) {
    return webFinderOperations.getFindersWhichCanBePublish(entity, controllerResponseType);
  }

  /**
   * This method gets all implementations of ControllerMVCResponseService interface to be able
   * to locate all installed ControllerMVCResponseService
   *
   * @return Map with responseTypes identifier and the ControllerMVCResponseService implementation
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
        LOGGER.warning("Cannot load ControllerMVCResponseService on ControllerCommands.");
        return null;
      }
    } else {
      return responseTypes;
    }
  }

  public ProjectOperations getProjectOperations() {
    return serviceInstaceManager.getServiceInstance(this, ProjectOperations.class);

  }

  /**
   * This method obtains JavaType converter to be able to obtain JavaType
   * from strings
   *
   * @return
   */
  @SuppressWarnings("unchecked")
  public Converter<JavaType> getJavaTypeConverter() {
    if (javaTypeConverter == null) {
      List<Converter> javaTypeConverters =
          serviceInstaceManager.getServiceInstance(this, Converter.class,
              new ServiceInstaceManager.Matcher<Converter>() {
                @Override
                public boolean match(Converter service) {
                  return service.supports(JavaType.class, "");
                }
              });

      if (!javaTypeConverters.isEmpty()) {
        javaTypeConverter = javaTypeConverters.get(0);
      }

      return javaTypeConverter;

    }
    return javaTypeConverter;
  }


  public TypeLocationService getTypeLocationService() {
    return serviceInstaceManager.getServiceInstance(this, TypeLocationService.class);
  }

}
