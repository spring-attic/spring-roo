package org.springframework.roo.addon.roobot.client;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.springframework.roo.felix.BundleSymbolicName;

/**
 * Interface for operations offered by this addon.
 *
 * @author Stefan Schmidt
 * @since 1.1
 */
public interface AddOnRooBotOperations {


	/**
	 * Display information for a given ({@link AddOnBundleSymbolicName}. 
	 * Information is piped to standard JDK {@link Logger.info}
	 * 
	 * @param the bundle symbolic name (required)
	 */
	void addOnInfo(AddOnBundleSymbolicName bsn);
	
	/**
	 * List all registered addons presently known to the Roo Shell.
	 * Information is piped to standard JDK {@link Logger.info}
	 * 
	 * @param refresh attempt a fresh download of roobot.xml (optional)
	 * @param linesPerResult maximum number of lines per add-on
	 * @param maxResults maximum number of results
	 */
	void listAddOns(boolean refresh, int linesPerResult, int maxResults);
	
	/**
	 * Retrieve a set of Addon bundle symbolic names.
	 * 
	 * @return a set of addon bundle symbolic names (never null but may be empty)
	 */
	Set<String> getAddOnBsnSet();
	
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
	 * Get a list of all cached addon bundles. 
	 * 
	 * @param refresh refresh attempt a fresh download of roobot.xml (optional)
	 * @return a set of addon bundles
	 */
	Map<String, AddOnBundleInfo> getAddOnCache(boolean refresh);
}