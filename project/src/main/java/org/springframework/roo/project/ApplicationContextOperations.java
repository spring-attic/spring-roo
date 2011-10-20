package org.springframework.roo.project;

import org.springframework.roo.model.JavaPackage;

/**
 * Interface to methods available in {@link ApplicationContextOperationsImpl}.
 *
 * @author Ben Alex
 * @since 1.1
 */
public interface ApplicationContextOperations {

	void createMiddleTierApplicationContext(JavaPackage topLevelPackage, String moduleName);
}