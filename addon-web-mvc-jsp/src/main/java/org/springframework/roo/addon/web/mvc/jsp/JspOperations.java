package org.springframework.roo.addon.web.mvc.jsp;

import org.springframework.roo.addon.web.mvc.jsp.i18n.I18n;
import org.springframework.roo.model.JavaType;
import org.w3c.dom.Document;

/**
 * Interface for {@link JspOperationsImpl}.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 */
public interface JspOperations {
	
	boolean isControllerAvailable();
	
	boolean isSetupAvailable();
	
	boolean isInstallLanguageCommandAvailable();

	/**
	 * installs all common view artifacts needed for MVC scaffolding
	 */
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
	
	/**
	 * Installs additional languages into Web MVC app.
	 * 
	 * @param language the language
	 */
	void installI18n(I18n language);
	
	/**
	 * Creates a new Spring MVC static view.
	 * 
	 * @param path the static view to create in (required, ie '/foo')
	 * @param title the title of the view (required)
	 * @param category the menu category name (required)
	 * @param viewName the mapping this view should adopt (required, ie 'index')
	 */
	void installView(String path, String viewName, String title, String category);
	
	/**
	 * Installs a new Spring MVC static view.
	 * 
	 * @param path the static view to create in (required, ie '/foo')
	 * @param viewName the mapping this view should adopt (required, ie 'index')
	 * @param title the title of the view (required)
	 *  @param category the menu category name (required)
	 * @param document the jspx document to use for the view
	 */
	void installView(String path, String viewName, String title, String category, Document document);
}