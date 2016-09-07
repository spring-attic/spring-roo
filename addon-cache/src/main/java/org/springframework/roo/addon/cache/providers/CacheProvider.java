package org.springframework.roo.addon.cache.providers;

/**
 * Provides a contract to follow by intermediate memory providers.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public interface CacheProvider {

  public static final String CACHE_TYPE_PROPERTY_KEY = "spring.cache.type";

  /**
   * Returns the name of the current intermediate memory provider.
   * 
   * @return the String with the name of the provider.
   */
  String getName();

  /**
   * Check if provider is installed in the project by searching its dependency within 
   * all the application modules.
   * 
   * @return true if the provider is installed in the project.
   */
  boolean isInstalled();

  /**
   * Installs the cache provider in the project.
   * 
   * @param the String with the profile for which the provider properties should 
   *            be added, if any.
   */
  void setup(String profile);

}
