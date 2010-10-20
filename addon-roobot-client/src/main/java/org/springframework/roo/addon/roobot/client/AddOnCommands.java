package org.springframework.roo.addon.roobot.client;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.felix.BundleSymbolicName;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Commands for this add-on.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component
@Service
public class AddOnCommands implements CommandMarker {

	@Reference private AddOnRooBotOperations operations;
	
	@CliCommand(value="addon info", help="Provide information about a specific Spring Roo Add-on")
	public void info(@CliOption(key="bundleSymbolicName", mandatory=true, help="The bundle symbolic name for the addon of interest") AddOnBundleSymbolicName bsn) {
		operations.addOnInfo(bsn);
	}
	
	@CliCommand(value="addon list", help="List all known Spring Roo Add-ons")
	public void refresh(@CliOption(key="refresh", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="The bundle symbolic name for the addon of interest") boolean refresh) {
		operations.listAddOns(refresh);
	}
	
	@CliCommand(value="addon install", help="Install Spring Roo Add-on")
	public void install(@CliOption(key="bundleSymbolicName", mandatory=true, help="The bundle symbolic name for the addon of interest") AddOnBundleSymbolicName bsn) {
		operations.installAddOn(bsn);
	}
	
	@CliCommand(value="addon remove", help="Remove Spring Roo Add-on")
	public void remove(@CliOption(key="bundleSymbolicName", mandatory=true, help="The bundle symbolic name for the addon of interest") BundleSymbolicName bsn) {
		operations.removeAddOn(bsn);
	}
}