package org.springframework.roo.addon.test.addon.integration;

import org.springframework.roo.model.JavaType;

/**
 * Interface of {@link IntegrationTestOperationsImpl}.
 * 
 * @author Ben Alex
 */
public interface IntegrationTestOperations {

  /**
   * Checks for the existence the META-INF/persistence.xml
   * 
   * @return true if the META-INF/persistence.xml exists, otherwise false
   */
  boolean isIntegrationTestInstallationPossible();

  /**
   * Creates an integration test for the entity. Automatically produces a
   * data-on-demand (DoD) class if one does not exist. Silently returns if the
   * integration test file already exists.
   * 
   * @param entity the entity to produce an integration test for (required)
   */
  void newIntegrationTest(JavaType entity);

  /**
   * Creates an integration test for the entity. Automatically produces a
   * data-on-demand (DoD) class if one does not exist. Silently returns if the
   * integration test file already exists.
   * 
   * @param entity the entity to produce an integration test for (required)
   * @param transactional indicates if the test case should be wrapped in a
   *            Spring transaction
   */
  void newIntegrationTest(JavaType entity, boolean transactional);

  /**
   * Creates a unit test class for the project class. Silently returns if the unit test
   * file already exists.
   * 
   * @param project class to produce a unit test for (required)
   */
  void newUnitTest(JavaType projectType);
}
