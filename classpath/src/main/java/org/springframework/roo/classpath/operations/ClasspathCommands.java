package org.springframework.roo.classpath.operations;

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
            @CliOption(key = "class", optionContext = "update,project", mandatory = true, help = "The name of the class to create") final JavaType name,
            @CliOption(key = "rooAnnotations", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Whether the generated class should have common Roo annotations") final boolean rooAnnotations,
            @CliOption(key = "path", mandatory = false, unspecifiedDefaultValue = "FOCUSED|SRC_MAIN_JAVA", specifiedDefaultValue = "FOCUSED|SRC_MAIN_JAVA", help = "Source directory to create the class in") final LogicalPath path,
            @CliOption(key = "extends", mandatory = false, unspecifiedDefaultValue = "java.lang.Object", help = "The superclass (defaults to java.lang.Object)") final JavaType superclass,
            @CliOption(key = "abstract", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Whether the generated class should be marked as abstract") final boolean createAbstract,
            @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords) {

        classpathOperations.createClass(name, rooAnnotations, path, superclass,
                createAbstract, permitReservedWords);
    }

    @CliCommand(value = "enum type", help = "Creates a new Java enum source file in any project path")
    public void createEnum(
            @CliOption(key = "class", optionContext = "update,project", mandatory = true, help = "The name of the enum to create") final JavaType name,
            @CliOption(key = "path", mandatory = false, unspecifiedDefaultValue = "FOCUSED|SRC_MAIN_JAVA", specifiedDefaultValue = "FOCUSED|SRC_MAIN_JAVA", help = "Source directory to create the enum in") final LogicalPath path,
            @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords) {

        classpathOperations.createEnum(name, path, permitReservedWords);
    }

    @CliCommand(value = "interface", help = "Creates a new Java interface source file in any project path")
    public void createInterface(
            @CliOption(key = "class", optionContext = "update,project", mandatory = true, help = "The name of the interface to create") final JavaType name,
            @CliOption(key = "path", mandatory = false, unspecifiedDefaultValue = "FOCUSED|SRC_MAIN_JAVA", specifiedDefaultValue = "FOCUSED|SRC_MAIN_JAVA", help = "Source directory to create the interface in") final LogicalPath path,
            @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords) {

        classpathOperations.createInterface(name, path, permitReservedWords);
    }

    @CliCommand(value = "enum constant", help = "Inserts a new enum constant into an enum")
    public void enumConstant(
            @CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The name of the enum class to receive this field") final JavaType name,
            @CliOption(key = "name", mandatory = true, help = "The name of the constant") final JavaSymbolName fieldName,
            @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords) {

        classpathOperations.enumConstant(name, fieldName, permitReservedWords);
    }

    @CliCommand(value = "focus", help = "Changes focus to a different type")
    public void focus(
            @CliOption(key = "class", mandatory = true, optionContext = "update,project", help = "The type to focus on") final JavaType type) {
        classpathOperations.focus(type);
    }

    @CliAvailabilityIndicator({ "class", "interface", "enum type",
            "enum constant" })
    public boolean isProjectAvailable() {
        return classpathOperations.isProjectAvailable();
    }
}
