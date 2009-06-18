package org.springframework.roo.addon.web.mvc.controller;

import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.lifecycle.ScopeDevelopmentShell;
import org.springframework.roo.support.util.Assert;

/**
 * Commands for the 'mvc controller' add-on to be used by the ROO shell.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
@ScopeDevelopmentShell
public class ControllerCommands implements CommandMarker {
	
	private ControllerOperations controllerOperations;
	
	public ControllerCommands(ControllerOperations controllerOperations) {
		Assert.notNull(controllerOperations, "ControllerOperations instance required");
		this.controllerOperations = controllerOperations;
	}
	
	/**
	 * @return true if the "new controller" command is available at this moment
	 */
	@CliAvailabilityIndicator({"new controller automatic", "new controller manu"})
	public boolean isNewControllerAvailable() {
		return controllerOperations.isNewControllerAvailable();
	}
	
	@CliCommand(value="new controller automatic", help="Create a new automatic Controller (ie where we maintain CRUD automatically)")
	public void newController(
			@CliOption(key={"name",""}, mandatory=true, help="The path and name of the controller object to be created") JavaType controller,
			@CliOption(key="formBackingObject", mandatory=false, optionContext="update,project", unspecifiedDefaultValue="*", help="The name of the entity object which the controller exposes to the web tier") JavaType entity) {
		controllerOperations.createAutomaticController(controller, entity);
	}

	@CliCommand(value="new controller manual", help="Create a new manual Controller (ie where you write the methods)")
	public void newController(
			@CliOption(key={"name",""}, mandatory=true, help="The path and name of the controller object to be created") JavaType controller,
			@CliOption(key="preferredMapping", mandatory=false, help="Indicates a specific request mapping path for this controller (eg /foo/)") String preferredMapping) {
		controllerOperations.createManualController(controller, preferredMapping);
	}
}