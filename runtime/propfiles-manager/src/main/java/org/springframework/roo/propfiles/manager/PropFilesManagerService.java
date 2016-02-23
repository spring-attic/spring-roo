package org.springframework.roo.propfiles.manager;

import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;

import org.springframework.roo.project.LogicalPath;

/**
 * Provides an interface to {@link PropFilesManagerServiceImpl}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public interface PropFilesManagerService {

  /**
   * Adds the contents of the properties map to the given properties file.
   * 
   * @param propertyFilePath the location of the property file (required)
   * @param propertyFilename the name of the property file within the
   *            specified path (required)
   * @param properties the map of properties to add
   * @param sorted indicates if the resulting properties should be sorted
   *            alphabetically
   * @param force boolean that indicates if is necessary to force operation
   */
  void addProperties(LogicalPath propertyFilePath, String propertyFilename,
      Map<String, String> properties, boolean sorted, boolean force);

  /**
   * Adds the contents of the properties map to the given properties file.
   * 
   * @param propertyFilePath the location of the property file (required)
   * @param propertyFilename the name of the property file within the
   *            specified path (required)
   * @param prefix the string to be used as prefix of every property
   * @param properties the map of properties to add
   * @param sorted indicates if the resulting properties should be sorted
   *            alphabetically
   * @param force boolean that indicates if is necessary to force operation
   */
  void addProperties(LogicalPath propertyFilePath, String propertyFilename, String prefix,
      Map<String, String> properties, boolean sorted, boolean force);

  /**
   * Adds a property only if the given key (and value) does not exist already.
   * 
   * @param propertyFilePath the location of the property file (required)
   * @param propertyFilename the name of the property file within the
   *            specified path (required)
   * @param key the property key to update (required)
   * @param value the property value to set into the property key (required)
   * @param force boolean that indicates if is necessary to force operation
   */
  void addPropertyIfNotExists(LogicalPath propertyFilePath, String propertyFilename, String key,
      String value, boolean force);

  /**
   * Adds a property only if the given key (and value) does not exist already.
   * 
   * @param propertyFilePath the location of the property file (required)
   * @param propertyFilename the name of the property file within the
   *            specified path (required)
   * @param prefix the property prefix to use          
   * @param key the property key to update (required)
   * @param value the property value to set into the property key (required)
   * @param force boolean that indicates if is necessary to force operation
   */
  void addPropertyIfNotExists(LogicalPath propertyFilePath, String propertyFilename, String prefix,
      String key, String value, boolean force);

  /**
   * Adds a property only if the given key (and value) does not exist already.
   * 
   * @param propertyFilePath the location of the property file (required)
   * @param propertyFilename the name of the property file within the
   *            specified path (required)
   * @param key the property key to update (required)
   * @param value the property value to set into the property key (required)
   * @param sorted indicates if the resulting properties should be sorted
   *            alphabetically
   * @param force boolean that indicates if is necessary to force operation
   */
  void addPropertyIfNotExists(LogicalPath propertyFilePath, String propertyFilename, String key,
      String value, boolean sorted, boolean force);

  /**
   * Adds a property only if the given key (and value) does not exist already.
   * 
   * @param propertyFilePath the location of the property file (required)
   * @param propertyFilename the name of the property file within the
   *            specified path (required)
   * @param prefix the property prefix to use
   * @param key the property key to update (required)
   * @param value the property value to set into the property key (required)
   * @param sorted indicates if the resulting properties should be sorted
   *            alphabetically
   * @param force boolean that indicates if is necessary to force operation
   */
  void addPropertyIfNotExists(LogicalPath propertyFilePath, String propertyFilename, String prefix,
      String key, String value, boolean sorted, boolean force);

  /**
   * Changes the specified property, throwing an exception if the file does
   * not exist.
   * 
   * @param propertyFilePath the location of the property file (required)
   * @param propertyFilename the name of the property file within the
   *            specified path (required)
   * @param key the property key to update (required)
   * @param value the property value to set into the property key (required)
   * @param force boolean that indicates if is necessary to force operation
   */
  void changeProperty(LogicalPath propertyFilePath, String propertyFilename, String key,
      String value, boolean force);

  /**
   * Changes the specified property, throwing an exception if the file does
   * not exist.
   * 
   * @param propertyFilePath the location of the property file (required)
   * @param propertyFilename the name of the property file within the
   *            specified path (required)
   * @param prefix the string that match with property start
   * @param key the property key to update (required)
   * @param value the property value to set into the property key (required)
   * @param force boolean that indicates if is necessary to force operation
   */
  void changeProperty(LogicalPath propertyFilePath, String propertyFilename, String prefix,
      String key, String value, boolean force);

  /**
   * Changes the specified property, throwing an exception if the file does
   * not exist.
   * 
   * @param propertyFilePath the location of the property file (required)
   * @param propertyFilename the name of the property file within the
   *            specified path (required)
   * @param key the property key to update (required)
   * @param sorted indicates if the resulting properties should be sorted
   *            alphabetically
   * @param value the property value to set into the property key (required)
   * @param force boolean that indicates if is necessary to force operation
   */
  void changeProperty(LogicalPath propertyFilePath, String propertyFilename, String key,
      String value, boolean sorted, boolean force);

  /**
   * Changes the specified property, throwing an exception if the file does
   * not exist.
   * 
   * @param propertyFilePath the location of the property file (required)
   * @param propertyFilename the name of the property file within the
   *            specified path (required)
   * @param prefix the string that match with the property starts
   * @param key the property key to update (required)
   * @param sorted indicates if the resulting properties should be sorted
   *            alphabetically
   * @param value the property value to set into the property key (required)
   * @param force boolean that indicates if is necessary to force operation
   */
  void changeProperty(LogicalPath propertyFilePath, String propertyFilename, String prefix,
      String key, String value, boolean sorted, boolean force);

  /**
   * Use the contents of the properties map to update the given properties file.
   * 
   * @param propertyFilePath the location of the property file (required)
   * @param propertyFilename the name of the property file within the
   *            specified path (required)
   * @param properties the map of properties to update
   * @param force boolean that indicates if is necessary to force operation
   */
  void changeProperties(LogicalPath propertyFilePath, String propertyFilename,
      Map<String, String> properties, boolean force);

  /**
   * Use the contents of the properties map to update the given properties file.
   * 
   * @param propertyFilePath the location of the property file (required)
   * @param propertyFilename the name of the property file within the
   *            specified path (required)
   * @param prefix the string that match with start of every property           
   * @param properties the map of properties to update
   * @param force boolean that indicates if is necessary to force operation
   */
  void changeProperties(LogicalPath propertyFilePath, String propertyFilename, String prefix,
      Map<String, String> properties, boolean force);

  /**
   * Use the contents of the properties map to update the given properties file.
   * 
   * @param propertyFilePath the location of the property file (required)
   * @param propertyFilename the name of the property file within the
   *            specified path (required)
   * @param properties the map of properties to update
   * @param sorted indicates if the resulting properties should be sorted
   *            alphabetically
   * @param force boolean that indicates if is necessary to force operation
   */
  void changeProperties(LogicalPath propertyFilePath, String propertyFilename,
      Map<String, String> properties, boolean sorted, boolean force);

  /**
   * Use the contents of the properties map to update the given properties file.
   * 
   * @param propertyFilePath the location of the property file (required)
   * @param propertyFilename the name of the property file within the
   *            specified path (required)
   * @param prefix the string that match with start of every property           
   * @param properties the map of properties to update
   * @param sorted indicates if the resulting properties should be sorted
   *            alphabetically
   * @param force boolean that indicates if is necessary to force operation
   */
  void changeProperties(LogicalPath propertyFilePath, String propertyFilename, String prefix,
      Map<String, String> properties, boolean sorted, boolean force);

  /**
   * Retrieves all property key/value pairs from the specified property,
   * throwing an exception if the file does not exist.
   * 
   * @param propertyFilePath the location of the property file (required)
   * @param propertyFilename the name of the property file within the
   *            specified path (required)
   * @return the key/value pairs (may return null if the property file does
   *         not exist)
   */
  Map<String, String> getProperties(LogicalPath propertyFilePath, String propertyFilename);

  /**
   * Retrieves the specified property, returning null if the property or file
   * does not exist.
   * 
   * @param propertyFilePath the location of the property file (required)
   * @param propertyFilename the name of the property file within the
   *            specified path (required)
   * @param key the property key to retrieve (required)
   * @return the property value (may return null if the property file or
   *         requested property does not exist)
   */
  String getProperty(LogicalPath propertyFilePath, String propertyFilename, String key);

  /**
   * Retrieves the specified property, returning null if the property or file
   * does not exist.
   * 
   * @param propertyFilePath the location of the property file (required)
   * @param propertyFilename the name of the property file within the
   *            specified path (required)
   * @param prefix the string that match with property starts
   * @param key the property key to retrieve (required)
   * @return the property value (may return null if the property file or
   *         requested property does not exist)
   */
  String getProperty(LogicalPath propertyFilePath, String propertyFilename, String prefix,
      String key);

  /**
   * Retrieves all property keys from the specified property file, throwing an
   * exception if the file does not exist.
   * 
   * @param propertyFilePath the location of the property file (required)
   * @param propertyFilename the name of the property file within the
   *            specified path (required)
   * @param includeValues if true, appends (" = theValue") to each returned
   *            string
   * @return the keys (may return null if the property file does not exist)
   */
  SortedSet<String> getPropertyKeys(LogicalPath propertyFilePath, String propertyFilename,
      boolean includeValues);

  /**
   * Retrieves all property keys that starts with the given prefix from the specified 
   * property file, throwing an exception if the file does not exist.
   * 
   * @param propertyFilePath the location of the property file (required)
   * @param propertyFilename the name of the property file within the
   *            specified path (required)
   * @param prefix if defined, only properties that starts with it will be returned.
   * @param includeValues if true, appends (" = theValue") to each returned
   *            string
   * @return the keys (may return null if the property file does not exist)
   */
  SortedSet<String> getPropertyKeys(LogicalPath propertyFilePath, String propertyFilename,
      String prefix, boolean includeValues);

  /**
   * Loads the properties from the given stream, closing it on completion
   * 
   * @param inputStream the stream from which to read (can be
   *            <code>null</code>)
   * @return an empty {@link Properties} if a null stream is given
   */
  Properties loadProperties(InputStream inputStream);

  /**
   * Loads the properties from the given classpath resource
   * 
   * @param filename the name of the properties file to load
   * @param loadingClass the class in whose package to look for the file
   * @return a non-<code>null</code> properties
   * @throws IllegalArgumentException if the given file can't be loaded
   * @since 1.2.0
   */
  Properties loadProperties(String filename, Class<?> loadingClass);

  /**
   * Removes the specified property, throwing an exception if the file does
   * not exist.
   * 
   * @param propertyFilePath the location of the property file (required)
   * @param propertyFilename the name of the property file within the
   *            specified path (required)
   * @param key the property key to remove (required)
   */
  void removeProperty(LogicalPath propertyFilePath, String propertyFilename, String key);

  /**
   * Removes the specified property, throwing an exception if the file does
   * not exist.
   * 
   * @param propertyFilePath the location of the property file (required)
   * @param propertyFilename the name of the property file within the
   *            specified path (required)
   * @param prefix the string that match with property starts
   * @param key the property key to remove (required)
   */
  void removeProperty(LogicalPath propertyFilePath, String propertyFilename, String prefix,
      String key);

  /**
   * Removes all properties that starts with the given prefix
   * 
   * @param propertyFilePath the location of the property file (required)
   * @param propertyFilename the name of the property file within the
   *            specified path (required)
   * @param prefix the string that match with property starts
   */
  void removePropertiesByPrefix(LogicalPath propertyFilePath, String propertyFilename, String prefix);
}
