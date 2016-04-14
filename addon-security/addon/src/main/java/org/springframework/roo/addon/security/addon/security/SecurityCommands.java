package org.springframework.roo.addon.security.addon.security;

import static org.springframework.roo.shell.OptionContexts.APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.*;

/**
 * Commands for the security add-on to be used by the ROO shell.
 * 
 * @author Ben Alex
 * @author Sergio Clares
 * @since 1.0
 */
@Component
@Service
public class SecurityCommands implements CommandMarker {

  @Reference
  private SecurityOperations securityOperations;
  @Reference
  private TypeLocationService typeLocationService;
  @Reference
  private ProjectOperations projectOperations;

  @CliAvailabilityIndicator("security setup")
  public boolean isInstallSecurityAvailable() {
    return securityOperations.isSecurityInstallationPossible();
  }

  @CliOptionVisibilityIndicator(command = "security setup", params = {"module"},
      help = "Module parameter is not available if there is only one application module")
  public boolean isModuleVisible(ShellContext shellContext) {
    if (typeLocationService.getModuleNames(ModuleFeatureName.APPLICATION).size() > 1) {
      return true;
    }
    return false;
  }

  @CliOptionMandatoryIndicator(params = "module", command = "security setup")
  public boolean isModuleRequired(ShellContext shellContext) {
    Pom module = projectOperations.getFocusedModule();
    if (!isModuleVisible(shellContext)
        || typeLocationService.hasModuleFeature(module, ModuleFeatureName.APPLICATION)) {
      return false;
    }
    return true;
  }

  @CliCommand(value = "security setup", help = "Install Spring Security into your project")
  public void installSecurity(
      @CliOption(key = "module", mandatory = true,
          help = "The application module where to install the persistence",
          unspecifiedDefaultValue = ".", optionContext = APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE) Pom module) {
    securityOperations.installSecurity(module);
  }

}
