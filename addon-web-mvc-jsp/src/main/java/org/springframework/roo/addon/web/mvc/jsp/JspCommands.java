package org.springframework.roo.addon.web.mvc.jsp;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Commands for Web-related add-on to be used by the Roo shell.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
@Component
@Service
public class JspCommands implements CommandMarker {
	
	@Reference private JspOperations jspOperations;
	
	@CliAvailabilityIndicator("controller class")
	public boolean isInstallWebFlowAvailable() {
		return jspOperations.isControllerCommandAvailable();
	}
	
	@CliCommand(value="controller class", help="Create a new manual Controller (ie where you write the methods)")
	public void newController(
			@CliOption(key={"class",""}, mandatory=true, help="The path and name of the controller object to be created") JavaType controller,
			@CliOption(key="preferredMapping", mandatory=false, help="Indicates a specific request mapping path for this controller (eg /foo/)") String preferredMapping) {
		jspOperations.createManualController(controller, preferredMapping);
	}
}