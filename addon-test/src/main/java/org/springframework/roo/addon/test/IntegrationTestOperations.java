package org.springframework.roo.addon.test;

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
	boolean isPersistentClassAvailable();

	/**
	 * Creates an integration test for the entity. Automatically produces a data-on-demand (DoD) class if one does not exist.
	 * Silently returns if the integration test file already exists.
	 * 
	 * @param entity the entity to produce an integration test for (required)
	 */
	void newIntegrationTest(JavaType entity);

	/**
	 * Creates a mock test for the entity. Silently returns if the mock test file already exists.
	 * 
	 * @param entity to produce a mock test for (required)
	 */
	void newMockTest(JavaType entity);
	
	/**
	 * Creates a test stub for the class. Silently returns if the test file already exists.
	 * 
	 * @param javaType to produce a test stub for (required)
	 */
	void newTestStub(JavaType javaType);
}