package org.springframework.roo.addon.propfiles;

import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.propfiles.manager.PropFilesManagerService;

/**
 * Provides property file configuration operations.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component
@Service
public class PropFileOperationsImpl implements PropFileOperations {

    @Reference private ProjectOperations projectOperations;
    @Reference private PropFilesManagerService propFilesManager;
    
    @Override
    public boolean isPropertiesCommandAvailable() {
        return projectOperations.isFocusedProjectAvailable();
    }

    @Override
    public void addProperties(final LogicalPath propertyFilePath,
            final String propertyFilename, final Map<String, String> properties,
            final boolean sorted, final boolean changeExisting) {
        propFilesManager.addProperties(propertyFilePath, propertyFilename,
                properties, sorted, changeExisting);
    }

    @Override
    public void addPropertyIfNotExists(final LogicalPath propertyFilePath,
            final String propertyFilename, final String key,
            final String value) {
        propFilesManager.addPropertyIfNotExists(propertyFilePath,
                propertyFilename, key, value);
    }

    @Override
    public void addPropertyIfNotExists(final LogicalPath propertyFilePath,
            final String propertyFilename, final String key, final String value,
            final boolean sorted) {
        propFilesManager.addPropertyIfNotExists(propertyFilePath,
                propertyFilename, key, value, sorted);
    }

    @Override
    public void changeProperty(final LogicalPath propertyFilePath,
            final String propertyFilename, final String key,
            final String value) {
        propFilesManager.changeProperty(propertyFilePath, propertyFilename, key,
                value);
    }

    @Override
    public void changeProperty(final LogicalPath propertyFilePath,
            final String propertyFilename, final String key, final String value,
            final boolean sorted) {
        propFilesManager.changeProperty(propertyFilePath, propertyFilename, key,
                value, sorted);
    }

    @Override
    public Map<String, String> getProperties(final LogicalPath propertyFilePath,
            final String propertyFilename) {
        return propFilesManager.getProperties(propertyFilePath,
                propertyFilename);
    }

    @Override
    public String getProperty(final LogicalPath propertyFilePath,
            final String propertyFilename, final String key) {
        return propFilesManager.getProperty(propertyFilePath, propertyFilename,
                key);
    }

    @Override
    public SortedSet<String> getPropertyKeys(final LogicalPath propertyFilePath,
            final String propertyFilename, final boolean includeValues) {
        return propFilesManager.getPropertyKeys(propertyFilePath,
                propertyFilename, includeValues);
    }

    @Override
    public Properties loadProperties(final InputStream inputStream) {
        return propFilesManager.loadProperties(inputStream);
    }

    @Override
    public Properties loadProperties(final String filename,
            final Class<?> loadingClass) {
        return propFilesManager.loadProperties(filename, loadingClass);
    }

    @Override
    public void removeProperty(final LogicalPath propertyFilePath,
            final String propertyFilename, final String key) {
        propFilesManager.removeProperty(propertyFilePath, propertyFilename,
                key);
    }
}
