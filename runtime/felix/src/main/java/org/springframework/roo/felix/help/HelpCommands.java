package org.springframework.roo.felix.help;

import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.process.manager.ProcessManager;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Enables a user to obtain Help
 * 
 * @author Juan Carlos García
 * @since 1.3
 */
@Service
@Component
public class HelpCommands implements CommandMarker {

  private static final String HELP_COMMAND = "help";
  private static final String REFERENCE_GUIDE_COMMAND = "reference guide";

  @Reference
  HelpService helpService;

  protected final static Logger LOGGER = HandlerUtils.getLogger(HelpCommands.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private Shell shell;
  private ProcessManager processManager;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  @CliAvailabilityIndicator(REFERENCE_GUIDE_COMMAND)
  public boolean isReferenceGuideAvailable() {
    return getShell().isDevelopmentMode() && getProcessManager().isDevelopmentMode();
  }

  @CliCommand(
      value = REFERENCE_GUIDE_COMMAND,
      help = "Writes the reference guide XML fragments (in DocBook format) into the current working directory")
  public void helpReferenceGuide() {
    helpService.helpReferenceGuide();
  }

  @CliCommand(value = HELP_COMMAND, help = "Shows system help")
  public void obtainHelp(@CliOption(key = {"", "command"}, optionContext = "availableCommands",
      help = "Command name to provide help for") final String buffer) {

    helpService.obtainHelp(buffer);
  }

  public ProcessManager getProcessManager() {
    if (processManager == null) {
      // Get all components implement ProcessManager interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ProcessManager.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          processManager = (ProcessManager) this.context.getService(ref);
          return processManager;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ProcessManager on ProcessManagerDiagnosticListener.");
        return null;
      }
    } else {
      return processManager;
    }

  }

  public Shell getShell() {
    if (shell == null) {
      // Get all Shell implement Shell interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(Shell.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          shell = (Shell) this.context.getService(ref);
          return shell;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load Shell on HelpCommands.");
        return null;
      }
    } else {
      return shell;
    }

  }
}
