package org.springframework.roo.addon.test.addon.integration;

import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.maven.Pom;

/**
 * Interface of {@link IntegrationTestOperationsImpl}.
 *
 * @author Ben Alex
 * @author Manuel Iborra
 */
public interface IntegrationTestOperations {

  /**
   * Checks for the existence the META-INF/persistence.xml
   *
   * @return true if the META-INF/persistence.xml exists, otherwise false
   */
  boolean isIntegrationTestInstallationPossible();

  /**
   * Creates an integration test for the repository. Automatically produces a
   * data-on-demand (DoD) class if one does not exist. Show message if the
   * integration test file already exists.
   *
   * @param klass class to produce an integration test
   * @param module Pom module where Swagger support should be included
   *
   */
  void newIntegrationTest(JavaType klass, Pom module);

  /**
   * Creates a unit test class for the project class. Silently returns if the unit test
   * file already exists.
   *
   * @param project class to produce a unit test for (required)
   */
  void newUnitTest(JavaType projectType);
}
