package org.springframework.roo.addon.web.flow;

import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.lifecycle.ScopeDevelopmentShell;
import org.springframework.roo.support.util.Assert;

/**
 * Commands for the 'web flow' add-on to be used by the Roo shell.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
@ScopeDevelopmentShell
public class WebFlowCommands implements CommandMarker {
	
	private WebFlowOperations webFlowOperations;
	
	public WebFlowCommands(WebFlowOperations webFlowOperations) {
		Assert.notNull(webFlowOperations, "Jms operations required");
		this.webFlowOperations = webFlowOperations;
	}
	
	@CliAvailabilityIndicator("web flow")
	public boolean isInstallWebFlowAvailable() {
		return webFlowOperations.isInstallWebFlowAvailable();
	}
	
	@CliCommand(value="web flow", help="Install Spring Web Flow configuration artifacts into your project")
	public void installWebFlow(
			@CliOption(key={"flowName"}, mandatory=false, help="The name for your web flow") String flowName) {
		webFlowOperations.installWebFlow(flowName);
	}
}