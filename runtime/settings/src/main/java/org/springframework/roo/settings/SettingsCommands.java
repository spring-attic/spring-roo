package org.springframework.roo.settings;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.ShellContext;

/**
 * Commands to manage configuration properties to be used by the Roo
 * shell.
 *
 * @author Paula Navarro
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class SettingsCommands implements CommandMarker {

  @Reference
  private SettingsOperations settingsOperations;

  @CliCommand(value = "settings list",
      help = "Lists all settings added into Roo project configuration. "
          + "These settings are located in _[PROJECT-ROOT]/.roo/config/project.properties_.")
  public void listSettings() {
    settingsOperations.listSettings();
  }

  @CliCommand(
      value = "settings add",
      help = "Adds or updates a Roo project setting, which can modify the "
          + "configuration of some commands acting in the current project. These settings are located in "
          + "_[PROJECT-ROOT]/.roo/config/project.properties_.")
  public void addSetting(
      @CliOption(key = "name", mandatory = true,
          help = "The setting name that should be added or changed.") final String name,
      @CliOption(key = "value", mandatory = true, help = "The value for this settings name.") final String value,
      ShellContext shellContext) {

    settingsOperations.addSetting(name, value, shellContext.isForce());
  }

  @CliCommand(value = "settings remove",
      help = "Removes a specific setting from Roo project configuration."
          + " Use 'settings list' to see the Roo settings added to the project.")
  public void removeSetting(@CliOption(key = "name", mandatory = true,
      help = "The settings name that should be removed.") final String name) {

    settingsOperations.removeSetting(name);
  }

}
