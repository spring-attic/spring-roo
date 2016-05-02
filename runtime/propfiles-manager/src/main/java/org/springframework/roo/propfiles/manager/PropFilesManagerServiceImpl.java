package org.springframework.roo.propfiles.manager;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.FileUtils;

/**
 * Provides service that could be used to manage all necessary .properties files
 * located on project.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class PropFilesManagerServiceImpl implements PropFilesManagerService {

  protected final static Logger LOGGER = HandlerUtils.getLogger(PropFilesManagerServiceImpl.class);

  private static final boolean SORTED = true;

  private FileManager fileManager;
  private ProjectOperations projectOperations;

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  @Override
  public void addProperties(final LogicalPath propertyFilePath, final String propertyFilename,
      final Map<String, String> properties, final boolean sorted, final boolean force) {
    manageProperty(propertyFilePath, propertyFilename, "", properties, sorted, force);
  }

  @Override
  public void addProperties(LogicalPath propertyFilePath, String propertyFilename, String prefix,
      Map<String, String> properties, boolean sorted, boolean force) {
    manageProperty(propertyFilePath, propertyFilename, prefix, properties, sorted, force);
  }

  @Override
  public void addPropertyIfNotExists(final LogicalPath propertyFilePath,
      final String propertyFilename, final String key, final String value, boolean force) {
    manageProperty(propertyFilePath, propertyFilename, "", asMap(key, value), !SORTED, force);
  }

  @Override
  public void addPropertyIfNotExists(LogicalPath propertyFilePath, String propertyFilename,
      String prefix, String key, String value, boolean force) {
    manageProperty(propertyFilePath, propertyFilename, prefix, asMap(key, value), !SORTED, force);
  }

  @Override
  public void addPropertyIfNotExists(final LogicalPath propertyFilePath,
      final String propertyFilename, final String key, final String value, final boolean sorted,
      boolean force) {
    manageProperty(propertyFilePath, propertyFilename, "", asMap(key, value), sorted, force);
  }

  @Override
  public void addPropertyIfNotExists(LogicalPath propertyFilePath, String propertyFilename,
      String prefix, String key, String value, boolean sorted, boolean force) {
    manageProperty(propertyFilePath, propertyFilename, prefix, asMap(key, value), sorted, force);
  }

  @Override
  public void changeProperty(final LogicalPath propertyFilePath, final String propertyFilename,
      final String key, final String value, boolean force) {
    manageProperty(propertyFilePath, propertyFilename, "", asMap(key, value), !SORTED, force);
  }

  @Override
  public void changeProperty(LogicalPath propertyFilePath, String propertyFilename, String prefix,
      String key, String value, boolean force) {
    manageProperty(propertyFilePath, propertyFilename, prefix, asMap(key, value), !SORTED, force);
  }

  @Override
  public void changeProperty(final LogicalPath propertyFilePath, final String propertyFilename,
      final String key, final String value, final boolean sorted, boolean force) {
    manageProperty(propertyFilePath, propertyFilename, "", asMap(key, value), sorted, force);
  }

  @Override
  public void changeProperty(LogicalPath propertyFilePath, String propertyFilename, String prefix,
      String key, String value, boolean sorted, boolean force) {
    manageProperty(propertyFilePath, propertyFilename, prefix, asMap(key, value), sorted, force);
  }

  @Override
  public void changeProperties(LogicalPath propertyFilePath, String propertyFilename,
      Map<String, String> properties, boolean force) {
    manageProperty(propertyFilePath, propertyFilename, "", properties, !SORTED, force);
  }

  @Override
  public void changeProperties(LogicalPath propertyFilePath, String propertyFilename,
      String prefix, Map<String, String> properties, boolean force) {
    manageProperty(propertyFilePath, propertyFilename, prefix, properties, !SORTED, force);
  }

  @Override
  public void changeProperties(LogicalPath propertyFilePath, String propertyFilename,
      Map<String, String> properties, boolean sorted, boolean force) {
    manageProperty(propertyFilePath, propertyFilename, "", properties, sorted, force);
  }

  @Override
  public void changeProperties(LogicalPath propertyFilePath, String propertyFilename,
      String prefix, Map<String, String> properties, boolean sorted, boolean force) {
    manageProperty(propertyFilePath, propertyFilename, prefix, properties, sorted, force);
  }

  @Override
  public Map<String, String> getProperties(final LogicalPath propertyFilePath,
      final String propertyFilename) {
    Validate.notNull(propertyFilePath, "Property file path required");
    Validate.notBlank(propertyFilename, "Property filename required");

    final String filePath =
        getProjectOperations().getPathResolver().getIdentifier(propertyFilePath, propertyFilename);
    final Properties props = new Properties();

    try {
      if (getFileManager().exists(filePath)) {
        loadProperties(props, new BufferedInputStream(new FileInputStream(filePath)));
      } else {
        throw new IllegalStateException(String.format(
            "ERROR: '%s' properties file doesn't exists.", filePath));
      }
    } catch (final IOException ioe) {
      throw new IllegalStateException(ioe);
    }

    final Map<String, String> result = new HashMap<String, String>();
    for (final Object key : props.keySet()) {
      result.put(key.toString(), props.getProperty(key.toString()));
    }
    return Collections.unmodifiableMap(result);
  }

  @Override
  public String getProperty(final LogicalPath propertyFilePath, final String propertyFilename,
      final String key) {
    return getProperty(propertyFilePath, propertyFilename, "", key);
  }

  @Override
  public String getProperty(LogicalPath propertyFilePath, String propertyFilename, String prefix,
      String key) {
    Validate.notNull(prefix, "Prefix could be blank but not null");
    Validate.notNull(propertyFilePath, "Property file path required");
    Validate.notBlank(propertyFilename, "Property filename required");
    Validate.notBlank(key, "Key required");

    final String filePath =
        getProjectOperations().getPathResolver().getIdentifier(propertyFilePath, propertyFilename);
    MutableFile mutableFile = null;
    final Properties props = new Properties();

    if (getFileManager().exists(filePath)) {
      mutableFile = getFileManager().updateFile(filePath);
      loadProperties(props, mutableFile.getInputStream());
    } else {
      return null;
    }

    // Including prefix if needed
    if (StringUtils.isNotBlank(prefix)) {
      key = prefix.concat(".").concat(key);
    }

    return props.getProperty(key);
  }

  @Override
  public SortedSet<String> getPropertyKeys(final LogicalPath propertyFilePath,
      final String propertyFilename, final boolean includeValues) {
    return getPropertyKeys(propertyFilePath, propertyFilename, "", includeValues);
  }

  @Override
  public SortedSet<String> getPropertyKeys(LogicalPath propertyFilePath, String propertyFilename,
      String prefix, boolean includeValues) {
    Validate.notNull(prefix, "Prefix could be blank but not null");
    Validate.notNull(propertyFilePath, "Property file path required");
    Validate.notBlank(propertyFilename, "Property filename required");

    final String filePath =
        getProjectOperations().getPathResolver().getIdentifier(propertyFilePath, propertyFilename);
    final Properties props = new Properties();

    try {
      if (getFileManager().exists(filePath)) {
        loadProperties(props, new BufferedInputStream(new FileInputStream(filePath)));
      } else {
        throw new IllegalStateException(String.format(
            "ERROR: '%s' properties file doesn't exists.", filePath));
      }
    } catch (final IOException ioe) {
      throw new IllegalStateException(ioe);
    }

    final SortedSet<String> result = new TreeSet<String>();
    for (final Object key : props.keySet()) {
      String info = key.toString();
      if (StringUtils.isNotBlank(prefix)) {
        if (info.startsWith(prefix)) {
          result.add(includeValues ? info.concat(" = ").concat(props.getProperty(key.toString()))
              : info);
        }
      } else {
        if (includeValues) {
          info += " = " + props.getProperty(key.toString());
        }
        result.add(info);
      }
    }
    return result;
  }

  @Override
  public Properties loadProperties(final InputStream inputStream) {
    final Properties properties = new Properties();
    if (inputStream != null) {
      loadProperties(properties, inputStream);
    }
    return properties;
  }

  @Override
  public Properties loadProperties(final String filename, final Class<?> loadingClass) {
    return loadProperties(FileUtils.getInputStream(loadingClass, filename));
  }

  @Override
  public void removeProperty(final LogicalPath propertyFilePath, final String propertyFilename,
      final String key) {
    removeProperty(propertyFilePath, propertyFilename, "", key);
  }

  @Override
  public void removeProperty(LogicalPath propertyFilePath, String propertyFilename, String prefix,
      String key) {
    Validate.notNull(prefix, "Prefix could be blank but not null");
    Validate.notNull(propertyFilePath, "Property file path required");
    Validate.notBlank(propertyFilename, "Property filename required");
    Validate.notBlank(key, "Key required");

    final String filePath =
        getProjectOperations().getPathResolver().getIdentifier(propertyFilePath, propertyFilename);
    MutableFile mutableFile = null;
    final Properties props = new Properties();

    if (getFileManager().exists(filePath)) {
      mutableFile = getFileManager().updateFile(filePath);
      loadProperties(props, mutableFile.getInputStream());
    } else {
      throw new IllegalStateException(String.format("ERROR: '%s' properties file doesn't exists.",
          filePath));
    }

    // Including prefix if needed
    if (StringUtils.isNotBlank(prefix)) {
      key = prefix.concat(".").concat(key);
    }

    if (props.containsKey(key)) {
      props.remove(key);
      storeProps(props, mutableFile.getOutputStream(), "Updated at " + new Date());
    }

  }

  @Override
  public void removePropertiesByPrefix(LogicalPath propertyFilePath, String propertyFilename,
      String prefix) {
    Validate.notBlank(prefix, "Prefix required");
    Validate.notNull(propertyFilePath, "Property file path required");
    Validate.notBlank(propertyFilename, "Property filename required");

    final String filePath =
        getProjectOperations().getPathResolver().getIdentifier(propertyFilePath, propertyFilename);
    MutableFile mutableFile = null;
    final Properties props = new Properties();

    if (getFileManager().exists(filePath)) {
      mutableFile = getFileManager().updateFile(filePath);
      loadProperties(props, mutableFile.getInputStream());
    } else {
      throw new IllegalStateException(String.format("ERROR: '%s' properties file doesn't exists.",
          filePath));
    }

    for (Entry property : props.entrySet()) {
      String key = (String) property.getKey();
      if (key != null && key.startsWith(prefix)) {
        props.remove(key);
      }
    }

    storeProps(props, mutableFile.getOutputStream(), "Updated at " + new Date());

  }

  // Util methods

  private Map<String, String> asMap(final String key, final String value) {
    final Map<String, String> properties = new HashMap<String, String>();
    properties.put(key, value);
    return properties;
  }

  private void loadProperties(final Properties props, final InputStream inputStream) {
    try {
      props.load(inputStream);
    } catch (final IOException e) {
      throw new IllegalStateException("Could not load properties", e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  private void manageProperty(final LogicalPath propertyFilePath, final String propertyFilename,
      final String prefix, final Map<String, String> properties, final boolean sorted,
      final boolean force) {
    Validate.notNull(prefix, "Prefix could be blank but not null");
    Validate.notNull(propertyFilePath, "Property file path required");
    Validate.notBlank(propertyFilename, "Property filename required");
    Validate.notNull(properties, "Property map required");

    final String filePath =
        getProjectOperations().getPathResolver().getIdentifier(propertyFilePath, propertyFilename);
    MutableFile mutableFile = null;

    Properties props;
    if (sorted) {
      props = new Properties() {
        private static final long serialVersionUID = 1L;

        // Override the keys() method to order the keys alphabetically
        @SuppressWarnings("all")
        public synchronized Enumeration keys() {
          final Object[] keys = keySet().toArray();
          Arrays.sort(keys);
          return new Enumeration() {
            int i = 0;

            public boolean hasMoreElements() {
              return i < keys.length;
            }

            public Object nextElement() {
              return keys[i++];
            }
          };
        }
      };
    } else {
      props = new Properties();
    }

    if (getFileManager().exists(filePath)) {
      mutableFile = getFileManager().updateFile(filePath);
      loadProperties(props, mutableFile.getInputStream());
    } else {
      // Unable to find the file, so let's create it
      mutableFile = getFileManager().createFile(filePath);
    }

    boolean saveNeeded = false;
    boolean needForce = false;
    Map<String, String> overwriteProperties = new HashMap<String, String>();

    for (final Entry<String, String> entry : properties.entrySet()) {
      String key = entry.getKey();

      // Adding prefix if needed
      if (StringUtils.isNotBlank(prefix)) {
        key = prefix.concat(".").concat(key);
      }

      final String newValue = entry.getValue();
      final String existingValue = props.getProperty(key);
      if (existingValue == null || !existingValue.equals(newValue) && force) {
        props.setProperty(key, newValue);
        saveNeeded = true;
      } else if (!existingValue.equals(newValue) && !force) {
        // ROO-3702: Show error when tries to update some properties that
        // already exists and --force global param is false. 
        needForce = true;
        overwriteProperties.put(key, existingValue);
      }
    }

    // ROO-3702: Show error when tries to update some properties that
    // already exists and --force global param is false. 
    if (needForce) {
      String propertyCount = overwriteProperties.size() > 1 ? "Properties" : "Property";

      String propertyLists = "";
      for (Entry<String, String> property : overwriteProperties.entrySet()) {
        String key = property.getKey();
        String value = property.getValue();
        propertyLists =
            propertyLists.concat("'").concat(key).concat(" = ").concat(value).concat("', ");
      }

      String msg =
          String.format("WARNING: %s %s already exists. "
              + "Use --force parameter to overwrite it.", propertyCount,
              propertyLists.substring(0, propertyLists.length() - 2));
      throw new RuntimeException(msg);
    }

    if (saveNeeded) {
      storeProps(props, mutableFile.getOutputStream(), "Updated at " + new Date());

      String propertyCount = props.size() > 1 ? "Properties" : "Property";
      String haveCount = props.size() > 1 ? "have" : "has";
      String propertyLists = "";
      for (Entry<Object, Object> property : props.entrySet()) {
        String key = (String) property.getKey();
        String value = (String) property.getValue();

        propertyLists =
            propertyLists.concat("'").concat(key).concat(" = ").concat(value).concat("', ");
      }

    }
  }

  private void storeProps(final Properties props, final OutputStream outputStream,
      final String comment) {
    Validate.notNull(outputStream, "OutputStream required");
    try {
      props.store(outputStream, comment);
    } catch (final IOException e) {
      throw new IllegalStateException("Could not store properties", e);
    } finally {
      IOUtils.closeQuietly(outputStream);
    }
  }

  public FileManager getFileManager() {
    if (fileManager == null) {
      // Get all Services implement FileManager interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(FileManager.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          fileManager = (FileManager) this.context.getService(ref);
          return fileManager;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load FileManager on PropFilesManagerServiceImpl.");
        return null;
      }
    } else {
      return fileManager;
    }

  }

  public ProjectOperations getProjectOperations() {
    if (projectOperations == null) {
      // Get all Services implement ProjectOperations interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ProjectOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          projectOperations = (ProjectOperations) this.context.getService(ref);
          return projectOperations;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ProjectOperations on PropFilesManagerServiceImpl.");
        return null;
      }
    } else {
      return projectOperations;
    }

  }
}
