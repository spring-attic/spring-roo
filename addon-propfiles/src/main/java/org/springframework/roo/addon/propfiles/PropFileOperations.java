package org.springframework.roo.addon.propfiles;

/**
 * Provides an interface to {@link PropFileOperationsImpl}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public interface PropFileOperations {

  /**
   * Check if properties command are available
   * 
   * @return
   */
  boolean arePropertiesCommandAvailable();

  /**
   * Adds or updates the specified property on an specific profile of a module, throwing
   * an exception if the property already exists and force is false.
   * 
   * @param moduleName module where profile will be located (required) 
   * @param key the property key to update (required)
   * @param value the property value to set into the property key (required)
   * @param profile application profile where current property should be added
   * @param force indicates if an existing value for a given key should be
   *            replaced or not
   */
  void addProperty(String moduleName, String key, String value, String profile, boolean force);

  /**
   * Removes the specified property from an specific profile of a module.
   * 
   * @param moduleName module where profile will be located (required) 
   * @param key the property key to remove (required)
   * @param profile application profile where current property should be removed
   */
  void removeProperty(String moduleName, String key, String profile);

  /**
   * Retrieves all property key from an specific profile of a module.
   * 
   * @param moduleName module where profile will be located (required)
   * @param profile application profile where current property should be removed
   */
  void listProperties(String moduleName, String profile);

}
