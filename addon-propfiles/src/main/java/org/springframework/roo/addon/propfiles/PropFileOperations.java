package org.springframework.roo.addon.propfiles;

import java.util.SortedSet;

import org.springframework.roo.project.Path;

/**
 * Provides an interface to {@link PropFileOperationsImpl}.
 * 
 * @author Ben Alex
 *
 */
public interface PropFileOperations {

	public abstract boolean isPropertiesCommandAvailable();

	/**
	 * Changes the specified property, throwing an exception if the file does not exist.
	 * 
	 * @param propertyFilePath the location of the property file (required)
	 * @param propertyFilename the name of the property file within the specified path (required)
	 * @param key the property key to update (required)
	 * @param value the property value to set into the property key (required)
	 */
	public abstract void changeProperty(Path propertyFilePath, String propertyFilename, String key, String value);

	/**
	 * Removes the specified property, throwing an exception if the file does not exist.
	 * 
	 * @param propertyFilePath the location of the property file (required)
	 * @param propertyFilename the name of the property file within the specified path (required)
	 * @param key the property key to remove (required)
	 */
	public abstract void removeProperty(Path propertyFilePath, String propertyFilename, String key);

	/**
	 * Retrieves the specified property, returning null if the property or file does not exist.
	 * 
	 * @param propertyFilePath the location of the property file (required)
	 * @param propertyFilename the name of the property file within the specified path (required)
	 * @param key the property key to retrieve (required)
	 * @return the property value (may return null if the property file or requested property does not exist)
	 */
	public abstract String getProperty(Path propertyFilePath, String propertyFilename, String key);

	/**
	 * Retrieves all property keys from the specified property, throwing an exception if the file does not exist.
	 * 
	 * @param propertyFilePath the location of the property file (required)
	 * @param propertyFilename the name of the property file within the specified path (required)
	 * @param includeValues if true, appends (" = theValue") to each returned string
	 * @return the keys (may return null if the property file does not exist)
	 */
	public abstract SortedSet<String> getPropertyKeys(Path propertyFilePath, String propertyFilename, boolean includeValues);

}