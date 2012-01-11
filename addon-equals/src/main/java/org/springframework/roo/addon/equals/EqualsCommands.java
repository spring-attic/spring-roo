package org.springframework.roo.addon.equals;

import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Commands for the Equals add-on to be used by the ROO shell.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
@Component
@Service
public class EqualsCommands implements CommandMarker {

    @Reference private EqualsOperations equalsOperations;

    @CliCommand(value = "equals", help = "Add equals and hashCode methods to a class")
    public void addEquals(
            @CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The name of the class") final JavaType javaType,
            @CliOption(key = "appendSuper", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Whether to call the super class equals and hashCode methods") final boolean appendSuper,
            @CliOption(key = "excludeFields", mandatory = false, specifiedDefaultValue = "", optionContext = "exclude-fields", help = "The fields to exclude in the equals and hashcode methods. Multiple field names must be a double-quoted list separated by spaces") final Set<String> excludeFields) {

        equalsOperations.addEqualsAndHashCodeMethods(javaType, appendSuper,
                excludeFields);
    }
}