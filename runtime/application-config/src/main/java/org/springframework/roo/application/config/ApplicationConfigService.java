package org.springframework.roo.application.config;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

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
     * @param profile string with profile where configuration will be located.
     */
    void addProperty(String key, String value, String profile);
    
    /**
     * Adds some property to application config file including given prefix
     * 
     * @param prefix string that will be included as property prefix
     * @param key string that identifies the property
     * @param value string with the value assigned to the property
     * @param profile string with profile where configuration will be located.
     */
    void addProperty(String prefix, String key, String value, String profile);

    /**
     * Adds the contents of the properties map to application config file.
     * 
     * @param properties the map of properties to add
     * @param profile string with profile where configuration will be located.
     */
    void addProperties(Map<String, String> properties, String profile);
    
    /**
     * Adds the contents of the properties map to application config file using
     * the same prefix to every properties included on map.
     * 
     * @param prefix string that will be included as property prefix
     * @param properties the map of properties to add
     * @param profile string with profile where configuration will be located.
     */
    void addProperties(String prefix, Map<String, String> properties, String profile);

    /**
     * Changes the specified property.
     * 
     * @param key the property key to update (required)
     * @param value the property value to set into the property key (required)
     * @param profile string with profile where configuration will be located.
     */
    void updateProperty(String key, String value, String profile);
    
    /**
     * Changes the specified property including prefix.
     * 
     * @param prefix included on given property
     * @param key the property key to update (required)
     * @param value the property value to set into the property key (required)
     * @param profile string with profile where configuration will be located.
     */
    void updateProperty(String prefix, String key, String value, String profile);
    
    /**
     * Update the contents of the properties map to application config file.
     * 
     * @param properties the map of properties to update
     * @param profile string with profile where configuration will be located.
     */
    void updateProperties(Map<String, String> properties, String profile);
    
    /**
     * Adds the contents of the properties map to application config file using
     * the same prefix to every properties included on map.
     * 
     * @param prefix included for every properties
     * @param properties the map of properties to update
     * @param profile string with profile where configuration will be located.
     */
    void updateProperties(String prefix, Map<String, String> properties, String profile);


    /**
     * Retrieves all property key/value pairs from the specified property,
     * throwing an exception if the file does not exist.
     * 
     * @param profile string with profile where configuration will be located.
     * 
     * @return the key/value pairs (may return null if the property file does
     *         not exist)
     */
    Map<String, String> getProperties(String profile);
    
    /**
     * Retrieves all property keys from the specified property, throwing an
     * exception if the file does not exist.
     * 
     * @param includeValues if true, appends (" = theValue") to each returned
     *            string
     * @param profile string with profile where configuration will be located.
     * 
     * @return the keys (may return null if the property file does not exist)
     */
    SortedSet<String> getPropertyKeys(boolean includeValues, String profile);
    
    /**
     * Retrieves all property keys from the specified property, throwing an
     * exception if the file does not exist.
     * 
     * @param prefix string that identifies property prefix. (if defined, only 
     *        properties that starts with it will be returned)
     * @param includeValues if true, appends (" = theValue") to each returned
     *            string
     * @param profile string with profile where configuration will be located.
     * 
     * @return the keys (may return null if the property file does not exist)
     */
    SortedSet<String> getPropertyKeys(String prefix, boolean includeValues, String profile);

    /**
     * Retrieves the specified property, returning null if the property or file
     * does not exist.
     * 
     * @param key the property key to retrieve (required)
     * @param profile string with profile where configuration will be located.
     * 
     * @return the property value (may return null if the property file or
     *         requested property does not exist)
     */
    String getProperty(String key, String profile);
    
    /**
     * Retrieves the specified property, returning null if the property
     * does not exist.
     * 
     * @param prefix included on given property
     * @param key the property key to retrieve (required)
     * @param profile string with profile where configuration will be located.
     * 
     * @return the property value (may return null if the property file or
     *         requested property does not exist)
     */
    String getProperty(String prefix, String key, String profile);

    /**
     * Removes the specified property.
     * 
     * @param profile string with profile where configuration will be located.
     * 
     * @param key the property key to remove (required)
     */
    void removeProperty(String key, String profile);
    
    /**
     * Removes the specified property including prefix.
     * 
     * @param prefix included on given property key
     * @param profile string with profile where configuration will be located.
     * 
     * @param key the property key to remove (required)
     */
    void removeProperty(String prefix, String key, String profile);
    
    /**
     * Removes the specified properties.
     * 
     * @param keys list of property keys to remove (required)
     * @param profile string with profile where configuration will be located.
     */
    void removeProperties(List<String> keys, String profile);
    
    /**
     * Removes the specified properties.
     * 
     * @param prefix that identifies property to be removed
     * @param profile string with profile where configuration will be located.
     */
    void removePropertiesByPrefix(String prefix, String profile);
    
    /**
     * Method that returns current location of Spring Config file 
     * (if user has not modified it should return src/main/resources/application.properties)
     * 
     * @return string with current location of Spring Config file
     */
    String getSpringConfigLocation();
    
    /**
     * Method that returns current location of Spring Config file 
     * (if user has not modified it should return src/main/resources/application-profile.properties)
     * 
     * @param profile string with profile where configuration will be located.
     * 
     * @return string with current location of Spring Config file
     */
    String getSpringConfigLocation(String profile);
    
    /**
     * Method that checks if Spring config file exists. Uses getSpringConfigLocation method to
     * obtain location.
     * 
     * @return boolean true if exists
     */
    boolean existsSpringConfigFile();
    
    /**
     * Method that checks if Spring config file exists for an existing profile. Uses 
     * getSpringConfigLocation method to obtain location.
     * 
     * @param profile string with profile where configuration will be located.
     * 
     * @return boolean true if exists
     */
    boolean existsSpringConfigFile(String profile);
}