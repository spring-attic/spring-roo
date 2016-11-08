package org.springframework.roo.addon.webflow;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.GenericEntity;

/**
 * Commands for the 'web flow' add-on to be used by the Roo shell.
 * 
 * @author Stefan Schmidt
 * @author Sergio Clares
 * 
 * @since 1.0
 */
@Component
@Service
public class WebFlowCommands implements CommandMarker {

  @Reference
  private WebFlowOperations webFlowOperations;

  @Reference
  private ProjectOperations projectOperations;

  @Reference
  private TypeLocationService typeLocationService;

  @CliAvailabilityIndicator("web flow")
  public boolean isInstallWebFlowAvailable() {
    return webFlowOperations.isWebFlowInstallationPossible();
  }

  @CliOptionAutocompleteIndicator(command = "web flow", param = "module",
      help = "You should specify an existing module name for option --module.")
  public List<String> getModulePossibleValues(ShellContext shellContext) {
    List<String> possibleValues = new ArrayList<String>();
    possibleValues.addAll(projectOperations.getModuleNames());
    return possibleValues;
  }

  @CliOptionVisibilityIndicator(command = "web flow", params = {"module"},
      help = "--module option is not visible when the project is a multimodule project.")
  public boolean isModuleVisible(ShellContext shellContext) {
    if (projectOperations.isMultimoduleProject()) {
      return true;
    }
    return false;
  }

  @CliOptionAutocompleteIndicator(command = "web flow", param = "class",
      help = "You should specify an existing class name for option '--class'.")
  public List<String> getClassPossibleValues(ShellContext shellContext) {

    // Get current value of class
    String currentText = shellContext.getParameters().get("class");
    List<String> allPossibleValues = new ArrayList<String>();

    // Add all modules to completions list
    Collection<String> modules = projectOperations.getModuleNames();
    for (String module : modules) {
      if (StringUtils.isNotBlank(module)
          && !module.equals(projectOperations.getFocusedModule().getModuleName())) {
        allPossibleValues.add(module.concat(LogicalPath.MODULE_PATH_SEPARATOR).concat("~."));
      }
    }

    // Getting all existing entities
    Set<ClassOrInterfaceTypeDetails> domainClassesInProject =
        typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_JPA_ENTITY,
            RooJavaType.ROO_DTO);
    for (ClassOrInterfaceTypeDetails entity : domainClassesInProject) {
      String name = replaceTopLevelPackageString(entity, currentText);
      if (!allPossibleValues.contains(name)) {
        allPossibleValues.add(name);
      }
    }

    // Always add base package
    allPossibleValues.add("~.");

    return allPossibleValues;
  }

  @CliCommand(value = "web flow",
      help = "Install Spring Web Flow configuration artifacts into your project")
  public void addWebFlow(@CliOption(key = {"flowName"}, mandatory = true,
      help = "The name for your web flow") final String flowName, @CliOption(key = {"module"},
      mandatory = false, help = "The name for your web flow") String moduleName, @CliOption(
      key = {"class"}, mandatory = false,
      help = "The class used to create the model object this flow is mainly "
          + "bound to. Can be an entity or a DTO") final JavaType klass) {

    if (moduleName == null) {

      // Get focused module
      moduleName = projectOperations.getFocusedModuleName();
    }
    webFlowOperations.installWebFlow(flowName, moduleName, klass);
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
