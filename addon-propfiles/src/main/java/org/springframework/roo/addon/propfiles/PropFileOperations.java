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
     * Adds or updates the specified property on an specific profile, throwing
     * an exception if the property already exists and force is false.
     * 
     * @param key the property key to update (required)
     * @param value the property value to set into the property key (required)
     * @param profile application profile where current property should be added
     * @param force indicates if an existing value for a given key should be
     *            replaced or not
     */
    void addProperty(String key, String value, String profile, boolean force);

    /**
     * Removes the specified property from an specific profile.
     * 
     * @param key the property key to remove (required)
     * @param profile application profile where current property should be removed
     */
    void removeProperty(String key, String profile);

    /**
     * Retrieves all property key from an specific profile.
     * 
     * @param profile application profile where current property should be removed
     */
    void listProperties(String profile);

}