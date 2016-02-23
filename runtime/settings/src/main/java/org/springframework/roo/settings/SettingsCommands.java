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

  @CliCommand(value = "settings list", help = "Lists all settings added into configuration")
  public void listSettings() {
    settingsOperations.listSettings();
  }

  @CliCommand(value = "settings add", help = "Adds or updates a particular setting")
  public void addSetting(
      @CliOption(key = "name", mandatory = true, help = "The setting name that should be changed") final String name,
      @CliOption(key = "value", mandatory = true, help = "The new vale for this settings name") final String value,
      ShellContext shellContext) {

    settingsOperations.addSetting(name, value, shellContext.isForce());
  }

  @CliCommand(value = "settings remove", help = "Removes an specific setting from configuration")
  public void removeSetting(@CliOption(key = "name", mandatory = true,
      help = "The settings name that should be removed") final String name) {

    settingsOperations.removeSetting(name);
  }

}
