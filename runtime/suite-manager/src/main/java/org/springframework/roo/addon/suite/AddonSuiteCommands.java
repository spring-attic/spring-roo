package org.springframework.roo.addon.suite;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Commands to manage Roo Addon Suites
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 */
@Component
@Service
public class AddonSuiteCommands implements CommandMarker {

  @Reference
  private AddonSuiteOperations operations;

  @CliCommand(value = "addon suite install name",
      help = "Install some 'Roo Addon Suite' from installed OBR Repository")
  public void installAddonSuiteByName(
      @CliOption(key = "symbolicName", mandatory = true,
          help = "Name that identifies the 'Roo Addon Suite'") final ObrAddonSuiteSymbolicName suiteSymbolicName)
      throws Exception {
    operations.installRooAddonSuiteByName(suiteSymbolicName);
  }

  @CliCommand(value = "addon suite install url", help = "Install some 'Roo Addon Suite' from URL")
  public void installAddonSuiteByURL(@CliOption(key = "url", mandatory = true,
      help = "URL of Roo Addon Suite .esa file") final String url) throws Exception {
    operations.installRooAddonSuiteByUrl(url);
  }

  @CliCommand(value = "addon suite uninstall", help = "Uninstall some installed 'Roo Addon Suite'")
  public void uninstallAddonSuite(@CliOption(key = "symbolicName", mandatory = true,
      help = "Name that identifies the 'Roo Addon Suite'") final AddonSuiteSymbolicName symbolicName)
      throws Exception {
    operations.uninstallRooAddonSuite(symbolicName);
  }

  @CliCommand(value = "addon suite start", help = "Start some installed 'Roo Addon Suite' ")
  public void startAddonSuite(@CliOption(key = "symbolicName", mandatory = true,
      help = "Name that identifies the 'Roo Addon Suite'") final AddonSuiteSymbolicName symbolicName)
      throws Exception {
    operations.startRooAddonSuite(symbolicName);
  }

  @CliCommand(value = "addon suite stop", help = "Stop some started 'Roo Addon Suite'")
  public void stopAddonSuite(@CliOption(key = "symbolicName", mandatory = true,
      help = "Name that identifies the 'Roo Addon Suite'") final AddonSuiteSymbolicName symbolicName)
      throws Exception {
    operations.stopRooAddonSuite(symbolicName);
  }

  @CliCommand(
      value = "addon suite list",
      help = "Lists all installed 'Roo Addon Suite'. If you want to list all available 'Roo Addon Suites' on Repository, use --repository parameter")
  public void stopAddonSuite(
      @CliOption(key = "repository", mandatory = false,
          help = "OBR Repository where the 'Roo Addon Suite' are located") final ObrRepositorySymbolicName obrRepository)
      throws Exception {

    if (obrRepository == null) {
      operations.listAllInstalledSubsystems();
    } else {
      operations.listAllSubsystemsOnRepository(obrRepository);
    }

  }
}
