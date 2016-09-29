package org.springframework.roo.addon.web.mvc.controller.addon.finder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.web.mvc.controller.addon.responses.ControllerMVCResponseService;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.NestedAnnotationAttributeValue;
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
import org.springframework.roo.shell.CliOptionVisibilityIndicator;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.ShellContext;
import org.springframework.roo.support.logging.HandlerUtils;

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

  private Map<String, ControllerMVCResponseService> responseTypes =
      new HashMap<String, ControllerMVCResponseService>();
  private Converter<JavaType> javaTypeConverter;
  private TypeLocationService typeLocationService;
  private ProjectOperations projectOperations;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
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
    Set<ClassOrInterfaceTypeDetails> repositoryTypes =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_REPOSITORY_JPA, RooJavaType.ROO_FINDERS);
    for (JavaType entity : getTypeLocationService().findTypesWithAnnotation(
        RooJavaType.ROO_JPA_ENTITY)) {
      for (ClassOrInterfaceTypeDetails repository : repositoryTypes) {
        if (repository.getAnnotation(RooJavaType.ROO_REPOSITORY_JPA).getAttribute("entity")
            .getValue().equals(entity)) {
          ClassOrInterfaceTypeDetails entityDetails =
              getTypeLocationService().getTypeDetails(entity);
          String name = replaceTopLevelPackageString(entityDetails, currentText);
          if (!results.contains(name)) {
            results.add(name);
          }
        }
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
      JavaType entity =
          getJavaTypeConverter().convertFromText(context.getParameters().get("entity"),
              JavaType.class, "");

      // Get finders
      finders = getFinders(entity);
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
  @CliCommand(value = "web mvc finder",
      help = "Adds @RooWebFinder annotation to MVC controller type")
  public void addController(
      @CliOption(key = "entity", mandatory = false,
          help = "The entity owning the finders that should be published.") JavaType entity,
      @CliOption(
          key = "all",
          mandatory = false,
          specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false",
          help = "Indicates if developer wants to publish in web layer all finders from all entities in project.") boolean all,
      @CliOption(key = "queryMethod", mandatory = false,
          help = "Indicates the name of the finder to add to web layer.") String queryMethod,
      @CliOption(
          key = "responseType",
          mandatory = false,
          help = "Indicates the responseType to be used by generated controller. Depending of the selected responseType, generated methods and views will vary.") String responseType,
      @CliOption(key = "package", mandatory = false, unspecifiedDefaultValue = "~.web",
          help = "Indicates the package where generated controller will be located.") JavaPackage controllerPackage,
      @CliOption(
          key = "pathPrefix",
          mandatory = false,
          unspecifiedDefaultValue = "",
          specifiedDefaultValue = "",
          help = "Indicates the default path value for accesing finder resources in controller, excluding first '/'.") String pathPrefix) {

    // Getting --responseType service
    Map<String, ControllerMVCResponseService> responseTypeServices =
        getInstalledControllerMVCResponseTypes();

    // Validate that provided responseType is a valid provided
    ControllerMVCResponseService controllerResponseType = null;
    if (responseType != null) {
      if (!responseTypeServices.containsKey(responseType)) {
        LOGGER
            .log(
                Level.SEVERE,
                "ERROR: Provided responseType is not valid. Use autocomplete feature to obtain valid responseTypes.");
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
        queryMethods = getFinders(entity);
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
   * Get all finder names associated to an entity
   * 
   * @param entity the JavaType representing the entity whose finder names should 
   *            be returned.
   * @return a List<String> with the finder names.
   */
  private List<String> getFinders(JavaType entity) {
    List<String> finders = new ArrayList<String>();
    AnnotationMetadata findersAnnotation = null;
    JavaType associatedRepository = null;

    // Get repository related to controller entity
    for (ClassOrInterfaceTypeDetails repository : getTypeLocationService()
        .findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_REPOSITORY_JPA)) {

      AnnotationAttributeValue<JavaType> entityAttribute =
          repository.getAnnotation(RooJavaType.ROO_REPOSITORY_JPA).getAttribute("entity");
      if (entityAttribute != null && entityAttribute.getValue().equals(entity)) {
        associatedRepository = repository.getType();
        findersAnnotation = repository.getAnnotation(RooJavaType.ROO_FINDERS);
        break;
      }
    }

    if (associatedRepository == null) {
      LOGGER
          .log(
              Level.SEVERE,
              String
                  .format(
                      "ERROR: Entity %s does not have a repository generated. Use 'repository jpa' command to solve this.",
                      entity.getSimpleTypeName()));
      return finders;

    }
    if (findersAnnotation == null) {
      LOGGER
          .log(
              Level.SEVERE,
              String
                  .format(
                      "ERROR: Repository % does not have any finder generated. Use 'finder add' command to solve this.",
                      associatedRepository.getSimpleTypeName()));
      return finders;

    }
    AnnotationAttributeValue<JavaType> managedFinders = findersAnnotation.getAttribute("finders");

    if (managedFinders != null) {
      List<?> values = (List<?>) managedFinders.getValue();
      Iterator<?> it = values.iterator();

      while (it.hasNext()) {
        NestedAnnotationAttributeValue finder = (NestedAnnotationAttributeValue) it.next();
        if (finder.getValue() != null && finder.getValue().getAttribute("finder") != null) {
          finders.add((String) finder.getValue().getAttribute("finder").getValue());
        }
      }
    }
    return finders;
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
        LOGGER.warning("Cannot load Converter<JavaType> on WebFinderCommands.");
        return null;
      }
    } else {
      return javaTypeConverter;
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
        LOGGER.warning("Cannot load TypeLocationService on ControllerCommands.");
        return null;
      }
    } else {
      return typeLocationService;
    }
  }

}
