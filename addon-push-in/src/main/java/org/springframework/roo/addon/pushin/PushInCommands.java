package org.springframework.roo.addon.pushin;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Commands for the 'push-in' add-on to be used by the ROO shell.
 * 
 * This command will provide necessary operations to make push-in of all
 * methods, fields and annotations declared on ITDs.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class PushInCommands implements CommandMarker {

  @Reference
  private PushInOperations pushInOperations;

  /**
   * Method that checks if push-in operation is available or not.
   * 
   * "push-in" command will be available only if some project was generated.
   * 
   * @return true if some project was created on focused directory.
   */
  @CliAvailabilityIndicator({"push-in all", "push-in method"})
  public boolean isPushInCommandAvailable() {
    return pushInOperations.isPushInCommandAvailable();
  }

  /**
   * Method that register "push-in method" command on Spring Roo Shell.
   * 
   * Push-in an specific method declared on an specified class ITDs to its .java files
   * 
   * @param klass
   *            JavaType with the specified class where developer wants to
   *            make push-in
   * @param method
   *            JavaSymbolName with the specified name of the method that
   *            developer wants to push-in
   * 
   */
  @CliCommand(value = "push-in method",
      help = "Push-in an specific method declared on an specified class ITDs to its .java files.")
  public void pushInMethod(
      @CliOption(key = "class", mandatory = true,
          help = "JavaType with the specified class where developer wants to make push-in") final JavaType klass,
      @CliOption(
          key = "method",
          mandatory = true,
          help = "JavaSymbolName with the specified name of the method that developer wants to push-in") JavaSymbolName method) {
    pushInOperations.pushInMethod(klass, method);
  }

  /**
   * Method that register push-in command on Spring Roo Shell.
   * 
   * Push-in all methods, fields and annotations declared on project ITDs to its .java
   * files.
   * 
   * @param package 
   *            JavaPackage with the specified package where push-in will be applied.
   * @param klass 
   *            JavaType with the specified class where push-in will be applied.
   */
  @CliCommand(
      value = "push-in all",
      help = "Push-in all methods, fields an annotations declared on an specified class ITDs to its .java files.")
  public void pushInAll(
      @CliOption(key = "package", mandatory = false,
          help = "If specified, push-in will only be applied to classes defined in this package.") JavaPackage packageName,
      @CliOption(key = "class", mandatory = false,
          help = "If specified, push-in will only be applied to this class") JavaType klass) {
    pushInOperations.pushInAll(packageName, klass);
  }

}
