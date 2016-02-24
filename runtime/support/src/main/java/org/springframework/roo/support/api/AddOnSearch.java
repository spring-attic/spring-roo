package org.springframework.roo.support.api;

import java.util.logging.Logger;

/**
 * Interface defining an add-on search service.
 * <p>
 * This interface is included in the support module because several of Roo's
 * core infrastructure modules require add-on search capabilities.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @since 1.1.1
 */
public interface AddOnSearch {

  public enum SearchType {
    ADDON, LIBRARY, JDBCDRIVER;
  }

  /**
   * Search all add-ons presently known this Roo instance, including add-ons
   * which have not been downloaded or installed by the user.
   * <p>
   * Information is optionally emitted to the console via {@link Logger#info}.
   * 
   * @param searchTerms comma separated list of search terms (required)
   * @return the total number of matches found, even if only some of these are
   *         displayed due to maxResults (or null if the add-on list is
   *         unavailable for some reason, eg network problems etc)
   */
  Integer searchAddOns(String searchTerms, SearchType type);
}
