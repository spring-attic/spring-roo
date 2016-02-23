package org.springframework.roo.addon.pushin;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Commands for the 'push-in' add-on to be used by the ROO shell.
 * 
 * This command will provide necessary operations to make push-in of all methods
 * and fields declared on ITDs.
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
  @CliAvailabilityIndicator({"push-in all", "push-in class"})
  public boolean isPushInCommandAvailable() {
    return pushInOperations.isPushInCommandAvailable();
  }

  /**
   * Method that register "push-in class" command on Spring Roo Shell.
   * 
   * Push-in all methods and fields declared on an specified class ITDs to its
   * .java files.
   * 
   * @param klass
   *            JavaType with the specified class where developer wants to
   *            make push-in
   * 
   */
  @CliCommand(
      value = "push-in class",
      help = "Push-in all methods and fields declared on an specified class ITDs to its .java files.")
  public void pushInClass(
      @CliOption(key = {"class", ""}, mandatory = true,
          help = "JavaType with the specified class where developer wants to make push-in") final JavaType klass) {
    pushInOperations.pushInClass(klass);
  }

  /**
   * Method that register push-in command on Spring Roo Shell.
   * 
   * Push-in all methods and fields declared on project ITDs to its .java
   * files.
   */
  @CliCommand(
      value = "push-in all",
      help = "Push-in all methods and fields declared on an specified class ITDs to its .java files.")
  public void pushInAll() {
    pushInOperations.pushInAll();
  }

}
