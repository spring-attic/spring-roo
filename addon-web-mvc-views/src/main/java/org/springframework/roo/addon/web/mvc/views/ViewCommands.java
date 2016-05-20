package org.springframework.roo.addon.web.mvc.views;

import static org.springframework.roo.shell.OptionContexts.APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.web.mvc.controller.addon.responses.ControllerMVCResponseService;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.project.FeatureNames;
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
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * This class provides necessary commands to be able to include views on
 * generated project
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class ViewCommands implements CommandMarker {

  private static Logger LOGGER = HandlerUtils.getLogger(ViewCommands.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private ViewOperations viewOperations;
  private ProjectOperations projectOperations;
  private TypeLocationService typeLocationService;
  private PathResolver pathResolver;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  /**
   * This indicator checks if --module parameter should be visible or not.
   * 
   * If exists more than one module that match with the properties of ModuleFeature APPLICATION,
   * --module parameter should be visible.
   * 
   * @param shellContext
   * @return
   */
  @CliOptionVisibilityIndicator(command = "web mvc view setup", params = {"module"},
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
  @CliOptionMandatoryIndicator(command = "web mvc view setup", params = {"module"})
  public boolean isModuleRequired(ShellContext shellContext) {
    Pom module = getProjectOperations().getFocusedModule();
    if (!isModuleVisible(shellContext)
        || getTypeLocationService().hasModuleFeature(module, ModuleFeatureName.APPLICATION)) {
      return false;
    }
    return true;
  }

  /**
   * This indicator returns all possible values for --type parameter.
   * 
   * Only not installed response types will be provided.
   * 
   * @param context
   * @return
   */
  @CliOptionAutocompleteIndicator(param = "type", command = "web mvc view setup",
      help = "--type parameter should be completed with the provided response types.")
  public List<String> getAllResponseTypeValues(ShellContext context) {
    // Getting all not installed services that implements ControllerMVCResponseService
    Map<String, ControllerMVCResponseService> notInstalledResponseTypes =
        getControllerMVCResponseTypes(false);

    // Generating all possible values
    List<String> responseTypes = new ArrayList<String>();

    for (Entry<String, ControllerMVCResponseService> responseType : notInstalledResponseTypes
        .entrySet()) {
      responseTypes.add(responseType.getKey());
    }

    return responseTypes;
  }

  /**
   * This method checks if web mvc view setup command is available or not.
   * 
   * View setup command will be available if exists some type that 
   * has not been installed.
   * 
   * @return
   */
  @CliAvailabilityIndicator("web mvc view setup")
  public boolean isSetupAvailable() {
    return getProjectOperations().isFeatureInstalled(FeatureNames.MVC)
        && !getControllerMVCResponseTypes(false).isEmpty();
  }

  /**
   * This method provides the Command definition to be able to install
   * provided responseType on generated project
   * 
   * @param type
   * @param module
   */
  @CliCommand(value = "web mvc view setup",
      help = "Includes all necessary resources of provided responseType on generated project")
  public void setup(
      @CliOption(
          key = "type",
          mandatory = true,
          help = "View identifier you want to install. Install your necessary views before to be used on controller generation command") String type,
      @CliOption(key = "module", mandatory = true,
          help = "The application module where to install views", unspecifiedDefaultValue = ".",
          optionContext = APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE) Pom module) {

    Map<String, ControllerMVCResponseService> responseTypes = getControllerMVCResponseTypes(false);
    if (!responseTypes.containsKey(type)) {
      throw new IllegalArgumentException("ERROR: You have provided an invalid type.");
    }

    getViewOperations().setup(responseTypes.get(type), module);
  }

  /**
   * This indicator returns all possible values for --type parameter.
   * 
   * Only installed response types will be provided.
   * 
   * @param context
   * @return
   */
  @CliOptionAutocompleteIndicator(param = "type", command = "web mvc templates setup",
      help = "--type parameter should be completed with the provided response types.")
  public List<String> getAllViewTypeValues(ShellContext context) {
    // Getting all installed services that implements ControllerMVCResponseService
    Map<String, ControllerMVCResponseService> installedResponseType =
        getControllerMVCResponseTypes(true);

    // Generating all possible values
    List<String> responseTypes = new ArrayList<String>();

    for (Entry<String, ControllerMVCResponseService> responseType : installedResponseType
        .entrySet()) {

      // Only that response types that generates views will be able
      // on this command.
      if (getMVCViewGenerationService(responseType.getKey()) != null) {
        responseTypes.add(responseType.getKey());
      }
    }

    return responseTypes;
  }

  /**
   * This method checks if web mvc templates setup command is available or not.
   * 
   * Templates setup command will be available if exists some type that 
   * has been installed.
   * 
   * @return
   */
  @CliAvailabilityIndicator("web mvc templates setup")
  public boolean isInstallTemplateAvailable() {
    return getProjectOperations().isFeatureInstalled(FeatureNames.MVC)
        && !getControllerMVCResponseTypes(true).isEmpty();
  }

  /**
   * This method provides the Command definition to be able to install
   * view generation templates on current project.
   * 
   * Installing this templates, developers will be able to customize view generation.
   * 
   * @param  type
   */
  @CliCommand(
      value = "web mvc templates setup",
      help = "Includes view generation templates on current project. Will allow developers to customize view generation.")
  public void installTemplates(
      @CliOption(
          key = "type",
          mandatory = true,
          help = "View identifier of templates you want to install. Only installed views are available.") String type) {

    Map<String, ControllerMVCResponseService> responseTypes = getControllerMVCResponseTypes(true);
    if (!responseTypes.containsKey(type)) {
      throw new IllegalArgumentException("ERROR: You have provided an invalid type.");
    }

    getMVCViewGenerationService(type).installTemplates();
  }

  // Get OSGi services

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
        LOGGER.warning("Cannot load TypeLocationService on ViewCommands.");
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
        LOGGER.warning("Cannot load ProjectOperations on ViewCommands.");
        return null;
      }
    } else {
      return projectOperations;
    }
  }

  public ViewOperations getViewOperations() {
    if (viewOperations == null) {
      // Get all Services implement ViewOperations interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ViewOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          viewOperations = (ViewOperations) this.context.getService(ref);
          return viewOperations;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ViewOperations on ViewCommands.");
        return null;
      }
    } else {
      return viewOperations;
    }
  }

  /**
   * This method gets all implementations of ControllerMVCResponseService interface to be able
   * to locate all ControllerMVCResponseService. Uses param installed to obtain only the installed
   * or not installed response types.
   * 
   * @param installed indicates if returned responseType should be installed or not.
   * 
   * @return Map with responseTypes identifier and the ControllerMVCResponseService implementation
   */
  public Map<String, ControllerMVCResponseService> getControllerMVCResponseTypes(boolean installed) {
    Map<String, ControllerMVCResponseService> responseTypes =
        new HashMap<String, ControllerMVCResponseService>();

    try {
      ServiceReference<?>[] references =
          this.context.getAllServiceReferences(ControllerMVCResponseService.class.getName(), null);

      for (ServiceReference<?> ref : references) {
        ControllerMVCResponseService responseTypeService =
            (ControllerMVCResponseService) this.context.getService(ref);
        boolean isAbleToInstall = false;
        for (Pom module : getProjectOperations().getPoms()) {
          if (responseTypeService.isInstalledInModule(module.getModuleName()) == installed) {
            isAbleToInstall = true;
            break;
          }
        }
        if (isAbleToInstall) {
          responseTypes.put(responseTypeService.getResponseType(), responseTypeService);
        }
      }
      return responseTypes;

    } catch (InvalidSyntaxException e) {
      LOGGER.warning("Cannot load ControllerMVCResponseService on ViewCommands.");
      return null;
    }
  }


  /**
   * This method gets MVCViewGenerationService implementation that contains necessary operations
   * to install templates inside generated project.
   * 
   * @param type
   * @return
   */
  public MVCViewGenerationService getMVCViewGenerationService(String type) {
    try {
      ServiceReference<?>[] references =
          this.context.getAllServiceReferences(MVCViewGenerationService.class.getName(), null);

      for (ServiceReference<?> ref : references) {
        MVCViewGenerationService viewGenerationService =
            (MVCViewGenerationService) this.context.getService(ref);
        if (viewGenerationService.getName().equals(type)) {
          return viewGenerationService;
        }
      }

      return null;

    } catch (InvalidSyntaxException e) {
      LOGGER.warning("Cannot load MVCViewGenerationService on ViewCommands.");
      return null;
    }
  }

  public PathResolver getPathResolver() {
    if (pathResolver == null) {
      // Get all Services implement PathResolver interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(PathResolver.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          pathResolver = (PathResolver) this.context.getService(ref);
          return pathResolver;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load PathResolver on ThymeleafMVCViewResponseService.");
        return null;
      }
    } else {
      return pathResolver;
    }
  }

}
