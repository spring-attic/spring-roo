package org.springframework.roo.addon.layers.repository.jpa.addon.finder;

import static org.springframework.roo.shell.OptionContexts.PROJECT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.layers.repository.jpa.addon.RepositoryJpaLocator;
import org.springframework.roo.addon.layers.repository.jpa.addon.RepositoryJpaMetadata;
import org.springframework.roo.addon.layers.repository.jpa.addon.finder.parser.FinderAutocomplete;
import org.springframework.roo.addon.layers.repository.jpa.addon.finder.parser.PartTree;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.converters.JavaTypeConverter;
import org.springframework.roo.converters.LastUsed;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectOperations;
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
 * Commands for the 'finder' add-on to be used by the ROO shell.
 *
 * These commands allow developers to generate Spring Data finders
 * on existing repositories.
 *
 * @author Stefan Schmidt
 * @author Paula Navarro
 * @author Juan Carlos García
 * @author Sergio Clares
 * @since 1.0
 */
@Component
@Service
public class FinderCommands implements CommandMarker, FinderAutocomplete {

  private static final Logger LOGGER = HandlerUtils.getLogger(FinderCommands.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();

  @Reference
  private FinderOperations finderOperations;
  @Reference
  private LastUsed lastUsed;

  // Map where entity details will be cached
  private Map<JavaType, MemberDetails> entitiesDetails;


  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
    this.entitiesDetails = new HashMap<JavaType, MemberDetails>();
    serviceInstaceManager.activate(this.context);
  }

  @CliAvailabilityIndicator({"finder add"})
  public boolean isFinderCommandAvailable() {
    return finderOperations.isFinderInstallationPossible();
  }

  @CliOptionVisibilityIndicator(command = "finder add", params = {"name"},
      help = "You must define --entity to be able to define --name parameter.")
  public boolean isNameVisible(ShellContext shellContext) {

    // Getting all defined parameters on autocompleted command
    Map<String, String> params = shellContext.getParameters();

    // If mandatory parameter entity is not defined, name parameter should not
    // be visible
    String entity = params.get("entity");
    if (StringUtils.isBlank(entity)) {
      return false;
    }

    // Get current entity member details to check if is a valid Spring Roo entity
    MemberDetails entityDetails = getEntityDetails(entity);

    // If not entity details, is not a valid entity, so --name parameter is not visible
    if (entityDetails == null) {
      return false;
    }

    return true;
  }

  /**
   * This indicator says if --returnType parameter should be visible or not.
   *
   * @param context ShellContext
   * @return false if domain entity specified in --entity parameter has no associated Projections.
   */
  @CliOptionVisibilityIndicator(params = "returnType", command = "finder add",
      help = "--returnType parameter is not visible if --entity parameter hasn't "
          + "been specified before or if there aren't exist any Projection class associated "
          + "to the current entity.")
  public boolean isReturnTypeParameterVisible(ShellContext shellContext) {

    // Get current value of 'entity'
    JavaType entity = getTypeFromEntityParam(shellContext);
    if (entity == null) {
      return false;
    }

    Set<ClassOrInterfaceTypeDetails> projectionsInProject =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_ENTITY_PROJECTION);
    for (ClassOrInterfaceTypeDetails projection : projectionsInProject) {

      // Add only projections associated to the entity specified in the command
      if (projection.getAnnotation(RooJavaType.ROO_ENTITY_PROJECTION).getAttribute("entity")
          .getValue().equals(entity)) {
        return true;
      }
    }

    return false;
  }

  @CliOptionAutocompleteIndicator(command = "finder add", includeSpaceOnFinish = false,
      param = "name",
      help = "--name parameter must follow Spring Data nomenclature. Please, write a "
          + "valid value using autocomplete feature (TAB or CTRL + Space)")
  public List<String> returnOptions(ShellContext shellContext) {

    List<String> allPossibleValues = new ArrayList<String>();

    // Getting all defined parameters on autocompleted command
    Map<String, String> contextParameters = shellContext.getParameters();

    // Getting current name value
    String name = contextParameters.get("name");

    try {

      // Use PartTree class to obtain all possible values
      PartTree part = new PartTree(name, getEntityDetails(contextParameters.get("entity")), this);

      // Check if part has value
      if (part != null) {
        allPossibleValues = part.getOptions();
      }

      return allPossibleValues;

    } catch (Exception e) {
      LOGGER.warning(e.getLocalizedMessage());
      return allPossibleValues;
    }

  }

  @CliOptionAutocompleteIndicator(command = "finder add", includeSpaceOnFinish = false,
      param = "entity", help = "--entity option should be an entity.")
  public List<String> getClassPossibleResults(ShellContext shellContext) {

    // ROO-3763: Clear current cache during --entity autocompletion.
    // With that, Spring Roo will maintain cache during --name autocompletion
    // but if --entity is autocomplete, cache should be refreshed to obtain
    // last changes on entities
    entitiesDetails = new HashMap<JavaType, MemberDetails>();

    // Get current value of entity
    String currentText = shellContext.getParameters().get("entity");

    List<String> allPossibleValues = new ArrayList<String>();

    Set<ClassOrInterfaceTypeDetails> entities =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_JPA_ENTITY);
    for (ClassOrInterfaceTypeDetails entity : entities) {
      String name = replaceTopLevelPackageString(entity, currentText);
      if (!allPossibleValues.contains(name)) {
        allPossibleValues.add(name);
      }
    }

    return allPossibleValues;
  }

  @CliOptionAutocompleteIndicator(
      command = "finder add",
      includeSpaceOnFinish = false,
      param = "returnType",
      help = "--returnType option could be a Projection class related with the specified entity or the same entity. "
          + "By default, uses the value of the 'defaultReturnType' parameter specified during the Jpa Repository creation.")
  public List<String> getReturnTypePossibleResults(ShellContext shellContext) {

    // Get current value of returnType
    String currentText = shellContext.getParameters().get("returnType");

    List<String> allPossibleValues = new ArrayList<String>();

    // Get current value of 'entity'
    JavaType entity = getTypeFromEntityParam(shellContext);

    Set<ClassOrInterfaceTypeDetails> entityProjections =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_ENTITY_PROJECTION);
    for (ClassOrInterfaceTypeDetails projection : entityProjections) {
      if (entity != null
          && projection.getAnnotation(RooJavaType.ROO_ENTITY_PROJECTION).getAttribute("entity")
              .getValue().equals(entity)) {
        String name = replaceTopLevelPackageString(projection, currentText);
        if (!allPossibleValues.contains(name)) {
          allPossibleValues.add(name);
        }
      }
    }

    // Always include the entity name, because if the defaultReturnType of the repository
    // is a projection, the developer should be able to specify an entity as return type.
    String entityName =
        replaceTopLevelPackageString(getTypeLocationService().getTypeDetails(entity), currentText);
    if (!allPossibleValues.contains(entityName)) {
      allPossibleValues.add(entityName);
    }

    return allPossibleValues;
  }

  @CliOptionAutocompleteIndicator(command = "finder add", includeSpaceOnFinish = false,
      param = "formBean", help = "--formBean option should be a DTO.")
  public List<String> getFormBeanPossibleResults(ShellContext shellContext) {

    // Get current value of entity
    String currentText = shellContext.getParameters().get("formBean");

    List<String> allPossibleValues = new ArrayList<String>();

    Set<ClassOrInterfaceTypeDetails> dtosInProject =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_DTO);
    for (ClassOrInterfaceTypeDetails dto : dtosInProject) {
      String name = replaceTopLevelPackageString(dto, currentText);
      if (!allPossibleValues.contains(name)) {
        allPossibleValues.add(name);
      }
    }

    return allPossibleValues;
  }

  /**
   * Indicates if 'formBean' option should be visible. It checks the existence of
   * DTO's in project.
   *
   * @param shellContext
   * @return true if project contains at least one DTO, false otherwise.
   */
  @CliOptionVisibilityIndicator(command = "finder add",
      help = "--formBean parameter is not visible if --entity parameter hasn't been specified "
          + "before or if there aren't exist any DTO in generated project", params = {"formBean"})
  public boolean isFormBeanVisible(ShellContext shellContext) {
    Set<ClassOrInterfaceTypeDetails> dtosInProject =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_DTO);
    if (dtosInProject.isEmpty()) {
      return false;
    }
    return true;
  }

  @CliOptionMandatoryIndicator(command = "finder add", params = {"formBean"})
  public boolean isFormBeanMandatory(ShellContext shellContext) {

    // Get current value of 'entity'
    JavaType entity = getTypeFromEntityParam(shellContext);
    if (entity == null) {
      return false;
    }

    // Get current value of returnType
    String returnType = shellContext.getParameters().get("returnType");

    // This parameter is not mandatory if returnType has not been specified
    if (StringUtils.isBlank(returnType)) {
      return false;
    }

    // If returnType has been specified, but it's an entity, this parameter is not
    // mandatory
    JavaTypeConverter converter = (JavaTypeConverter) getJavaTypeConverter().get(0);
    JavaType type = converter.convertFromText(returnType, JavaType.class, PROJECT);
    if (type == null) {
      return false;
    }
    ClassOrInterfaceTypeDetails details = getTypeLocationService().getTypeDetails(type);
    if (details == null) {
      return false;
    }
    AnnotationMetadata entityAnnotation = details.getAnnotation(RooJavaType.ROO_JPA_ENTITY);
    if (entityAnnotation != null) {
      return false;
    }

    return true;

  }

  @CliCommand(
      value = "finder add",
      help = "Installs a finder in the given target (must be an entity). This command needs an existing "
          + "repository for the target entity, you can create it with `repository jpa` command. The "
          + "finder will be added to targeted entity associated repository and associated service if "
          + "exists or when it will be created.")
  public void installFinders(
      @CliOption(
          key = "entity",
          mandatory = true,
          unspecifiedDefaultValue = "*",
          optionContext = PROJECT,
          help = "The entity for which the finders are generated. When working on a mono module project, "
              + "simply specify the name of the entity. If you consider it necessary, you can also "
              + "specify the package. Ex.: `--class ~.domain.MyEntity` (where `~` is the base package). "
              + "When working with multiple modules, you should specify the name of the class and the "
              + "module where it is. Ex.: `--class model:~.domain.MyEntity`. If the module is not "
              + "specified, it is assumed that the entity is in the module which has the focus.") final JavaType entity,
      @CliOption(
          key = "name",
          mandatory = true,
          help = "The finder string defined as a Spring Data query. Use Spring Data JPA nomenclature. "
              + "Possible values are: any finder name following Spring Data nomenclature. "
              + "This option will not be available until `--entity` is specified.") final JavaSymbolName finderName,
      @CliOption(
          key = "formBean",
          mandatory = true,
          help = "The finder's search parameter. Should be a DTO and it must have at least same fields "
              + "(name and type) as those included in the finder `--name`, which can be target entity"
              + " fields or related entity fields. "
              + "Possible values are: any of the DTO's in the project. "
              + "This option is mandatory if `--returnType` is specified and its a projection. "
              + "This option is not available if `--entity` parameter has not been specified before or "
              + "if it does not exist any DTO in generated project. "
              + "Default if option not present: the entity specified in `--entity` option.") final JavaType formBean,
      @CliOption(
          key = "returnType",
          mandatory = false,
          optionContext = PROJECT,
          help = "The finder's results return type. "
              + "Possible values are: Projection classes annotated with `@RooEntityProjection` and "
              + "related to the entity specified in `--entity` option (use `entity projection` command), "
              + "or the same entity. "
              + "This option is not available if `--entity` parameter has not been specified before or "
              + "if it does not exist any Projection class associated to the targeted entity. "
              + "Default if not present: the default return type of the repository related to the entity, "
              + "which can be specified with `--defaultReturnType` parameter in `repository jpa` command.") JavaType returnType) {

    // Check if specified finderName follows Spring Data nomenclature
    PartTree partTree = new PartTree(finderName.getSymbolName(), getEntityDetails(entity), this);

    // If generated partTree is not valid, shows an exception
    Validate
        .isTrue(
            partTree.isValid(),
            "--name parameter must follow Spring Data nomenclature. Please, write a valid value using autocomplete feature (TAB or CTRL + Space)");

    // If returnType has not been specified, use the specified "defaultReturnType" from the
    // related repository
    if (returnType == null) {
      // Obtain the related repository metadata
      RepositoryJpaMetadata repositoryMetadata =
          getRepositoryJpaLocator().getFirstRepositoryMetadata(entity);
      Validate
          .notNull(repositoryMetadata,
              "ERROR: You must create a repository related with this entity before to generate a finder");

      // Use the repository metadata to obtain the default return type
      returnType = repositoryMetadata.getDefaultReturnType();
    }

    Validate.notNull(returnType, "ERROR: The new finder must define a returnType");

    // Check if the returnType is an entity. If is is not an entity validate 
    // that the formBean is not null
    ClassOrInterfaceTypeDetails type = getTypeLocationService().getTypeDetails(returnType);
    if (type.getAnnotation(RooJavaType.ROO_JPA_ENTITY) == null) {
      Validate.notNull(formBean,
          "--formBean is requied when --returnType parameter is a projection.");
    }

    finderOperations.installFinder(entity, finderName, formBean, returnType);

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
   * Tries to obtain JavaType indicated in command or which has the focus
   * in the Shell
   *
   * @param shellContext the Roo Shell context
   * @return JavaType or null if no class has the focus or no class is
   * specified in the command
   */
  private JavaType getTypeFromEntityParam(ShellContext shellContext) {
    // Try to get 'entity' from ShellContext
    String typeString = shellContext.getParameters().get("entity");
    JavaType type = null;
    if (typeString != null) {
      JavaTypeConverter converter = (JavaTypeConverter) getJavaTypeConverter().get(0);
      type = converter.convertFromText(typeString, JavaType.class, PROJECT);
    } else {
      type = lastUsed.getJavaType();
    }

    return type;
  }

  /**
   * Method that obtains entity details from entity name
   *
   * @param entityName
   * @return MemberDetails
   */
  public MemberDetails getEntityDetails(String entityName) {

    String moduleName = getProjectOperations().getFocusedModuleName();

    if (entityName.contains(":")) {
      moduleName = StringUtils.split(entityName, ":")[0];
      entityName = StringUtils.split(entityName, ":")[1];
    }

    // Getting JavaType for entityName
    // Check first if contains base package (~)
    if (entityName.contains("~")) {
      entityName =
          entityName.replace("~", getProjectOperations().getTopLevelPackage(moduleName)
              .getFullyQualifiedPackageName());
    }
    JavaType entityType = new JavaType(entityName, moduleName);

    return getEntityDetails(entityType);

  }

  @Override
  public MemberDetails getEntityDetails(JavaType entity) {

    Validate.notNull(entity, "ERROR: Entity should be provided");

    if (entitiesDetails.containsKey(entity)) {
      return entitiesDetails.get(entity);
    }

    // We know the file exists, as there's already entity metadata for it
    final ClassOrInterfaceTypeDetails cid = getTypeLocationService().getTypeDetails(entity);

    if (cid == null) {
      return null;
    }

    if (cid.getAnnotation(RooJavaType.ROO_JPA_ENTITY) == null) {
      LOGGER.warning("Unable to find the entity annotation on '" + entity + "'");
      return null;
    }

    entitiesDetails.put(entity,
        getMemberDetailsScanner().getMemberDetails(getClass().getName(), cid));
    return entitiesDetails.get(entity);
  }

  public ProjectOperations getProjectOperations() {
    return serviceInstaceManager.getServiceInstance(this, ProjectOperations.class);
  }

  public TypeLocationService getTypeLocationService() {
    return serviceInstaceManager.getServiceInstance(this, TypeLocationService.class);
  }

  public TypeManagementService getTypeManagementService() {
    return serviceInstaceManager.getServiceInstance(this, TypeManagementService.class);
  }


  public MemberDetailsScanner getMemberDetailsScanner() {
    return serviceInstaceManager.getServiceInstance(this, MemberDetailsScanner.class);
  }

  public RepositoryJpaLocator getRepositoryJpaLocator() {
    return serviceInstaceManager.getServiceInstance(this, RepositoryJpaLocator.class);
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
