package org.springframework.roo.addon.web.mvc.controller;

import java.util.Set;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;

/**
 * Provides Controller configuration operations.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 */
public interface ControllerOperations {

    /**
     * Creates a new Spring MVC controller which will be automatically
     * scaffolded.
     * <p>
     * Request mappings assigned by this method will always commence with "/"
     * and end with "/**". You may present this prefix and/or this suffix if you
     * wish, although it will automatically be added should it not be provided.
     * 
     * @param controller the controller class to create (required)
     * @param entity the entity this controller should edit (required)
     * @param disallowedOperations specify a set of disallowed operation names
     *            (required, but can be empty)
     * @param path the path which the controller should be accessible via REST
     *            requests
     */
    void createAutomaticController(JavaType controller, JavaType entity,
            Set<String> disallowedOperations, String path);

    /**
     * Creates Spring MVC controllers for all JPA entities in the project.
     * 
     * @param javaPackage The package where the new controllers are scaffolded.
     */
    void generateAll(JavaPackage javaPackage);

    boolean isControllerInstallationPossible();

    @Deprecated
    boolean isNewControllerAvailable();

    void setup();
}