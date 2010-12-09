package org.springframework.roo.addon.test;

import org.springframework.roo.model.JavaType;

/**
 * Interface of {@link IntegrationTestOperationsImpl}.
 * 
 * @author Ben Alex
 */
public interface IntegrationTestOperations {

	/**
	 * Creates a mock test for the entity. Silently returns if the mock test file already exists.
	 * 
	 * @param entity to produce a mock test for (required)
	 */
	void newMockTest(JavaType entity);
	
	/**
	 * Creates a test stub for the entity. Silently returns if the test file already exists.
	 * 
	 * @param entity to produce a test stub for (required)
	 */
	void newTestStub(JavaType entity);
}