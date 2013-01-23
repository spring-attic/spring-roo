package org.springframework.roo.addon.json;

import static org.springframework.roo.shell.OptionContexts.UPDATE_PROJECT;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Commands for addon-json
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component
@Service
public class JsonCommands implements CommandMarker {

    @Reference private JsonOperations jsonOperations;

    @CliCommand(value = "json add", help = "Adds @RooJson annotation to target type")
    public void add(
            @CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = UPDATE_PROJECT, help = "The java type to apply this annotation to") final JavaType target,
            @CliOption(key = "rootName", mandatory = false, help = "The root name which should be used to wrap the JSON document") final String rootName,
            @CliOption(key = "deepSerialize", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", mandatory = false, help = "Indication if deep serialization should be enabled.") final boolean deep) {

        jsonOperations.annotateType(target, rootName, deep);
    }

    @CliCommand(value = "json all", help = "Adds @RooJson annotation to all types annotated with @RooJavaBean")
    public void all(
            @CliOption(key = "deepSerialize", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", mandatory = false, help = "Indication if deep serialization should be enabled") final boolean deep) {

        jsonOperations.annotateAll(deep);
    }

    @CliAvailabilityIndicator({ "json setup", "json add", "json all" })
    public boolean isPropertyAvailable() {
        return jsonOperations.isJsonInstallationPossible();
    }
}