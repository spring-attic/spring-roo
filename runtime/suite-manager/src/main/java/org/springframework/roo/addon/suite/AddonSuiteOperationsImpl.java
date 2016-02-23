package org.springframework.roo.addon.suite;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.subsystem.Subsystem;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * 
 * AddonSuiteOperations implementation that manage Roo Addon Suite on Spring Roo
 * Shell
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 */
@Component
@Service
public class AddonSuiteOperationsImpl implements AddonSuiteOperations {

  private BundleContext context;
  private static final Logger LOGGER = HandlerUtils.getLogger(AddonSuiteOperationsImpl.class);

  private CommandProcessor commandProcessor;
  private RepositoryAdmin repositoryAdmin;

  protected void activate(final ComponentContext cContext) {
    context = cContext.getBundleContext();
  }

  @Override
  public void installRooAddonSuiteByName(ObrAddonSuiteSymbolicName suiteSymbolicName)
      throws Exception {
    perform("subsystem:deploy " + suiteSymbolicName.getKey());
  }

  @Override
  public void installRooAddonSuiteByUrl(String url) throws Exception {
    perform("subsystem:install " + url);
  }

  @Override
  public void startRooAddonSuite(AddonSuiteSymbolicName symbolicName) {
    LOGGER
        .log(Level.INFO, String.format("Starting '%s' Roo Addon Suite...", symbolicName.getKey()));
    Subsystem subsystem = getSubsystemByName(symbolicName);
    if (subsystem != null) {
      subsystem.start();
      LOGGER.log(Level.INFO, String.format("'%s' Roo Addon Suite started!", symbolicName.getKey()));
    } else {
      LOGGER.warning(String.format("Cannot start Subsystem '%s'", symbolicName.getKey()));
    }
  }

  @Override
  public void stopRooAddonSuite(AddonSuiteSymbolicName symbolicName) {
    LOGGER
        .log(Level.INFO, String.format("Stopping '%s' Roo Addon Suite...", symbolicName.getKey()));
    Subsystem subsystem = getSubsystemByName(symbolicName);
    if (subsystem != null) {
      subsystem.stop();
      LOGGER.log(Level.INFO, String.format("'%s' Roo Addon Suite stopped!", symbolicName.getKey()));
    } else {
      LOGGER.warning(String.format("Cannot stop Subsystem '%s'", symbolicName.getKey()));
    }
  }

  @Override
  public void uninstallRooAddonSuite(AddonSuiteSymbolicName symbolicName) {
    LOGGER.log(Level.INFO,
        String.format("Uninstalling '%s' Roo Addon Suite...", symbolicName.getKey()));
    Subsystem subsystem = getSubsystemByName(symbolicName);
    if (subsystem != null) {
      subsystem.uninstall();
      LOGGER.log(Level.INFO,
          String.format("'%s' Roo Addon Suite uninstalled!", symbolicName.getKey()));
    } else {
      LOGGER.warning(String.format("Cannot uninstall Subsystem '%s'", symbolicName.getKey()));
    }
  }

  @Override
  public void listAllInstalledSubsystems() {

    LOGGER.log(Level.INFO, "Getting all 'Roo Addon Suites' installed on Spring Roo Shell... ");
    LOGGER.log(Level.INFO, " ");

    // Get all Services implement Subsystem interface
    try {
      ServiceReference<?>[] references =
          context.getAllServiceReferences(Subsystem.class.getName(), null);
      for (ServiceReference<?> ref : references) {
        Subsystem subsystem = (Subsystem) context.getService(ref);
        LOGGER.log(Level.INFO, "   " + subsystem.getSymbolicName());
      }

      LOGGER.log(Level.INFO, " ");
      LOGGER.log(Level.INFO, String.format(
          "%s Roo Addon Suites were found at your Spring Roo installation", references.length));

    } catch (InvalidSyntaxException e) {
      LOGGER.warning("Cannot load Subsystem on AddonSuiteOperationsImpl.");
    }

  }

  @Override
  public void listAllSubsystemsOnRepository(ObrRepositorySymbolicName obrRepository) {
    LOGGER.log(Level.INFO, "Getting all 'Roo Addon Suites' located on '" + obrRepository.getKey()
        + "' repository... ");
    LOGGER.log(Level.INFO, " ");

    int repos = 0;

    // Getting all repositories
    Repository[] repositories = getRepositoryAdmin().listRepositories();

    for (Repository repo : repositories) {
      if (repo.getURI().equals(obrRepository.getKey())) {
        // Getting all resources from repository
        Resource[] repositoryResource = repo.getResources();
        for (Resource resource : repositoryResource) {
          // If current resource ends with .esa, means that is a ROO Addon Suite
          if (resource.getURI().endsWith(".esa")) {
            LOGGER.log(Level.INFO, "   " + resource.getSymbolicName());
            repos++;
          }
        }
      }
    }

    LOGGER.log(Level.INFO, " ");
    LOGGER.log(
        Level.INFO,
        String.format("%s Roo Addon Suites were found on '%s' repository", repos,
            obrRepository.getKey()));

  }

  /**
   * Method to obtain installed subsystem by name
   * 
   * @param symbolicName
   * @return
   */
  private Subsystem getSubsystemByName(AddonSuiteSymbolicName symbolicName) {
    // Get all Services implement Subsystem interface
    try {
      ServiceReference<?>[] references =
          context.getAllServiceReferences(Subsystem.class.getName(), null);
      for (ServiceReference<?> ref : references) {
        Subsystem subsystem = (Subsystem) context.getService(ref);
        if (subsystem.getSymbolicName().equals(symbolicName.getKey())) {
          return subsystem;
        }
      }

      return null;

    } catch (InvalidSyntaxException e) {
      LOGGER.warning("Cannot load Subsystem on AddonSuiteOperationsImpl.");
      return null;
    }
  }

  private void perform(final String commandLine) throws Exception {
    if ("shutdown".equals(commandLine)) {
      context.getBundle(0).stop();
      return;
    }

    ByteArrayOutputStream sysOut = new ByteArrayOutputStream();
    ByteArrayOutputStream sysErr = new ByteArrayOutputStream();

    final PrintStream printStreamOut = new PrintStream(sysOut);
    final PrintStream printErrOut = new PrintStream(sysErr);
    try {
      final CommandSession commandSession =
          getCommandProcessor().createSession(System.in, printStreamOut, printErrOut);
      Object result = commandSession.execute(commandLine);

      if (result != null) {
        printStreamOut.println(commandSession.format(result, Converter.INSPECT));
      }

      if (sysOut.size() > 0) {
        LOGGER.log(Level.INFO, new String(sysOut.toByteArray()));
      }

      if (sysErr.size() > 0) {
        LOGGER.log(Level.SEVERE, new String(sysErr.toByteArray()));
      }
    } catch (Throwable ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
    } finally {
      printStreamOut.close();
      printErrOut.close();
    }
  }

  /**
   * Method to get CommandProcessor Service implementation
   * 
   * @return
   */
  public CommandProcessor getCommandProcessor() {
    if (commandProcessor == null) {
      // Get all Services implement CommandProcessor interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(CommandProcessor.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          commandProcessor = (CommandProcessor) context.getService(ref);
          return commandProcessor;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load CommandProcessor on AddonSuiteOperationsImpl.");
        return null;
      }
    } else {
      return commandProcessor;
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
        LOGGER.warning("Cannot load RepositoryAdmin on AddonSuiteOperationsImpl.");
        return null;
      }
    } else {
      return repositoryAdmin;
    }
  }
}
