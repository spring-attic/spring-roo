package org.springframework.roo.obr.addon.search;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.felix.BundleSymbolicName;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Commands to manage Addons on OBR repositories
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 */
@Component
@Service
public class ObrAddOnCommands implements CommandMarker {

    @Reference private ObrAddOnSearchOperations operations;


    @CliCommand(value = "addon info bundle", help = "Provide information about a specific Spring Roo Add-on")
    public void infoBundle(
            @CliOption(key = "bundleSymbolicName", mandatory = true, help = "The bundle symbolic name for the add-on of interest") final ObrAddOnBundleSymbolicName bsn) {

    	operations.addOnInfo(bsn);
    }

    @CliCommand(value = "addon info id", help = "Provide information about a specific Spring Roo Add-on")
    public void infoId(
            @CliOption(key = { "", "searchResultId" }, mandatory = true, help = "The bundle ID as presented via the addon list or addon search command") final String bundleId) {

    	operations.addOnInfo(bundleId);
    }

    @CliCommand(value = "addon install bundle", help = "Install Spring Roo Add-on")
    public void installBsn(
            @CliOption(key = "bundleSymbolicName", mandatory = true, help = "The bundle symbolic name for the add-on of interest") final ObrAddOnBundleSymbolicName bsn) {
    	operations.installAddOn(bsn);
    }

    @CliCommand(value = "addon install id", help = "Install Spring Roo Add-on")
    public void installId(
            @CliOption(key = { "", "searchResultId" }, mandatory = true, help = "The bundle ID as presented via the addon list or addon search command") final String bundleId) {

    	operations.installAddOn(bundleId);
    }

    @CliCommand(value = "addon list", help = "List all known Spring Roo Add-ons (up to the maximum number displayed on a single page)")
    public void list(
            @CliOption(key = "refresh", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Refresh the add-on index from the Internet") final boolean refresh,
            @CliOption(key = "linesPerResult", mandatory = false, unspecifiedDefaultValue = "2", specifiedDefaultValue = "2", help = "The maximum number of lines displayed per add-on") final int linesPerResult,
            @CliOption(key = "maxResults", mandatory = false, unspecifiedDefaultValue = "99", specifiedDefaultValue = "99", help = "The maximum number of add-ons to list") final int maxResults,
            @CliOption(key = "trustedOnly", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Only display trusted add-ons in search results") final boolean trustedOnly,
            @CliOption(key = "communityOnly", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Only display community provided add-ons in search results") final boolean communityOnly,
            @CliOption(key = "compatibleOnly", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Only display compatible add-ons in search results") final boolean compatibleOnly) {

        // A list is really just a search without criteria. We keep some
        // criteria to allow reasonable filtering and display logic to take
        // place.
    	operations.searchAddOns(true, null, refresh, linesPerResult,
                maxResults, trustedOnly, compatibleOnly, communityOnly, null);
    }

    @CliCommand(value = "addon remove", help = "Remove Spring Roo Add-on")
    public void remove(
            @CliOption(key = "bundleSymbolicName", mandatory = true, help = "The bundle symbolic name for the add-on of interest") final BundleSymbolicName bsn) {

    	operations.removeAddOn(bsn);
    }

    @CliCommand(value = "addon search", help = "Search all known Spring Roo Add-ons")
    public void search(
            @CliOption(key = { "", "requiresDescription" }, mandatory = false, specifiedDefaultValue = "*", unspecifiedDefaultValue = "*", help = "A comma separated list of search terms") final String searchTerms,
            @CliOption(key = "refresh", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Refresh the add-on index from the Internet") final boolean refresh,
            @CliOption(key = "linesPerResult", mandatory = false, unspecifiedDefaultValue = "2", specifiedDefaultValue = "2", help = "The maximum number of lines displayed per add-on") final int linesPerResult,
            @CliOption(key = "maxResults", mandatory = false, unspecifiedDefaultValue = "20", specifiedDefaultValue = "20", help = "The maximum number of add-ons to list") final int maxResults,
            @CliOption(key = "trustedOnly", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Only display trusted add-ons in search results") final boolean trustedOnly,
            @CliOption(key = "compatibleOnly", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Only display compatible add-ons in search results") final boolean compatibleOnly,
            @CliOption(key = "communityOnly", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Only display community provided add-ons in search results") final boolean communityOnly,
            @CliOption(key = "requiresCommand", mandatory = false, help = "Only display add-ons in search results that offer this command") final String requiresCommand) {

    	operations.searchAddOns(true, searchTerms, refresh,
                linesPerResult, maxResults, trustedOnly, compatibleOnly,
                communityOnly, requiresCommand);
    }

}