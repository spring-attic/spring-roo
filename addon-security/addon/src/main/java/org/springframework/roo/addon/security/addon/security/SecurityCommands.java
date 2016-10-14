package org.springframework.roo.addon.security.addon.security;

import static org.springframework.roo.shell.OptionContexts.APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.security.addon.security.providers.SecurityProvider;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.TypeLocationService;
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

/**
 * Commands for the security add-on to be used by the ROO shell.
 * 
 * @author Ben Alex
 * @author Sergio Clares
 * @author Juan Carlos García
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
    // If some SecurityProvider is available to be installed, this command will be available
    // showing only these ones.
    List<SecurityProvider> securityProviders = securityOperations.getAllSecurityProviders();
    for (SecurityProvider provider : securityProviders) {
      if (provider.isInstallationAvailable()) {
        return true;
      }
    }

    return false;
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

  @CliOptionAutocompleteIndicator(command = "security setup", param = "type",
      help = "You must select a valid security provider.", validate = false)
  public List<String> getAllSecurityProviders(ShellContext context) {

    List<String> results = new ArrayList<String>();

    List<SecurityProvider> securityProviders = securityOperations.getAllSecurityProviders();
    for (SecurityProvider provider : securityProviders) {
      if (provider.isInstallationAvailable()) {
        results.add(provider.getName());
      }
    }

    return results;

  }

  @CliCommand(value = "security setup", help = "Install Spring Security into your project")
  public void installSecurity(
      @CliOption(key = "type", mandatory = false,
          help = "The Spring Security provider to install.", unspecifiedDefaultValue = "DEFAULT",
          specifiedDefaultValue = "DEFAULT") String type,
      @CliOption(key = "module", mandatory = true,
          help = "The application module where to install the persistence",
          unspecifiedDefaultValue = ".", optionContext = APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE) Pom module) {

    securityOperations.installSecurity(getSecurityProviderFromName(type), module);

  }

  /**
   * This method obtains the implementation of the Spring Security Provider
   * using the provided name.
   * 
   * @param name The name of the SecurityProvider to obtain
   * 
   * @return A SecurityProvider with the same name as the provided one.
   */
  private SecurityProvider getSecurityProviderFromName(String name) {

    List<SecurityProvider> securityProviders = securityOperations.getAllSecurityProviders();
    for (SecurityProvider type : securityProviders) {
      if (type.getName().equals(name)) {
        return type;
      }
    }

    return null;
  }


}
