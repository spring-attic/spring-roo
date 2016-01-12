package org.springframework.roo.application.config;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.propfiles.manager.PropFilesManagerService;

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

    private static final Path APPLICATION_CONFIG_FILE_LOCATION = Path.SRC_MAIN_RESOURCES;
    private static final String APPLICATION_CONFIG_FILE_NAME = "application.properties";

    @Reference private PropFilesManagerService propFilesManager;
    @Reference private PathResolver pathResolver;
    @Reference private FileManager fileManager;

    @Override
    public void addProperty(final String key, final String value) {
        propFilesManager.addPropertyIfNotExists(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                APPLICATION_CONFIG_FILE_NAME, key, value, true);
    }

    @Override
    public void addProperty(final String prefix, final String key,
            final String value) {
        propFilesManager.addPropertyIfNotExists(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                APPLICATION_CONFIG_FILE_NAME, prefix, key, value, true);
    }

    @Override
    public void addProperties(final Map<String, String> properties) {
        propFilesManager.addProperties(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                APPLICATION_CONFIG_FILE_NAME, properties, true, true);
    }

    @Override
    public void addProperties(final String prefix,
            final Map<String, String> properties) {
        propFilesManager.addProperties(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                APPLICATION_CONFIG_FILE_NAME, prefix, properties, true, true);
    }

    @Override
    public void updateProperty(final String key, final String value) {
        propFilesManager.changeProperty(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                APPLICATION_CONFIG_FILE_NAME, key, value, true);
    }

    @Override
    public void updateProperty(final String prefix, final String key,
            final String value) {
        propFilesManager.changeProperty(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                APPLICATION_CONFIG_FILE_NAME, prefix, key, value, true);
    }

    @Override
    public void updateProperties(final Map<String, String> properties) {
        propFilesManager.changeProperties(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                APPLICATION_CONFIG_FILE_NAME, properties, true);
    }

    @Override
    public void updateProperties(final String prefix,
            final Map<String, String> properties) {
        propFilesManager.changeProperties(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                APPLICATION_CONFIG_FILE_NAME, prefix, properties, true);
    }

    @Override
    public Map<String, String> getProperties() {
        return propFilesManager.getProperties(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                APPLICATION_CONFIG_FILE_NAME);
    }

    @Override
    public SortedSet<String> getPropertyKeys(boolean includeValues) {
        return propFilesManager.getPropertyKeys(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                APPLICATION_CONFIG_FILE_NAME, includeValues);
    }

    @Override
    public SortedSet<String> getPropertyKeys(String prefix,
            boolean includeValues) {
        return propFilesManager.getPropertyKeys(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                APPLICATION_CONFIG_FILE_NAME, prefix, includeValues);
    }

    @Override
    public String getProperty(final String key) {
        return propFilesManager.getProperty(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                APPLICATION_CONFIG_FILE_NAME, key);
    }

    @Override
    public String getProperty(final String prefix, final String key) {
        return propFilesManager.getProperty(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                APPLICATION_CONFIG_FILE_NAME, prefix, key);
    }

    @Override
    public void removeProperty(final String key) {
        propFilesManager.removeProperty(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                APPLICATION_CONFIG_FILE_NAME, key);
    }

    @Override
    public void removeProperty(final String prefix, String key) {
        propFilesManager.removeProperty(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                APPLICATION_CONFIG_FILE_NAME, prefix, key);
    }

    @Override
    public void removeProperties(List<String> keys) {
        for (String key : keys) {
            removeProperty(key);
        }
    }

    @Override
    public void removePropertiesByPrefix(String prefix) {
        propFilesManager.removePropertiesByPrefix(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                APPLICATION_CONFIG_FILE_NAME, prefix);
    }

    @Override
    public String getSpringConfigLocation() {
        return pathResolver
                .getFocusedIdentifier(APPLICATION_CONFIG_FILE_LOCATION,
                        APPLICATION_CONFIG_FILE_NAME);
    }

    @Override
    public boolean existsSpringConfigFile() {
        return fileManager.exists(getSpringConfigLocation());
    }
}
