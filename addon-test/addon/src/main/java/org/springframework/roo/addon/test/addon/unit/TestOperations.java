package org.springframework.roo.addon.test.addon.unit;

import org.springframework.roo.model.JavaType;

/**
 * Interface of {@link TestOperationsImpl}.
 *
 * @author Sergio Clares
 */
public interface TestOperations {

  /**
   * Creates an unit test for the given type. Automatically produces the needed
   * data-on-demand (DoD) if does not exist. Shows a message if the unit test 
   * file already exists.
   * 
   * @param type the {@link JavaType} to produce the test for.
   */
  void createUnitTest(JavaType type);

}
