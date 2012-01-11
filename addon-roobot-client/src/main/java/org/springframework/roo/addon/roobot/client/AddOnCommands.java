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

/**
 * Commands for this add-on.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component
@Service
public class AddOnCommands implements CommandMarker {

    @Reference private AddOnFeedbackOperations addOnfeedbackOperations;
    @Reference private AddOnRooBotOperations addOnRooBotOperations;
    @Reference private StaticFieldConverter staticFieldConverter;

    protected void activate(final ComponentContext context) {
        staticFieldConverter.add(Rating.class);
    }

    protected void deactivate(final ComponentContext context) {
        staticFieldConverter.remove(Rating.class);
    }

    @CliCommand(value = "addon feedback bundle", help = "Provide anonymous ratings and comments on a Spring Roo Add-on (your feedback will be published publicly)")
    public void feedbackBundle(
            @CliOption(key = "bundleSymbolicName", mandatory = true, help = "The bundle symbolic name for the add-on of interest") final BundleSymbolicName bsn,
            @CliOption(key = "rating", mandatory = true, help = "How much did you like this add-on?") final Rating rating,
            @CliOption(key = "comment", mandatory = false, help = "Your comments on this add-on eg \"this is my comment!\"; limit of 140 characters") final String comment) {

        addOnfeedbackOperations.feedbackBundle(bsn, rating, comment);
    }

    @CliCommand(value = "addon info bundle", help = "Provide information about a specific Spring Roo Add-on")
    public void infoBundle(
            @CliOption(key = "bundleSymbolicName", mandatory = true, help = "The bundle symbolic name for the add-on of interest") final AddOnBundleSymbolicName bsn) {

        addOnRooBotOperations.addOnInfo(bsn);
    }

    @CliCommand(value = "addon info id", help = "Provide information about a specific Spring Roo Add-on")
    public void infoId(
            @CliOption(key = { "", "searchResultId" }, mandatory = true, help = "The bundle ID as presented via the addon list or addon search command") final String bundleId) {

        addOnRooBotOperations.addOnInfo(bundleId);
    }

    @CliCommand(value = "addon install bundle", help = "Install Spring Roo Add-on")
    public void installBsn(
            @CliOption(key = "bundleSymbolicName", mandatory = true, help = "The bundle symbolic name for the add-on of interest") final AddOnBundleSymbolicName bsn) {
        addOnRooBotOperations.installAddOn(bsn);
    }

    @CliCommand(value = "addon install id", help = "Install Spring Roo Add-on")
    public void installId(
            @CliOption(key = { "", "searchResultId" }, mandatory = true, help = "The bundle ID as presented via the addon list or addon search command") final String bundleId) {

        addOnRooBotOperations.installAddOn(bundleId);
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
        addOnRooBotOperations.searchAddOns(true, null, refresh, linesPerResult,
                maxResults, trustedOnly, compatibleOnly, communityOnly, null);
    }

    @CliCommand(value = "addon remove", help = "Remove Spring Roo Add-on")
    public void remove(
            @CliOption(key = "bundleSymbolicName", mandatory = true, help = "The bundle symbolic name for the add-on of interest") final BundleSymbolicName bsn) {

        addOnRooBotOperations.removeAddOn(bsn);
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

        addOnRooBotOperations.searchAddOns(true, searchTerms, refresh,
                linesPerResult, maxResults, trustedOnly, compatibleOnly,
                communityOnly, requiresCommand);
    }

    @CliCommand(value = "addon upgrade all", help = "Upgrade all relevant Spring Roo Add-ons / Components for the current stability level")
    public void ugradeAll() {
        addOnRooBotOperations.upgradeAddOns();
    }

    @CliCommand(value = "addon upgrade available", help = "List available Spring Roo Add-on / Component upgrades")
    public void ugradeAvailable(
            @CliOption(key = "addonStabilityLevel", mandatory = false, help = "The stability level of add-ons or components which are presented for upgrading (default: ANY)") final AddOnStabilityLevel level) {

        addOnRooBotOperations.upgradesAvailable(level);
    }

    @CliCommand(value = "addon upgrade bundle", help = "Upgrade a specific Spring Roo Add-on / Component")
    public void ugradeBundle(
            @CliOption(key = "bundleSymbolicName", mandatory = true, help = "The bundle symbolic name for the add-on to upgrade") final AddOnBundleSymbolicName bsn) {

        addOnRooBotOperations.upgradeAddOn(bsn);
    }

    @CliCommand(value = "addon upgrade id", help = "Upgrade a specific Spring Roo Add-on / Component from a search result ID")
    public void ugradeId(
            @CliOption(key = { "", "searchResultId" }, mandatory = true, help = "The bundle ID as presented via the addon list or addon search command") final String bundleId) {

        addOnRooBotOperations.upgradeAddOn(bundleId);
    }

    @CliCommand(value = "addon upgrade settings", help = "Settings for Add-on upgrade operations")
    public void ugradeSettings(
            @CliOption(key = "addonStabilityLevel", mandatory = false, help = "The stability level of add-ons or components which are presented for upgrading") final AddOnStabilityLevel level) {
        addOnRooBotOperations.upgradeSettings(level);
    }
}