package org.springframework.roo.addon.test;

import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.maven.Pom;

/**
 * Interface of {@link TestOperationsImpl}.
 *
 * @author Sergio Clares
 */
public interface TestOperations {

  /**
   * Creates an unit test for the given type. Automatically produces 
   * data-on-demand (DoD) if needed. Shows a message if the unit test 
   * file already exists.
   * 
   * @param type the {@link JavaType} to produce the test for.
   */
  void createUnitTest(JavaType type);

  /**
   * Creates an integration test for the given type. Automatically produces 
   * data-on-demand (DoD) if needed. Shows a message if the integration 
   * test file already exists.
   * 
   * @param type the {@link JavaType} to produce the test for.
   * @param module the @SpringBootApplication module where test class should be 
   *            created.
   */
  void createIntegrationTest(JavaType type, Pom module);

}
