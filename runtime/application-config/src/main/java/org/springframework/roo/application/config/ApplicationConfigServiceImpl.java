package org.springframework.roo.application.config;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;

/**
 * Provides a service to manage all necessary properties located on application
 * configuration files.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class ApplicationConfigServiceImpl implements ApplicationConfigService {

    private static final String APPLICATION_CONFIG_FILE_NAME = "application";

    @Reference private ProjectOperations projectOperations;
    @Reference private FileManager fileManager;

    @Override
    public void addProperty(final String key, final String value) {
        manageProperty("", asMap(key, value), false);
    }

    @Override
    public void addProperty(final String prefix, final String key,
            final String value) {
        manageProperty(prefix, asMap(key, value), false);
    }

    @Override
    public void addProperties(final Map<String, String> properties) {
        manageProperty("", properties, false);
    }

    @Override
    public void addProperties(final String prefix,
            final Map<String, String> properties) {
        manageProperty(prefix, properties, false);
    }

    @Override
    public void updateProperty(final String key, final String value) {
        manageProperty("", asMap(key, value), true);
    }

    @Override
    public void updateProperty(final String prefix, final String key,
            final String value) {
        manageProperty(prefix, asMap(key, value), true);
    }

    @Override
    public void updateProperties(final Map<String, String> properties) {
        manageProperty("", properties, true);
    }

    @Override
    public void updateProperties(final String prefix,
            final Map<String, String> properties) {
        manageProperty(prefix, properties, true);
    }

    @Override
    public Map<String, String> getProperties() {
        final String filePath = projectOperations.getPathResolver()
                .getIdentifier(
                        LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, ""),
                        APPLICATION_CONFIG_FILE_NAME);
        final Properties props = new Properties();

        try {
            if (fileManager.exists(filePath)) {
                loadProperties(props,
                        new BufferedInputStream(new FileInputStream(filePath)));
            }
            else {
                throw new IllegalStateException("Properties file not found");
            }
        }
        catch (final IOException ioe) {
            throw new IllegalStateException(ioe);
        }

        final Map<String, String> result = new HashMap<String, String>();
        for (final Object key : props.keySet()) {
            result.put(key.toString(), props.getProperty(key.toString()));
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public String getProperty(final String key) {
        return getProperty("", key);
    }

    @Override
    public String getProperty(final String prefix, final String key) {
        Validate.notNull(prefix, "Prefix required");
        Validate.notBlank(key, "Key required");

        final String filePath = projectOperations.getPathResolver()
                .getIdentifier(
                        LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, ""),
                        APPLICATION_CONFIG_FILE_NAME);
        MutableFile mutableFile = null;
        final Properties props = new Properties();

        if (fileManager.exists(filePath)) {
            mutableFile = fileManager.updateFile(filePath);
            loadProperties(props, mutableFile.getInputStream());
        }
        else {
            return null;
        }

        return props.getProperty(key);
    }

    @Override
    public void removeProperty(final String key) {
        removeProperty("", key);
    }
    
    @Override
    public void removeProperty(final String prefix, String key) {
        Validate.notNull(prefix, "Prefix required");
        Validate.notBlank(key, "Key required");

        final String filePath = projectOperations.getPathResolver()
                .getIdentifier(
                        LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, ""),
                        APPLICATION_CONFIG_FILE_NAME);
        MutableFile mutableFile = null;
        final Properties props = new Properties();

        if (fileManager.exists(filePath)) {
            mutableFile = fileManager.updateFile(filePath);
            loadProperties(props, mutableFile.getInputStream());
        }
        else {
            throw new IllegalStateException("Properties file not found");
        }
        
        // Adding prefix if needed
        if(StringUtils.isNotBlank(prefix)){
            key = prefix.concat(".").concat(key);
        }

        props.remove(key);

        storeProps(props, mutableFile.getOutputStream(),
                "Updated at " + new Date());
    }

    @Override
    public void removeProperties(List<String> keys) {
        for(String key : keys){
            removeProperty(key);
        }
    }

    @Override
    public void removePropertiesByPrefix(String prefix) {
        Validate.notBlank(prefix, "Prefix required.");

        final String filePath = projectOperations.getPathResolver()
                .getIdentifier(
                        LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, ""),
                        APPLICATION_CONFIG_FILE_NAME);
        MutableFile mutableFile = null;
        final Properties props = new Properties();

        if (fileManager.exists(filePath)) {
            mutableFile = fileManager.updateFile(filePath);
            loadProperties(props, mutableFile.getInputStream());
        }
        else {
            throw new IllegalStateException("Properties file not found");
        }

        for (final Object key : props.keySet()) {
            if(key.toString().startsWith(prefix.concat("."))){
                props.remove(key);
            }
        }

        storeProps(props, mutableFile.getOutputStream(),
                "Updated at " + new Date());
    }

    // Util methods

    private Map<String, String> asMap(final String key, final String value) {
        final Map<String, String> properties = new HashMap<String, String>();
        properties.put(key, value);
        return properties;
    }

    private void loadProperties(final Properties props,
            final InputStream inputStream) {
        try {
            props.load(inputStream);
        }
        catch (final IOException e) {
            throw new IllegalStateException("Could not load properties", e);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private void manageProperty(String prefix,
            final Map<String, String> properties,
            final boolean changeExisting) {
        Validate.notNull(prefix, "Prefix required. Could be empty.");
        Validate.notNull(properties, "Property map required");
        final String filePath = projectOperations.getPathResolver()
                .getIdentifier(
                        LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, ""),
                        APPLICATION_CONFIG_FILE_NAME);

        MutableFile mutableFile = null;

        Properties props = new Properties() {
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

        if (fileManager.exists(filePath)) {
            mutableFile = fileManager.updateFile(filePath);
            loadProperties(props, mutableFile.getInputStream());
        }
        else {
            // Unable to find the file, so let's create it
            mutableFile = fileManager.createFile(filePath);
        }

        boolean saveNeeded = false;
        for (final Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();

            // Adding prefix if needed
            if (StringUtils.isNotBlank(prefix)) {
                key = prefix.concat(".").concat(key);
            }

            final String newValue = entry.getValue();
            final String existingValue = props.getProperty(key);
            if (existingValue == null
                    || !existingValue.equals(newValue) && changeExisting) {
                props.setProperty(key, newValue);
                saveNeeded = true;
            }
        }

        if (saveNeeded) {
            storeProps(props, mutableFile.getOutputStream(),
                    "Updated at " + new Date());
        }
    }

    private void storeProps(final Properties props,
            final OutputStream outputStream, final String comment) {
        Validate.notNull(outputStream, "OutputStream required");
        try {
            props.store(outputStream, comment);
        }
        catch (final IOException e) {
            throw new IllegalStateException("Could not store properties", e);
        }
        finally {
            IOUtils.closeQuietly(outputStream);
        }
    }
}
