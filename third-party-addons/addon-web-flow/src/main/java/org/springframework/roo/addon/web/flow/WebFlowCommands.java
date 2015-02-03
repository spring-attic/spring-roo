package org.springframework.roo.addon.web.flow;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Commands for the 'web flow' add-on to be used by the Roo shell.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component
@Service
public class WebFlowCommands implements CommandMarker {

    @Reference private WebFlowOperations webFlowOperations;

    @CliCommand(value = "web flow", help = "Install Spring Web Flow configuration artifacts into your project")
    public void installWebFlow(
            @CliOption(key = { "flowName" }, mandatory = false, help = "The name for your web flow") final String flowName) {

        webFlowOperations.installWebFlow(flowName);
    }

    @CliAvailabilityIndicator("web flow")
    public boolean isInstallWebFlowAvailable() {
        return webFlowOperations.isWebFlowInstallationPossible();
    }
}