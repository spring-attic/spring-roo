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
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.ExitShellRequest;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.shell.converters.StaticFieldConverter;
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
 */
@Component
@Service
public class FelixDelegator implements CommandMarker, ShellStatusListener {
    private ComponentContext context;
    @Reference private Shell rooShell;
    @Reference private CommandProcessor commandProcessor;
    @Reference private StaticFieldConverter staticFieldConverter;

    protected static final Logger LOGGER = HandlerUtils
            .getLogger(LoggingOutputStream.class);

    protected void activate(final ComponentContext context) {
        this.context = context;
        rooShell.addShellStatusListener(this);
        staticFieldConverter.add(LogLevel.class);
        staticFieldConverter.add(PsOptions.class);
    }

    protected void deactivate(final ComponentContext context) {
        this.context = null;
        rooShell.removeShellStatusListener(this);
        staticFieldConverter.remove(LogLevel.class);
        staticFieldConverter.remove(PsOptions.class);
    }

    @CliCommand(value = "osgi find", help = "Finds bundles by name")
    public void find(
            @CliOption(key = "bundleSymbolicName", mandatory = true, help = "A bundle symbolic name to find") final BundleSymbolicName bsn)
            throws Exception {

        perform("find " + bsn.getKey());
    }

    @CliCommand(value = "osgi headers", help = "Display headers for a specific bundle")
    public void headers(
            @CliOption(key = "bundleSymbolicName", mandatory = false, help = "Limit results to a specific bundle symbolic name") final BundleSymbolicName bsn)
            throws Exception {

        if (bsn == null) {
            perform("headers");
        }
        else {
            perform("headers "
                    + bsn.findBundleIdWithoutFail(context.getBundleContext()));
        }
    }

    @CliCommand(value = "osgi install", help = "Installs a bundle JAR from a given URL")
    public void install(
            @CliOption(key = "url", mandatory = true, help = "The URL to obtain the bundle from") final String url)
            throws Exception {

        perform("install " + url);
    }

    @CliCommand(value = "osgi log", help = "Displays the OSGi log information")
    public void log(
            @CliOption(key = "maximumEntries", mandatory = false, help = "The maximum number of log messages to display") final Integer maximumEntries,
            @CliOption(key = "level", mandatory = false, help = "The minimum level of messages to display") final LogLevel logLevel)
            throws Exception {

        final StringBuilder sb = new StringBuilder();
        sb.append("log");
        if (maximumEntries != null) {
            sb.append(" ").append(maximumEntries);
        }
        if (logLevel != null) {
            sb.append(" ").append(logLevel.getFelixCode());
        }
        perform(sb.toString());
    }

    @CliCommand(value = "osgi ps", help = "Displays OSGi bundle information")
    public void log(
            @CliOption(key = "format", mandatory = false, specifiedDefaultValue = "BUNDLE_NAME", unspecifiedDefaultValue = "BUNDLE_NAME", help = "The format of bundle information") final PsOptions format)
            throws Exception {

        final StringBuilder sb = new StringBuilder();
        sb.append("ps");
        if (format != null) {
            sb.append(format.getFelixCode());
        }
        perform(sb.toString());
    }

    @CliCommand(value = "osgi obr deploy", help = "Deploys a specific OSGi Bundle Repository (OBR) bundle")
    public void obrDeploy(
            @CliOption(key = "bundleSymbolicName", mandatory = true, optionContext = "obr", help = "The specific bundle to deploy") final BundleSymbolicName bsn)
            throws Exception {

        perform("obr deploy " + bsn.getKey());
    }

    @CliCommand(value = "osgi obr info", help = "Displays information on a specific OSGi Bundle Repository (OBR) bundle")
    public void obrInfo(
            @CliOption(key = "bundleSymbolicName", mandatory = true, optionContext = "obr", help = "The specific bundle to display information for") final BundleSymbolicName bsn)
            throws Exception {

        perform("obr info " + bsn.getKey());
    }

    @CliCommand(value = "osgi obr list", help = "Lists all available bundles from the OSGi Bundle Repository (OBR) system")
    public void obrList(
            @CliOption(key = "keywords", mandatory = false, help = "Keywords to locate") final String keywords)
            throws Exception {

        final StringBuilder sb = new StringBuilder();
        sb.append("obr list -v");
        if (keywords != null) {
            sb.append(" ").append(keywords);
        }
        perform(sb.toString());
    }

    @CliCommand(value = "osgi obr start", help = "Starts a specific OSGi Bundle Repository (OBR) bundle")
    public void obrStart(
            @CliOption(key = "bundleSymbolicName", mandatory = true, optionContext = "obr", help = "The specific bundle to start") final BundleSymbolicName bsn)
            throws Exception {

        perform("obr start " + bsn.getKey());
    }

    @CliCommand(value = "osgi obr url add", help = "Adds a new OSGi Bundle Repository (OBR) repository file URL")
    public void obrUrlAdd(
            @CliOption(key = "url", mandatory = true, help = "The URL to add (eg http://felix.apache.org/obr/releases.xml)") final String url)
            throws Exception {

        perform("obr add-url " + url);
    }

    @CliCommand(value = "osgi obr url list", help = "Lists the currently-configured OSGi Bundle Repository (OBR) repository file URLs")
    public void obrUrlList() throws Exception {
        perform("obr list-url");
    }

    @CliCommand(value = "osgi obr url refresh", help = "Refreshes an existing OSGi Bundle Repository (OBR) repository file URL")
    public void obrUrlRefresh(
            @CliOption(key = "url", mandatory = true, help = "The URL to refresh (list existing URLs via 'osgi obr url list')") final String url)
            throws Exception {

        perform("obr refresh-url " + url);
    }

    @CliCommand(value = "osgi obr url remove", help = "Removes an existing OSGi Bundle Repository (OBR) repository file URL")
    public void obrUrlRemove(
            @CliOption(key = "url", mandatory = true, help = "The URL to remove (list existing URLs via 'osgi obr url list')") final String url)
            throws Exception {

        perform("obr remove-url " + url);
    }

    public void onShellStatusChange(final ShellStatus oldStatus,
            final ShellStatus newStatus) {
        if (newStatus.getStatus().equals(Status.SHUTTING_DOWN)) {
            try {
                if (rooShell != null) {
                    if (rooShell.getExitShellRequest() != null) {
                        // ROO-836
                        System.setProperty("roo.exit", Integer
                                .toString(rooShell.getExitShellRequest()
                                        .getExitCode()));
                    }
                    System.setProperty("developmentMode",
                            Boolean.toString(rooShell.isDevelopmentMode()));
                }
                perform("shutdown");
            }
            catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private void perform(final String commandLine) throws Exception {
        if("shutdown".equals(commandLine)) {
            context.getBundleContext().getBundle(0).stop();
            return;
        }

        ByteArrayOutputStream sysOut = new ByteArrayOutputStream();
        ByteArrayOutputStream sysErr = new ByteArrayOutputStream();

        final PrintStream printStreamOut = new PrintStream(sysOut);
        final PrintStream printErrOut = new PrintStream(sysErr);
        try {
            final CommandSession commandSession = commandProcessor.createSession(System.in, printStreamOut, printErrOut);
            Object result = commandSession.execute(commandLine);

            if(result != null) {
                printStreamOut.println(commandSession.format(result, Converter.INSPECT));
            }

            if(sysOut.size() > 0) {
                LOGGER.log(Level.INFO, new String(sysOut.toByteArray()));
            }

            if(sysErr.size() > 0) {
                LOGGER.log(Level.SEVERE, new String(sysErr.toByteArray()));
            }
        }
        catch(Throwable ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        finally {
            printStreamOut.close();
            printErrOut.close();
        }
    }

    @CliCommand(value = { "exit", "quit" }, help = "Exits the shell")
    public ExitShellRequest quit() {
        return ExitShellRequest.NORMAL_EXIT;
    }

    @CliCommand(value = "osgi resolve", help = "Resolves a specific bundle ID")
    public void resolve(
            @CliOption(key = "bundleSymbolicName", mandatory = true, help = "The specific bundle to resolve") final BundleSymbolicName bsn)
            throws Exception {

        perform("resolve "
                + bsn.findBundleIdWithoutFail(context.getBundleContext()));
    }

    @CliCommand(value = "osgi scr config", help = "Lists the current SCR configuration")
    public void scrConfig() throws Exception {
        perform("scr config");
    }

    @CliCommand(value = "osgi scr disable", help = "Disables a specific SCR-defined component")
    public void scrDisable(
            @CliOption(key = "componentId", mandatory = true, help = "The specific component identifier (use 'osgi scr list' to list component identifiers)") final Integer id)
            throws Exception {

        perform("scr disable " + id);
    }

    @CliCommand(value = "osgi scr enable", help = "Enables a specific SCR-defined component")
    public void scrEnable(
            @CliOption(key = "componentId", mandatory = true, help = "The specific component identifier (use 'osgi scr list' to list component identifiers)") final Integer id)
            throws Exception {

        perform("scr enable " + id);
    }

    @CliCommand(value = "osgi scr info", help = "Lists information about a specific SCR-defined component")
    public void scrInfo(
            @CliOption(key = "componentId", mandatory = true, help = "The specific component identifier (use 'osgi scr list' to list component identifiers)") final Integer id)
            throws Exception {

        perform("scr info " + id);
    }

    @CliCommand(value = "osgi scr list", help = "Lists all SCR-defined components")
    public void scrList(
            @CliOption(key = "bundleId", mandatory = false, help = "Limit results to a specific bundle") final BundleSymbolicName bsn)
            throws Exception {

        if (bsn == null) {
            perform("scr list");
        }
        else {
            perform("scr list "
                    + bsn.findBundleIdWithoutFail(context.getBundleContext()));
        }
    }

    @CliCommand(value = "osgi framework command", help = "Passes a command directly through to the Felix shell infrastructure")
    public void shell(
            @CliOption(key = "", mandatory = false, specifiedDefaultValue = "help", unspecifiedDefaultValue = "help", help = "The command to pass to Felix (WARNING: no validation or security checks are performed)") final String commandLine)
            throws Exception {

        perform(commandLine);
    }

    @CliCommand(value = "osgi start", help = "Starts a bundle JAR from a given URL")
    public void start(
            @CliOption(key = "url", mandatory = true, help = "The URL to obtain the bundle from") final String url)
            throws Exception {

        perform("start " + url);
    }

    @CliCommand(value = "osgi uninstall", help = "Uninstalls a specific bundle")
    public void uninstall(
            @CliOption(key = "bundleSymbolicName", mandatory = true, help = "The specific bundle to uninstall") final BundleSymbolicName bsn)
            throws Exception {

        perform("uninstall "
                + bsn.findBundleIdWithoutFail(context.getBundleContext()));
    }

    @CliCommand(value = "osgi update", help = "Updates a specific bundle")
    public void update(
            @CliOption(key = "bundleSymbolicName", mandatory = true, help = "The specific bundle to update ") final BundleSymbolicName bsn,
            @CliOption(key = "url", mandatory = false, help = "The URL to obtain the updated bundle from") final String url)
            throws Exception {

        final Long id = bsn.findBundleIdWithoutFail(context.getBundleContext());
        if (url == null) {
            perform("update " + id);
        }
        else {
            perform("update " + id + " " + url);
        }
    }

    @CliCommand(value = "osgi version", help = "Displays OSGi framework version")
    public void version() throws Exception {
        perform("version");
    }
}
