package org.springframework.roo.addon.pushin;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CliOptionVisibilityIndicator;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.ShellContext;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Commands for the 'push-in' add-on to be used by the ROO shell.
 * 
 * This command marker will provide necessary operations to make push-in of all
 * methods, fields and annotations declared on ITDs.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class PushInCommands implements CommandMarker {

  private static final Logger LOGGER = HandlerUtils.getLogger(PushInCommands.class);

  @Reference
  private PushInOperations pushInOperations;

  /**
   * Method that checks if push-in operation is available or not.
   * 
   * "push-in" command will be available only if some project was generated.
   * 
   * @return true if some project was created on focused directory.
   */
  @CliAvailabilityIndicator("push-in")
  public boolean isPushInCommandAvailable() {
    return pushInOperations.isPushInCommandAvailable();
  }

  /**
   * Method that checks visibility of --all parameter.
   * 
   * This parameter will be available only if --package, --class or --method 
   * parameter has not been specified before.
   * 
   * @param context
   *            ShellContext used to obtain specified parameters
   * @return true if --all parameter is visible, false if not.
   */
  @CliOptionVisibilityIndicator(
      command = "push-in",
      params = "all",
      help = "--all parameter is not available if --package, --class or --method parameter has been specified.")
  public boolean isAllParameterVisible(ShellContext context) {
    Map<String, String> specifiedParameters = context.getParameters();

    if (specifiedParameters.containsKey("package") || specifiedParameters.containsKey("class")
        || specifiedParameters.containsKey("method")) {
      return false;
    }

    return true;

  }

  /**
   * Method that checks visibility of --package, --class and --method parameters.
   * 
   * This parameter will be available only if --all parameter 
   * has not been specified before.
   * 
   * @param context
   *            ShellContext used to obtain specified parameters
   * @return true if parameters are visible, false if not.
   */
  @CliOptionVisibilityIndicator(
      command = "push-in",
      params = {"package", "class", "method"},
      help = "--package, --class and --method parameters are not available if --all parameter has been specified.")
  public boolean isOtherParametersVisible(ShellContext context) {
    Map<String, String> specifiedParameters = context.getParameters();

    if (specifiedParameters.containsKey("all")) {
      return false;
    }

    return true;

  }

  /**
   * Method that register "push-in" command on Spring Roo Shell.
   * 
   * Push-in all methods, fields, annotations, imports, extends, etc.. declared on 
   * ITDs to its .java files. You could specify --all parameter to apply push-in on every
   * component of generated project, or you could define package, class or method where wants 
   * to apply push-in.
   * 
   * @param all
   *            String that indicates if push-in process should be applied to entire project. All specified
   *            values will be ignored.
   * @param package 
   *            JavaPackage with the specified package where developers wants to make 
   *            push-in
   * @param klass
   *            JavaType with the specified class where developer wants to
   *            make push-in
   * @param method
   *            String with the specified name of the method that
   *            developer wants to push-in
   * @param context
   *            ShellContext used to know if --force parameter has been used by developer
   *    
   */
  @CliCommand(
      value = "push-in",
      help = "Push-in all methods, fields, annotations, imports, extends, etc.. declared on  ITDs to its .java files. You could specify --all parameter to apply push-in on every component of generated project, or you could define package, class or method where wants to apply push-in.")
  public void pushIn(
      @CliOption(
          key = "all",
          mandatory = false,
          specifiedDefaultValue = "",
          help = "Parameter that indicates if push-in process should be applied to entire project. If specified, all the other parameters will be unavailable. It doesn't allow any value.") String all,
      @CliOption(key = "package", mandatory = false,
          help = "JavaPackage with the specified package where developers wants to make push-in") JavaPackage specifiedPackage,
      @CliOption(key = "class", mandatory = false,
          help = "JavaType with the specified class where developer wants to make push-in") final JavaType klass,
      @CliOption(
          key = "method",
          mandatory = false,
          help = "String with the specified name of the method that developer wants to push-in. You could use a Regular Expression to make push-in of more than one method on the same execution.") String method,
      ShellContext context) {

    // Developer must specify at least one parameter
    if (all == null && specifiedPackage == null && klass == null && method == null) {
      LOGGER.log(Level.WARNING, "ERROR: You must specify at least one parameter. ");
      return;
    }

    // Check if all parameter contains value
    if (all != null && StringUtils.isNotEmpty(all)) {
      LOGGER.log(Level.WARNING, "ERROR: --all parameter doesn't allow any value.");
      return;
    }

    // Check if developer wants to apply push-in on every component of generated project
    if (all != null) {
      pushInOperations.pushInAll(context.isForce());
    } else {
      pushInOperations.pushIn(specifiedPackage, klass, method, context.isForce());
    }

  }


}
