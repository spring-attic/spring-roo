package org.springframework.roo.addon.cache;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.cache.providers.CacheProvider;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CliOptionAutocompleteIndicator;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.ShellContext;
import org.springframework.roo.support.logging.HandlerUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Commands for the 'cache' add-on to be used by the ROO shell.
 * 
 * This command marker will provide necessary operations to add intermediate 
 * memory support on generated project.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class CacheCommands implements CommandMarker {

  private static final Logger LOGGER = HandlerUtils.getLogger(CacheCommands.class);

  //------------ OSGi component attributes ----------------
  private BundleContext context;

  @Reference
  private CacheOperations cacheOperations;

  private List<CacheProvider> cacheProviders = new ArrayList<CacheProvider>();

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  /**
   * Method that checks if cache setup operation is available or not.
   * 
   * "cache setup" command will be available only if some project was generated 
   * and persistence installed.
   * 
   * @return true if some project was created on focused directory and it has 
   * persistence installed.
   */
  @CliAvailabilityIndicator("cache setup")
  public boolean isCacheSetupCommandAvailable() {
    return cacheOperations.isCacheSetupAvailable();
  }

  /**
   * This indicator returns the cache provider possible values.
   * 
   * @param shellContext
   * @return a List<String> with the possible and allowed values.
   */
  @CliOptionAutocompleteIndicator(command = "cache setup", param = "provider",
      help = "'--provider' value should be a supported provider (GUAVA).")
  public List<String> getProviderPossibleValues(ShellContext shellContext) {
    List<String> possibleValues = new ArrayList<String>();
    for (CacheProvider provider : getCacheProviders()) {
      possibleValues.add(provider.getName());
    }

    return possibleValues;
  }

  /**
   * Method that register "cache setup" command on Spring Roo Shell.
   * 
   * Installs support for intermediate memory. Users can specify different providers 
   * to use for managing it.
   * 
   * @param provider
   *            the String with the name of a provider to use for intermediate memory managing.
   * @param shellContext
   *            ShellContext used to know if --force parameter has been used by developer
   *    
   */
  @CliCommand(
      value = "cache setup",
      help = "Installs support for intermediate memory. Users can specify different providers to use for managing it.")
  public void cacheSetup(
      @CliOption(key = "provider", mandatory = false,
          help = "Parameter that indicates the provider to use for managing intermediate memory.") String provider,
      ShellContext shellContext) {

    // Check for provider value
    CacheProvider selectedCacheProvider = null;
    if (StringUtils.isNotBlank(provider)) {
      for (CacheProvider cacheProvider : getCacheProviders()) {
        if (provider.equals(cacheProvider.getName())) {
          selectedCacheProvider = cacheProvider;
        }
      }
    }

    cacheOperations.setupCache(selectedCacheProvider, shellContext.getProfile());
  }

  /**
   * Gets the right implementation of FieldCreatorProvider for a JavaType
   * 
   * @param type the JavaType to get the implementation
   * @return FieldCreatorProvider implementation
   */
  public List<CacheProvider> getCacheProviders() {

    // Get all Services implement FieldCreatorProvider interface
    if (cacheProviders.isEmpty()) {
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(CacheProvider.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          CacheProvider cacheProvider = (CacheProvider) this.context.getService(ref);
          cacheProviders.add(cacheProvider);
        }

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load CacheProvider on CacheCommands.");
        return null;
      }
    }

    return cacheProviders;
  }

}
