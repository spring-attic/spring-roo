package org.springframework.roo.addon.web.mvc.jsp;

import org.springframework.roo.addon.web.mvc.jsp.i18n.I18n;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Feature;
import org.springframework.roo.project.LogicalPath;
import org.w3c.dom.Document;

/**
 * Provides operations to create various view layer resources.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 */
public interface JspOperations extends Feature {

    /**
     * Creates a new Spring MVC controller.
     * <p>
     * Request mappings assigned by this method will always commence with "/"
     * and end with "/**". You may present this prefix and/or this suffix if you
     * wish, although it will automatically be added should it not be provided.
     * 
     * @param controller the controller class to create (required)
     * @param preferredMapping the mapping this controller should adopt
     *            (optional; if unspecified it will be based on the controller
     *            name)
     * @param webappPath
     */
    void createManualController(JavaType controller, String preferredMapping,
            LogicalPath webappPath);

    /**
     * Installs the common view artifacts needed for MVC scaffolding into the
     * currently focused module.
     */
    void installCommonViewArtefacts();

    /**
     * Installs all common view artifacts needed for MVC scaffolding into the
     * given module.
     * 
     * @param moduleName the name of the module into which to install the
     *            artifacts; can be empty for the root or only module
     */
    void installCommonViewArtefacts(String moduleName);

    /**
     * Installs additional languages into Web MVC app.
     * 
     * @param language the language
     * @param webappPath
     */
    void installI18n(I18n language, LogicalPath webappPath);

    /**
     * Installs a new Spring MVC static view.
     * 
     * @param path the static view to create in (required, ie '/foo')
     * @param viewName the mapping this view should adopt (required, ie 'index')
     * @param title the title of the view (required)
     * @param category the menu category name (required)
     * @param document the jspx document to use for the view
     * @param webappPath
     */
    void installView(String path, String viewName, String title,
            String category, Document document, LogicalPath webappPath);

    /**
     * Creates a new Spring MVC static view.
     * 
     * @param path the static view to create in (required, ie '/foo')
     * @param title the title of the view (required)
     * @param category the menu category name (required)
     * @param viewName the mapping this view should adopt (required, ie 'index')
     * @param webappPath
     */
    void installView(String path, String viewName, String title,
            String category, LogicalPath webappPath);

    boolean isControllerAvailable();

    boolean isInstallLanguageCommandAvailable();

    boolean isMvcInstallationPossible();

    /**
     * Replaces an existing tag library with the latest version (set backup flag
     * to backup your application first)
     * 
     * @param backup indicates wether your application should be backed up prior
     *            to replacing the tagx library
     * @param webappPath
     */
    void updateTags(boolean backup, LogicalPath webappPath);
}