package org.springframework.roo.addon.layers.repository.jpa.addon;

import static org.springframework.roo.shell.OptionContexts.PROJECT;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
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
import org.springframework.roo.shell.ShellContext;

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

  @Reference
  private RepositoryJpaOperations repositoryJpaOperations;
  @Reference
  private ProjectOperations projectOperations;
  @Reference
  private TypeLocationService typeLocationService;

  @CliAvailabilityIndicator({"repository jpa"})
  public boolean isRepositoryCommandAvailable() {
    return repositoryJpaOperations.isRepositoryInstallationPossible();
  }

  @CliOptionAutocompleteIndicator(command = "repository jpa", param = "entity",
      help = "--entity option should be an entity.")
  public List<String> getClassPossibleResults(ShellContext shellContext) {

    // Get current value of class
    String currentText = shellContext.getParameters().get("entity");

    List<String> allPossibleValues = new ArrayList<String>();

    Set<ClassOrInterfaceTypeDetails> dtosInProject =
        typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_JPA_ENTITY);
    for (ClassOrInterfaceTypeDetails dto : dtosInProject) {
      String name = replaceTopLevelPackageString(dto, currentText);
      if (!allPossibleValues.contains(name)) {
        allPossibleValues.add(name);
      }
    }

    return allPossibleValues;
  }

  @CliOptionAutocompleteIndicator(command = "repository jpa", param = "defaultSearchResult",
      help = "--defaultSearchResult option should be a DTO class.")
  public List<String> getDTOResults(ShellContext shellContext) {

    // Get current value of class
    String currentText = shellContext.getParameters().get("defaultSearchResult");

    List<String> allPossibleValues = new ArrayList<String>();

    Set<ClassOrInterfaceTypeDetails> dtosInProject =
        typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_DTO);
    for (ClassOrInterfaceTypeDetails dto : dtosInProject) {
      String name = replaceTopLevelPackageString(dto, currentText);
      if (!allPossibleValues.contains(name)) {
        allPossibleValues.add(name);
      }
    }

    if (allPossibleValues.isEmpty()) {
      allPossibleValues.add("");
    }

    return allPossibleValues;
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
   * This indicator says if --package parameter should be visible or not
   *
   * If --all parameter has not been specified, --package parameter will not be visible
   * to prevent conflicts.
   * 
   * @param context ShellContext
   * @return
   */
  @CliOptionVisibilityIndicator(
      params = "package",
      command = "repository jpa",
      help = "--package parameter is not be visible if --all parameter has not been specified before.")
  public boolean isPackageParameterVisible(ShellContext context) {
    if (context.getParameters().containsKey("all")) {
      return true;
    }
    return false;
  }

  /**
   * This indicator says if --all parameter should be visible or not
   *
   * If --interface parameter has been specified, --all parameter will not be visible
   * to prevent conflicts.
   * 
   * @param context ShellContext
   * @return
   */
  @CliOptionVisibilityIndicator(
      params = "all",
      command = "repository jpa",
      help = "--all parameter is not be visible if --interface parameter has been specified before.")
  public boolean isAllParameterVisible(ShellContext context) {
    if (context.getParameters().containsKey("interface")) {
      return false;
    }
    return true;
  }

  /**
   * This indicator says if --entity and --defaultSearchResult parameter are visible.
   * 
   * If --interface is specified, --entity and --defaultSearchResult will be visible
   * 
   * @param context ShellContext
   * @return
   */
  @CliOptionVisibilityIndicator(
      params = {"entity", "defaultSearchResult"},
      command = "repository jpa",
      help = "--entity or --defaultSearchResult parameters are not be visible --entity parameter hasn't been specified before.")
  public boolean areEntityAndSearchResultVisible(ShellContext context) {
    if (context.getParameters().containsKey("interface")) {
      return true;
    }
    return false;
  }

  /**
   * This indicator says if --interface parameter is visible.
   * 
   * If --all is specified, --interface won't be visible
   * 
   * @param context ShellContext
   * @return
   */
  @CliOptionVisibilityIndicator(params = "interface", command = "repository jpa",
      help = "--interface parameter is not be visible --all parameter has been specified before.")
  public boolean isInterfaceParameterVisible(ShellContext context) {
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
      @CliOption(key = "interface", mandatory = false,
          help = "The java Spring Data repository to generate.") final JavaType interfaceType,
      @CliOption(key = "entity", mandatory = false, optionContext = PROJECT,
          help = "The domain entity this repository should expose") final JavaType domainType,
      @CliOption(key = "defaultSearchResult", mandatory = false, optionContext = PROJECT,
          help = "The findAll finder return type. Should be a DTO class.") JavaType defaultSearchResult,
      @CliOption(key = "package", mandatory = true,
          help = "The package where repositories will be generated") final JavaPackage repositoriesPackage) {

    if (all) {
      repositoryJpaOperations.generateAllRepositories(repositoriesPackage);
    } else {
      repositoryJpaOperations.addRepository(interfaceType, domainType, defaultSearchResult);
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
}
