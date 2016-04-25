package org.springframework.roo.addon.layers.repository.jpa.addon;

import static org.springframework.roo.shell.OptionContexts.PROJECT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

  @CliAvailabilityIndicator({"repository jpa add", "repository jpa all"})
  public boolean isRepositoryCommandAvailable() {
    return repositoryJpaOperations.isRepositoryInstallationPossible();
  }

  @CliCommand(value = "repository jpa all",
      help = "Generates new Spring Data repository for all entities.")
  public void all(
      @CliOption(key = "package", mandatory = true,
          help = "The package where repositories will be generated") final JavaPackage repositoriesPackage) {

    repositoryJpaOperations.generateAllRepositories(repositoriesPackage);
  }

  @CliOptionVisibilityIndicator(params = {"interface"}, command = "repository jpa add",
      help = "You should specify --entity param to be able to specify repository name")
  public boolean isClassVisible(ShellContext shellContext) {
    // Get all defined parameters
    Map<String, String> parameters = shellContext.getParameters();

    // If --entity has been defined, show --class parameter
    if (parameters.containsKey("entity") && StringUtils.isNotBlank(parameters.get("entity"))) {
      return true;
    }

    return false;
  }

  @CliOptionAutocompleteIndicator(command = "repository jpa add", param = "entity",
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


  @CliOptionAutocompleteIndicator(command = "repository jpa add", param = "defaultSearchResult",
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

  @CliCommand(value = "repository jpa add",
      help = "Generates new Spring Data repository for specified entity.")
  public void repository(
      @CliOption(key = "interface", mandatory = true,
          help = "The java Spring Data repository to generate.") final JavaType interfaceType,
      @CliOption(key = "entity", mandatory = true, unspecifiedDefaultValue = "*",
          optionContext = PROJECT, help = "The domain entity this repository should expose") final JavaType domainType,
      @CliOption(key = "defaultSearchResult", mandatory = false, optionContext = PROJECT,
          help = "The findAll finder return type. Should be a DTO class.") JavaType defaultSearchResult) {

    repositoryJpaOperations.addRepository(interfaceType, domainType, defaultSearchResult);
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
