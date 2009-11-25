package org.springframework.roo.addon.mvc.jsp;

import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.lifecycle.ScopeDevelopmentShell;
import org.springframework.roo.support.util.Assert;

/**
 * Commands for Web-related add-on to be used by the Roo shell.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
@ScopeDevelopmentShell
public class JspCommands implements CommandMarker {
	
	private JspOperations jspOperations;
	
	public JspCommands(JspOperations webFlowOperations) {
		Assert.notNull(webFlowOperations, "Jms operations required");
		this.jspOperations = webFlowOperations;
	}
	
	@CliAvailabilityIndicator("controller class")
	public boolean isInstallWebFlowAvailable() {
		return jspOperations.isControllerCommandAvailable();
	}
	
	@CliCommand(value="controller class", help="Create a new manual Controller (ie where you write the methods)")
	public void newController(
			@CliOption(key={"name",""}, mandatory=true, help="The path and name of the controller object to be created") JavaType controller,
			@CliOption(key="preferredMapping", mandatory=false, help="Indicates a specific request mapping path for this controller (eg /foo/)") String preferredMapping) {
		jspOperations.createManualController(controller, preferredMapping);
	}
}