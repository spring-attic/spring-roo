package org.springframework.roo.addon.test;

import static org.springframework.roo.shell.OptionContexts.APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE;
import static org.springframework.roo.shell.OptionContexts.UPDATE_PROJECT;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.test.providers.TestCreatorProvider;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.converters.LastUsed;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.ReservedWords;
import org.springframework.roo.project.LogicalPath;
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

/**
 * Shell commands for {@link UnitTestOperationsImpl}.
 *
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class TestCommands implements CommandMarker {

  protected final static Logger LOGGER = HandlerUtils.getLogger(TestCommands.class);

  private BundleContext context;

  @Reference
  private TestOperations testOperations;
  @Reference
  private TypeLocationService typeLocationService;
  @Reference
  private ProjectOperations projectOperations;
  @Reference
  private LastUsed lastUsed;

  // TestCreatorProvider implementations
  private List<TestCreatorProvider> testCreators = new ArrayList<TestCreatorProvider>();

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  protected void deactivate(final ComponentContext context) {
    this.context = null;
  }

  @CliAvailabilityIndicator({"test unit"})
  public boolean isTestUnitCommandAvailable() {
    JavaType type = lastUsed.getJavaType();
    if (type != null) {
      for (TestCreatorProvider provider : getValidTestCreatorsForType(type)) {
        if (provider.isUnitTestCreationAvailable()) {
          return true;
        }
      }
    }
    return getUnitTestCreationAvailable();
  }

  @CliOptionAutocompleteIndicator(command = "test unit", help = "Option `--class` must "
      + "be a non-abstract valid type. Please, use auto-complete feature to select it.",
      param = "class")
  public List<String> getClassPosibleValues(ShellContext shellContext) {

    // Get current value of class
    String currentText = shellContext.getParameters().get("class");

    // Create results to return
    List<String> results = new ArrayList<String>();

    // Look for all valid types for all available test creators
    for (TestCreatorProvider creator : getAllTestCreators()) {
      if (creator.isUnitTestCreationAvailable()) {
        for (JavaType annotationType : creator.getValidTypes()) {

          // Look for types with this annotation type
          Set<ClassOrInterfaceTypeDetails> types =
              typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(annotationType);
          for (ClassOrInterfaceTypeDetails typeCid : types) {
            String name = replaceTopLevelPackageString(typeCid.getType(), currentText);
            if (!results.contains(name) && !typeCid.isAbstract()) {
              results.add(name);
            }
          }
        }
      }
    }

    return results;
  }

  @CliCommand(value = "test unit",
      help = "Creates a unit test class with a basic structure and with the "
          + "necessary testing components, for the specified class.")
  public void newMockTest(
      @CliOption(
          key = "class",
          mandatory = true,
          optionContext = UPDATE_PROJECT,
          help = "The name of the project class which this unit test class is targeting. If you consider "
              + "it necessary, you can also specify the package. Ex.: `--class ~.model.MyClass` (where "
              + "`~` is the base package). When working with multiple modules, you should specify the name"
              + " of the class and the module where it is. Ex.: `--class model:~.MyClass`. If the module "
              + "is not specified, it is assumed that the class is in the module which has the focus. ") final JavaType type,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean permitReservedWords) {

    if (!permitReservedWords) {
      ReservedWords.verifyReservedWordsNotPresent(type);
    }

    Validate
        .isTrue(
            BeanInfoUtils.isEntityReasonablyNamed(type),
            "Cannot create an integration test for an entity named 'Test' or 'TestCase' under any circumstances");

    testOperations.createUnitTest(type);
  }

  @CliAvailabilityIndicator({"test integration"})
  public boolean isTestIntegrationCommandAvailable() {
    JavaType type = lastUsed.getJavaType();
    if (type != null) {
      for (TestCreatorProvider provider : getValidTestCreatorsForType(type)) {
        if (provider.isIntegrationTestCreationAvailable()) {
          return true;
        }
      }
    }
    return getIntegrationTestCreationAvailable();
  }

  @CliOptionVisibilityIndicator(command = "test integration", params = {"module"},
      help = "Module parameter is not available if there is only one application module")
  public boolean isModuleVisible(ShellContext shellContext) {
    if (typeLocationService.getModuleNames(ModuleFeatureName.APPLICATION).size() > 1) {
      return true;
    }
    return false;
  }

  @CliOptionMandatoryIndicator(command = "test integration", params = {"module"})
  public boolean isModuleRequired(ShellContext shellContext) {
    Pom module = projectOperations.getFocusedModule();
    if (!isModuleVisible(shellContext)
        || typeLocationService.hasModuleFeature(module, ModuleFeatureName.APPLICATION)) {
      return false;
    }
    return true;
  }

  @CliOptionAutocompleteIndicator(command = "test integration", param = "class",
      help = "Option `--class` must "
          + "be a non-abstract valid type. Please, use auto-complete feature to select it.")
  public List<String> getAllEntities(ShellContext shellContext) {

    // Get current value of class
    String currentText = shellContext.getParameters().get("class");

    // Create results to return
    List<String> results = new ArrayList<String>();

    // Look for all valid types for all available test creators
    for (TestCreatorProvider creator : getAllTestCreators()) {
      if (creator.isIntegrationTestCreationAvailable()) {
        for (JavaType annotationType : creator.getValidTypes()) {

          // Look for types with this annotation type
          Set<ClassOrInterfaceTypeDetails> types =
              typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(annotationType);
          for (ClassOrInterfaceTypeDetails typeCid : types) {
            String name = replaceTopLevelPackageString(typeCid.getType(), currentText);
            if (!results.contains(name)) {
              results.add(name);
            }
          }
        }
      }
    }

    return results;
  }

  @CliCommand(value = "test integration",
      help = "Creates a new integration test class for the specified class. The generated test "
          + "class will contain a basic structure and the necessary testing components.")
  public void newIntegrationTest(
      @CliOption(
          key = "class",
          mandatory = false,
          unspecifiedDefaultValue = "*",
          help = "The name of the class to create an integration test. If you consider it necessary, you can "
              + "also specify the package. Ex.: `--class ~.package.MyClass` (where `~` is the "
              + "base package). When working with multiple modules, you should specify the name of the "
              + "class and the module where it is. Ex.: `--class module:~.MyClass`. If the "
              + "module is not specified, it is assumed that the class is in the module which has the "
              + "focus. "
              + "Possible values are: any of the valid classes in the project which support "
              + "automatically integration test creation.") final JavaType klass,
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
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean permitReservedWords) {

    if (!permitReservedWords) {
      ReservedWords.verifyReservedWordsNotPresent(klass);
    }

    Validate
        .isTrue(
            BeanInfoUtils.isEntityReasonablyNamed(klass),
            "Cannot create an integration test for an entity named 'Test' or 'TestCase' under any circumstances");

    testOperations.createIntegrationTest(klass, module);
  }

  /**
   * Replaces a JavaType fullyQualifiedName for a shorter name using '~' for
   * TopLevelPackage
   *
   * @param type ClassOrInterfaceTypeDetails of a JavaType
   * @param currentText String current text for option value
   * @return the String representing a JavaType with its name shortened
   */
  private String replaceTopLevelPackageString(JavaType type, String currentText) {
    String javaTypeFullyQualilfiedName = type.getFullyQualifiedTypeName();
    String javaTypeString = "";
    String topLevelPackageString = "";

    // Add module value to topLevelPackage when necessary
    if (StringUtils.isNotBlank(type.getModule())
        && !type.getModule().equals(projectOperations.getFocusedModuleName())) {

      // Target module is not focused
      javaTypeString = type.getModule().concat(LogicalPath.MODULE_PATH_SEPARATOR);
      topLevelPackageString =
          projectOperations.getTopLevelPackage(type.getModule()).getFullyQualifiedPackageName();
    } else if (StringUtils.isNotBlank(type.getModule())
        && type.getModule().equals(projectOperations.getFocusedModuleName())
        && (currentText.startsWith(type.getModule()) || type.getModule().startsWith(currentText))
        && StringUtils.isNotBlank(currentText)) {

      // Target module is focused but user wrote it
      javaTypeString = type.getModule().concat(LogicalPath.MODULE_PATH_SEPARATOR);
      topLevelPackageString =
          projectOperations.getTopLevelPackage(type.getModule()).getFullyQualifiedPackageName();
    } else {

      // Not multimodule project
      topLevelPackageString =
          projectOperations.getFocusedTopLevelPackage().getFullyQualifiedPackageName();
    }

    // Autocomplete with abbreviate or full qualified mode
    String auxString =
        javaTypeString.concat(StringUtils.replace(javaTypeFullyQualilfiedName,
            topLevelPackageString, "~"));
    if ((StringUtils.isBlank(currentText) || auxString.startsWith(currentText))
        && StringUtils.contains(javaTypeFullyQualilfiedName, topLevelPackageString)) {

      // Value is for autocomplete only or user wrote abbreviate value
      javaTypeString = auxString;
    } else {

      // Value could be for autocomplete or for validation
      javaTypeString = String.format("%s%s", javaTypeString, javaTypeFullyQualilfiedName);
    }

    return javaTypeString;
  }

  /**
   * Gets all the valid implementations of TestCreatorProvider for a JavaType.
   *
   * @param type the JavaType to get the valid implementations.
   * @return a `List` with the {@link TestCreatorProvider} valid 
   *            implementations. Never `null`.
   */
  public List<TestCreatorProvider> getValidTestCreatorsForType(JavaType type) {

    // Get all Services implement TestCreatorProvider interface
    if (this.testCreators.isEmpty()) {
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(TestCreatorProvider.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          TestCreatorProvider testCreatorProvider =
              (TestCreatorProvider) this.context.getService(ref);
          this.testCreators.add(testCreatorProvider);
        }

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TestCreatorProvider on TestCommands.");
        return null;
      }
    }

    List<TestCreatorProvider> validTestCreators = new ArrayList<TestCreatorProvider>();
    for (TestCreatorProvider provider : this.testCreators) {
      if (provider.isValid(type)) {
        validTestCreators.add(provider);
      }
    }

    return validTestCreators;
  }

  /**
   * Gets all the implementations of TestCreatorProvider
   *
   * @param type the JavaType to get the valid implementations.
   * @return a `List` with the {@link TestCreatorProvider} valid 
   *            implementations. Never `null`.
   */
  public List<TestCreatorProvider> getAllTestCreators() {

    // Get all Services implement TestCreatorProvider interface
    if (this.testCreators.isEmpty()) {
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(TestCreatorProvider.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          TestCreatorProvider testCreatorProvider =
              (TestCreatorProvider) this.context.getService(ref);
          this.testCreators.add(testCreatorProvider);
        }

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TestCreatorProvider on TestCommands.");
        return null;
      }
    }

    return this.testCreators;
  }

  /**
   * Checks all {@link TestCreatorProvider} implementations looking for any 
   * available for 'test unit' command.
   *
   * @return `true` if any of the implementations is available or
   *            `false` if none of the implementations are available.
   */
  private boolean getUnitTestCreationAvailable() {

    // Get all Services implement TestCreatorProvider interface
    if (this.testCreators.isEmpty()) {
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(TestCreatorProvider.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          TestCreatorProvider testCreatorProvider =
              (TestCreatorProvider) this.context.getService(ref);
          this.testCreators.add(testCreatorProvider);
        }

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TestCreatorProvider on TestCommands.");
        return false;
      }
    }

    for (TestCreatorProvider provider : this.testCreators) {
      if (provider.isUnitTestCreationAvailable()) {
        return true;
      }
    }

    return false;
  }

  /**
   * Checks all {@link TestCreatorProvider} implementations looking for any 
   * available for 'test integration' command.
   *
   * @return `true` if any of the implementations is available or
   *            `false` if none of the implementations are available.
   */
  private boolean getIntegrationTestCreationAvailable() {

    // Get all Services implement TestCreatorProvider interface
    if (this.testCreators.isEmpty()) {
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(TestCreatorProvider.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          TestCreatorProvider testCreatorProvider =
              (TestCreatorProvider) this.context.getService(ref);
          this.testCreators.add(testCreatorProvider);
        }

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TestCreatorProvider on UnitTestCommands.");
        return false;
      }
    }

    for (TestCreatorProvider provider : this.testCreators) {
      if (provider.isIntegrationTestCreationAvailable()) {
        return true;
      }
    }

    return false;
  }

}
