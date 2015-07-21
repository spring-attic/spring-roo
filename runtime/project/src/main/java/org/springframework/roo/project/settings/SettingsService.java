package org.springframework.roo.project.settings;

import java.util.Map;

/**
 * Provides service API to manage settings declared on generated Spring Roo Project
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public interface SettingsService {

	/**
	 * Checks if current project has settings
	 * 
	 * @return true if exists settings on current project.
	 */
	boolean hasSettings();

	/**
	 * Checks if current project has settings with specified settings id.
	 * 
	 * @param settingsId that identifies the setting. Usually will be the same 
	 * as Feature name. 
	 * 
	 * @return true if exists settings with id sId on current project
	 */
	boolean hasSettings(String sId);

	/**
	 * Gets the value of the specified property path on specified
	 * settingsId.
	 * 
	 * @param settingsId that identifies the setting. Usually will be the same 
	 * as Feature name. 
	 * @param propertyName to getValue. Ex: <i>propertyA.group.name</i>
	 * 
	 * @return Object with value of property from settingsId item, null if not found.
	 */
	Object getValue(String sId, String propertyName);
	
	/**
	 * Gets the value of the specified property path on specified
	 * settingsId.
	 * 
	 * @param settingsId that identifies the setting. Usually will be the same 
	 * as Feature name. 
	 * @param propertyName to getValue. Ex: <i>propertyA.group.name</i>
	 * 
	 * @return String with value of property from settingsId item, null if not found.
	 * @throws IllegalArgumentException propertyName doesn't point to String value
	 */
	String getStringValue(String sId, String propertyName);
	
	/**
	 * Gets the value of the specified property path on specified
	 * settingsId.
	 * 
	 * @param settingsId that identifies the setting. Usually will be the same 
	 * as Feature name. 
	 * @param propertyName to getValue. Ex: <i>propertyA.group.name</i>
	 * 
	 * @return String with value of property from settingsId item, null if not found.
	 * @throws IllegalArgumentException propertyName doesn't point to Map<String, Object> value
	 */
	Map<String,Object> getMapValue(String sId, String propertyName);
	
	/**
	 * Checks if specified property name will be return an String value or not.
	 * 
	 * @param settingsId that identifies the setting. Usually will be the same 
	 * as Feature name. 
	 * @param propertyName to getValue. Ex: <i>propertyA.group.name</i>
	 * @return true if property value is an String value.
	 */
	boolean isStringValue(String sId, String propertyName);
	
	/**
	 * Checks if specified property name has value or not.
	 * 
	 * @param settingsId that identifies the setting. Usually will be the same 
	 * as Feature name. 
	 * @param propertyName to getValue. Ex: <i>propertyA.group.name</i>
	 * @return true if property value is an String value.
	 */
	boolean hasValue(String sId, String propertyName);

}
