package org.srpingframework.roo.osgi.gogo.commands;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Property;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.subsystem.Subsystem;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * 
 * Adding Subsystem Gogo commands
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component(immediate = true)
@Service
public class SubsystemCommands implements BundleActivator {

  private static final Logger LOGGER = HandlerUtils.getLogger(SubsystemCommands.class);

  private static final String REPOSITORY_DEPENDENCY_CAPABILITY_NAME = "repositories";
  private static final String SUBSYSTEM_DEPENDENCY_CAPABILITY_NAME = "subsystems";

  private BundleContext bundleContext;
  private RepositoryAdmin repositoryAdmin;

  private List<Repository> repositories;

  // TODO: Improve Gogo commands ROO implementation using
  // apache felix @Command annotation

  protected void activate(final ComponentContext context) {
    try {
      start(context.getBundleContext());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void start(BundleContext context) throws Exception {
    bundleContext = context;
    Dictionary<String, Object> props = new Hashtable<String, Object>();
    props.put("osgi.command.function", new String[] {"deploy", "install", "uninstall", "start",
        "stop", "list",});
    props.put("osgi.command.scope", "subsystem");
    context.registerService(getClass().getName(), this, props);
  }

  public void deploy(String symbolicName) throws Exception {
    repositories = new ArrayList<Repository>();
    // Getting all installed repositories
    populateRepositories();
    // Getting subsystem resource from OBR Repository by resource
    // symbolicName
    Resource subsystemResource = getSubsystemResource(symbolicName);
    // Install related repositories or related subsystems if needed
    LOGGER.log(Level.INFO, "Subsystem dependency manager started.");
    LOGGER.log(Level.INFO, "");
    installSubsystemDependencies(subsystemResource);
    LOGGER.log(Level.INFO, "Subsystem dependency manager finished.");
    LOGGER.log(Level.INFO, "");
    // Install subsystem using symbolicName
    Subsystem subsystem = install(subsystemResource.getURI());
    // Starting installed subsystem
    start(subsystem.getSubsystemId());
  }

  public Subsystem install(String url) {
    LOGGER.log(Level.INFO, "Installing subsystem...");
    Subsystem rootSubsystem = getSubsystem(0);
    Subsystem s = rootSubsystem.install(url);
    LOGGER.log(Level.INFO, "Subsystem successfully installed: " + s.getSymbolicName() + "; id: "
        + s.getSubsystemId());
    LOGGER.log(Level.INFO, " ");
    return s;
  }

  public void uninstall(long id) {
    LOGGER.log(Level.INFO, "Uninstalling subsystem: " + id);
    LOGGER.log(Level.INFO, " ");
    Subsystem subsystem = getSubsystem(id);
    subsystem.uninstall();
    LOGGER.log(Level.INFO, "Subsystem successfully uninstalled: " + subsystem.getSymbolicName()
        + "; id: " + subsystem.getSubsystemId());
    LOGGER.log(Level.INFO, " ");
  }

  public void start(long id) {
    LOGGER.log(Level.INFO, "Starting subsystem: " + id);
    Subsystem subsystem = getSubsystem(id);
    subsystem.start();
    LOGGER.log(Level.INFO, "Subsystem successfully started: " + subsystem.getSymbolicName()
        + "; id: " + subsystem.getSubsystemId());
    LOGGER.log(Level.INFO, " ");
  }

  public void stop(long id) {
    LOGGER.log(Level.INFO, "Stopping subsystem: " + id);
    LOGGER.log(Level.INFO, " ");
    Subsystem subsystem = getSubsystem(id);
    subsystem.stop();
    LOGGER.log(Level.INFO, "Subsystem successfully stopped: " + subsystem.getSymbolicName()
        + "; id: " + subsystem.getSubsystemId());
    LOGGER.log(Level.INFO, " ");

  }

  public void list() throws InvalidSyntaxException {
    Map<Long, String> subsystems = new TreeMap<Long, String>();
    for (ServiceReference<Subsystem> ref : bundleContext
        .getServiceReferences(Subsystem.class, null)) {
      Subsystem s = bundleContext.getService(ref);
      if (s != null) {
        subsystems.put(
            s.getSubsystemId(),
            String.format("%d\t%s\t%s %s", s.getSubsystemId(), s.getState(), s.getSymbolicName(),
                s.getVersion()));
      }
    }
    for (String entry : subsystems.values()) {
      LOGGER.log(Level.INFO, entry);
    }
  }

  /**
   * Method that checks capabilities of selected subsystem resource and if appears any related repository 
   * or any related subsystem are going to be installed.
   *  
   * @param subsystemResource
   * @throws Exception 
   */
  private void installSubsystemDependencies(Resource subsystemResource) throws Exception {

    // Getting capabilites of subsytem resource
    Capability[] capabilities = subsystemResource.getCapabilities();

    // Creating lists to save repositories and subsystems
    List<Capability> repositoriesCapability = new ArrayList<Capability>();
    List<Capability> subsystemsCapability = new ArrayList<Capability>();

    LOGGER.log(Level.INFO, "Getting 'Roo Addon Suite' dependencies...");
    LOGGER.log(Level.INFO, " ");

    // Saving repositories and subsystems
    for (Capability capability : capabilities) {
      if (capability.getName().equals(REPOSITORY_DEPENDENCY_CAPABILITY_NAME)) {
        repositoriesCapability.add(capability);
      } else if (capability.getName().equals(SUBSYSTEM_DEPENDENCY_CAPABILITY_NAME)) {
        subsystemsCapability.add(capability);
      }
    }

    showDependenciesInfo(repositoriesCapability, subsystemsCapability);

    // If selected Roo Addon Suite doesn't have any dependency
    // continue installing individual 'Roo Addon Suite'
    if (repositoriesCapability.isEmpty() && subsystemsCapability.isEmpty()) {
      return;
    }

    // Installing repositories if needed
    if (!repositoriesCapability.isEmpty()) {
      LOGGER.log(Level.INFO, "   Adding repositories to Spring Roo installation");
      LOGGER.log(Level.INFO, "   ----------------------------------------------------------");
      LOGGER.log(Level.INFO, "");
      // Adding all repositories in order
      for (Capability repositoryCapability : repositoriesCapability) {
        Property[] repositoryList = repositoryCapability.getProperties();
        for (Property repository : repositoryList) {
          String repoURL = repository.getValue();
          getRepositoryAdmin().addRepository(repoURL);
          LOGGER.log(Level.INFO, "      " + repoURL + " added");
        }
      }

      LOGGER.log(Level.INFO, "");
    }

    // Installing subsystems if needed
    if (!subsystemsCapability.isEmpty()) {
      LOGGER.log(Level.INFO, "   Installing subsystems into Spring Roo installation");
      LOGGER.log(Level.INFO, "   ----------------------------------------------------------");
      LOGGER.log(Level.INFO, "");
      // Installing subsystems in order
      for (Capability subsystemCapability : subsystemsCapability) {
        Property[] subsystemList = subsystemCapability.getProperties();
        for (Property subsystem : subsystemList) {
          String subsystemURL = subsystem.getValue();
          getSubsystem(0).install(subsystemURL);
          LOGGER.log(Level.INFO, "      " + subsystemURL + " installed");
        }
      }

      LOGGER.log(Level.INFO, "");
    }

  }

  /**
   * Method to show Subsystem dependencies on Spring Roo Shell
   * 
   * @param repositories
   * @param subsystems
   */
  private void showDependenciesInfo(List<Capability> repositories, List<Capability> subsystems) {

    // If empty dependencies
    if (repositories.isEmpty() && subsystems.isEmpty()) {
      LOGGER.log(Level.INFO, "   0 dependencies were found on selected 'Roo Addon Suite'");
      LOGGER.log(Level.INFO, "");
      return;
    }

    // If selected subsystem has some repositories dependency
    if (!repositories.isEmpty()) {
      LOGGER.log(Level.INFO, "   Repository Dependencies");
      LOGGER.log(Level.INFO, "   ---------------------------------");
      for (Capability repo : repositories) {
        Property[] repositoriesProperties = repo.getProperties();
        for (Property prop : repositoriesProperties) {
          LOGGER.log(Level.INFO, "      " + prop.getValue());
        }
        LOGGER.log(Level.INFO, " ");
        LOGGER.log(Level.INFO, String.format(
            "   %s repository dependencies were found on selected 'Roo Addon Suite'",
            repositoriesProperties.length));
        LOGGER.log(Level.INFO, "");
      }
    }

    // If selected subsystem has some subsystem dependency
    if (!subsystems.isEmpty()) {
      LOGGER.log(Level.INFO, "   Subsystem Dependencies");
      LOGGER.log(Level.INFO, "   ---------------------------------");
      for (Capability subsystem : subsystems) {
        Property[] subsystemProperties = subsystem.getProperties();
        for (Property prop : subsystemProperties) {
          LOGGER.log(Level.INFO, "      " + prop.getValue());
        }
        LOGGER.log(Level.INFO, " ");
        LOGGER.log(Level.INFO, String.format(
            "   %s subsystem dependencies were found on selected 'Roo Addon Suite'",
            subsystemProperties.length));
        LOGGER.log(Level.INFO, "");
      }
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
   * Method to get RepositoryAdmin Service implementation
   * 
   * @return
   */
  public RepositoryAdmin getRepositoryAdmin() {
    if (repositoryAdmin == null) {
      // Get all Services implement RepositoryAdmin interface
      try {
        ServiceReference<?>[] references =
            bundleContext.getAllServiceReferences(RepositoryAdmin.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          repositoryAdmin = (RepositoryAdmin) bundleContext.getService(ref);
          return repositoryAdmin;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load RepositoryAdmin on SubsystemCommands.");
        return null;
      }
    } else {
      return repositoryAdmin;
    }
  }

  private Subsystem getSubsystem(long id) {
    try {
      for (ServiceReference<Subsystem> ref : bundleContext.getServiceReferences(Subsystem.class,
          "(subsystem.id=" + id + ")")) {
        Subsystem svc = bundleContext.getService(ref);
        if (svc != null)
          return svc;
      }
    } catch (InvalidSyntaxException e) {
      throw new RuntimeException(e);
    }
    throw new RuntimeException("Unable to find subsystem " + id);
  }

  /**
     * Method to obtain subsystem resource by symbolicName
     * 
     * @return
     */
  private Resource getSubsystemResource(String symbolicName) {
    for (Repository repo : repositories) {
      // Getting all resources from every repo
      Resource[] repoResources = repo.getResources();

      for (Resource resource : repoResources) {
        // Getting resource
        if (resource.getSymbolicName().equals(symbolicName)) {
          return resource;
        }
      }
    }

    throw new RuntimeException("Unable to find any Roo Addon Suite on installed repositories "
        + "with symbolic name  " + symbolicName);
  }

  public void stop(BundleContext context) throws Exception {}
}
