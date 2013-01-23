package org.springframework.roo.addon.email;

import static org.springframework.roo.shell.OptionContexts.UPDATE_PROJECT;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.converters.StaticFieldConverter;

/**
 * Commands for the 'email' add-on to be used by the Roo shell.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component
@Service
public class MailCommands implements CommandMarker {

    @Reference private MailOperations mailOperations;
    @Reference private StaticFieldConverter staticFieldConverter;

    protected void activate(final ComponentContext context) {
        staticFieldConverter.add(MailProtocol.class);
    }

    @CliCommand(value = "email template setup", help = "Configures a template for a SimpleMailMessage")
    public void configureEmailTemplate(
            @CliOption(key = { "from" }, mandatory = false, help = "The 'from' email (optional)") final String from,
            @CliOption(key = { "subject" }, mandatory = false, help = "The message subject (obtional)") final String subject) {

        mailOperations.configureTemplateMessage(from, subject);
    }

    protected void deactivate(final ComponentContext context) {
        staticFieldConverter.remove(MailProtocol.class);
    }

    @CliCommand(value = "field email template", help = "Inserts a MailTemplate field into an existing type")
    public void injectEmailTemplate(
            @CliOption(key = { "", "fieldName" }, mandatory = false, specifiedDefaultValue = "mailTemplate", unspecifiedDefaultValue = "mailTemplate", help = "The name of the field to add") final JavaSymbolName fieldName,
            @CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = UPDATE_PROJECT, help = "The name of the class to receive this field") final JavaType typeName,
            @CliOption(key = "async", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates if the injected method should be executed asynchronously") final boolean async) {

        mailOperations.injectEmailTemplate(typeName, fieldName, async);
    }

    @CliCommand(value = "email sender setup", help = "Install a Spring JavaMailSender in your project")
    public void installEmail(
            @CliOption(key = { "hostServer" }, mandatory = true, help = "The host server") final String hostServer,
            @CliOption(key = { "protocol" }, mandatory = false, help = "The protocol used by mail server") final MailProtocol protocol,
            @CliOption(key = { "port" }, mandatory = false, help = "The port used by mail server") final String port,
            @CliOption(key = { "encoding" }, mandatory = false, help = "The encoding used for mail") final String encoding,
            @CliOption(key = { "username" }, mandatory = false, help = "The mail account username") final String username,
            @CliOption(key = { "password" }, mandatory = false, help = "The mail account password") final String password) {

        mailOperations.installEmail(hostServer, protocol, port, encoding,
                username, password);
    }

    /**
     * Indicates whether the mail template commands are available
     * 
     * @return see above
     * @deprecated call {@link #isMailTemplateAvailable()} instead
     */
    @Deprecated
    public boolean isInsertJmsAvailable() {
        return isMailTemplateAvailable();
    }

    @CliAvailabilityIndicator("email sender setup")
    public boolean isInstallEmailAvailable() {
        return mailOperations.isEmailInstallationPossible();
    }

    /**
     * Indicates whether the mail template commands are available
     * 
     * @return see above
     * @since 1.2.0
     */
    @CliAvailabilityIndicator({ "field email template", "email template setup" })
    public boolean isMailTemplateAvailable() {
        return mailOperations.isManageEmailAvailable();
    }
}