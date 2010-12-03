package org.springframework.roo.addon.roobot.client;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.felix.BundleSymbolicName;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.util.Assert;

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
	public void info(@CliOption(key="bundleSymbolicName", mandatory=false, help="The bundle symbolic name for the add-on of interest") AddOnBundleSymbolicName bsn,
			@CliOption(key="bundleId", mandatory=false, help="The bundle ID as presented via the addon list or addon search command") String bundleId) {
		Assert.isTrue(bsn == null || bundleId == null || bundleId.length() > 0, "Either the bundle symbolic name or a bundle ID need to be specified");
		if (bsn != null) {
			operations.addOnInfo(bsn);
		} else {
			operations.addOnInfo(bundleId);
		}
	}
	
	@CliCommand(value="addon list", help="List all known Spring Roo Add-ons")
	public void list(@CliOption(key="refresh", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="The bundle symbolic name for the add-on of interest") boolean refresh,
			@CliOption(key="linesPerResult", mandatory=false, unspecifiedDefaultValue="1", specifiedDefaultValue="1", help="The maximum number of lines displayed per add-on") int linesPerResult,
			@CliOption(key="maxResults", mandatory=false, unspecifiedDefaultValue="20", specifiedDefaultValue="20", help="The maximum number of add-ons to list") int maxResults,
			@CliOption(key="trustedOnly", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="Only display trusted add-ons in search results") boolean trustedOnly,
			@CliOption(key="compatibleOnly", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="Only display compatible add-ons in search results") boolean compatibleOnly,
			@CliOption(key="requiresCommand", mandatory=false, help="Only display compatible add-ons in search results") String requiresCommand) {
		operations.listAddOns(refresh, linesPerResult, maxResults, trustedOnly, compatibleOnly, requiresCommand);
	}
	
	@CliCommand(value="addon search", help="Search all known Spring Roo Add-ons")
	public void search(@CliOption(key="requiresDescription", mandatory=true, help="A comma separated list of search terms") String searchTerms,
			@CliOption(key="refresh", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="The bundle symbolic name for the add-on of interest") boolean refresh,
			@CliOption(key="linesPerResult", mandatory=false, unspecifiedDefaultValue="1", specifiedDefaultValue="1", help="The maximum number of lines displayed per add-on") int linesPerResult,
			@CliOption(key="maxResults", mandatory=false, unspecifiedDefaultValue="20", specifiedDefaultValue="20", help="The maximum number of add-ons to list") int maxResults,
			@CliOption(key="trustedOnly", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="Only display trusted add-ons in search results") boolean trustedOnly,
			@CliOption(key="compatibleOnly", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="Only display compatible add-ons in search results") boolean compatibleOnly,
			@CliOption(key="requiresCommand", mandatory=false, help="Only display compatible add-ons in search results") String requiresCommand) {
		operations.searchAddOns(searchTerms, refresh, linesPerResult, maxResults, trustedOnly, compatibleOnly, requiresCommand);
	}
	
	@CliCommand(value="addon install", help="Install Spring Roo Add-on")
	public void install(@CliOption(key="bundleSymbolicName", mandatory=false, help="The bundle symbolic name for the add-on of interest") AddOnBundleSymbolicName bsn,
			@CliOption(key="bundleId", mandatory=false, help="The bundle ID as presented via the addon list or addon search command") String bundleId) {
		Assert.isTrue(bsn == null || bundleId == null || bundleId.length() > 0, "Either the bundle symbolic name or a bundle ID need to be specified");
		if (bsn != null) {
			operations.installAddOn(bsn);
		} else {
			operations.installAddOn(bundleId);
		}
	}
	
	@CliCommand(value="addon remove", help="Remove Spring Roo Add-on")
	public void remove(@CliOption(key="bundleSymbolicName", mandatory=true, help="The bundle symbolic name for the add-on of interest") BundleSymbolicName bsn) {
		operations.removeAddOn(bsn);
	}
}