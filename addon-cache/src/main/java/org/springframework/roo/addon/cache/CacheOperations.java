package org.springframework.roo.addon.cache;

import org.springframework.roo.addon.cache.providers.CacheProvider;

/**
 * Interface to {@link CacheOperationsImpl}.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public interface CacheOperations {

  /**
   * Method that checks if cache setup operation is available or not.
   * 
   * "cache setup" command will be available only if some project was generated 
   * and persistence was installed.
   * 
   * @return true if some project was created on focused directory and persistence 
   * was installed.
   */
  boolean isCacheSetupAvailable();

  /**
   * Method that makes the necessary operations to install intermediate memory on 
   * generated project.
   * 
   * @param provider the {@link CacheProvider} to manage the intermediate memory.
   * @param the String with the profile for which the provider properties should 
   *            be added, if any.
   */
  void setupCache(CacheProvider provider, String profile);

}
