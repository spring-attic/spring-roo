package org.springframework.roo.classpath.operations;

import static org.springframework.roo.shell.OptionContexts.INTERFACE;
import static org.springframework.roo.shell.OptionContexts.SUPERCLASS;
import static org.springframework.roo.shell.OptionContexts.UPDATE_PROJECT;

import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Shell commands for creating classes, interfaces, and enums.
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.0
 */
@Component
@Service
public class ClasspathCommands implements CommandMarker {

    @Reference private ClasspathOperations classpathOperations;

    @CliCommand(value = "class", help = "Creates a new Java class source file in any project path")
    public void createClass(
            @CliOption(key = "class", optionContext = UPDATE_PROJECT, mandatory = true, help = "The name of the class to create") final JavaType name,
            @CliOption(key = "rooAnnotations", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Whether the generated class should have common Roo annotations") final boolean rooAnnotations,
            @CliOption(key = "path", mandatory = false, unspecifiedDefaultValue = "FOCUSED|SRC_MAIN_JAVA", specifiedDefaultValue = "FOCUSED|SRC_MAIN_JAVA", help = "Source directory to create the class in") final LogicalPath path,
            @CliOption(key = "extends", mandatory = false, unspecifiedDefaultValue = "java.lang.Object", optionContext = SUPERCLASS, help = "The superclass (defaults to java.lang.Object)") final JavaType superclass,
            @CliOption(key = "implements", mandatory = false, optionContext = INTERFACE, help = "The interface to implement") final JavaType implementsType,
            @CliOption(key = "abstract", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Whether the generated class should be marked as abstract") final boolean createAbstract,
            @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords) {

        classpathOperations.createClass(name, rooAnnotations, path, superclass,
                implementsType, createAbstract, permitReservedWords);
    }

    @CliCommand(value = "constructor", help = "Creates a class constructor")
    public void createConstructor(
            @CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = UPDATE_PROJECT, help = "The name of the class to receive this constructor") final JavaType name,
            @CliOption(key = "fields", mandatory = false, specifiedDefaultValue = "", optionContext = "constructor-fields", help = "The fields to include in the constructor. Multiple field names must be a double-quoted list separated by spaces") final Set<String> fields) {

        classpathOperations.createConstructor(name, fields);
    }

    @CliCommand(value = "enum type", help = "Creates a new Java enum source file in any project path")
    public void createEnum(
            @CliOption(key = "class", optionContext = UPDATE_PROJECT, mandatory = true, help = "The name of the enum to create") final JavaType name,
            @CliOption(key = "path", mandatory = false, unspecifiedDefaultValue = "FOCUSED|SRC_MAIN_JAVA", specifiedDefaultValue = "FOCUSED|SRC_MAIN_JAVA", help = "Source directory to create the enum in") final LogicalPath path,
            @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords) {

        classpathOperations.createEnum(name, path, permitReservedWords);
    }

    @CliCommand(value = "interface", help = "Creates a new Java interface source file in any project path")
    public void createInterface(
            @CliOption(key = "class", optionContext = UPDATE_PROJECT, mandatory = true, help = "The name of the interface to create") final JavaType name,
            @CliOption(key = "path", mandatory = false, unspecifiedDefaultValue = "FOCUSED|SRC_MAIN_JAVA", specifiedDefaultValue = "FOCUSED|SRC_MAIN_JAVA", help = "Source directory to create the interface in") final LogicalPath path,
            @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords) {

        classpathOperations.createInterface(name, path, permitReservedWords);
    }

    @CliCommand(value = "enum constant", help = "Inserts a new enum constant into an enum")
    public void enumConstant(
            @CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = UPDATE_PROJECT, help = "The name of the enum class to receive this field") final JavaType name,
            @CliOption(key = "name", mandatory = true, help = "The name of the constant") final JavaSymbolName fieldName,
            @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords) {

        classpathOperations.enumConstant(name, fieldName, permitReservedWords);
    }

    @CliCommand(value = "focus", help = "Changes focus to a different type")
    public void focus(
            @CliOption(key = "class", mandatory = true, optionContext = UPDATE_PROJECT, help = "The type to focus on") final JavaType type) {
        classpathOperations.focus(type);
    }

    @CliAvailabilityIndicator({ "class", "constructor", "interface",
            "enum type", "enum constant" })
    public boolean isProjectAvailable() {
        return classpathOperations.isProjectAvailable();
    }
}
