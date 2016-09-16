package org.springframework.roo.addon.layers.repository.jpa.addon;

import static org.springframework.roo.shell.OptionContexts.PROJECT;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.field.addon.FieldCommands;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.converters.LastUsed;
import org.springframework.roo.model.JavaPackage;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Commands for the JPA repository add-on.
 * 
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @since 1.2.0
 */
@Component
@Service
public class RepositoryJpaCommands implements CommandMarker {

  protected final static Logger LOGGER = HandlerUtils.getLogger(FieldCommands.class);

  //------------ OSGi component attributes ----------------
  private BundleContext context;

  @Reference
  private RepositoryJpaOperations repositoryJpaOperations;
  @Reference
  private ProjectOperations projectOperations;
  @Reference
  private TypeLocationService typeLocationService;
  @Reference
  private LastUsed lastUsed;

  private Converter<JavaType> javaTypeConverter;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  protected void deactivate(final ComponentContext context) {
    this.context = null;
  }

  @CliAvailabilityIndicator({"repository jpa"})
  public boolean isRepositoryCommandAvailable() {
    return repositoryJpaOperations.isRepositoryInstallationPossible();
  }

  @CliOptionAutocompleteIndicator(command = "repository jpa", param = "entity",
      help = "--entity option should be an entity.")
  public List<String> getEntityPossibleResults(ShellContext shellContext) {

    // Get current value of class
    String currentText = shellContext.getParameters().get("entity");

    List<String> allPossibleValues = new ArrayList<String>();

    // Getting all existing entities
    Set<ClassOrInterfaceTypeDetails> entitiesInProject =
        typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_JPA_ENTITY);
    for (ClassOrInterfaceTypeDetails entity : entitiesInProject) {
      String name = replaceTopLevelPackageString(entity, currentText);
      if (!allPossibleValues.contains(name)) {
        allPossibleValues.add(name);
      }
    }

    return allPossibleValues;
  }

  @CliOptionAutocompleteIndicator(command = "repository jpa", param = "interface",
      help = "--interface option should be a new type.", validate = false,
      includeSpaceOnFinish = false)
  public List<String> getInterfacePossibleResults(ShellContext shellContext) {

    List<String> allPossibleValues = new ArrayList<String>();

    // Add all modules to completions list
    Collection<String> modules = projectOperations.getModuleNames();
    for (String module : modules) {
      if (StringUtils.isNotBlank(module)
          && !module.equals(projectOperations.getFocusedModule().getModuleName())) {
        allPossibleValues.add(module.concat(LogicalPath.MODULE_PATH_SEPARATOR).concat("~."));
      }
    }

    // Always add base package
    allPossibleValues.add("~.");

    return allPossibleValues;
  }

  /**
   * This indicator return all Projection classes associated to an entity specified
   * in the 'entity' parameter.
   * 
   * @param shellContext the Roo ShellContext.
   * @return List<String> with fullyQualifiedNames of each associated Projection. 
   */
  @CliOptionAutocompleteIndicator(
      command = "repository jpa",
      param = "defaultReturnType",
      help = "--defaultReturnType option should be a Projection class associated to the entity specified in --entity.")
  public List<String> getAssociatedProjectionResults(ShellContext shellContext) {
    List<String> allPossibleValues = new ArrayList<String>();

    // Get current value of 'defaultReturnType'
    String currentText = shellContext.getParameters().get("defaultReturnType");

    // Get current value of 'entity'
    JavaType entity = getTypeFromEntityParam(shellContext);

    Set<ClassOrInterfaceTypeDetails> projectionsInProject =
        typeLocationService
            .findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_ENTITY_PROJECTION);
    for (ClassOrInterfaceTypeDetails projection : projectionsInProject) {

      // Add only projections associated to the entity specified in the command
      if (projection.getAnnotation(RooJavaType.ROO_ENTITY_PROJECTION).getAttribute("entity")
          .getValue().equals(entity)) {
        String name = replaceTopLevelPackageString(projection, currentText);
        if (!allPossibleValues.contains(name)) {
          allPossibleValues.add(name);
        }
      }
    }

    return allPossibleValues;
  }

  /**
   * This indicator says if --package parameter should be visible or not
   *
   * If --all parameter has not been specified, --package parameter will not be visible
   * to prevent conflicts.
   * 
   * @param context ShellContext
   * @return
   */
  @CliOptionVisibilityIndicator(
      params = "defaultReturnType",
      command = "repository jpa",
      help = "--defaultReturnType parameter is not visible if domain entity specified in --entity parameter has no associated Projections.")
  public boolean isDefaultReturnTypeParameterVisible(ShellContext shellContext) {

    // Get current value of 'entity'
    JavaType entity = getTypeFromEntityParam(shellContext);
    if (entity == null) {
      return false;
    }

    Set<ClassOrInterfaceTypeDetails> projectionsInProject =
        typeLocationService
            .findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_ENTITY_PROJECTION);
    boolean visible = false;
    for (ClassOrInterfaceTypeDetails projection : projectionsInProject) {

      // Add only projections associated to the entity specified in the command
      if (projection.getAnnotation(RooJavaType.ROO_ENTITY_PROJECTION).getAttribute("entity")
          .getValue().equals(entity)) {
        visible = true;
      }
    }

    return visible;
  }

  /**
   * This indicator says if --package parameter should be mandatory or not
   *
   * If --all parameter has been specified, --package parameter will be mandatory.
   * 
   * @param context ShellContext
   * @return
   */
  @CliOptionMandatoryIndicator(params = "package", command = "repository jpa")
  public boolean isPackageParameterMandatory(ShellContext context) {
    if (context.getParameters().containsKey("all")) {
      return true;
    }
    return false;
  }

  /**
   * This indicator says if --interface parameter should be mandatory or not
   *
   * If --entity parameter has been specified and we are working under multimodule
   * project, --interface parameter will be mandatory.
   * 
   * @param context ShellContext
   * @return
   */
  @CliOptionMandatoryIndicator(params = "interface", command = "repository jpa")
  public boolean isInterfaceParameterMandatory(ShellContext context) {
    if (context.getParameters().containsKey("entity") && projectOperations.isMultimoduleProject()) {
      return true;
    }
    return false;
  }

  /**
   * This indicator says if --package parameter should be visible or not
   *
   * If --all parameter has not been specified, --package parameter will not be visible
   * to prevent conflicts.
   * 
   * @param context ShellContext
   * @return
   */
  @CliOptionVisibilityIndicator(params = "package", command = "repository jpa",
      help = "--package parameter is not visible if --all parameter has not been specified before.")
  public boolean isPackageParameterVisible(ShellContext context) {
    if (context.getParameters().containsKey("all")) {
      return true;
    }
    return false;
  }

  /**
   * This indicator says if --all parameter should be visible or not
   *
   * If --entity parameter has been specified, --all parameter will not be visible
   * to prevent conflicts.
   * 
   * @param context ShellContext
   * @return
   */
  @CliOptionVisibilityIndicator(params = "all", command = "repository jpa",
      help = "--all parameter is not be visible if --entity parameter has been specified before.")
  public boolean isAllParameterVisible(ShellContext context) {
    if (context.getParameters().containsKey("entity")) {
      return false;
    }
    return true;
  }

  /**
   * This indicator says if --interface and --defaultSearchResult parameter are visible.
   * 
   * If --entity is specified, --interface and --defaultSearchResult will be visible
   * 
   * @param context ShellContext
   * @return
   */
  @CliOptionVisibilityIndicator(
      params = {"interface", "defaultSearchResult"},
      command = "repository jpa",
      help = "--interface or --defaultSearchResult parameters are not be visible if --entity parameter hasn't been specified before.")
  public boolean areInterfaceAndSearchResultVisible(ShellContext context) {
    if (context.getParameters().containsKey("entity")) {
      return true;
    }
    return false;
  }

  /**
   * This indicator says if --entity parameter is visible.
   * 
   * If --all is specified, --entity won't be visible
   * 
   * @param context ShellContext
   * @return
   */
  @CliOptionVisibilityIndicator(params = "entity", command = "repository jpa",
      help = "--entity parameter is not be visible --all parameter has been specified before.")
  public boolean isEntityParameterVisible(ShellContext context) {
    if (context.getParameters().containsKey("all")) {
      return false;
    }
    return true;
  }

  @CliCommand(value = "repository jpa",
      help = "Generates new Spring Data repository for specified entity.")
  public void repository(
      @CliOption(
          key = "all",
          mandatory = false,
          specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false",
          help = "Indicates if developer wants to generate repositories for every entity of current project ") boolean all,
      @CliOption(key = "interface", mandatory = true,
          help = "The java Spring Data repository to generate.") final JavaType interfaceType,
      @CliOption(key = "entity", mandatory = false, optionContext = PROJECT,
          help = "The domain entity this repository should expose") final JavaType domainType,
      @CliOption(key = "defaultReturnType", mandatory = false,
          help = "The findAll finder return type. Should be a Projection class associated "
              + "to the entity specified in '--entity'.") JavaType defaultReturnType,
      @CliOption(key = "package", mandatory = true,
          help = "The package where repositories will be generated") final JavaPackage repositoriesPackage) {

    if (all) {
      repositoryJpaOperations.generateAllRepositories(repositoriesPackage);
    } else {
      repositoryJpaOperations.addRepository(interfaceType, domainType, defaultReturnType);
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
        && !cid.getType().getModule().equals(projectOperations.getFocusedModuleName())) {

      // Target module is not focused
      javaTypeString = cid.getType().getModule().concat(LogicalPath.MODULE_PATH_SEPARATOR);
      topLevelPackageString =
          projectOperations.getTopLevelPackage(cid.getType().getModule())
              .getFullyQualifiedPackageName();
    } else if (StringUtils.isNotBlank(cid.getType().getModule())
        && cid.getType().getModule().equals(projectOperations.getFocusedModuleName())
        && (currentText.startsWith(cid.getType().getModule()) || cid.getType().getModule()
            .startsWith(currentText)) && StringUtils.isNotBlank(currentText)) {

      // Target module is focused but user wrote it
      javaTypeString = cid.getType().getModule().concat(LogicalPath.MODULE_PATH_SEPARATOR);
      topLevelPackageString =
          projectOperations.getTopLevelPackage(cid.getType().getModule())
              .getFullyQualifiedPackageName();
    } else {

      // Not multimodule project
      topLevelPackageString =
          projectOperations.getFocusedTopLevelPackage().getFullyQualifiedPackageName();
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
    // Try to get 'class' from ShellContext
    String typeString = shellContext.getParameters().get("entity");
    JavaType type = null;
    if (typeString != null) {
      type = getJavaTypeConverter().convertFromText(typeString, JavaType.class, PROJECT);
    } else {
      type = lastUsed.getJavaType();
    }

    return type;
  }

  @SuppressWarnings("unchecked")
  public Converter<JavaType> getJavaTypeConverter() {
    if (javaTypeConverter == null) {

      // Get all Services implement JavaTypeConverter interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(Converter.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          Converter<?> converter = (Converter<?>) this.context.getService(ref);
          if (converter.supports(JavaType.class, PROJECT)) {
            javaTypeConverter = (Converter<JavaType>) converter;
            return javaTypeConverter;
          }
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("ERROR: Cannot load JavaTypeConverter on FieldCommands.");
        return null;
      }
    } else {
      return javaTypeConverter;
    }
  }
}
