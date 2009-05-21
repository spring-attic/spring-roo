package org.springframework.roo.addon.jms;

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
 * Commands for the 'install jms' add-on to be used by the ROO shell.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
@ScopeDevelopmentShell
public class JmsCommands implements CommandMarker {
	
	private JmsOperations jmsOperations;
	
	public JmsCommands(StaticFieldConverter staticFieldConverter, JmsOperations jmsOperations) {
		Assert.notNull(staticFieldConverter, "Static field converter required");
		Assert.notNull(jmsOperations, "Jms operations required");
		staticFieldConverter.add(JmsProvider.class);
		this.jmsOperations = jmsOperations;
	}
	
	/**
	 * @return true if the "install JMS" command is available at this moment
	 */
	@CliAvailabilityIndicator("install jms")
	public boolean isInstallJmsAvailable() {
		return jmsOperations.isInstallJmsAvailable();
	}
	
	@CliCommand(value="install jms", help="Install a JMS provider in your project")
	public void installJms(
			@CliOption(key={"provider"}, mandatory=true, help="The persistence provider to support") JmsProvider jmsProvider,
			@CliOption(key={"queueName"}, mandatory=false, help="The name of the queue") String queueName,
			@CliOption(key={"topicName"}, mandatory=false, help="The name of the topic") String topicName) {
		jmsOperations.installJms(jmsProvider, queueName, topicName);
	}
	
	/**
	 * @return true if the "add field jms template, new java jms listener" command is available at this moment
	 */
	@CliAvailabilityIndicator({"add field jms template","new java jms listener"})
	public boolean isInsertJmsAvailable() {
		return jmsOperations.isManageJmsAvailable();
	}
	
	@CliCommand(value="add field jms template", help="insert a JmsTemplate field into an existing type")
	public void injectJmsProducer(
			@CliOption(key={"","fieldName"}, mandatory=false, specifiedDefaultValue="jmsTemplate", unspecifiedDefaultValue="jmsTemplate", help="The name of the field to add") JavaSymbolName fieldName,
			@CliOption(key="class", mandatory=false, unspecifiedDefaultValue="*", optionContext="update,project", help="The name of the class to receive this field") JavaType typeName) {
		jmsOperations.injectJmsTemplate(typeName, fieldName);
	}
	
	@CliCommand(value="new java jms listener", help="Create a new type which is a asynchronous JMS consumer")
	public void addJmsListener(
			@CliOption(key="class", mandatory=true, help="The name of the class to create") JavaType typeName,
			@CliOption(key={"queueOrTopicName"}, mandatory=true, help="The name of the queue or topic to listen to") String queueOrTopicName) {
		jmsOperations.addJmsListener(typeName, queueOrTopicName);
	}
}