package org.springframework.roo.obr.addon.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.felix.BundleSymbolicName;
import org.springframework.roo.obr.addon.search.model.ObrBundle;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * 
 * AddonSearch implementation that search available addons on installed OBR
 * Repositories using OSGi Services
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 */
@Component
@Service
public class ObrAddonSearchOperationsImpl implements ObrAddOnSearchOperations {

  private static final String CAPABILITY_COMMANDS_NAME = "roo-addon";
  private static final String CAPABILITY_JDBCDRIVER_NAME = "jdbcdriver";
  private static final String CAPABILITY_LIBRARY_NAME = "library";
  private BundleContext context;
  private static final Logger LOGGER = HandlerUtils.getLogger(ObrAddonSearchOperationsImpl.class);

  private RepositoryAdmin repositoryAdmin;
  private Shell shell;

  private final Object mutex = new Object();
  private List<Repository> repositories;
  private List<ObrBundle> bundlesToInstall;
  private Map<String, ObrBundle> bundleCache;
  private Map<String, ObrBundle> searchResultCache;

  protected void activate(final ComponentContext cContext) {
    context = cContext.getBundleContext();
    repositories = new ArrayList<Repository>();
    bundlesToInstall = new ArrayList<ObrBundle>();
    bundleCache = new HashMap<String, ObrBundle>();
    searchResultCache = new HashMap<String, ObrBundle>();
    // Add default repositories
    addDefaultRepositories();
    // Populate Repositories
    populateRepositories();
    // Populate Bundle Cache
    populateBundleCache();
  }

  @Override
  public Integer searchAddOns(String searchTerms, SearchType type) {

    final List<ObrBundle> result = findAddons(searchTerms, type);
    return result != null ? result.size() : null;

  }

  public List<ObrBundle> findAddons(final String searchTerms, final SearchType type) {
    synchronized (mutex) {

      LOGGER
          .log(Level.INFO, String.format("Searching '%s' on installed repositories", searchTerms));

      // Populating Repositories
      populateRepositories();

      if (repositories.isEmpty()) {
        LOGGER.log(Level.INFO, "No repositories installed on Spring Roo yet");
        return bundlesToInstall;
      }

      // Loading bundles to install depending of search type
      populateBundlesToInstall(searchTerms, type);

      // Showing info about matches found
      if (bundlesToInstall.isEmpty()) {
        LOGGER.log(Level.INFO,
            String.format("0 matches found with '%s' on installed repositories", searchTerms));
        return bundlesToInstall;
      }

      LOGGER.log(
          Level.INFO,
          String.format("%s matches found with '%s' on installed repositories",
              bundlesToInstall.size(), searchTerms));

      // Showing list about how to install bundles
      printResultList(bundlesToInstall);

      // Refreshing available bundles
      populateBundleCache();
    }


    return bundlesToInstall;

  }

  private void addDefaultRepositories() {

    // Getting wrapping repository url
    String wrappingRepoUrl = context.getProperty("wrapping.repository.url");
    try {
      getRepositoryAdmin().addRepository(wrappingRepoUrl);

    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "WARNING: Wrapping repository could not be installed");
    }
  }

  /**
   * Method to populate current Repositories using OSGi Serive
   */
  private void populateRepositories() {

    // Cleaning Repositories
    repositories.clear();

    // Validating that RepositoryAdmin exists
    Validate.notNull(getRepositoryAdmin(), "RepositoryAdmin not found");

    for (Repository repo : getRepositoryAdmin().listRepositories()) {
      repositories.add(repo);
    }
  }

  /**
   * Method to populate bundles to install. Depending of the search type, will be displayed 
   * different kinds of bundles.

   * @param requiresCommand
   * @param type
   */
  private void populateBundlesToInstall(String searchTerms, SearchType type) {

    // Refreshing Repositories
    populateRepositories();

    // Cleaning Bundles to install
    bundlesToInstall.clear();

    // Cleaning previous search
    searchResultCache.clear();

    int bundleId = 0;

    for (Repository repo : repositories) {
      // Getting all resources from every repo
      Resource[] repoResources = repo.getResources();

      for (Resource repoResource : repoResources) {
        // Creating bundle of current resource
        ObrBundle bundle =
            new ObrBundle(repoResource.getSymbolicName(), repoResource.getPresentationName(),
                repoResource.getSize(), repoResource.getVersion(), repoResource.getURI());

        // If current bundle is installed, continue with the next one
        if (checkIfBundleIsInstalled(bundle)) {
          continue;
        }

        // Getting Resource Capabilites
        Capability[] resourceCapabilities = repoResource.getCapabilities();

        for (Capability capability : resourceCapabilities) {

          // Depending of search type, is necessary to look for different
          // capabilities
          if (type.equals(SearchType.ADDON)) {

            // Getting resource commands
            if (capability.getName().equals(CAPABILITY_COMMANDS_NAME)) {
              // Getting all resource properties
              Map<String, Object> capabilityProperties = capability.getPropertiesAsMap();

              boolean match = false;

              for (Entry capabilityProperty : capabilityProperties.entrySet()) {
                String capabilityCommand = (String) capabilityProperty.getValue();
                bundle.addCommand(capabilityCommand);
                if (capabilityCommand.startsWith(searchTerms)) {
                  match = true;
                }
              }

              if (match) {
                bundleId++;
                bundlesToInstall.add(bundle);
                searchResultCache.put(String.format("%02d", bundleId), bundle);
              }
            }


          } else if (type.equals(SearchType.JDBCDRIVER)) {

            // Getting resource driver
            if (capability.getName().equals(CAPABILITY_JDBCDRIVER_NAME)) {
              // Getting all resource properties
              Map<String, Object> capabilityProperties = capability.getPropertiesAsMap();

              boolean match = false;

              for (Entry capabilityProperty : capabilityProperties.entrySet()) {
                String capabilityKey = (String) capabilityProperty.getKey();
                // Getting driver class
                if (capabilityKey.toLowerCase().equals("driver")) {
                  String capabilityDriver = (String) capabilityProperty.getValue();
                  if (capabilityDriver.startsWith(searchTerms)) {
                    match = true;
                  }
                }
              }

              if (match) {
                bundleId++;
                bundlesToInstall.add(bundle);
                searchResultCache.put(String.format("%02d", bundleId), bundle);
              }
            }


          } else if (type.equals(SearchType.LIBRARY)) {
            // TODO: Implement library bundle search
          }
        }
      }
    }
  }

  /**
   * Method to check if some bundle is installed on OSGi
   * @param bundle
   * @return
   */
  private boolean checkIfBundleIsInstalled(ObrBundle bundle) {
    // Refreshing installed bundles
    Bundle[] allInstalledBundles = context.getBundles();
    // Checking if bundles is installed
    for (Bundle installedBundle : allInstalledBundles) {
      if (installedBundle.getSymbolicName().equals(bundle.getSymbolicName())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Method to populate current Bundles on installed repositories
   */
  private void populateBundleCache() {

    // Refreshing Repositories
    populateRepositories();

    // Cleaning Bundle Cache
    bundleCache.clear();

    for (Repository repo : repositories) {
      Resource[] repoResources = repo.getResources();

      for (Resource repoResource : repoResources) {
        // Creating bundle of current resource
        ObrBundle bundle =
            new ObrBundle(repoResource.getSymbolicName(), repoResource.getPresentationName(),
                repoResource.getSize(), repoResource.getVersion(), repoResource.getURI());

        // Getting Resource Capabilites
        Capability[] resourceCapabilities = repoResource.getCapabilities();

        for (Capability capability : resourceCapabilities) {
          // Getting resource commands
          if (capability.getName().equals(CAPABILITY_COMMANDS_NAME)
              || capability.getName().equals(CAPABILITY_JDBCDRIVER_NAME)
              || capability.getName().equals(CAPABILITY_LIBRARY_NAME)) {
            // Getting all resource properties
            Map<String, Object> capabilityProperties = capability.getPropertiesAsMap();

            for (Entry capabilityProperty : capabilityProperties.entrySet()) {
              String capabilityCommand = (String) capabilityProperty.getValue();
              bundle.addCommand(capabilityCommand);
            }
            bundleCache.put(bundle.getSymbolicName(), bundle);
          }
        }

      }
    }
  }

  private void printResultList(List<ObrBundle> bundles) {
    final StringBuilder sb = new StringBuilder();
    int bundleId = 1;
    int maxSymbolicNameLength = getSymbolicNameMaxLength(bundles);
    LOGGER.warning(String.format("ID   BUNDLE SYMBOLIC NAME%s   DESCRIPTION",
        printSpaces(maxSymbolicNameLength - "BUNDLE SYMBOLIC NAME".length())));
    LOGGER
        .warning("--------------------------------------------------------------------------------");
    for (final ObrBundle bundle : bundles) {
      final String bundleKey = String.format("%02d", bundleId++);
      sb.append(bundleKey);
      sb.append("   ");
      sb.append(bundle.getSymbolicName());
      sb.append(String.format("%s   ", printSpaces(maxSymbolicNameLength
          - bundle.getSymbolicName().length())));
      sb.append(bundle.getPresentationName());
      if (sb.toString().trim().length() > 0) {
        LOGGER.info(sb.toString());
      }
      sb.setLength(0);
    }
    LOGGER
        .warning("--------------------------------------------------------------------------------");
    LOGGER
        .info("[HINT] use 'addon info bundle --bundleSymbolicName' to see details about a search result");
    LOGGER
        .info("[HINT] use 'addon install bundle --bundleSymbolicName' to install a specific add-on version");
  }


  /**
   * Method to print Spaces by total Spaces
   */
  public String printSpaces(int numberOfSpaces) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < numberOfSpaces; i++) {
      sb.append(" ");
    }
    return sb.toString();
  }

  /**
   * Method to obtain max length of Symbolic Names 
   */
  public int getSymbolicNameMaxLength(List<ObrBundle> bundles) {
    int maxSymbolicNameLength = 0;
    for (final ObrBundle bundle : bundles) {
      String symbolicName = bundle.getSymbolicName();
      if (symbolicName.length() > maxSymbolicNameLength) {
        maxSymbolicNameLength = symbolicName.length();
      }
    }
    return maxSymbolicNameLength;
  }


  @Override
  public void addOnInfo(ObrAddOnBundleSymbolicName bsn) {
    Validate.notNull(bsn, "A valid add-on bundle symbolic name is required");

    // Refreshing bundle cache
    populateBundleCache();

    synchronized (mutex) {
      String bsnString = bsn.getKey();
      if (bsnString.contains(";")) {
        bsnString = bsnString.split(";")[0];
      }
      final ObrBundle bundle = bundleCache.get(bsnString);
      if (bundle == null) {
        LOGGER.warning("Unable to find specified bundle with symbolic name: " + bsn.getKey());
        return;
      }
      addOnInfo(bundle, bundle.getVersion());
    }

  }

  @Override
  public void addOnInfo(String bundleId) {
    Validate.notBlank(bundleId, "A valid bundle ID is required");
    synchronized (mutex) {
      ObrBundle bundle = null;
      if (searchResultCache != null) {
        bundle = searchResultCache.get(String.format("%02d", Integer.parseInt(bundleId)));
      }
      if (bundle == null) {
        LOGGER.warning("A valid bundle ID is required");
        return;
      }
      addOnInfo(bundle, bundle.getVersion());
    }

  }

  @Override
  public Map<String, ObrBundle> getAddOnCache() {
    synchronized (mutex) {
      populateBundleCache();
      return Collections.unmodifiableMap(bundleCache);
    }
  }

  @Override
  public InstallOrUpgradeStatus installAddOn(ObrAddOnBundleSymbolicName bsn) {

    // Refreshing bundle cache
    populateBundleCache();

    synchronized (mutex) {
      Validate.notNull(bsn, "A valid add-on bundle symbolic name is required");
      String bsnString = bsn.getKey();
      if (bsnString.contains(";")) {
        bsnString = bsnString.split(";")[0];
      }
      final ObrBundle bundle = bundleCache.get(bsnString);
      if (bundle == null) {
        LOGGER.warning("Could not find specified bundle with symbolic name: " + bsn.getKey());
        return InstallOrUpgradeStatus.FAILED;
      }
      return installAddon(bundle, bsn.getKey());
    }
  }

  @Override
  public InstallOrUpgradeStatus installAddOn(String bundleId) {
    synchronized (mutex) {
      Validate.notBlank(bundleId, "A valid bundle ID is required");
      ObrBundle bundle = null;
      if (searchResultCache != null) {
        bundle = searchResultCache.get(String.format("%02d", Integer.parseInt(bundleId)));
      }
      if (bundle == null) {
        LOGGER.warning("To install an addon a valid bundle ID is required");
        return InstallOrUpgradeStatus.FAILED;
      }
      return installAddon(bundle, bundle.getSymbolicName());
    }
  }

  @Override
  public InstallOrUpgradeStatus installAddOnByUrl(String url) {
    synchronized (mutex) {
      Validate.notNull(url, "Valid url is required");
      boolean success = getShell().executeCommand("!g felix:install " + url);
      success = getShell().executeCommand("!g felix:start " + url);

      return success ? InstallOrUpgradeStatus.SUCCESS : InstallOrUpgradeStatus.FAILED;
    }
  }

  @Override
  public InstallOrUpgradeStatus removeAddOn(BundleSymbolicName bsn) {
    synchronized (mutex) {
      InstallOrUpgradeStatus status;
      try {

        Validate.notNull(bsn, "Bundle symbolic name required");
        bsn.findBundleWithoutFail(context).uninstall();

        LOGGER.log(Level.INFO, String.format("Bundle '%s' : Uninstalled!", bsn.getKey()));
        LOGGER.log(Level.INFO, "");
        status = InstallOrUpgradeStatus.SUCCESS;
        return status;
      } catch (BundleException e) {
        LOGGER.warning("Unable to remove add-on: " + bsn.getKey());
        status = InstallOrUpgradeStatus.FAILED;
        return status;
      }
    }
  }

  /**
   * This method shows addon info on Spring Roo Shell
   * 
   * @param bundle
   * @param bundleVersion
   */
  private void addOnInfo(final ObrBundle bundle, final Version bundleVersion) {
    logInfo("Name", bundle.getPresentationName());
    logInfo("BSN", bundle.getSymbolicName());
    logInfo("Version", bundleVersion.toString());
    logInfo("JAR Size", bundle.getSize() + " bytes");
    logInfo("JAR URL", bundle.getUri());
    StringBuilder sb = new StringBuilder();
    for (final String command : bundle.getCommands()) {
      sb.append(command).append(", ");
    }
    logInfo("Commands", sb.toString());
  }

  private void logInfo(final String label, String content) {
    final StringBuilder sb = new StringBuilder();
    sb.append(label);
    for (int i = 0; i < 13 - label.length(); i++) {
      sb.append(".");
    }
    sb.append(": ");
    if (content.length() < 65) {
      sb.append(content);
      LOGGER.info(sb.toString());
    } else {
      final List<String> split = new ArrayList<String>(Arrays.asList(content.split("\\s")));
      if (split.size() == 1) {
        while (content.length() > 65) {
          sb.append(content.substring(0, 65));
          content = content.substring(65);
          LOGGER.info(sb.toString());
          sb.setLength(0);
          sb.append("               ");
        }
        if (content.length() > 0) {
          LOGGER.info(sb.append(content).toString());
        }
      } else {
        while (split.size() > 0) {
          while (!split.isEmpty() && split.get(0).length() + sb.length() < 79) {
            sb.append(split.get(0)).append(" ");
            split.remove(0);
          }
          LOGGER.info(sb.toString().substring(0, sb.toString().length() - 1));
          sb.setLength(0);
          sb.append("               ");
        }
      }
    }
  }


  /**
   * This method will install bundle on Spring Roo Repository
   * 
   * @param bundleVersion
   * @param bsn
   * @return
   */
  private InstallOrUpgradeStatus installAddon(final ObrBundle bundle, final String bsn) {
    final InstallOrUpgradeStatus status = installOrUpgradeAddOn(bundle);
    switch (status) {
      case SUCCESS:
        LOGGER.info("Successfully installed add-on: " + bundle.getPresentationName()
            + " [version: " + bundle.getVersion() + "]");
        break;
      default:
        LOGGER.warning("Unable to install add-on: " + bundle.getPresentationName() + " [version: "
            + bundle.getVersion() + "]");
        break;
    }

    return status;
  }

  /**
   * 
   * This method will install Addon
   * 
   * @param bundle
   * @param bsn
   * @param install
   * @return
   */
  private InstallOrUpgradeStatus installOrUpgradeAddOn(final ObrBundle bundle) {

    boolean success = getShell().executeCommand("!g obr:deploy " + bundle.getSymbolicName());
    success = getShell().executeCommand("!g obr:start " + bundle.getSymbolicName());

    return success ? InstallOrUpgradeStatus.SUCCESS : InstallOrUpgradeStatus.FAILED;
  }

  @Override
  public void list() {
    synchronized (mutex) {
      getShell().executeCommand("!g lb");
    }
  }

  /**
   * Method to get RepositoryAdmin Service implementation
   * 
   * @return
   */
  public RepositoryAdmin getRepositoryAdmin() {
    if (repositoryAdmin == null) {
      // Get all Services implement RepositoryAdmin interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(RepositoryAdmin.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          repositoryAdmin = (RepositoryAdmin) context.getService(ref);
          return repositoryAdmin;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load RepositoryAdmin on AddonSearchImpl.");
        return null;
      }
    } else {
      return repositoryAdmin;
    }
  }

  /**
   * Method to get Shell Service implementation
   * 
   * @return
   */
  public Shell getShell() {
    if (shell == null) {
      // Get all Services implement Shell interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(Shell.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          shell = (Shell) context.getService(ref);
          return shell;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load Shell on AddonSearchImpl.");
        return null;
      }
    } else {
      return shell;
    }
  }
}
