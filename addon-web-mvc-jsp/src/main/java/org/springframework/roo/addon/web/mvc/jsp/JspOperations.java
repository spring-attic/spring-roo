package org.springframework.roo.addon.web.mvc.jsp;

import org.springframework.roo.model.JavaType;

/**
 * Interface for {@link JspOperationsImpl}.
 * 
 * @author Ben Alex
 *
 */
public interface JspOperations {

	boolean isControllerCommandAvailable();

	void installCommonViewArtefacts();

	/**
	 * Creates a new Spring MVC controller.
	 * 
	 * <p>
	 * Request mappings assigned by this method will always commence with "/" and end with "/**".
	 * You may present this prefix and/or this suffix if you wish, although it will automatically be added
	 * should it not be provided.
	 * 
	 * @param controller the controller class to create (required)
	 * @param preferredMapping the mapping this controller should adopt (optional; if unspecified it will be based on the controller name)
	 */
	void createManualController(JavaType controller, String preferredMapping);

}