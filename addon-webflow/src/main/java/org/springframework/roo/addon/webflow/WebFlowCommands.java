package org.springframework.roo.addon.webflow;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CliOptionAutocompleteIndicator;
import org.springframework.roo.shell.CliOptionVisibilityIndicator;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.ShellContext;

import java.util.ArrayList;
import java.util.List;

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

  @CliCommand(value = "web flow",
      help = "Install Spring Web Flow configuration artifacts into your project")
  public void addWebFlow(@CliOption(key = {"flowName"}, mandatory = true,
      help = "The name for your web flow") final String flowName, @CliOption(key = {"module"},
      mandatory = false, help = "The name for your web flow") String moduleName) {

    if (moduleName == null) {

      // Get focused module
      moduleName = projectOperations.getFocusedModuleName();
    }
    webFlowOperations.installWebFlow(flowName, moduleName);
  }
}
