package org.springframework.roo.addon.propfiles;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
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

  @CliAvailabilityIndicator({"property add", "property remove", "property list"})
  public boolean arePropertiesCommandAvailable() {
    return propFileOperations.arePropertiesCommandAvailable();
  }

  @CliCommand(value = "property add",
      help = "Adds or updates a particular property from application config properties file.")
  public void setProperty(@CliOption(key = "key", mandatory = true,
      help = "The property key that should be changed") final String key, @CliOption(key = "value",
      mandatory = true, help = "The new vale for this property key") final String value,
      ShellContext shellContext) {

    propFileOperations.addProperty(key, value, shellContext.getProfile(), shellContext.isForce());
  }

  @CliCommand(value = "property remove",
      help = "Removes a particular property from application config properties file.")
  public void removeProperty(@CliOption(key = {"key"}, mandatory = true,
      help = "The property key that should be removed") final String key, ShellContext shellContext) {

    propFileOperations.removeProperty(key, shellContext.getProfile());
  }

  @CliCommand(value = "property list",
      help = "List all properties from application config properties file.")
  public void listProperties(ShellContext shellContext) {

    propFileOperations.listProperties(shellContext.getProfile());
  }
}
