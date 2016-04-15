package org.springframework.roo.addon.web.mvc.controller.addon;

import static org.springframework.roo.shell.OptionContexts.APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
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
import org.springframework.roo.support.logging.HandlerUtils;


/**
 * This class provides necessary commands to be able to include Spring MVC on generated
 * project and generate new controllers.
 * 
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @author Paula Navarro
 * @since 1.0
 */
@Component
@Service
public class ControllerCommands implements CommandMarker {

  private static Logger LOGGER = HandlerUtils.getLogger(ControllerCommands.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private Map<String, ServerProvider> serverProviders = new HashMap<String, ServerProvider>();

  private ControllerOperations controllerOperations;
  private ProjectOperations projectOperations;
  private TypeLocationService typeLocationService;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  /**
   * This indicator checks if --module parameter should be visible or not.
   * 
   * If exists more than one module that match with the properties of ModuleFeature APPLICATION,
   * --module parameter should be mandatory.
   * 
   * @param shellContext
   * @return
   */
  @CliOptionVisibilityIndicator(command = "web mvc setup", params = {"module"},
      help = "Module parameter is not available if there is only one application module")
  public boolean isModuleVisible(ShellContext shellContext) {
    if (getTypeLocationService().getModuleNames(ModuleFeatureName.APPLICATION).size() > 1) {
      return true;
    }
    return false;
  }

  /**
   * This indicator checks if --module parameter should be mandatory or not. 
   * 
   * If focused module doesn't match with the properties of ModuleFeature APPLICATION,
   * --module parameter should be mandatory.
   * 
   * @param shellContext
   * @return
   */
  @CliOptionMandatoryIndicator(command = "web mvc setup", params = {"module"})
  public boolean isModuleRequired(ShellContext shellContext) {
    Pom module = getProjectOperations().getFocusedModule();
    if (!isModuleVisible(shellContext)
        || getTypeLocationService().hasModuleFeature(module, ModuleFeatureName.APPLICATION)) {
      return false;
    }
    return true;
  }

  /**
   * This indicator returns the servers where the application can be deployed
   * @param context
   * @return
   */
  @CliOptionAutocompleteIndicator(command = "web mvc setup", param = "appServer",
      help = "Only valid application servers are available")
  public List<String> getAllAppServers(ShellContext context) {
    if (serverProviders.isEmpty()) {
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ServerProvider.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          ServerProvider serverProvider = (ServerProvider) this.context.getService(ref);
          serverProviders.put(serverProvider.getName(), serverProvider);
        }
        return new ArrayList<String>(serverProviders.keySet());

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ServerProviders on ControllerCommands.");
        return null;
      }
    } else {
      return new ArrayList<String>(serverProviders.keySet());
    }
  }


  /**
   * This indicator checks if Spring MVC setup is available
   *
   * If a valid project has been generated and Spring MVC has not been installed yet, 
   * this command will be available.
   * 
   * @return
   */
  @CliAvailabilityIndicator(value = "web mvc setup")
  public boolean isSetupAvailable() {
    return getControllerOperations().isSetupAvailable();
  }

  /**
   * This method provides the Command definition to be able to include
   * Spring MVC on generated project.
   * 
   * @param module
   * @param appServer
   */
  @CliCommand(value = "web mvc setup", help = "Includes Spring MVC on generated project")
  public void setup(
      @CliOption(key = "module", mandatory = true,
          help = "The application module where to install the persistence",
          unspecifiedDefaultValue = ".", optionContext = APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE) Pom module,
      @CliOption(key = "appServer", mandatory = false,
          help = "The server where deploy the application", unspecifiedDefaultValue = "EMBEDDED") String appServer) {

    if (!serverProviders.containsKey(appServer)) {
      throw new IllegalArgumentException("ERROR: Invalid server provider");
    }

    getControllerOperations().setup(module, serverProviders.get(appServer));
  }

  public TypeLocationService getTypeLocationService() {
    if (typeLocationService == null) {
      // Get all Services implement TypeLocationService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(TypeLocationService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          typeLocationService = (TypeLocationService) this.context.getService(ref);
          return typeLocationService;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TypeLocationService on ControllerCommands.");
        return null;
      }
    } else {
      return typeLocationService;
    }
  }

  public ProjectOperations getProjectOperations() {
    if (projectOperations == null) {
      // Get all Services implement ProjectOperations interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ProjectOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          projectOperations = (ProjectOperations) this.context.getService(ref);
          return projectOperations;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ProjectOperations on ControllerCommands.");
        return null;
      }
    } else {
      return projectOperations;
    }
  }

  public ControllerOperations getControllerOperations() {
    if (controllerOperations == null) {
      // Get all Services implement ControllerOperations interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ControllerOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          controllerOperations = (ControllerOperations) this.context.getService(ref);
          return controllerOperations;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ControllerOperations on ControllerCommands.");
        return null;
      }
    } else {
      return controllerOperations;
    }
  }


}
