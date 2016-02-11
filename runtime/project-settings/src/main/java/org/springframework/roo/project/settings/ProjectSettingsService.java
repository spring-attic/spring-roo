package org.springframework.roo.project.settings;

import java.util.Map;
import java.util.SortedSet;

/**
 * Provides an interface to {@link ProjectSettingsServiceImpl}.
 *
 * @author Paula Navarro
 * @since 2.0
 */
public interface ProjectSettingsService {

	/**
	 * Adds some property to project settings file.
	 *
	 * @param key
	 *            string that identifies the property
	 * @param value
	 *            string with the value assigned to the property
	 * @param force
	 *            boolean that indicates if is necessary to force operation
	 */
	void addProperty(String key, String value, boolean force);
	
	/**
     * Remove property from Spring Roo shell config properties file
     *
     * @param key
     *            string that identifies the property
     */
    void removeProperty(String key);

	/**
	 * Retrieves all property key/value pairs, throwing an exception if the file
	 * does not exist.
	 *
	 * @return the key/value pairs (may return null if the property file does
	 *         not exist)
	 */
	Map<String, String> getProperties();

	/**
	 * Retrieves all property keys, throwing an exception if the file does not
	 * exist.
	 *
	 * @param includeValues
	 *            if true, appends (" = theValue") to each returned string
	 *
	 * @return the keys (may return null if the property file does not exist)
	 */
	SortedSet<String> getPropertyKeys(boolean includeValue);

	/**
	 * Retrieves the specified property, returning null if the property or file
	 * does not exist.
	 *
	 * @param key
	 *            the property key to retrieve (required)
	 *
	 * @return the property value (may return null if the property file or
	 *         requested property does not exist)
	 */
	String getProperty(String key);

	/**
	 * Method that returns current location of Project Settings file
	 *
	 * @return string with current location of Project Settings file
	 */
	String getProjectSettingsLocation();

	/**
	 * Method that checks if project settings file exists. Uses
	 * getProjectSettingLocation method to obtain location.
	 *
	 * @return boolean true if exists
	 */
	boolean existsProjectSettingsFile();

	/**
	 * Method that creates project settings folder and its configuration files
	 */
	void createProjectSettingsFile();

}