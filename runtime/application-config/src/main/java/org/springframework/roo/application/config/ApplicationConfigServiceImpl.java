package org.springframework.roo.application.config;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.apache.commons.lang3.StringUtils;
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
    private static final String APPLICATION_CONFIG_FILE_NAME = "application";
    private static final String APPLICATION_CONFIG_FILE_EXTENSION = ".properties";

    @Reference private PropFilesManagerService propFilesManager;
    @Reference private PathResolver pathResolver;
    @Reference private FileManager fileManager;

    @Override
    public void addProperty(final String key, final String value, String profile, boolean force) {

        propFilesManager.addPropertyIfNotExists(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                getAppliCationConfigFileName(profile), key, value, true, force);
    }

    @Override
    public void addProperty(final String prefix, final String key,
            final String value, String profile, boolean force) {
        propFilesManager.addPropertyIfNotExists(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                getAppliCationConfigFileName(profile), prefix, key, value, true, force);
    }

    @Override
    public void addProperties(final Map<String, String> properties, String profile, boolean force) {
        propFilesManager.addProperties(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                getAppliCationConfigFileName(profile), properties, true, force);
    }

    @Override
    public void addProperties(final String prefix,
            final Map<String, String> properties, String profile, boolean force) {
        propFilesManager.addProperties(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                getAppliCationConfigFileName(profile), prefix, properties, true, force);
    }

    @Override
    public void updateProperty(final String key, final String value, String profile, boolean force) {
        propFilesManager.changeProperty(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                getAppliCationConfigFileName(profile), key, value, true, force);
    }

    @Override
    public void updateProperty(final String prefix, final String key,
            final String value, String profile, boolean force) {
        propFilesManager.changeProperty(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                getAppliCationConfigFileName(profile), prefix, key, value, true, force);
    }

    @Override
    public void updateProperties(final Map<String, String> properties, String profile, boolean force) {
        propFilesManager.changeProperties(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                getAppliCationConfigFileName(profile), properties, true, force);
    }

    @Override
    public void updateProperties(final String prefix,
            final Map<String, String> properties, String profile, boolean force) {
        propFilesManager.changeProperties(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                getAppliCationConfigFileName(profile), prefix, properties, true, force);
    }

    @Override
    public Map<String, String> getProperties(String profile) {
        return propFilesManager.getProperties(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                getAppliCationConfigFileName(profile));
    }

    @Override
    public SortedSet<String> getPropertyKeys(boolean includeValues, String profile) {
        return propFilesManager.getPropertyKeys(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                getAppliCationConfigFileName(profile), includeValues);
    }

    @Override
    public SortedSet<String> getPropertyKeys(String prefix,
            boolean includeValues, String profile) {
        return propFilesManager.getPropertyKeys(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                getAppliCationConfigFileName(profile), prefix, includeValues);
    }

    @Override
    public String getProperty(final String key, String profile) {
        return propFilesManager.getProperty(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                getAppliCationConfigFileName(profile), key);
    }

    @Override
    public String getProperty(final String prefix, final String key, String profile) {
        return propFilesManager.getProperty(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                getAppliCationConfigFileName(profile), prefix, key);
    }

    @Override
    public void removeProperty(final String key, String profile) {
        propFilesManager.removeProperty(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                getAppliCationConfigFileName(profile), key);
    }

    @Override
    public void removeProperty(final String prefix, String key, String profile) {
        propFilesManager.removeProperty(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                getAppliCationConfigFileName(profile), prefix, key);
    }

    @Override
    public void removeProperties(List<String> keys, String profile) {
        for (String key : keys) {
            removeProperty(key, profile);
        }
    }

    @Override
    public void removePropertiesByPrefix(String prefix, String profile) {
        propFilesManager.removePropertiesByPrefix(
                LogicalPath.getInstance(APPLICATION_CONFIG_FILE_LOCATION, ""),
                getAppliCationConfigFileName(profile), prefix);
    }

    @Override
    public String getSpringConfigLocation() {
        return pathResolver
                .getFocusedIdentifier(APPLICATION_CONFIG_FILE_LOCATION,
                        getAppliCationConfigFileName(null));
    }
    
    @Override
    public String getSpringConfigLocation(String profile) {
        return pathResolver
                .getFocusedIdentifier(APPLICATION_CONFIG_FILE_LOCATION,
                        getAppliCationConfigFileName(profile));
    }

    @Override
    public boolean existsSpringConfigFile() {
        return fileManager.exists(getSpringConfigLocation());
    }
    
    @Override
    public boolean existsSpringConfigFile(String profile) {
        return fileManager.exists(getSpringConfigLocation(profile));
    }
    
    /**
     * Method that generates application config file name using application config file name
     * profile parameter if exists, and config file extension.
     * 
     * @param profile
     * @return
     */
    private String getAppliCationConfigFileName(String profile) {
        String fileName = APPLICATION_CONFIG_FILE_NAME;
        if (profile != null && StringUtils.isNotBlank(profile)) {
            fileName = fileName.concat("-").concat(profile);
        }
        fileName = fileName.concat(APPLICATION_CONFIG_FILE_EXTENSION);

        return fileName;
    }
}
