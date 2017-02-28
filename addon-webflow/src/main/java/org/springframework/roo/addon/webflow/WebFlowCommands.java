package org.springframework.roo.addon.webflow;

import static org.springframework.roo.shell.OptionContexts.APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
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
import org.springframework.roo.shell.ShellContext;
import org.springframework.roo.shell.SimpleParser;
import org.springframework.roo.support.logging.HandlerUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

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

  private static final Logger LOGGER = HandlerUtils.getLogger(SimpleParser.class);

  @Reference
  private WebFlowOperations webFlowOperations;

  @Reference
  private ProjectOperations projectOperations;

  @Reference
  private TypeLocationService typeLocationService;

  @Reference
  private PathResolver pathResolver;

  @Reference
  private FileManager fileManager;

  @CliAvailabilityIndicator("web flow")
  public boolean isInstallWebFlowAvailable() {
    return webFlowOperations.isWebFlowInstallationPossible();
  }

  @CliOptionMandatoryIndicator(command = "web flow", params = "module")
  public boolean isModuleRequired(ShellContext shellContext) {
    Pom module = projectOperations.getFocusedModule();
    if (!isModuleVisible(shellContext)
        || typeLocationService.hasModuleFeature(module, ModuleFeatureName.APPLICATION)) {
      return false;
    }
    return true;
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
    if (typeLocationService.getModuleNames(ModuleFeatureName.APPLICATION).size() > 1) {
      return true;
    }
    return false;
  }

  @CliOptionAutocompleteIndicator(command = "web flow", param = "class",
      help = "You should specify an existing and serializable class for option " + "'--class'.",
      validate = false)
  public List<String> getClassPossibleValues(ShellContext shellContext) {

    // Get current value of class
    String currentText = shellContext.getParameters().get("class");
    List<String> allPossibleValues = new ArrayList<String>();

    // Getting all existing entities
    Set<ClassOrInterfaceTypeDetails> domainClassesInProject =
        typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_JPA_ENTITY,
            RooJavaType.ROO_DTO);
    for (ClassOrInterfaceTypeDetails classDetails : domainClassesInProject) {

      // Check if class implements serializable (needed for WebFlow)
      boolean isSerializable = false;

      // First, chech for @RooSerializable
      if (classDetails.getAnnotation(RooJavaType.ROO_SERIALIZABLE) != null) {
        isSerializable = true;
      }

      // Check for the explicit 'implements Serializable'
      if (!isSerializable) {
        List<JavaType> implementsTypes = classDetails.getImplementsTypes();
        for (JavaType type : implementsTypes) {
          if (type.equals(JdkJavaType.SERIALIZABLE)) {
            isSerializable = true;
            break;
          }
        }
      }

      if (isSerializable) {

        // Add to possible values
        String name = replaceTopLevelPackageString(classDetails, currentText);
        if (!allPossibleValues.contains(name)) {
          allPossibleValues.add(name);
        }
      }
    }

    if (allPossibleValues.isEmpty()) {

      // Any entity or DTO in project is serializable
      LOGGER.info("Any auto-complete value offered because the project hasn't any entity "
          + "or DTO which implement Serializable");
    }

    return allPossibleValues;
  }

  @CliCommand(value = "web flow", help = "Installs a Spring Web Flow into your project.")
  public void addWebFlow(
      @CliOption(key = {"flowName"}, mandatory = true, help = "The name for your web flow.") final String flowName,
      @CliOption(
          key = {"module"},
          mandatory = true,
          help = "The application module where create the web flow. "
              + "This option is mandatory if the focus is not set in an 'application' module and "
              + "there are more than one 'application' modules, that is, a module containing an "
              + "`@SpringBootApplication` class. "
              + "This option is available only if there are more than one application module and none of"
              + " them is focused. "
              + "Default if option not present: the unique 'application' module, or focused 'application'"
              + " module.", unspecifiedDefaultValue = ".",
          optionContext = APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE) Pom module,
      @CliOption(key = {"class"}, mandatory = false,
          help = "The class used to create the model object this flow is mainly "
              + "bound to. Can be an entity or a DTO and must be serializable.") final JavaType klass) {

    if (module == null) {

      // Get focused module
      module = projectOperations.getFocusedModule();
    }

    // Check if exists other entity with the same name
    if (klass != null) {
      final String fiilePathIdentifier =
          pathResolver.getCanonicalPath(klass.getModule(), Path.SRC_MAIN_JAVA, klass);
      if (!fileManager.exists(fiilePathIdentifier)) {
        throw new IllegalArgumentException(String.format(
            "Class '%s' doesn't exist. Try to use a different class name on "
                + "--class parameter. You can use 'dto' or 'entity jpa' command to "
                + "create it. It needs to be serializable.", klass));
      }
    }

    webFlowOperations.installWebFlow(flowName, module.getModuleName(), klass);
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
