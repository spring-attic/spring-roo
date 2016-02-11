package org.springframework.roo.project.settings;

/**
 * Interface of operations this add-on offers. Typically used by a command type
 * or an external add-on.
 *
 * @author Paula Navarro
 * @author Juan Carlos García
 * @since 2.0
 */
public interface ProjectSettingsOperations {

    /**
     * Adds some setting to project settings file.
     *
     * @param name string that identifies the property
     * @param value string with the value assigned to the property
     * @param force boolean true forces to update setting value if it already exists
     */
    void addSetting(String name, String value, boolean force);
    
    /**
     * Remove an specified property from Spring Roo Shell config file.
     *
     * @param name string that identifies the property
     */
    void removeSetting(String value);


    /**
     * Shows all settings name/value pairs
     */
    void listSettings();

}