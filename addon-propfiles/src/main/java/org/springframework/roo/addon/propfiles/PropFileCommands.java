package org.springframework.roo.addon.propfiles;

import static org.springframework.roo.shell.OptionContexts.APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CliOptionMandatoryIndicator;
import org.springframework.roo.shell.CliOptionVisibilityIndicator;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.ShellContext;

/**
 * Provides commands to include properties on application config properties
 * file.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class PropFileCommands implements CommandMarker {

  @Reference
  private PropFileOperations propFileOperations;
  @Reference
  private TypeLocationService typeLocationService;
  @Reference
  private ProjectOperations projectOperations;

  private boolean isModuleRequired(ShellContext shellContext) {
    Pom module = projectOperations.getFocusedModule();
    if (!isModuleVisible(shellContext)
        || typeLocationService.hasModuleFeature(module, ModuleFeatureName.APPLICATION)) {
      return false;
    }
    return true;
  }

  private boolean isModuleVisible(ShellContext shellContext) {
    if (typeLocationService.getModuleNames(ModuleFeatureName.APPLICATION).size() > 1) {
      return true;
    }
    return false;
  }


  @CliAvailabilityIndicator({"property add", "property remove", "property list"})
  public boolean arePropertiesCommandAvailable() {
    return propFileOperations.arePropertiesCommandAvailable();
  }

  @CliOptionVisibilityIndicator(command = "property add", params = {"module"},
      help = "Module parameter is not available if there is only one application module")
  public boolean isAddPropertyModuleRequired(ShellContext shellContext) {
    return isModuleVisible(shellContext);
  }

  @CliOptionMandatoryIndicator(params = "module", command = "property add")
  public boolean isAddPropertyModuleVisible(ShellContext shellContext) {
    return isModuleRequired(shellContext);
  }

  @CliCommand(value = "property add",
      help = "Adds or updates a particular property from application config properties file.")
  public void setProperty(
      @CliOption(key = "key", mandatory = true, help = "The property key that should be changed") final String key,
      @CliOption(key = "value", mandatory = true, help = "The new vale for this property key") final String value,
      @CliOption(key = "module", mandatory = true, help = "Module where property will be added",
          unspecifiedDefaultValue = ".", optionContext = APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE) Pom module,
      ShellContext shellContext) {

    propFileOperations.addProperty(module.getModuleName(), key, value, shellContext.getProfile(),
        shellContext.isForce());
  }

  @CliOptionVisibilityIndicator(command = "property remove", params = {"module"},
      help = "Module parameter is not available if there is only one application module")
  public boolean isRemovePropertyModuleRequired(ShellContext shellContext) {
    return isModuleVisible(shellContext);
  }

  @CliOptionMandatoryIndicator(params = "module", command = "property remove")
  public boolean isRemovePropertyModuleVisible(ShellContext shellContext) {
    return isModuleRequired(shellContext);
  }

  @CliCommand(value = "property remove",
      help = "Removes a particular property from application config properties file.")
  public void removeProperty(
      @CliOption(key = {"key"}, mandatory = true, help = "The property key that should be removed") final String key,
      @CliOption(key = "module", mandatory = true, help = "Module where property will be removed",
          unspecifiedDefaultValue = ".", optionContext = APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE) Pom module,
      ShellContext shellContext) {

    propFileOperations.removeProperty(module.getModuleName(), key, shellContext.getProfile());
  }

  @CliOptionVisibilityIndicator(command = "property list", params = {"module"},
      help = "Module parameter is not available if there is only one application module")
  public boolean isListPropertyModuleRequired(ShellContext shellContext) {
    return isModuleVisible(shellContext);
  }

  @CliOptionMandatoryIndicator(params = "module", command = "property list")
  public boolean isListPropertyModuleVisible(ShellContext shellContext) {
    return isModuleRequired(shellContext);
  }

  @CliCommand(value = "property list",
      help = "List all properties from application config properties file.")
  public void listProperties(@CliOption(key = "module", mandatory = true,
      unspecifiedDefaultValue = ".", help = "Module which properties will be listed",
      optionContext = APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE) Pom module,
      ShellContext shellContext) {

    propFileOperations.listProperties(module.getModuleName(), shellContext.getProfile());
  }
}
