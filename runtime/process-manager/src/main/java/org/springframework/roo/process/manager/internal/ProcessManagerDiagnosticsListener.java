package org.springframework.roo.process.manager.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.process.manager.ProcessManager;
import org.springframework.roo.process.manager.event.ProcessManagerStatus;
import org.springframework.roo.process.manager.event.ProcessManagerStatusListener;
import org.springframework.roo.process.manager.event.ProcessManagerStatusProvider;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.shell.osgi.AbstractFlashingObject;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Allows monitoring of {@link ProcessManager} for development mode users.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @since 1.1
 */
@Service
@Component
public class ProcessManagerDiagnosticsListener extends AbstractFlashingObject implements
    ProcessManagerStatusListener, CommandMarker {

  private static final String PROCESS_MANAGER_DEBUG_COMMAND = "process manager debug";


  private boolean isDebug = false;
  @Reference
  private ProcessManagerStatusProvider processManagerStatusProvider;

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(ProcessManagerDiagnosticsListener.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
    processManagerStatusProvider.addProcessManagerStatusListener(this);
    isDebug = System.getProperty("roo-args") != null && isDevelopmentMode();
  }

  protected void deactivate(final ComponentContext context) {
    processManagerStatusProvider.removeProcessManagerStatusListener(this);
  }

  @CliAvailabilityIndicator(PROCESS_MANAGER_DEBUG_COMMAND)
  public boolean isProcessManagerDebugAvailable() {
    return getShell().isDevelopmentMode() && getProcessManager().isDevelopmentMode();
  }

  public void onProcessManagerStatusChange(final ProcessManagerStatus oldStatus,
      final ProcessManagerStatus newStatus) {
    if (isDebug) {
      flash(Level.FINE, newStatus.name(), MY_SLOT);
    }
  }

  @CliCommand(value = PROCESS_MANAGER_DEBUG_COMMAND,
      help = "Indicates if process manager debugging is desired")
  public void processManagerDebug(@CliOption(key = {"", "enabled"}, mandatory = false,
      specifiedDefaultValue = "true", unspecifiedDefaultValue = "true",
      help = "Activates debug mode") final boolean debug) {
    isDebug = debug;
  }

  public ProcessManager getProcessManager() {
    // Get all components implement ProcessManager interface
    try {
      ServiceReference<?>[] references =
          this.context.getAllServiceReferences(ProcessManager.class.getName(), null);

      for (ServiceReference<?> ref : references) {
        return (ProcessManager) this.context.getService(ref);
      }

      return null;

    } catch (InvalidSyntaxException e) {
      LOGGER.warning("Cannot load ProcessManager on ProcessManagerDiagnosticListener.");
      return null;
    }
  }

  public Shell getShell() {
    // Get all Shell implement Shell interface
    try {
      ServiceReference<?>[] references =
          this.context.getAllServiceReferences(Shell.class.getName(), null);

      for (ServiceReference<?> ref : references) {
        return (Shell) this.context.getService(ref);
      }

      return null;

    } catch (InvalidSyntaxException e) {
      LOGGER.warning("Cannot load Shell on ProcessManagerDiagnosticListener.");
      return null;
    }
  }
}
