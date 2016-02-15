package org.springframework.roo.project.settings;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.ShellContext;

/**
 * Commands to manage the project configuration properties to be used by the Roo shell.
 *
 * @author Paula Navarro
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class ProjectSettingsCommands implements CommandMarker {

    @Reference
    private ProjectSettingsOperations projectSettingsOperations;

    @CliCommand(value = "project settings list", help = "List roo project settings")
    public void listSettings() {
        projectSettingsOperations.listSettings();
    }

    @CliCommand(value = "project settings add", help = "Add a new pair of key=value setting")
    public void addSetting(
            @CliOption(key = "name", mandatory = true, help = "Setting name") final String name,
            @CliOption(key = "value", mandatory = true, help = "Value related to specified setting") final String value,
            ShellContext shellContext) {

        projectSettingsOperations.addSetting(name, value, shellContext.isForce());
    }
    
    @CliCommand(value = "project settings remove", help = "Removes an specific property from Spring Roo configuration file")
    public void removeSetting(
            @CliOption(key = "name", mandatory = true, help = "Setting name") final String name) {

        projectSettingsOperations.removeSetting(name);
    }

}
