package org.springframework.roo.obr.addon.search;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.roo.felix.BundleSymbolicName;
import org.springframework.roo.obr.addon.search.model.ObrBundle;
import org.springframework.roo.support.api.AddOnSearch;

/**
 * Interface for operations offered by this addon.
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 */
public interface ObrAddOnSearchOperations extends AddOnSearch {

  enum InstallOrUpgradeStatus {
    FAILED, INVALID_OBR_URL, PGP_VERIFICATION_NEEDED, SHELL_RESTART_NEEDED, SUCCESS;
  }

  /**
   * Display information for a given ({@link ObrAddOnBundleSymbolicName}.
   * Information is piped to standard JDK {@link Logger#info}
   * 
   * @param bsn the bundle symbolic name (required)
   */
  void addOnInfo(ObrAddOnBundleSymbolicName bsn);

  /**
   * Display information for a given bundle ID. Information is piped to
   * standard JDK {@link Logger#info}
   * 
   * @param bundleId the bundle ID (required)
   */
  void addOnInfo(String bundleId);

  /**
   * Find all add-ons presently known to this Roo instance, including add-ons
   * which have not been downloaded or installed by the user.
   * <p>
   * Information is optionally emitted to the console via {@link Logger#info}.
   * @param searchTerms comma separated list of search terms (required)
   * @return the total number of matches found, even if only some of these are
   *         displayed due to maxResults (or null if the add-on list is
   *         unavailable for some reason, eg network problems etc)
   * @since 1.2.0
   */
  List<ObrBundle> findAddons(String searchTerms, SearchType type);

  /**
   * Get a list of all cached addon bundles.
   * 
   * @return a set of addon bundles
   */
  Map<String, ObrBundle> getAddOnCache();

  /**
   * Install addon with given {@link ObrAddOnBundleSymbolicName}.
   * 
   * @param bsn the bundle symbolic name (required)
   */
  InstallOrUpgradeStatus installAddOn(ObrAddOnBundleSymbolicName bsn);


  /**
   * Install addon with given URL
   * 
   * @param url
   * @return
   */
  InstallOrUpgradeStatus installAddOnByUrl(String url);

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
   * List all installed addons. Equivalent to 'lb' felix command
   */
  void list();

}
