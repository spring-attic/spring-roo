package org.springframework.roo.felix;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.ExitShellRequest;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.shell.event.ShellStatus;
import org.springframework.roo.shell.event.ShellStatus.Status;
import org.springframework.roo.shell.event.ShellStatusListener;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.logging.LoggingOutputStream;

/**
 * Delegates to commands provided via Felix's Shell API.
 * <p>
 * Also monitors the Roo Shell to determine when it wishes to shutdown. This
 * shutdown request is then passed through to Felix for processing.
 * 
 * @author Ben Alex
 * @author Juan Carlos García
 */
@Component
@Service
public class FelixDelegator implements CommandMarker, ShellStatusListener {

  private BundleContext context;

  @Reference
  private Shell rooShell;
  @Reference
  private CommandProcessor commandProcessor;

  protected static final Logger LOGGER = HandlerUtils.getLogger(LoggingOutputStream.class);

  protected void activate(final ComponentContext cContext) {
    context = cContext.getBundleContext();
    rooShell.addShellStatusListener(this);
  }

  protected void deactivate(final ComponentContext context) {
    this.context = null;
    rooShell.removeShellStatusListener(this);
  }

  @CliCommand(value = "!g",
      help = "Passes a command directly through to the Felix shell infrastructure")
  public void shell(
      @CliOption(
          key = "",
          mandatory = false,
          specifiedDefaultValue = "help",
          unspecifiedDefaultValue = "help",
          help = "The command to pass to Felix (WARNING: no validation or security checks are performed)") final String commandLine)
      throws Exception {

    perform(commandLine);
  }

  @CliCommand(value = {"exit", "quit"}, help = "Exits the shell")
  public ExitShellRequest quit() {
    return ExitShellRequest.NORMAL_EXIT;
  }

  public void onShellStatusChange(final ShellStatus oldStatus, final ShellStatus newStatus) {
    if (newStatus.getStatus().equals(Status.SHUTTING_DOWN)) {
      try {
        if (rooShell != null) {
          if (rooShell.getExitShellRequest() != null) {
            // ROO-836
            System.setProperty("roo.exit",
                Integer.toString(rooShell.getExitShellRequest().getExitCode()));
          }
          System.setProperty("developmentMode", Boolean.toString(rooShell.isDevelopmentMode()));
        }
        perform("shutdown");
      } catch (final Exception e) {
        throw new IllegalStateException(e);
      }
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
          commandProcessor.createSession(System.in, printStreamOut, printErrOut);
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
}
