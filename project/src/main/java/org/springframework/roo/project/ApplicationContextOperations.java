package org.springframework.roo.project;

import org.springframework.roo.model.JavaPackage;

/**
 * Interface to methods available in {@link ApplicationContextOperationsImpl}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public interface ApplicationContextOperations {

    /**
     * Creates a Spring application context XML configuration file
     * 
     * @param topLevelPackage
     * @param moduleName the fully-qualified name of the Maven module to which
     *            the new application context belongs (empty means the root or
     *            only module, otherwise a relative path delimited with
     *            {@link java.io.File#separator})
     */
    void createMiddleTierApplicationContext(JavaPackage topLevelPackage,
            String moduleName);
}