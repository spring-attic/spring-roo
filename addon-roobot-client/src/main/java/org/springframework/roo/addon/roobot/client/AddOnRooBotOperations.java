package org.springframework.roo.addon.roobot.client;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.roo.addon.roobot.client.model.Bundle;
import org.springframework.roo.felix.BundleSymbolicName;
import org.springframework.roo.support.api.AddOnSearch;

/**
 * Interface for operations offered by this addon.
 *
 * @author Stefan Schmidt
 * @since 1.1
 */
public interface AddOnRooBotOperations extends AddOnSearch {

	public enum InstallOrUpgradeStatus {
		SUCCESS, FAILED, INVALID_OBR_URL, PGP_VERIFICATION_NEEDED, SHELL_RESTART_NEEDED;
	}
	
	/**
	 * Display information for a given ({@link AddOnBundleSymbolicName}. 
	 * Information is piped to standard JDK {@link Logger#info}
	 * 
	 * @param bsn the bundle symbolic name (required)
	 */
	void addOnInfo(AddOnBundleSymbolicName bsn);
	
	/**
	 * Display information for a given bundle ID. 
	 * Information is piped to standard JDK {@link Logger#info}
	 * 
	 * @param bundleId the bundle ID (required)
	 */
	void addOnInfo(String bundleId);
	
	/**
	 * Find all add-ons presently known to this Roo instance, including add-ons which have
	 * not been downloaded or installed by the user.
	 * 
	 * <p>
	 * Information is optionally emitted to the console via {@link Logger#info}.
	 * 
	 * @param showFeedback if false will never output any messages to the console (required)
	 * @param searchTerms comma separated list of search terms (required)
	 * @param refresh attempt a fresh download of roobot.xml (optional)
	 * @param linesPerResult maximum number of lines per add-on (optional)
	 * @param maxResults maximum number of results to display (optional)
	 * @param trustedOnly display only trusted add-ons in search results (optional)
	 * @param compatibleOnly display only compatible add-ons in search results (optional)
	 * @param communityOnly display only community-provided add-ons in search results (optional)
	 * @param requiresCommand display only add-ons which offer the specified command (optional)
	 * @return the total number of matches found, even if only some of these are displayed due to maxResults
	 * (or null if the add-on list is unavailable for some reason, eg network problems etc)
	 * @since 1.2.0
	 */
	List<Bundle> findAddons(boolean showFeedback, String searchTerms, boolean refresh, int linesPerResult, int maxResults, boolean trustedOnly, boolean compatibleOnly, boolean communityOnly, String requiresCommand);
	
	/**
	 * Install addon with given {@link AddOnBundleSymbolicName}.
	 * 
	 * @param bsn the bundle symbolic name (required)
	 */
	InstallOrUpgradeStatus installAddOn(AddOnBundleSymbolicName bsn);
	
	/**
	 * Install addon with given Add-On ID.
	 * 
	 * @param bundleId the bundle id (required)
	 */
	InstallOrUpgradeStatus installAddOn(String bundleId);
	
	/**
	 * Remove addon with given {@link BundleSymbolicName}.
	 * 
	 * @param bsn the bundle symbolic name (required)
	 */
	InstallOrUpgradeStatus removeAddOn(BundleSymbolicName bsn);
	
	/**
	 * Display information about the available upgrades
	 * 
	 * @param addonStabilityLevel the add-on stability level taken into account for the upgrade 
	 */
	void upgradesAvailable(AddOnStabilityLevel addonStabilityLevel);
	
	/**
	 * Upgrade all add-ons according to the user defined add-on stability level. 
	 */
	void upgradeAddOns();
	
	/**
	 * Upgrade specific add-on only.
	 * 
	 * @param bsn the bundle symbolic name (required)
	 */
	InstallOrUpgradeStatus upgradeAddOn(AddOnBundleSymbolicName bsn);
	
	/**
	 * Upgrade specific add-on only.
	 * 
	 * @param bundleId the bundle id (required)
	 */
	InstallOrUpgradeStatus upgradeAddOn(String bundleId);
	
	/**
	 * Define the stability level for add-on upgrades
	 * 
	 * @param addOnStabilityLevel the stability level for add-on upgrades (required)
	 */
	void upgradeSettings(AddOnStabilityLevel addOnStabilityLevel);
	
	/**
	 * Get a list of all cached addon bundles. 
	 * 
	 * @param refresh refresh attempt a fresh download of roobot.xml (optional)
	 * @return a set of addon bundles
	 */
	Map<String, Bundle> getAddOnCache(boolean refresh);
}