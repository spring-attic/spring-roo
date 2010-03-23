package org.springframework.roo.addon.web.mvc.controller;

import java.util.Set;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;

/**
 * Interface to {@link ControllerOperationsImpl}.
 * 
 * @author Ben Alex
 *
 */
public interface ControllerOperations {

	void generateAll(JavaPackage javaPackage);

	boolean isNewControllerAvailable();

	/**
	 * Creates a new Spring MVC controller which will be automatically scaffolded.
	 * 
	 * <p>
	 * Request mappings assigned by this method will always commence with "/" and end with "/**".
	 * You may present this prefix and/or this suffix if you wish, although it will automatically be added
	 * should it not be provided.
	 * 
	 * @param controller the controller class to create (required)
	 * @param entity the entity this controller should edit (required)
	 * @param set of disallowed operations (required, but can be empty)
	 */
	void createAutomaticController(JavaType controller, JavaType entity, Set<String> disallowedOperations, String path);

}