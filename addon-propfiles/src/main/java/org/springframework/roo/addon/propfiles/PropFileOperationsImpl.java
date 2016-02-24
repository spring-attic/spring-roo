package org.springframework.roo.addon.propfiles;

import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.application.config.ApplicationConfigService;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Provides necessary operations to include properties on application config
 * properties file.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class PropFileOperationsImpl implements PropFileOperations {

  protected final static Logger LOGGER = HandlerUtils.getLogger(PropFileOperationsImpl.class);

  @Reference
  private ProjectOperations projectOperations;
  @Reference
  private ApplicationConfigService applicationConfigService;

  @Override
  public boolean arePropertiesCommandAvailable() {
    return projectOperations.isFocusedProjectAvailable();
  }

  @Override
  public void addProperty(final String key, final String value, final String profile,
      final boolean force) {
    applicationConfigService.addProperty(key, value, profile, force);
  }

  @Override
  public void removeProperty(String key, String profile) {
    applicationConfigService.removeProperty(key, profile);

  }

  @Override
  public void listProperties(String profile) {
    Map<String, String> properties = applicationConfigService.getProperties(profile);

    if (properties.size() > 0) {

      printHeader();

      for (Entry<String, String> property : properties.entrySet()) {
        LOGGER.log(Level.INFO, property.getKey().concat("=").concat(property.getValue()));
      }

    } else {
      LOGGER.log(Level.INFO, String.format(
          "WARNING: No properties found on '%s' application config properties file.",
          applicationConfigService.getSpringConfigLocation(profile)));
    }

  }

  /**
   * Method that prints header of Spring Roo Configuration
   */
  public void printHeader() {
    String header =
        "#===============================================#\n"
            + "#      APPLICATION CONFIGURATION PROPERTIES     #\n"
            + "#===============================================#\n";
    LOGGER.log(Level.INFO, header);
  }

}
