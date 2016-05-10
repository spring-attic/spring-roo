package org.springframework.roo.addon.web.mvc.jsp;

import org.springframework.roo.addon.web.mvc.views.i18n.I18n;
import org.springframework.roo.project.LogicalPath;
import org.w3c.dom.Document;

/**
 * Provides operations to create various view layer resources.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @author Juan Carlos García
 */
public interface JspOperations {

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
   * Installs a new Spring MVC static view.
   * 
   * @param path the static view to create in (required, ie '/foo')
   * @param viewName the mapping this view should adopt (required, ie 'index')
   * @param title the title of the view (required)
   * @param category the menu category name (required)
   * @param document the jspx document to use for the view
   * @param webappPath
   */
  void installView(String path, String viewName, String title, String category, Document document,
      LogicalPath webappPath);

  /**
   * Creates a new Spring MVC static view.
   * 
   * @param path the static view to create in (required, ie '/foo')
   * @param title the title of the view (required)
   * @param category the menu category name (required)
   * @param viewName the mapping this view should adopt (required, ie 'index')
   * @param webappPath
   */
  void installView(String path, String viewName, String title, String category,
      LogicalPath webappPath);

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
