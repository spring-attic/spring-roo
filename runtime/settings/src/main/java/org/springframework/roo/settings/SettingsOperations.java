package org.springframework.roo.settings;

/**
 * Interface of operations to manage the configuration, such as create,
 * remove and list properties. Typically used by a command type or an external
 * add-on.
 *
 * @author Paula Navarro
 * @author Juan Carlos García
 * @since 2.0
 */
public interface SettingsOperations {

  /**
   * Sets a new configuration property. If the property exists,
   * parameter force indicates if this property is updated or keeps its value.
   * Otherwise the new property is added into configuration.
   * 
   * @param key
   *            string that identifies the property
   * @param value
   *            string with the value assigned to the property
   * @param force
   *            boolean that indicates if is necessary to force operation
   */
  void addSetting(String name, String value, boolean force);

  /**
   * Removes a property and its value from configuration.
   *
   * @param name
   *            string that identifies the property
   */
  void removeSetting(String value);

  /**
   * Shows all settings name/value pairs stored into configuration
   */
  void listSettings();

}
