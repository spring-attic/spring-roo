package org.springframework.roo.addon.roobot.client;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.roobot.client.model.Rating;
import org.springframework.roo.felix.BundleSymbolicName;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.converters.StaticFieldConverter;
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
	@Reference private AddOnFeedbackOperations feedbackOperations;
	@Reference private StaticFieldConverter staticFieldConverter;

	protected void activate(ComponentContext context) {
		staticFieldConverter.add(Rating.class);
	}

	protected void deactivate(ComponentContext context) {
		staticFieldConverter.remove(Rating.class);
	}
	
	@CliCommand(value="addon info", help="Provide information about a specific Spring Roo Add-on")
	public void info(@CliOption(key={"", "bundleId"}, mandatory=false, help="The bundle ID as presented via the addon list or addon search command") String bundleId,
			@CliOption(key="bundleSymbolicName", mandatory=false, help="The bundle symbolic name for the add-on of interest") AddOnBundleSymbolicName bsn) {
		Assert.isTrue(bsn != null || (bundleId != null && bundleId.length() > 0), "Either a --bundleId (from a search) or --bundleSymbolicName is required");
		if (bsn != null) {
			operations.addOnInfo(bsn);
		} else {
			operations.addOnInfo(bundleId);
		}
	}
	
	@CliCommand(value="addon list", help="List all known Spring Roo Add-ons (up to the maximum number displayed on a single page)")
	public void list(@CliOption(key="refresh", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="Refresh the add-on index from the Internet") boolean refresh,
			@CliOption(key="linesPerResult", mandatory=false, unspecifiedDefaultValue="1", specifiedDefaultValue="1", help="The maximum number of lines displayed per add-on") int linesPerResult,
			@CliOption(key="maxResults", mandatory=false, unspecifiedDefaultValue="99", specifiedDefaultValue="99", help="The maximum number of add-ons to list") int maxResults,
			@CliOption(key="trustedOnly", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="Only display trusted add-ons in search results") boolean trustedOnly,
			@CliOption(key="compatibleOnly", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="Only display compatible add-ons in search results") boolean compatibleOnly) {
		// A list is really just a search without criteria. We keep some criteria to allow reasonable filtering and display logic to take place.
		operations.searchAddOns(true, null, refresh, linesPerResult, maxResults, trustedOnly, compatibleOnly, null);
	}
	
	@CliCommand(value="addon search", help="Search all known Spring Roo Add-ons")
	public void search(@CliOption(key="requiresDescription", mandatory=false, help="A comma separated list of search terms") String searchTerms,
			@CliOption(key="refresh", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="Refresh the add-on index from the Internet") boolean refresh,
			@CliOption(key="linesPerResult", mandatory=false, unspecifiedDefaultValue="1", specifiedDefaultValue="1", help="The maximum number of lines displayed per add-on") int linesPerResult,
			@CliOption(key="maxResults", mandatory=false, unspecifiedDefaultValue="25", specifiedDefaultValue="25", help="The maximum number of add-ons to list") int maxResults,
			@CliOption(key="trustedOnly", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="Only display trusted add-ons in search results") boolean trustedOnly,
			@CliOption(key="compatibleOnly", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="Only display compatible add-ons in search results") boolean compatibleOnly,
			@CliOption(key="requiresCommand", mandatory=false, help="Only display add-ons in search results that offer this command") String requiresCommand) {
		operations.searchAddOns(true, searchTerms, refresh, linesPerResult, maxResults, trustedOnly, compatibleOnly, requiresCommand);
	}
	
	@CliCommand(value="addon install", help="Install Spring Roo Add-on")
	public void install(@CliOption(key={"", "bundleId"}, mandatory=false, help="The bundle ID as presented via the addon list or addon search command") String bundleId,
			@CliOption(key="bundleSymbolicName", mandatory=false, help="The bundle symbolic name for the add-on of interest") AddOnBundleSymbolicName bsn) {
		Assert.isTrue(bsn != null || (bundleId != null && bundleId.length() > 0), "Either a --bundleId (from a search) or --bundleSymbolicName is required");
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
	
	@CliCommand(value="addon feedback bundle", help="Provide anonymous ratings and comments on a Spring Roo Add-on (your feedback will be published publicly)")
	public void feedbackBundle(@CliOption(key="bundleSymbolicName", mandatory=true, help="The bundle symbolic name for the add-on of interest") BundleSymbolicName bsn,
			@CliOption(key="rating", mandatory=true, help="How much did you like this add-on?") Rating rating,
			@CliOption(key="comment", mandatory=false, help="Your comments on this add-on eg \"this is my comment!\"; limit of 140 characters") String comment) {
		feedbackOperations.feedbackBundle(bsn, rating, comment);
	}
	
}