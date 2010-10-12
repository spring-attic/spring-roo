package org.springframework.roo.addon.roobot.client;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Commands for addon manager.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component
@Service
public class AddOnCommands implements CommandMarker {

	@Reference private AddOnRooBotOperations operations;
	
	@CliCommand(value="addon info", help="Provide information about Spring Roo addons")
	public void info(@CliOption(key="bundleSymbolicName", mandatory=true, help="The bundle symbolic name for the addon of interest") AddOnBundleSymbolicName bsn) {
		operations.addOnInfo(bsn);
	}
	
	@CliCommand(value="addon list", help="List all known Spring Roo addons")
	public void refresh(@CliOption(key="refresh", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="The bundle symbolic name for the addon of interest") boolean refresh) {
		operations.listAddOns(refresh);
	}
	
	@CliCommand(value="addon install", help="Install Roo Addon")
	public void install(@CliOption(key="bundleSymbolicName", mandatory=true, help="The bundle symbolic name for the addon of interest") AddOnBundleSymbolicName bsn) {
		operations.installAddOn(bsn);
	}
}