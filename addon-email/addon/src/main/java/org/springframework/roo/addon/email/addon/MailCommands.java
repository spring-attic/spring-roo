package org.springframework.roo.addon.email.addon;

import static org.springframework.roo.shell.OptionContexts.APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CliOptionAutocompleteIndicator;
import org.springframework.roo.shell.CliOptionMandatoryIndicator;
import org.springframework.roo.shell.CliOptionVisibilityIndicator;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.ShellContext;
import org.springframework.roo.support.logging.HandlerUtils;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Commands for the 'email' add-on to be used by the Roo shell.
 *
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @author Manuel Iborra
 * @since 1.0
 */
@Component
@Service
public class MailCommands implements CommandMarker {

  private static final Logger LOGGER = HandlerUtils.getLogger(MailCommands.class);


  @Reference
  private MailOperations mailOperations;
  @Reference
  private TypeLocationService typeLocationService;
  @Reference
  private ProjectOperations projectOperations;

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  protected void deactivate(final ComponentContext context) {
    this.context = null;
  }

  @CliAvailabilityIndicator("email sender setup")
  public boolean isInstallSenderEmailAvailable() {
    return mailOperations.isEmailInstallationPossible();
  }

  @CliOptionVisibilityIndicator(command = "email sender setup", params = {"jndiName"},
      help = "jndiName parameter is not available if any of host, "
          + "port, protocol, username, password or starttls are selected.")
  public boolean isJndiVisibleSenderSetup(ShellContext shellContext) {

    Map<String, String> params = shellContext.getParameters();

    // If user define host, port, protocol, username, password or starttls
    // parameters, jndiName should not be visible.
    if (params.containsKey("host") || params.containsKey("port") || params.containsKey("protocol")
        || params.containsKey("username") || params.containsKey("password")
        || params.containsKey("starttls")) {
      return false;
    }

    return true;
  }

  @CliOptionVisibilityIndicator(command = "email sender setup", params = {"host", "port",
      "protocol", "username", "password", "starttls"},
      help = "Connection parameters are not available if jndiName is specified.")
  public boolean areConnectionParamsVisibleSenderSetup(ShellContext shellContext) {

    Map<String, String> params = shellContext.getParameters();

    // If user define jndiName parameter, connection parameters should not
    // be visible
    if (params.containsKey("jndiName")) {
      return false;
    }

    return true;
  }

  @CliOptionVisibilityIndicator(command = "email sender setup", params = {"module"},
      help = "Module parameter is not available if there is only one application module")
  public boolean isModuleVisibleSenderSetup(ShellContext shellContext) {
    if (typeLocationService.getModuleNames(ModuleFeatureName.APPLICATION).size() > 1) {
      return true;
    }
    return false;
  }

  @CliOptionMandatoryIndicator(command = "email sender setup", params = "module")
  public boolean isModuleRequiredSenderSetup(ShellContext shellContext) {
    Pom module = projectOperations.getFocusedModule();
    if (!isModuleVisibleSenderSetup(shellContext)
        || typeLocationService.hasModuleFeature(module, ModuleFeatureName.APPLICATION)) {
      return false;
    }
    return true;
  }

  @CliOptionAutocompleteIndicator(
      command = "email sender setup",
      param = "service",
      help = "--service parameter parameter is the service where will be added the support to send emails")
  public List<String> returnServicesImpl(ShellContext shellContext) {
    return mailOperations.getAllServiceImpl(shellContext.getParameters().get("service"));
  }

  @CliCommand(value = "email sender setup",
      help = "Install a Spring JavaMailSender in your project")
  public void installSenderEmail(
      @CliOption(key = {"host"}, mandatory = false, help = "The host server. "
          + "This option is not available if `--jndiName` has already been specified.") final String host,
      @CliOption(key = {"port"}, mandatory = false, help = "The port used by mail server. "
          + "This option is not available if `--jndiName` has already been specified.") final String port,
      @CliOption(key = {"protocol"}, mandatory = false, help = "The protocol used by mail server. "
          + "This option is not available if `--jndiName` has already been specified.") final String protocol,
      @CliOption(key = {"username"}, mandatory = false, help = "The mail account username. "
          + "This option is not available if `--jndiName` has already been specified.") final String username,
      @CliOption(key = {"password"}, mandatory = false, help = "The mail account password. "
          + "This option is not available if `--jndiName` has already been specified.") final String password,
      @CliOption(key = {"starttls"}, mandatory = false,
          help = "If true, enables the use of the STARTTLS command. "
              + "This option is not available if `--jndiName` has already been specified.") final Boolean starttls,
      @CliOption(key = {"jndiName"}, mandatory = false,
          help = "The jndi name where the mail configuration has been defined. "
              + "This option is not available if any of `--host`, `--port`, "
              + "`--protocol`, `--username`, `--password` or `--starttls` has "
              + "been specified before.") final String jndiName,
      @CliOption(key = {"profile"}, mandatory = false,
          help = "The profile where the properties will be set.") final String profile,
      @CliOption(
          key = "module",
          mandatory = true,
          help = "The application module where generate the integration test. "
              + "This option is mandatory if the focus is not set in an 'application' module and there "
              + "are more than one 'application' modules, that is, a module containing an "
              + "`@SpringBootApplication` class. "
              + "This option is available only if there are more than one application module and none of"
              + " them is focused. "
              + "Default if option not present: the unique 'application' module, or focused 'application'"
              + " module.", unspecifiedDefaultValue = ".",
          optionContext = APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE) Pom module,
      @CliOption(
          key = "service",
          mandatory = false,
          help = "The service where include an instance of JavaMailSender, which is a service that "
              + "have methods to send emails.") final JavaType service, ShellContext shellContext) {

    mailOperations.installSendEmailSupport(host, port, protocol, username, password, starttls,
        jndiName, profile, module, service, shellContext.isForce());
  }


  @CliAvailabilityIndicator("email receiver setup")
  public boolean isInstallReceiverEmailAvailable() {
    return mailOperations.isEmailInstallationPossible();
  }

  @CliOptionVisibilityIndicator(command = "email receiver setup", params = {"jndiName"},
      help = "jndiName parameter is not available if any of host, "
          + "port, protocol, username, password or starttls are selected.")
  public boolean isJndiVisibleReceiverSetup(ShellContext shellContext) {

    Map<String, String> params = shellContext.getParameters();

    // If user define host, port, protocol, username, password or starttls
    // parameters, jndiName should not be visible.
    if (params.containsKey("host") || params.containsKey("port") || params.containsKey("protocol")
        || params.containsKey("username") || params.containsKey("password")
        || params.containsKey("starttls")) {
      return false;
    }

    return true;
  }

  @CliOptionVisibilityIndicator(command = "email receiver setup", params = {"host", "port",
      "protocol", "username", "password", "starttls"},
      help = "Connection parameters are not available if jndiName is specified.")
  public boolean areConnectionParamsVisibleReceiverSetup(ShellContext shellContext) {

    Map<String, String> params = shellContext.getParameters();

    // If user define jndiName parameter, connection parameters should not
    // be visible
    if (params.containsKey("jndiName")) {
      return false;
    }

    return true;
  }

  @CliOptionVisibilityIndicator(command = "email receiver setup", params = {"module"},
      help = "Module parameter is not available if there is only one application module")
  public boolean isModuleVisibleReceiverSetup(ShellContext shellContext) {
    if (typeLocationService.getModuleNames(ModuleFeatureName.APPLICATION).size() > 1) {
      return true;
    }
    return false;
  }

  @CliOptionMandatoryIndicator(command = "email receiver setup", params = "module")
  public boolean isModuleRequiredReceiverSetup(ShellContext shellContext) {
    Pom module = projectOperations.getFocusedModule();
    if (!isModuleVisibleReceiverSetup(shellContext)
        || typeLocationService.hasModuleFeature(module, ModuleFeatureName.APPLICATION)) {
      return false;
    }
    return true;
  }

  @CliOptionAutocompleteIndicator(
      command = "email receiver setup",
      param = "service",
      help = "--service parameter parameter is the service where will be added the support to receive emails")
  public List<String> returnServicesImplReceiverSetup(ShellContext shellContext) {
    return mailOperations.getAllServiceImpl(shellContext.getParameters().get("service"));
  }


  @CliCommand(value = "email receiver setup",
      help = "Installs a Spring JavaMailReceiver in your project.")
  public void installReceiverEmail(
      @CliOption(key = {"host"}, mandatory = false, help = "The host server. "
          + "This option is not available if `--jndiName` has already been specified.") final String host,
      @CliOption(key = {"port"}, mandatory = false, help = "The port used by mail server. "
          + "This option is not available if `--jndiName` has already been specified.") final String port,
      @CliOption(key = {"protocol"}, mandatory = false, help = "The protocol used by mail server. "
          + "This option is not available if `--jndiName` has already been specified.") final String protocol,
      @CliOption(key = {"username"}, mandatory = false, help = "The mail account username. "
          + "This option is not available if `--jndiName` has already been specified.") final String username,
      @CliOption(key = {"password"}, mandatory = false, help = "The mail account password. "
          + "This option is not available if `--jndiName` has already been specified.") final String password,
      @CliOption(key = {"starttls"}, mandatory = false,
          help = "If true, enables the use of the STARTTLS command. "
              + "This option is not available if `--jndiName` has already been specified.") final Boolean starttls,
      @CliOption(key = {"jndiName"}, mandatory = false,
          help = "The jndi name where the mail configuration has been defined. "
              + "This option is not available if any of `--host`, `--port`, "
              + "`--protocol`, `--username`, `--password` or `--starttls` has "
              + "been specified before.") final String jndiName,
      @CliOption(key = {"profile"}, mandatory = false,
          help = "The profile where the properties will be set.") final String profile,
      @CliOption(
          key = "module",
          mandatory = true,
          help = "The application module where generate the integration test. "
              + "This option is mandatory if the focus is not set in an 'application' module and there "
              + "are more than one 'application' modules, that is, a module containing an "
              + "`@SpringBootApplication` class. "
              + "This option is available only if there are more than one application module and none of"
              + " them is focused. "
              + "Default if option not present: the unique 'application' module, or focused 'application'"
              + " module.", unspecifiedDefaultValue = ".",
          optionContext = APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE) Pom module, @CliOption(
          key = "service", mandatory = false,
          help = "The service where include an instance of MailReceiverService, which is a "
              + "service that have methods to receive emails.") final JavaType service,
      ShellContext shellContext) {

    mailOperations.installReceiveEmailSupport(host, port, protocol, username, password, starttls,
        jndiName, profile, module, service, shellContext.isForce());
  }



}
