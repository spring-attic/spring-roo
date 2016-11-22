package org.springframework.roo.addon.test.addon.integration;


import static org.springframework.roo.shell.OptionContexts.APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.operations.ClasspathOperations;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.ReservedWords;
import org.springframework.roo.model.RooJavaType;
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
import org.springframework.roo.support.osgi.ServiceInstaceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Shell commands for {@link IntegrationTestOperationsImpl}.
 *
 * @author Ben Alex
 * @author Manuel Iborra
 * @since 1.0
 */
@Component
@Service
public class IntegrationTestCommands implements CommandMarker {

  @Reference
  private IntegrationTestOperations integrationTestOperations;
  @Reference
  private TypeLocationService typeLocationService;
  @Reference
  private ClasspathOperations classpathOperations;
  @Reference
  private ProjectOperations projectOperations;

  private ServiceInstaceManager serviceManager = new ServiceInstaceManager();

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
    this.serviceManager.activate(this.context);
  }

  protected void deactivate(final ComponentContext context) {
    this.context = null;
    this.serviceManager.deactivate();
  }

  @CliAvailabilityIndicator({"test integration"})
  public boolean isPersistentClassAvailable() {
    return integrationTestOperations.isIntegrationTestInstallationPossible();
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

  @CliOptionAutocompleteIndicator(
      command = "test integration",
      param = "class",
      help = "--class parameter must be an existing class annotated with @RooJpaRepository. Please, assign a valid one.")
  public List<String> getAllEntities(ShellContext shellContext) {

    // Get current value of class
    String currentText = shellContext.getParameters().get("class");

    // Create results to return
    List<String> results = new ArrayList<String>();

    // Get entity full qualified names
    Set<ClassOrInterfaceTypeDetails> repositories =
        typeLocationService
            .findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_REPOSITORY_JPA);
    for (ClassOrInterfaceTypeDetails repository : repositories) {
      String name = classpathOperations.replaceTopLevelPackageString(repository, currentText);
      if (!results.contains(name)) {
        results.add(name);
      }
    }

    return results;
  }

  @CliCommand(
      value = "test integration",
      help = "Creates a new integration test class for the specified entity using its associated "
          + "repository. The generated test class will contain a basic structure and the necessary "
          + "testing components.")
  public void newIntegrationTest(
      @CliOption(
          key = "class",
          mandatory = false,
          unspecifiedDefaultValue = "*",
          help = "The name of the repository to create an integration test. If you consider it necessary, you can "
              + "also specify the package. Ex.: `--class ~.repository.MyRepository` (where `~` is the "
              + "base package). When working with multiple modules, you should specify the name of the "
              + "class and the module where it is. Ex.: `--class repository:~.MyRepository`. If the "
              + "module is not specified, it is assumed that the repository is in the module which has the "
              + "focus. " + "Possible values are: any of the repositories in the project.") final JavaType klass,
      @CliOption(
          key = "module",
          mandatory = true,
          help = "The application module where generate the integration test."
              + "This option is mandatory if the focus is not set in an application module, that is, a "
              + "module containing an `@SpringBootApplication` class. "
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

    integrationTestOperations.newIntegrationTest(klass, module);
  }
}
