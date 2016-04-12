package org.springframework.roo.addon.test.addon.unit;

import org.springframework.roo.model.JavaType;

/**
 * Interface of {@link UnitTestOperationsImpl}.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public interface UnitTestOperations {

  /**
   * Check if persistence is installed in project.
   * 
   * @return true if persistence is installed, otherwise false.
   */
  boolean isUnitTestInstallationPossible();

  /**
   * Creates a unit test class for the provided project class. Silently returns if the 
   * unit test file already exists.
   * 
   * @param project class to produce a unit test for (required)
   */
  void newUnitTest(JavaType projectType);

}
