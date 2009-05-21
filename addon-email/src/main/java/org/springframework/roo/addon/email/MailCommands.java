package org.springframework.roo.addon.email;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.converters.StaticFieldConverter;
import org.springframework.roo.support.lifecycle.ScopeDevelopmentShell;
import org.springframework.roo.support.util.Assert;

/**
 * Commands for the 'install email' add-on to be used by the ROO shell.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
@ScopeDevelopmentShell
public class MailCommands implements CommandMarker {
	
	private MailOperations mailOperations;
	
	public MailCommands(StaticFieldConverter staticFieldConverter, MailOperations mailOperations) {
		Assert.notNull(staticFieldConverter, "Static field converter required");
		Assert.notNull(mailOperations, "Jms operations required");
		staticFieldConverter.add(MailProtocol.class);
		this.mailOperations = mailOperations;
	}
	
	/**
	 * @return true if the "install email" command is available at this moment
	 */
	@CliAvailabilityIndicator("install email provider")
	public boolean isInstallEmailAvailable() {
		return mailOperations.isInstallEmailAvailable();
	}
	
	@CliCommand(value="install email provider", help="Install a Email provider in your project")
	public void installEmail(
			@CliOption(key={"hostServer"}, mandatory=true, help="The host server") String hostServer,
			@CliOption(key={"protocol"}, mandatory=false, help="The protocol used by mail server") MailProtocol protocol,
			@CliOption(key={"port"}, mandatory=false, help="The port used by mail server") String port,
			@CliOption(key={"encoding"}, mandatory=false, help="The encoding used for mail") String encoding,
			@CliOption(key={"username"}, mandatory=false, help="The mail account username") String username,
			@CliOption(key={"password"}, mandatory=false, help="The mail account password") String password) {
		mailOperations.installEmail(hostServer, protocol, port, encoding, username, password);
	}
	
	/**
	 * @return true if the "add field mail template" command is available at this moment
	 */
	@CliAvailabilityIndicator({"add field email template","configure email template"})
	public boolean isInsertJmsAvailable() {
		return mailOperations.isManageEmailAvailable();
	}
	
	@CliCommand(value="add field email template", help="insert a MailTemplate field into an existing type")
	public void injectEmailTemplate(
			@CliOption(key={"","fieldName"}, mandatory=false, specifiedDefaultValue="mailTemplate", unspecifiedDefaultValue="mailTemplate", help="The name of the field to add") JavaSymbolName fieldName,
			@CliOption(key="class", mandatory=false, unspecifiedDefaultValue="*", optionContext="update,project", help="The name of the class to receive this field") JavaType typeName) {
		mailOperations.injectEmailTemplate(typeName, fieldName);
	}
	
	@CliCommand(value="configure email template", help="configure a template for a SimpleMailMessage")
	public void configureEmailTemplate(
			@CliOption(key={"from"}, mandatory=false, help="The 'from' email (optional)") String from,
			@CliOption(key={"subject"}, mandatory=false, help="The message subject (obtional)") String subject) {
		mailOperations.configureTemplateMessage(from, subject);
	}
}