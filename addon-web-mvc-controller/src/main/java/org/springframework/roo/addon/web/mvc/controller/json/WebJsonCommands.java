package org.springframework.roo.addon.web.mvc.controller.json;

import static org.springframework.roo.shell.OptionContexts.UPDATE;
import static org.springframework.roo.shell.OptionContexts.UPDATE_PROJECT;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Commands which provide JSON functionality through Spring MVC controllers.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Component
@Service
public class WebJsonCommands implements CommandMarker {

    @Reference private WebJsonOperations webJsonOperations;

    @CliCommand(value = "web mvc json add", help = "Adds @RooJson annotation to target type")
    public void add(
            @CliOption(key = "jsonObject", mandatory = true, help = "The JSON-enabled object which backs this Spring MVC controller.") final JavaType jsonObject,
            @CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = UPDATE_PROJECT, help = "The java type to apply this annotation to") final JavaType target) {

        webJsonOperations.annotateType(target, jsonObject);
    }

    @CliCommand(value = "web mvc json all", help = "Adds or creates MVC controllers annotated with @RooWebJson annotation")
    public void all(
            @CliOption(key = "package", mandatory = false, optionContext = UPDATE, help = "The package in which new controllers will be placed") final JavaPackage javaPackage) {

        webJsonOperations.annotateAll(javaPackage);
    }

    @CliAvailabilityIndicator({ "web mvc json add", "web mvc json all" })
    public boolean isCommandAvailable() {
        return webJsonOperations.isWebJsonCommandAvailable();
    }

    @CliAvailabilityIndicator({ "web mvc json setup" })
    public boolean isSetupAvailable() {
        return webJsonOperations.isWebJsonInstallationPossible();
    }

    @CliCommand(value = "web mvc json setup", help = "Set up Spring MVC to support JSON")
    public void setup() {
        webJsonOperations.setup();
    }
}
