package org.springframework.roo.addon.roobot.client;

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
	 * Install addon with given {@link AddOnBundleSymbolicName}.
	 * 
	 * @param bsn the bundle symbolic name (required)
	 */
	void installAddOn(AddOnBundleSymbolicName bsn);
	
	/**
	 * Install addon with given Add-On ID.
	 * 
	 * @param bundleId the bundle id (required)
	 */
	void installAddOn(String bundleId);
	
	/**
	 * Remove addon with given {@link BundleSymbolicName}.
	 * 
	 * @param bsn the bundle symbolic name (required)
	 */
	void removeAddOn(BundleSymbolicName bsn);
	
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
	void upgradeAddOn(AddOnBundleSymbolicName bsn);
	
	/**
	 * Upgrade specific add-on only.
	 * 
	 * @param bundleId the bundle id (required)
	 */
	void upgradeAddOn(String bundleId);
	
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