package org.springframework.roo.settings.project;

import java.util.Map;
import java.util.SortedSet;

/**
 *
 * API that must implement those services implementations that manage the
 * project configuration.
 *
 * The project configuration is a system that allows to configurate how Spring
 * Roo shell works for every project, by setting the value of roo configuration
 * properties.
 *
 * 
 * Due to the project configuration can be stored in several formats and stores
 * it is recommended that each implementation of this interface would be
 * specialized in each of the stores and formats provided by Roo: properties,
 * xml, yaml, ...
 *
 * @author Paula Navarro
 * @since 2.0
 */
public interface ProjectSettingsService {

  /**
   * Sets a new project configuration property. If the property exists,
   * parameter force indicates if this property is updated or keeps its value.
   * Otherwise the new property is added into project settings.
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
   * Removes a property and its value from project configuration.
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
   * Method that creates project settings folder and project configuration
   * file (project.properties)
   */
  void createProjectSettingsFile();

}
