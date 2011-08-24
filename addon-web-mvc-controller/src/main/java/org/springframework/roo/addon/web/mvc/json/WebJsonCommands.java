package org.springframework.roo.addon.web.mvc.json;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.mvc.controller.WebMvcOperations;
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
@Component(immediate = true)
@Service
public class WebJsonCommands implements CommandMarker {
	
	@Reference private WebJsonOperations operations;
	@Reference private WebMvcOperations mvcOperations;

	@CliAvailabilityIndicator({ "web mvc json add", "web mvc json all" }) 
	public boolean isCommandAvailable() {
		return operations.isCommandAvailable();
	}
	
	@CliAvailabilityIndicator({ "web mvc json setup" }) 
	public boolean isSetupAvailable() {
		return operations.isSetupAvailable();
	}
	
	@CliCommand(value = "web mvc json setup", help = "Setup Spring MVC for Json support.")
	public void setup() {
		mvcOperations.installMinmalWebArtefacts();
	}

	@CliCommand(value = "web mvc json add", help = "Adds @RooJson annotation to target type") 
	public void add(
			@CliOption(key = "jsonObject", mandatory = true, help = "The JSON-enabled object which backs this Spring MVC controller.") JavaType jsonObject,
			@CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The java type to apply this annotation to") JavaType target) {		
		operations.annotateType(target, jsonObject);
	}
	
	@CliCommand(value = "web mvc json all", help = "Adds or creates MVC controllers annotated with @RooWebJson annotation") 
	public void all(
			@CliOption(key = "package", mandatory = false, optionContext = "update", help = "The package in which new controllers will be placed") JavaPackage javaPackage) {
		operations.annotateAll(javaPackage);
	}
}
