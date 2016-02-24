package org.springframework.roo.settings.project;

import java.io.File;
import java.util.Map;
import java.util.SortedSet;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.propfiles.manager.PropFilesManagerService;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.OSGiUtils;
import org.springframework.roo.support.util.FileUtils;

/**
 * 
 * Project configuration manager implementation to manage the Spring Roo
 * configuration stored in the properties file ".roo/config/project.properties"
 *
 * @author Paula Navarro
 * @since 2.0
 */
@Component
@Service
public class ProjectSettingsServiceImpl implements ProjectSettingsService {

  private static final Path PROJECT_CONFIG_FOLDER_LOCATION = Path.ROOT_ROO_CONFIG;
  private static final String PROJECT_CONFIG_FILE_FOLDER = "config/";
  private static final String PROJECT_CONFIG_FILE_NAME = "project.properties";

  protected final static Logger LOGGER = HandlerUtils.getLogger(ProjectSettingsServiceImpl.class);

  @Reference
  private PathResolver pathResolver;

  private PropFilesManagerService propFilesManager;
  private FileManager fileManager;

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  @Override
  public void addProperty(final String key, final String value, final boolean force) {
    getPropFilesManager().addPropertyIfNotExists(
        LogicalPath.getInstance(PROJECT_CONFIG_FOLDER_LOCATION, ""), getProjectSettingsFileName(),
        key, value, force);
  }

  @Override
  public void removeProperty(final String key) {
    getPropFilesManager().removeProperty(
        LogicalPath.getInstance(PROJECT_CONFIG_FOLDER_LOCATION, ""), getProjectSettingsFileName(),
        key);
  }

  @Override
  public Map<String, String> getProperties() {
    return getPropFilesManager().getProperties(
        LogicalPath.getInstance(PROJECT_CONFIG_FOLDER_LOCATION, ""), getProjectSettingsFileName());
  }

  @Override
  public SortedSet<String> getPropertyKeys(boolean includeValues) {
    return getPropFilesManager().getPropertyKeys(
        LogicalPath.getInstance(PROJECT_CONFIG_FOLDER_LOCATION, ""), getProjectSettingsFileName(),
        includeValues);
  }

  @Override
  public String getProperty(final String key) {
    return getPropFilesManager().getProperty(
        LogicalPath.getInstance(PROJECT_CONFIG_FOLDER_LOCATION, ""), getProjectSettingsFileName(),
        key);
  }

  @Override
  public String getProjectSettingsLocation() {

    return pathResolver.getFocusedIdentifier(PROJECT_CONFIG_FOLDER_LOCATION,
        getProjectSettingsFileName());
  }

  @Override
  public boolean existsProjectSettingsFile() {
    return getFileManager().exists(getProjectSettingsLocation());
  }

  /**
   * Method that generates application configuration filename path using
   * project setting folder and filename. This filename has a ".project"
   * extension.
   *
   * @return
   */
  private String getProjectSettingsFileName() {
    String fileName = PROJECT_CONFIG_FILE_FOLDER;
    fileName = fileName.concat(PROJECT_CONFIG_FILE_NAME);

    return fileName;
  }

  @Override
  public void createProjectSettingsFile() {
    getFileManager().createFile(getProjectSettingsLocation());
  }

  /**
   * Method that finds the propFilesManagerService.
   * 
   * @return the propFilesManagerService. Null is returned if service is not
   *         found
   */
  public PropFilesManagerService getPropFilesManager() {
    if (propFilesManager == null) {
      // Get all Services implement PropFilesManagerService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(PropFilesManagerService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          propFilesManager = (PropFilesManagerService) this.context.getService(ref);
          return propFilesManager;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load PropFilesManagerService on ProjectSettingsServiceImpl.");
        return null;
      }
    } else {
      return propFilesManager;
    }

  }

  /**
   * Method that finds the filesManager.
   * 
   * @return the filesManager. Null is returned if service is not found
   */
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
        LOGGER.warning("Cannot load FileManager on ProjectSettingsServiceImpl.");
        return null;
      }
    } else {
      return fileManager;
    }

  }
}
