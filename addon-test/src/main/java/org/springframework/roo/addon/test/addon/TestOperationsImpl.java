package org.springframework.roo.addon.test.addon;

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
import org.springframework.roo.addon.test.addon.providers.TestCreatorProvider;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.logging.HandlerUtils;

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

  private BundleContext context;

  @Reference
  private TypeLocationService typeLocationService;

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

    // Adding test classes
    List<TestCreatorProvider> validTestCreators = getValidTestCreatorsForType(type);
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
  public void createIntegrationTest(JavaType type) {

    // Check if specified type exists in the project
    String physicalTypeIdentifier = typeLocationService.getPhysicalTypeIdentifier(type);
    if (physicalTypeIdentifier == null) {
      throw new IllegalArgumentException(String.format(
          "The class '%s' doesn't exists in the project. Please, specify an existing class", type));
    }

    // Adding test classes
    List<TestCreatorProvider> validTestCreators = getValidTestCreatorsForType(type);
    if (validTestCreators.isEmpty()) {
      throw new IllegalArgumentException(
          "Unable to find a valid test creator for this type of class. "
              + "Please, select another type of class to generate the test, such a repository.");
    } else {
      for (TestCreatorProvider creator : validTestCreators) {
        creator.createIntegrationTest(type);
      }
    }
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
