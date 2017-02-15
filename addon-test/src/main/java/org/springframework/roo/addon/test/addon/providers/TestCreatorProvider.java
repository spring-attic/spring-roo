package org.springframework.roo.addon.test.addon.providers;

import org.springframework.roo.model.JavaType;

/**
 * Provides a test creation API which can be implemented by each add-on that
 * creates tests with its specific behavior.
 * 
 * This interface permits test creation via Roo commands with different
 * configurations depending on the type of class to test.
 *
 * @author Sergio Clares
 * @since 2.0
 */
public interface TestCreatorProvider {

  /**
   * Whether an implementation of this interface is valid for the class type
   * which the test is going to be created for.
   *
   * @return `true` if the implementation is valid, `false` otherwise.
   */
  boolean isValid(JavaType javaType);

  /**
   * Check if 'test unit' command is available.
   * 
   * @return `true` if 'test unit' command is available, `false` otherwise.
   */
  boolean isUnitTestCreationAvailable();

  /**
   * Creates an unit test class for the provided project class. Silently 
   * returns if the unit test file already exists.
   * 
   * @param projectType the class to produce an unit test for (required).
   */
  void createUnitTest(JavaType projectType);

  /**
   * Check if 'test integration' command is available.
   * 
   * @return `true` if 'test integration' command is available, `false` otherwise.
   */
  boolean isIntegrationTestCreationAvailable();

  /**
   * Creates an integration test class for the provided project class. Silently 
   * returns if the integration test file already exists.
   * 
   * @param projectType the class to produce an integration test for (required).
   */
  void createIntegrationTest(JavaType projectType);

}
