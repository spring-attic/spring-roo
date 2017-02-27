package org.springframework.roo.addon.web.mvc.controller.addon;

import static org.springframework.roo.shell.OptionContexts.APPLICATION_FEATURE;
import static org.springframework.roo.shell.OptionContexts.PROJECT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.springframework.roo.addon.web.mvc.controller.addon.finder.SearchAnnotationValues;
import org.springframework.roo.addon.web.mvc.controller.addon.responses.ControllerMVCResponseService;
import org.springframework.roo.addon.web.mvc.controller.annotations.ControllerType;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.operations.ClasspathOperations;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.converters.JavaTypeConverter;
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

  @CliOptionAutocompleteIndicator(command = "web mvc setup", param = "module",
      help = "--module parameter" + " should be autocomplete with an application module.")
  public List<String> getAllApplicationModules(ShellContext shellContext) {
    return new ArrayList(getTypeLocationService().getModuleNames(ModuleFeatureName.APPLICATION));
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
  @CliCommand(value = "web mvc setup",
      help = "Includes Spring MVC configuration on generated project. "
          + "Needed for several MVC related commands.")
  public void setup(
      @CliOption(
          key = "module",
          mandatory = true,
          help = "The application module where to install the Spring MVC support. "
              + "This option is mandatory if the focus is not set in an application module, that is, a "
              + "module containing an `@SpringBootApplication` class. "
              + "This option is available only if there are more than one application module and none of"
              + " them is focused. "
              + "Default if option not present: the unique 'application' module, or focused 'application'"
              + " module.") Pom module) {

    // If module is null and only exists one APPLICATION module, use that by default
    boolean usesDefaultModule = false;
    if (module == null) {
      Collection<Pom> applicationModules =
          getTypeLocationService().getModules(ModuleFeatureName.APPLICATION);
      if (applicationModules.size() == 1) {
        module = applicationModules.iterator().next();
        usesDefaultModule = true;
      }
    }
    getControllerOperations().setup(module, usesDefaultModule);
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
      help = "--entity parameter is not visible if --all parameter has been specified before.")
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
      help = "--package, --pathPrefix and --responseType parameters are not visible if --all parameter or --entity parameter has not been specified before.")
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
      help = "Generates new `@RooController's` in the directory _src/main/java_ of the selected project "
          + "module (if any). The generated controllers should manage specific entities in the project.")
  public void addController(
      @CliOption(
          key = "all",
          mandatory = false,
          specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false",
          help = "Indicates if developer wants to generate controllers for every entity of current "
              + "project. "
              + "This option is mandatory if `--entity` is not specified. Otherwise, using `--entity` "
              + "will cause the parameter `--all` won't be available. "
              + "Default if option present: `true`; default if option not present: `false`.") boolean all,
      @CliOption(
          key = "entity",
          mandatory = false,
          help = "The domain entity this controller should manage. When working on a single module "
              + "project, simply specify the name of the entity. If you consider it necessary, you can "
              + "also specify the package. Ex.: `--class ~.domain.MyEntity` (where `~` is the base package). "
              + "When working with multiple modules, you should specify the name of the entity and the "
              + "module where it is. Ex.: `--class model:~.domain.MyEntity`. If the module is not "
              + "specified, it is assumed that the entity is in the module which has the focus. "
              + "Possible values are: any of the entities in the project. "
              + "This option is mandatory if `--all` is not specified. Otherwise, using `--all` "
              + "will cause the parameter `--entity` won't be available.") JavaType entity,
      @CliOption(
          key = "responseType",
          mandatory = false,
          unspecifiedDefaultValue = "JSON",
          specifiedDefaultValue = "JSON",
          help = "Indicates the responseType to be used by generated controller. Depending on the selected "
              + "responseType, generated methods and views will vary. "
              + "Possible values are: `JSON` plus any response type installed with `web mvc view setup` "
              + "command. "
              + "This option is available once `--all` or `--entity` parameters have been specified. "
              + "Default: `JSON`.") String responseType,
      @CliOption(
          key = "package",
          mandatory = false,
          optionContext = APPLICATION_FEATURE,
          help = "Indicates which package should be used to include generated controllers. In "
              + "multi-module project you should specify the module name before the package name. "
              + "Ex.: `--package application:org.springframework.roo.web` but, if module name is not "
              + "present, the Roo Shell focused module will be used. "
              + "This option is available only if `--all` or `--entity` option has been specified. "
              + "Default value if not present: `~.web` package, or 'application:~.web' if multi-module "
              + "project.") JavaPackage controllersPackage,
      @CliOption(
          key = "pathPrefix",
          mandatory = false,
          specifiedDefaultValue = "",
          unspecifiedDefaultValue = "",
          help = "Indicates `@RequestMapping` prefix to be used on this controller. It is not necessary "
              + "to specify '/' as Spring Roo shell will include it automatically. "
              + "This option is available only if `--all` or `--entity` option has been specified.") String pathPrefix) {

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
      help = "--field parameter must be an existing @OneToMany or @ManyToMany field of the specified entity in parameter --entity.",
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
      help = "--package, --pathPrefix and --responseType parameters are not visible until --all parameter or --entity parameter has been specified.")
  public boolean areDetailParametersVisibles(ShellContext context) {
    if (context.getParameters().containsKey("all") || context.getParameters().containsKey("entity")) {
      return true;
    }
    return false;
  }

  @CliOptionVisibilityIndicator(params = {"views"}, command = "web mvc detail",
      help = "--views parameter is not visible until a valid --responseType has specified.")
  public boolean isViewParameterAvailable(ShellContext context) {
    if (context.getParameters().containsKey("responseType")) {

      // Get the specified one
      String currentResponseType = context.getParameters().get("responseType");

      // Check the responseType
      Map<String, ControllerMVCResponseService> responseTypeServices =
          getInstalledControllerMVCResponseTypes();
      for (Entry<String, ControllerMVCResponseService> responseTypeService : responseTypeServices
          .entrySet()) {
        if (responseTypeService.getKey().equals(currentResponseType)
            && responseTypeService.getValue().providesViews()) {
          return true;
        }
      }

    }
    return false;
  }

  @CliOptionAutocompleteIndicator(
      command = "web mvc detail",
      param = "views",
      includeSpaceOnFinish = false,
      help = "--views parameter could be autocomplete with a separated comma list including 'list' and 'show'. If --entity parameter "
          + "has been specified, is possible to autocomplete with existing finder views too.")
  public List<String> getAllAvailableViews(ShellContext shellContext) {
    List<String> viewsValuesToReturn = new ArrayList<String>();

    // Get current views in --views value
    String currentViewsValue = shellContext.getParameters().get("views");
    String[] views = StringUtils.split(currentViewsValue, ",");

    // Check for bad written separators and return no options
    if (currentViewsValue.contains(",.") || currentViewsValue.contains(".,")) {
      return viewsValuesToReturn;
    }

    // Check if --entity parameter has been specified
    JavaType currentEntity = getTypeFromEntityParam(shellContext);
    List<String> finderViews = new ArrayList<String>();
    if (currentEntity != null) {
      // Obtain search controllers for the current entity
      Collection<ClassOrInterfaceTypeDetails> searchControllers =
          getControllerLocator().getControllers(currentEntity, ControllerType.SEARCH,
              RooJavaType.ROO_THYMELEAF);
      for (ClassOrInterfaceTypeDetails searchController : searchControllers) {
        SearchAnnotationValues searchAnnotationValues =
            new SearchAnnotationValues(searchController);
        if (searchAnnotationValues.getFinders() != null
            && searchAnnotationValues.getFinders().length > 0) {
          finderViews.addAll(Arrays.asList(searchAnnotationValues.getFinders()));
        }
      }
    }

    // Check if it is first view
    if (currentViewsValue.equals("")) {
      viewsValuesToReturn.add("list");
      viewsValuesToReturn.add("show");
      viewsValuesToReturn.addAll(finderViews);
    } else if (currentViewsValue.endsWith(",")) {
      String finishedViews = "";
      for (int i = 0; i < views.length; i++) {
        finishedViews += views[i] + ",";
      }
      if (!finishedViews.contains("list,")) {
        viewsValuesToReturn.add(finishedViews + "list");
      }
      if (!finishedViews.contains("show,")) {
        viewsValuesToReturn.add(finishedViews + "show");
      }
      for (String finderView : finderViews) {
        if (!finishedViews.contains(finderView + ",")) {
          viewsValuesToReturn.add(finishedViews + finderView);
        }
      }
    } else if (views.length == 1) {
      viewsValuesToReturn.add("list");
      viewsValuesToReturn.add("show");
      viewsValuesToReturn.addAll(finderViews);
    } else {
      String finishedViews = "";
      for (int i = 0; i < views.length - 1; i++) {
        finishedViews += views[i] + ",";
      }
      if (!finishedViews.contains("list,")) {
        viewsValuesToReturn.add(finishedViews + "list");
      }
      if (!finishedViews.contains("show,")) {
        viewsValuesToReturn.add(finishedViews + "show");
      }
      for (String finderView : finderViews) {
        if (!finishedViews.contains(finderView + ",")) {
          viewsValuesToReturn.add(finishedViews + finderView);
        }
      }
    }

    return viewsValuesToReturn;
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
      help = "Generates new `@RooController` for relation fields which detail wants to be managed. "
          + "It must be a `@OneToMany` field. Generated controllers will have `@RooDetail` with info "
          + "about the parent entity and the parent views where the detail will be displayed.")
  public void addDetailController(
      @CliOption(
          key = "all",
          mandatory = false,
          specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false",
          help = "Indicates if developer wants to generate detail controllers for each `@OneToMany` "
              + "relation of field in each entity in the project. "
              + "This option is mandatory if `--entity` is not specified. Otherwise, using `--entity` "
              + "will cause the parameter `--all` won't be available. "
              + "Default if option present: `true`; default if option not present: `false`.") boolean all,
      @CliOption(
          key = "entity",
          mandatory = false,
          help = "Indicates the entity which this detail controller manages. When working on a single "
              + "module project, simply specify the name of the entity. If you consider it necessary, you "
              + "can also specify the package. Ex.: `--class ~.domain.MyEntity` (where `~` is the base "
              + "package). When working with multiple modules, you should specify the name of the entity "
              + "and the module where it is. Ex.: `--class model:~.domain.MyEntity`. If the module is "
              + "not specified, it is assumed that the entity is in the module which has the focus. "
              + "Possible values are: any of the entities in the project. "
              + "This option is mandatory if `--all` is not specified. Otherwise, using `--all` "
              + "will cause the parameter `--entity` won't be available.") JavaType entity,
      @CliOption(
          key = "field",
          mandatory = false,
          specifiedDefaultValue = "",
          unspecifiedDefaultValue = "",
          help = "Indicates the entity's field on which the detail controller is generated. It must be "
              + "a `@OneToMany` field. "
              + "Possible values are: fields representing a `@OneToMany` relation of the entity specified"
              + " in `--entity` parameter. "
              + "This param is only available if `--entity` parameter has been specified before.") String field,
      @CliOption(
          key = "package",
          mandatory = false,
          optionContext = APPLICATION_FEATURE,
          help = "Indicates the Java package where the detail controllers should be generated. In"
              + " multi-module project you should specify the module name before the package name. "
              + "Ex.: `--package application:org.springframework.roo.web` but, if module name is not "
              + "present, the Roo Shell focused module will be used. "
              + "This option is available only if `--all` or `--entity` option has been specified. "
              + "Default if option not present: `~.web` package, or 'application:~.web' if multi-module "
              + "project.") JavaPackage controllersPackage,
      @CliOption(
          key = "responseType",
          mandatory = false,
          unspecifiedDefaultValue = "JSON",
          specifiedDefaultValue = "JSON",
          help = "Indicates the responseType to be used by generated detail controllers. Depending on "
              + "the selected responseType, generated methods and views will vary. "
              + "Possible values are: `JSON` plus any response type installed with `web mvc view setup` "
              + "command. "
              + "This option is available once `--all` or `--entity` parameters have been specified. "
              + "Default: `JSON`.") String responseType,
      @CliOption(
          key = "views",
          mandatory = false,
          specifiedDefaultValue = "list",
          help = "Separated comma list where developer could specify the different parent views where "
              + "this new detail will be displayed. "
              + "This parameter is not available if the provided `--responseType` doesn't use views to display "
              + "the data. "
              + "Possible values are: 'list', 'show' or the different parent finder views (if exists). "
              + "Default if option not present: The parent 'list' view if it exists.") String viewsList) {

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
          responseTypeServices.get(responseType), controllersPackage, viewsList);
    } else {
      getControllerOperations().createOrUpdateDetailControllerForEntity(entity, field,
          responseTypeServices.get(responseType), controllersPackage, viewsList);
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
      AnnotationMetadata manyToManyAnnotation = field.getAnnotation(JpaJavaType.MANY_TO_MANY);
      if ((oneToManyAnnotation != null || manyToManyAnnotation != null)
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

  /**
   * Tries to obtain JavaType indicated in command or which has the focus in the
   * Shell
   *
   * @param shellContext the Roo Shell context
   * @return JavaType or null if no class has the focus or no class is specified
   *         in the command
   */
  private JavaType getTypeFromEntityParam(ShellContext shellContext) {
    // Try to get 'class' from ShellContext
    String typeString = shellContext.getParameters().get("entity");
    JavaType type = null;
    if (typeString != null) {
      JavaTypeConverter converter = (JavaTypeConverter) getJavaTypeConverter().get(0);
      type = converter.convertFromText(typeString, JavaType.class, PROJECT);
    }

    return type;
  }

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

  public ControllerLocator getControllerLocator() {
    return serviceInstaceManager.getServiceInstance(this, ControllerLocator.class);
  }

  @SuppressWarnings("unchecked")
  public List<Converter> getJavaTypeConverter() {
    return serviceInstaceManager.getServiceInstance(this, Converter.class,
        new ServiceInstaceManager.Matcher<Converter>() {
          @Override
          public boolean match(Converter service) {
            if (service instanceof JavaTypeConverter) {
              return true;
            }
            return false;
          }
        });
  }

}
