package org.springframework.roo.application.config;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.springframework.roo.project.LogicalPath;

/**
 * Provides an interface to {@link ApplicationConfigServiceImpl}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public interface ApplicationConfigService {
    
    /**
     * Adds some property to application config file.
     * 
     * @param key string that identifies the property
     * @param value string with the value assigned to the property
     */
    void addProperty(String key, String value);
    
    /**
     * Adds some property to application config file including given prefix
     * 
     * @param prefix string that will be included as property prefix
     * @param key string that identifies the property
     * @param value string with the value assigned to the property
     */
    void addProperty(String prefix, String key, String value);

    /**
     * Adds the contents of the properties map to application config file.
     * 
     * @param properties the map of properties to add
     */
    void addProperties(Map<String, String> properties);
    
    /**
     * Adds the contents of the properties map to application config file using
     * the same prefix to every properties included on map.
     * 
     * @param prefix string that will be included as property prefix
     * @param properties the map of properties to add
     */
    void addProperties(String prefix, Map<String, String> properties);

    /**
     * Changes the specified property.
     * 
     * @param key the property key to update (required)
     * @param value the property value to set into the property key (required)
     */
    void updateProperty(String key, String value);
    
    /**
     * Changes the specified property including prefix.
     * 
     * @param prefix included on given property
     * @param key the property key to update (required)
     * @param value the property value to set into the property key (required)
     */
    void updateProperty(String prefix, String key, String value);
    
    /**
     * Update the contents of the properties map to application config file.
     * 
     * @param properties the map of properties to update
     */
    void updateProperties(Map<String, String> properties);
    
    /**
     * Adds the contents of the properties map to application config file using
     * the same prefix to every properties included on map.
     * 
     * @param prefix included for every properties
     * @param properties the map of properties to update
     */
    void updateProperties(String prefix, Map<String, String> properties);


    /**
     * Retrieves all property key/value pairs from the specified property,
     * throwing an exception if the file does not exist.
     * 
     * @return the key/value pairs (may return null if the property file does
     *         not exist)
     */
    Map<String, String> getProperties();
    
    /**
     * Retrieves all property keys from the specified property, throwing an
     * exception if the file does not exist.
     * 
     * @param includeValues if true, appends (" = theValue") to each returned
     *            string
     * @return the keys (may return null if the property file does not exist)
     */
    SortedSet<String> getPropertyKeys(boolean includeValues);
    
    /**
     * Retrieves all property keys from the specified property, throwing an
     * exception if the file does not exist.
     * 
     * @param prefix string that identifies property prefix. (if defined, only 
     *        properties that starts with it will be returned)
     * @param includeValues if true, appends (" = theValue") to each returned
     *            string
     * @return the keys (may return null if the property file does not exist)
     */
    SortedSet<String> getPropertyKeys(String prefix, boolean includeValues);

    /**
     * Retrieves the specified property, returning null if the property or file
     * does not exist.
     * 
     * @param key the property key to retrieve (required)
     * @return the property value (may return null if the property file or
     *         requested property does not exist)
     */
    String getProperty(String key);
    
    /**
     * Retrieves the specified property, returning null if the property
     * does not exist.
     * 
     * @param prefix included on given property
     * @param key the property key to retrieve (required)
     * @return the property value (may return null if the property file or
     *         requested property does not exist)
     */
    String getProperty(String prefix, String key);

    /**
     * Removes the specified property.
     * 
     * @param key the property key to remove (required)
     */
    void removeProperty(String key);
    
    /**
     * Removes the specified property including prefix.
     * 
     * @param prefix included on given property key
     * @param key the property key to remove (required)
     */
    void removeProperty(String prefix, String key);
    
    /**
     * Removes the specified properties.
     * 
     * @param keys list of property keys to remove (required)
     */
    void removeProperties(List<String> keys);
    
    /**
     * Removes the specified properties.
     * 
     * @param prefix that identifies property to be removed
     */
    void removePropertiesByPrefix(String prefix);
    
    /**
     * Method that returns current location of Spring Config file 
     * (if user has not modified it should return src/main/resources/application.properties)
     * 
     * @return string with current location of Spring Config file
     */
    String getSpringConfigLocation(); 
    
    /**
     * Method that checks if Spring config file exists. Uses getSpringConfigLocation method to
     * obtain location.
     * 
     * @return boolean true if exists
     */
    boolean existsSpringConfigFile();
}