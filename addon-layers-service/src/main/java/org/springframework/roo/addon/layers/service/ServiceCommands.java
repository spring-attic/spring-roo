package org.springframework.roo.addon.layers.service;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.converters.JavaTypeConverter;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Shell commands that create domain services.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Component
@Service
public class ServiceCommands implements CommandMarker {

    @Reference private ServiceOperations serviceOperations;

    @CliAvailabilityIndicator("service")
    public boolean isServiceCommandAvailable() {
        return serviceOperations.isServiceInstallationPossible();
    }

    @CliCommand(value = "service", help = "Adds @RooService annotation to target type")
    public void service(
            @CliOption(key = "interface", mandatory = true, help = "The java interface to apply this annotation to") final JavaType interfaceType,
            @CliOption(key = "class", mandatory = false, help = "Implementation class for the specified interface") JavaType classType,
            @CliOption(key = "entity", unspecifiedDefaultValue = "*", optionContext = JavaTypeConverter.PROJECT, mandatory = false, help = "The domain entity this service should expose") final JavaType domainType) {

        if (classType == null) {
            classType = new JavaType(interfaceType.getFullyQualifiedTypeName()
                    + "Impl");
        }
        serviceOperations.setupService(interfaceType, classType, domainType);
    }
}