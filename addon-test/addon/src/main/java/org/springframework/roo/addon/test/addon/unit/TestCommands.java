package org.springframework.roo.addon.test.addon.unit;

import static org.springframework.roo.shell.OptionContexts.UPDATE_PROJECT;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.test.addon.providers.TestCreatorProvider;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.converters.LastUsed;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CliOptionAutocompleteIndicator;
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
  public boolean isPersistentClassAvailable() {
    JavaType type = lastUsed.getJavaType();
    if (type != null) {
      for (TestCreatorProvider provider : getValidTestCreatorsForType(type)) {
        if (provider.isUnitTestCreationAvailable()) {
          return true;
        }
      }
    }
    return getTestCreationAvailable();
  }

  @CliOptionAutocompleteIndicator(command = "test unit", help = "Option `--class` must "
      + "be a non-abstract entity annotated with `@RooJpaEntity`.", param = "class")
  public List<String> getClassPosibleValues(ShellContext shellContext) {

    // Get current value of class
    String currentText = shellContext.getParameters().get("class");

    // Create results to return
    List<String> results = new ArrayList<String>();

    // Get entity fully qualified names
    Set<ClassOrInterfaceTypeDetails> entities =
        typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_JPA_ENTITY);
    for (ClassOrInterfaceTypeDetails entity : entities) {
      String name = replaceTopLevelPackageString(entity, currentText);
      if (!results.contains(name) && !entity.isAbstract()) {
        results.add(name);
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
              + "is not specified, it is assumed that the class is in the module which has the focus. ") final JavaType type) {

    testOperations.createUnitTest(type);
  }

  /**
   * Replaces a JavaType fullyQualifiedName for a shorter name using '~' for
   * TopLevelPackage
   *
   * @param cid ClassOrInterfaceTypeDetails of a JavaType
   * @param currentText String current text for option value
   * @return the String representing a JavaType with its name shortened
   */
  private String replaceTopLevelPackageString(ClassOrInterfaceTypeDetails cid, String currentText) {
    String javaTypeFullyQualilfiedName = cid.getType().getFullyQualifiedTypeName();
    String javaTypeString = "";
    String topLevelPackageString = "";

    // Add module value to topLevelPackage when necessary
    if (StringUtils.isNotBlank(cid.getType().getModule())
        && !cid.getType().getModule().equals(projectOperations.getFocusedModuleName())) {

      // Target module is not focused
      javaTypeString = cid.getType().getModule().concat(LogicalPath.MODULE_PATH_SEPARATOR);
      topLevelPackageString =
          projectOperations.getTopLevelPackage(cid.getType().getModule())
              .getFullyQualifiedPackageName();
    } else if (StringUtils.isNotBlank(cid.getType().getModule())
        && cid.getType().getModule().equals(projectOperations.getFocusedModuleName())
        && (currentText.startsWith(cid.getType().getModule()) || cid.getType().getModule()
            .startsWith(currentText)) && StringUtils.isNotBlank(currentText)) {

      // Target module is focused but user wrote it
      javaTypeString = cid.getType().getModule().concat(LogicalPath.MODULE_PATH_SEPARATOR);
      topLevelPackageString =
          projectOperations.getTopLevelPackage(cid.getType().getModule())
              .getFullyQualifiedPackageName();
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
        LOGGER.warning("Cannot load TestCreatorProvider on UnitTestCommands.");
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
   * Checks all {@link TestCreatorProvider} implementations looking for any 
   * available.
   *
   * @return `true` if any of the implementations is available or
   *            `false` if none of the implementations are available.
   */
  private boolean getTestCreationAvailable() {

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
      if (provider.isUnitTestCreationAvailable()) {
        return true;
      }
    }

    return false;
  }

}
