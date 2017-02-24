package org.springframework.roo.classpath.operations;

import static org.springframework.roo.shell.OptionContexts.INTERFACE;
import static org.springframework.roo.shell.OptionContexts.SUPERCLASS;
import static org.springframework.roo.shell.OptionContexts.UPDATE_PROJECT;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.ShellContext;

import java.util.Set;

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

  @Reference
  private ClasspathOperations classpathOperations;
  @Reference
  private TypeLocationService typeLocationService;
  @Reference
  private MemberDetailsScanner memberDetailsScanner;

  @CliCommand(value = "class", help = "Creates a new Java class source file in any project path")
  public void createClass(
      @CliOption(key = "class", optionContext = UPDATE_PROJECT, mandatory = true,
          help = "The name of the class to create. If you consider "
              + "it necessary, you can also specify the package (base package can be specified "
              + "with `~`). Ex.: `--class ~.domain.MyClass`. You can specify module as well, if "
              + "necessary. Ex.: `--class model:~.domain.MyClass`. When working with a "
              + "multi-module project, if module is not specified the class will be created "
              + "in the module which has the focus.") final JavaType name,
      @CliOption(
          key = "rooAnnotations",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether the generated class should have common Roo annotations (`@RooToString`, `@RooEquals` and `@RooSerializable`). "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean rooAnnotations,
      @CliOption(key = "path", mandatory = false,
          unspecifiedDefaultValue = "FOCUSED:SRC_MAIN_JAVA",
          specifiedDefaultValue = "FOCUSED:SRC_MAIN_JAVA",
          help = "Source directory to create the class in. Default: [FOCUSED-MODULE]/src/main/java") final LogicalPath path,
      @CliOption(
          key = "extends",
          mandatory = false,
          unspecifiedDefaultValue = "java.lang.Object",
          optionContext = SUPERCLASS,
          help = "The superclass fully qualified name. Default if option not present: `java.lang.Object`.") final JavaType superclass,
      @CliOption(key = "implements", mandatory = false, optionContext = INTERFACE,
          help = "The interface to implement") final JavaType implementsType,
      @CliOption(
          key = "abstract",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether the generated class should be marked as abstract. Default if option present: `true`; default if option not present: `false`.") final boolean createAbstract,
      @CliOption(
          key = "permitReservedWords",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo. Default if option present: `true`; default if option not present: `false`.") final boolean permitReservedWords,
      ShellContext shellContext) {

    // Check if already exists a class with same name
    String physicalTypeIdentifier = typeLocationService.getPhysicalTypeIdentifier(name);
    if (physicalTypeIdentifier != null && !shellContext.isForce()) {
      throw new IllegalArgumentException(
          String
              .format(
                  "The class '%s' already exists and cannot be created. Use '--force' parameter to overrite it.",
                  name));
    }

    classpathOperations.createClass(name, rooAnnotations, path, superclass, implementsType,
        createAbstract, permitReservedWords);
  }

  @CliCommand(value = "constructor", help = "Creates a class constructor.")
  public void createConstructor(
      @CliOption(
          key = "class",
          mandatory = false,
          unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT,
          help = "The name of the class to receive this constructor. If you consider it necessary, "
              + "you can also specify the package (base package can be specified with `~`). "
              + "Ex.: `--class ~.domain.MyEntity`. You can specify module as well, if necessary. "
              + "Ex.: `--class model:~.domain.MyEntity`. When working with a multi-module project, "
              + "if module is not specified, it is assumed that the class is in the module that has "
              + "set the focus. If this param is not specified, it is assumed that the target class "
              + "is the one focused.") final JavaType name,
      @CliOption(
          key = "fields",
          mandatory = false,
          specifiedDefaultValue = "",
          optionContext = "constructor-fields",
          help = "The fields to include in the constructor. Multiple field names must be a double-quoted list separated by spaces") final Set<String> fields) {

    classpathOperations.createConstructor(name, fields);
  }

  @CliCommand(value = "enum type",
      help = "Creates a new Java enum source file in any project path.")
  public void createEnum(
      @CliOption(
          key = "class",
          optionContext = UPDATE_PROJECT,
          mandatory = true,
          help = "The name of the enum class to create. If you consider it necessary, you can also specify "
              + "the package (base package can be specified with `~`). Ex.: `--class ~.domain.MyEnumClass`. "
              + "You can specify module as well, if necessary. Ex.: `--class model:~.domain.MyEnumClass`. "
              + "When working with a multi-module project, if module is not specified the projection will "
              + "be created in the module which has the focus.") final JavaType name,
      @CliOption(key = "path", mandatory = false,
          unspecifiedDefaultValue = "FOCUSED:SRC_MAIN_JAVA",
          specifiedDefaultValue = "FOCUSED:SRC_MAIN_JAVA",
          help = "Source directory where create the enum. "
              + "Default: _[FOCUSED-MODULE]/src/main/java_") final LogicalPath path,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean permitReservedWords,
      ShellContext shellContext) {

    // Check if already exists a class with same name
    String physicalTypeIdentifier = typeLocationService.getPhysicalTypeIdentifier(name);
    if (physicalTypeIdentifier != null && !shellContext.isForce()) {
      throw new IllegalArgumentException(
          String
              .format(
                  "The enum type '%s' already exists and cannot be created. Use '--force' parameter to overrite it.",
                  name));
    }

    classpathOperations.createEnum(name, path, permitReservedWords);
  }

  @CliCommand(value = "interface",
      help = "Creates a new Java interface source file in any project path.")
  public void createInterface(
      @CliOption(
          key = "class",
          optionContext = UPDATE_PROJECT,
          mandatory = true,
          help = "The name of the class to create. If you consider it necessary, you can also specify "
              + "the package (base package can be specified with `~`). Ex.: `--class ~.domain.MyClass`. "
              + "You can specify module as well, if necessary. Ex.: `--class model:~.domain.MyClass`. "
              + "When working with a multi-module project, if module is not specified the class will "
              + "be created in the module which has the focus.") final JavaType name,
      @CliOption(key = "path", mandatory = false,
          unspecifiedDefaultValue = "FOCUSED:SRC_MAIN_JAVA",
          specifiedDefaultValue = "FOCUSED:SRC_MAIN_JAVA",
          help = "Source directory to create the interface in. "
              + "Default: _[FOCUSED-MODULE]/src/main/java_.") final LogicalPath path,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean permitReservedWords,
      ShellContext shellContext) {

    // Check if already exists a class with same name
    String physicalTypeIdentifier = typeLocationService.getPhysicalTypeIdentifier(name);
    if (physicalTypeIdentifier != null && !shellContext.isForce()) {
      throw new IllegalArgumentException(
          String
              .format(
                  "The interface '%s' already exists and cannot be created. Use '--force' parameter to overrite it.",
                  name));
    }

    classpathOperations.createInterface(name, path, permitReservedWords);
  }

  @CliCommand(value = "enum constant", help = "Inserts a new enum constant into an enum class.")
  public void enumConstant(
      @CliOption(key = "name", mandatory = true,
          help = "The name of the constant. It will converted to upper case automatically.") final JavaSymbolName fieldName,
      @CliOption(
          key = "class",
          mandatory = false,
          unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT,
          help = "TThe name of the enum class to receive this constant. When working on a mono module project, "
              + "simply specify the name of the class in which the new constant will be included. If you "
              + "consider it necessary, you can also specify the package. Ex.: `--class ~.domain.MyEnumClass` "
              + "(where `~` is the base package). When working with multiple modules, you should specify the "
              + "name of the class and the module where it is. Ex.: `--class model:~.domain.MyEnumClass`. "
              + "If the module is not specified, it is assumed that the class is in the module which has the "
              + "focus. " + "Default if option not present: the class focused by Roo shell.") final JavaType name,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean permitReservedWords) {

    classpathOperations.enumConstant(name, fieldName, permitReservedWords);
  }

  @CliCommand(value = "focus", help = "Changes Roo Shell focus to a different type in the project.")
  public void focus(
      @CliOption(
          key = "class",
          mandatory = true,
          optionContext = UPDATE_PROJECT,
          help = "The type to focus on. When working on a mono module project, simply specify the name of"
              + " the class in which the new constant will be included. If you consider it necessary, you"
              + " can also specify the package. Ex.: `--class ~.domain.MyEnumClass` (where `~` is the "
              + "base package). When working with multiple modules, you should specify the name of the "
              + "class and the module where it is. Ex.: `--class model:~.domain.MyEnumClass`. If the "
              + "module is not specified, it is assumed that the class is in the module which has the "
              + "focus.") final JavaType type) {
    classpathOperations.focus(type);
  }

  @CliAvailabilityIndicator({"class", "constructor", "interface", "enum type", "enum constant",
      "focus"})
  public boolean isProjectAvailable() {
    return classpathOperations.isProjectAvailable();
  }
}
