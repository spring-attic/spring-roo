package org.springframework.roo.addon.jms;

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
 * Commands for the 'install jms' add-on to be used by the ROO shell.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component
@Service
public class JmsCommands implements CommandMarker {

    @Reference private JmsOperations jmsOperations;
    @Reference private StaticFieldConverter staticFieldConverter;

    protected void activate(final ComponentContext context) {
        staticFieldConverter.add(JmsProvider.class);
        staticFieldConverter.add(JmsDestinationType.class);
    }

    @CliCommand(value = "jms listener class", help = "Create an asynchronous JMS consumer")
    public void addJmsListener(
            @CliOption(key = "class", mandatory = true, help = "The name of the class to create") final JavaType typeName,
            @CliOption(key = { "destinationName" }, mandatory = false, unspecifiedDefaultValue = "myDestination", specifiedDefaultValue = "myDestination", help = "The name of the destination") final String name,
            @CliOption(key = { "destinationType" }, mandatory = false, unspecifiedDefaultValue = "QUEUE", specifiedDefaultValue = "QUEUE", help = "The type of the destination") final JmsDestinationType type) {

        jmsOperations.addJmsListener(typeName, name, type);
    }

    protected void deactivate(final ComponentContext context) {
        staticFieldConverter.remove(JmsProvider.class);
        staticFieldConverter.remove(JmsDestinationType.class);
    }

    @CliCommand(value = "field jms template", help = "Insert a JmsOperations field into an existing type")
    public void injectJmsProducer(
            @CliOption(key = { "", "fieldName" }, mandatory = false, specifiedDefaultValue = "jmsOperations", unspecifiedDefaultValue = "jmsOperations", help = "The name of the field to add") final JavaSymbolName fieldName,
            @CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The name of the class to receive this field") final JavaType typeName,
            @CliOption(key = "async", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates if the injected method should be executed asynchronously") final boolean async) {

        jmsOperations.injectJmsTemplate(typeName, fieldName, async);
    }

    @CliCommand(value = "jms setup", help = "Install a JMS provider into your project")
    public void installJms(
            @CliOption(key = { "provider" }, mandatory = true, help = "The persistence provider to support") final JmsProvider jmsProvider,
            @CliOption(key = { "destinationName" }, mandatory = false, unspecifiedDefaultValue = "myDestination", specifiedDefaultValue = "myDestination", help = "The name of the destination") final String name,
            @CliOption(key = { "destinationType" }, mandatory = false, unspecifiedDefaultValue = "QUEUE", specifiedDefaultValue = "QUEUE", help = "The type of the destination") final JmsDestinationType type) {

        jmsOperations.installJms(jmsProvider, name, type);
    }

    @CliAvailabilityIndicator({ "field jms template", "jms listener class" })
    public boolean isInsertJmsAvailable() {
        return jmsOperations.isManageJmsAvailable();
    }

    @CliAvailabilityIndicator("jms setup")
    public boolean isInstallJmsAvailable() {
        return jmsOperations.isJmsInstallationPossible();
    }
}