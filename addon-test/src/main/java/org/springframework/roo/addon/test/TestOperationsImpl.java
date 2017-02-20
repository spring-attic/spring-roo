package org.springframework.roo.addon.test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.test.providers.TestCreatorProvider;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.DependencyScope;
import org.springframework.roo.project.DependencyType;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * Provides convenience methods that can be used to create unit and integration tests.
 *
 * @author Ben Alex
 * @author Sergio Clares
 * @since 1.0
 */
@Component
@Service
public class TestOperationsImpl implements TestOperations {

  protected final static Logger LOGGER = HandlerUtils.getLogger(TestCommands.class);

  private static final Dependency JUNIT_DEPENDENCY = new Dependency("junit", "junit", null,
      DependencyType.JAR, DependencyScope.TEST);
  private static final Dependency ASSERTJ_CORE_DEPENDENCY = new Dependency("org.assertj",
      "assertj-core", null, DependencyType.JAR, DependencyScope.TEST);
  private static final Dependency SPRING_TEST_DEPENDENCY = new Dependency("org.springframework",
      "spring-test", null, DependencyType.JAR, DependencyScope.TEST);
  private static final Plugin MAVEN_SUREFIRE_PLUGIN = new Plugin("org.apache.maven.plugins",
      "maven-surefire-plugin", null);
  private static final Dependency SPRING_BOOT_TEST_DEPENDENCY = new Dependency(
      "org.springframework.boot", "spring-boot-test", null, DependencyType.JAR,
      DependencyScope.TEST);

  private BundleContext context;

  @Reference
  private TypeLocationService typeLocationService;
  @Reference
  private ProjectOperations projectOperations;

  // TestCreatorProvider implementations
  private List<TestCreatorProvider> testCreators = new ArrayList<TestCreatorProvider>();

  protected void activate(final ComponentContext cContext) {
    this.context = cContext.getBundleContext();
  }

  protected void deactivate(final ComponentContext context) {
    this.context = null;
  }

  @Override
  public void createUnitTest(JavaType type) {

    // Check if specified type exists in the project
    String physicalTypeIdentifier = typeLocationService.getPhysicalTypeIdentifier(type);
    if (physicalTypeIdentifier == null) {
      throw new IllegalArgumentException(String.format(
          "The class '%s' doesn't exists in the project. Please, specify an existing class", type));
    }

    // Adding unit test dependencies
    List<TestCreatorProvider> validTestCreators = getValidTestCreatorsForType(type);
    if (!validTestCreators.isEmpty()) {
      addUnitTestDependencies(type.getModule());
    }

    // Creating tests
    if (validTestCreators.isEmpty()) {
      throw new IllegalArgumentException(
          "Unable to find a valid test creator for this type of class. "
              + "Please, select another type of class to generate the test, such as an entity.");
    } else {
      for (TestCreatorProvider creator : validTestCreators) {
        creator.createUnitTest(type);
      }
    }
  }

  @Override
  public void createIntegrationTest(JavaType type, Pom module) {

    // Check if specified type exists in the project
    String physicalTypeIdentifier = typeLocationService.getPhysicalTypeIdentifier(type);
    if (physicalTypeIdentifier == null) {
      throw new IllegalArgumentException(String.format(
          "The class '%s' doesn't exists in the project. Please, specify an existing class", type));
    }

    // Adding integration test dependencies
    List<TestCreatorProvider> validTestCreators = getValidTestCreatorsForType(type);
    if (!validTestCreators.isEmpty()) {
      addIntegrationTestDependencies(module.getModuleName());
    }

    // Creating tests
    if (validTestCreators.isEmpty()) {
      throw new IllegalArgumentException(
          "Unable to find a valid test creator for this type of class. "
              + "Please, select another type of class to generate the test, such a repository.");
    } else {
      for (TestCreatorProvider creator : validTestCreators) {
        creator.createIntegrationTest(type, module);
      }
    }
  }

  /**
   * Add needed dependencies and plugins to run created integration tests.
   * 
   * @param module {@link String} the module name where add dependencies.
   */
  private void addIntegrationTestDependencies(String moduleName) {

    // Add dependencies if needed
    projectOperations.addDependency(moduleName, JUNIT_DEPENDENCY);
    projectOperations.addDependency(moduleName, ASSERTJ_CORE_DEPENDENCY);
    projectOperations.addDependency(moduleName, SPRING_TEST_DEPENDENCY);
    projectOperations.addDependency(moduleName, SPRING_BOOT_TEST_DEPENDENCY);

    // Add plugin maven-failsafe-plugin
    Pom module = projectOperations.getPomFromModuleName(moduleName);
    // Stop if the plugin is already installed
    for (final Plugin plugin : module.getBuildPlugins()) {
      if (plugin.getArtifactId().equals("maven-failsafe-plugin")) {
        return;
      }
    }

    final Element configuration = XmlUtils.getConfiguration(getClass());
    final Element plugin = XmlUtils.findFirstElement("/configuration/plugin", configuration);

    // Now install the plugin itself
    if (plugin != null) {
      projectOperations.addBuildPlugin(moduleName, new Plugin(plugin));
    }
  }

  /**
   * Add needed dependencies and plugins to run created unit tests.
   * 
   * @param module {@link String} the module name where add dependencies.
   */
  private void addUnitTestDependencies(String module) {

    // Add dependencies if needed
    projectOperations.addDependency(module, JUNIT_DEPENDENCY);
    projectOperations.addDependency(module, ASSERTJ_CORE_DEPENDENCY);
    projectOperations.addDependency(module, SPRING_TEST_DEPENDENCY);

    // Add plugins if needed
    projectOperations.addBuildPlugin(module, MAVEN_SUREFIRE_PLUGIN);
  }

  /**
   * Gets all the valid implementations of TestCreatorProvider for a JavaType.
   *
   * @param type the JavaType to get the valid implementations.
   * @return a `List` with the {@link TestCreatorProvider} valid 
   *            implementations. Never `null`.
   */
  private List<TestCreatorProvider> getValidTestCreatorsForType(JavaType type) {

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
        LOGGER.warning("Cannot load TestCreatorProvider on TestOperationsImpl.");
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

}
