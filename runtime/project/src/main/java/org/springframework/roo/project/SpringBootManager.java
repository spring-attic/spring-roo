package org.springframework.roo.project;

import org.springframework.roo.model.JavaPackage;

/**
 * Provides operations to manage generated Spring Boot applications
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public interface SpringBootManager {

    /**
     * Method that generates Java class annotated with @SpringBootApplication on src/main/java
     * 
     * @param topLevelPackage
     * @param projectName
     */
    void createSpringBootApplicationClass(JavaPackage topLevelPackage, String projectName);
    
    /**
     * Method that generates application.properties file on src/main/resources
     */
    void createSpringBootApplicationPropertiesFile();
    
    
    /**
     * Method that creates Java class annotated with @SpringApplicationConfiguration on src/test/java 
     * and will be used by JUnit Tests
     * 
     * @param topLevelPackage
     * @param projectName
     */
    void createApplicationTestsClass(JavaPackage topLevelPackage, String projectName);

}
