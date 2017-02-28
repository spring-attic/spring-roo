package org.springframework.roo.addon.web.mvc.exceptions.addon;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.logging.HandlerUtils;

import java.util.logging.Logger;

/**
 * = Commands for the Exceptions addon to be used by the Roo Shell
 *
 * @author Fran Cardoso
 * @since 2.0
 */
@Component
@Service
public class ExceptionsCommands implements CommandMarker {

  /**
   * Get hold of the Roo support Logger.
   */
  protected final static Logger LOGGER = HandlerUtils.getLogger(ExceptionsCommands.class);

  /**
   * Get a reference to the Roo ProjectOperations from the underlying OSGi container.
   */
  @Reference
  private ProjectOperations projectOperations;

  /**
   * Get a reference to the ExceptionsOperations from the underlying OSGi container
   */
  @Reference
  private ExceptionsOperations exceptionsOperations;

  /**
   * Get a reference to the TypeLocationService from the underlying OSGi container
   */
  @Reference
  private TypeLocationService typeLocationService;

  /**
   * Method which returns whether the 'web mvc exception handler' command should be available.
   *
   * @return <code>true</code> if required feature is not already installed.
   */
  @CliAvailabilityIndicator("web mvc exception handler")
  public boolean isExceptionHandlerCommandAvailable() {
    return projectOperations.isFeatureInstalled(FeatureNames.MVC);
  }

  /**
   * This method provides the Command definition to be able to add an exception
   * handler.
   */
  @CliCommand(value = "web mvc exception handler",
      help = "Adds methods to handle an application exception in a specified "
          + "controller or a class annotated with `@ControllerAdvice`.")
  public void exceptionHandler(
      @CliOption(key = "exception", mandatory = true, help = "The exception to handle. "
          + "If you consider it necessary, you can also specify the package. "
          + "Ex.: `--class ~.model.MyClass` (where `~` is the base package). When working with "
          + "multiple modules, you should specify the name of the class and the module where "
          + "it is. Ex.: `--class model:~.MyClass`. If the module is not specified, it is "
          + "assumed that the class is in the module which has the focus.") final JavaType exception,
      @CliOption(
          key = "controller",
          mandatory = false,
          help = "Controller where include the handler methods. "
              + "If you consider it necessary, you can also specify the package. "
              + "Ex.: `--class ~.model.MyClass` (where `~` is the base package). When working with "
              + "multiple modules, you should specify the name of the class and the module where "
              + "it is. Ex.: `--class model:~.MyClass`. If the module is not specified, it is "
              + "assumed that the class is in the module which has the focus.") final JavaType controller,
      @CliOption(
          key = "class",
          mandatory = false,
          help = "Class annotated with `@ControllerAdvice` where include the handler methods. "
              + "If you consider it necessary, you can also specify the package. "
              + "Ex.: `--class ~.model.MyClass` (where `~` is the base package). When working with "
              + "multiple modules, you should specify the name of the class and the module where "
              + "it is. Ex.: `--class model:~.MyClass`. If the module is not specified, it is "
              + "assumed that the class is in the module which has the focus.") final JavaType controllerAdvice,
      @CliOption(key = "errorView", mandatory = false,
          help = "View to be returned when specified exception is thrown") final String errorView) {

    exceptionsOperations.addExceptionHandler(exception, controller, controllerAdvice, errorView);
  }
}
