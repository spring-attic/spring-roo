package org.springframework.roo.application.config;

import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
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

    private static final String APPLICATION_CONFIG_FILE_NAME = "application";

    @Reference private PropFilesManagerService propFilesManager;

    @Override
    public void addProperty(final String key, final String value) {
        propFilesManager.addPropertyIfNotExists(
                LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, ""),
                APPLICATION_CONFIG_FILE_NAME, key, value, true);
    }

    @Override
    public void addProperty(final String prefix, final String key,
            final String value) {
        propFilesManager.addPropertyIfNotExists(
                LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, ""),
                APPLICATION_CONFIG_FILE_NAME, prefix, key, value, true);
    }

    @Override
    public void addProperties(final Map<String, String> properties) {
        propFilesManager.addProperties(
                LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, ""),
                APPLICATION_CONFIG_FILE_NAME, properties, true, false);
    }

    @Override
    public void addProperties(final String prefix,
            final Map<String, String> properties) {
        propFilesManager.addProperties(
                LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, ""),
                APPLICATION_CONFIG_FILE_NAME, prefix, properties, true, false);
    }

    @Override
    public void updateProperty(final String key, final String value) {
        propFilesManager.changeProperty(
                LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, ""),
                APPLICATION_CONFIG_FILE_NAME, key, value, true);
    }

    @Override
    public void updateProperty(final String prefix, final String key,
            final String value) {
        propFilesManager.changeProperty(
                LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, ""),
                APPLICATION_CONFIG_FILE_NAME, prefix, key, value, true);
    }

    @Override
    public void updateProperties(final Map<String, String> properties) {
        propFilesManager.changeProperties(
                LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, ""),
                APPLICATION_CONFIG_FILE_NAME, properties, true);
    }

    @Override
    public void updateProperties(final String prefix,
            final Map<String, String> properties) {
        propFilesManager.changeProperties(
                LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, ""),
                APPLICATION_CONFIG_FILE_NAME, prefix, properties, true);
    }

    @Override
    public Map<String, String> getProperties() {
        return propFilesManager.getProperties(
                LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, ""),
                APPLICATION_CONFIG_FILE_NAME);
    }

    @Override
    public String getProperty(final String key) {
        return propFilesManager.getProperty(
                LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, ""),
                APPLICATION_CONFIG_FILE_NAME, key);
    }

    @Override
    public String getProperty(final String prefix, final String key) {
        return propFilesManager.getProperty(
                LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, ""),
                APPLICATION_CONFIG_FILE_NAME, prefix, key);
    }

    @Override
    public void removeProperty(final String key) {
        propFilesManager.removeProperty(
                LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, ""),
                APPLICATION_CONFIG_FILE_NAME, key);
    }

    @Override
    public void removeProperty(final String prefix, String key) {
        propFilesManager.removeProperty(
                LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, ""),
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
        propFilesManager.removePropertiesByPrefix(LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, ""),
                APPLICATION_CONFIG_FILE_NAME, prefix);
    }

}
