package org.springframework.roo.addon.web.mvc.views;

import org.springframework.roo.classpath.scanner.MemberDetails;

/**
 * 
 * This interface will provide necessary operations to be able to 
 * generate views.
 * 
 * All component or service that wants to include some new view inside 
 * generated project will delegate in this interface to include them. 
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public interface MVCViewGenerationService {

  /**
   * This operation returns the unique identifier name of 
   * a MVCViewGenerationService 
   * 
   * @return String with an unique identifier name
   */
  String getName();

  /**
   * This operation returns the view folder where views
   * will be included.
   * 
   * @return String with the views folder path
   */
  String getViewsFolder();

  /**
   * This operation returns the file extension to use
   * on generated views.
   * 
   * @return String with the file extension
   */
  String getViewsExtension();

  /**
   * This operation returns the folder where layouts
   * will be included.
   * 
   * @return String with the views folder path
   */
  String getLayoutsFolder();

  /**
   * This operation returns the folder where fragment will be included.
   * If needed.
   * 
   * @return String with the views folder path
   */
  String getFragmentsFolder();

  /**
   * This operation will add a list view using entityDetails 
   * and the provided context
   * 
   * @param entity Details of an entity to be able to generate view
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addListView(MemberDetails entity, ViewContext ctx);

  /**
   * This operation will add a show view using entityDetails 
   * and the provided context
   * 
   * @param entity Details of an entity to be able to generate view
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addShowView(MemberDetails entity, ViewContext ctx);

  /**
   * This operation will add a create view using entityDetails 
   * and the provided context
   * 
   * @param entity Details of an entity to be able to generate view
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addCreateView(MemberDetails entity, ViewContext ctx);

  /**
   * This operation will add an update view using entityDetails 
   * and the provided context
   * 
   * @param entity Details of an entity to be able to generate view
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addUpdateView(MemberDetails entity, ViewContext ctx);

  /**
   * This operation will add a finder view using entityDetails 
   * and the provided context
   * 
   * @param entity Details of an entity to be able to generate view
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addFinderView(MemberDetails entity, String finderName, ViewContext ctx);

  /**
   * This operation will add the application index view using 
   * the provided context
   * 
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addIndexView(ViewContext ctx);

  /**
   * This operation will add the application error view using 
   * the provided context
   * 
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addErrorView(ViewContext ctx);


  /**
   * This operation will add the default-layout view using 
   * the provided context
   * 
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addDefaultLayout(ViewContext ctx);

  /**
   * This operation will add the footer fragment using 
   * the provided context
   * 
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addFooter(ViewContext ctx);

  /**
   * This operation will add the header fragment using 
   * the provided context
   * 
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addHeader(ViewContext ctx);

  /**
   * This operation will add the menu fragment using 
   * the provided context
   * 
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addMenu(ViewContext ctx);

  /**
   * This operation will add the session fragment using 
   * the provided context
   * 
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addSession(ViewContext ctx);

  /**
   * This operation will updates menu view.
   * 
   * TODO: Maybe, instead of modify all menu view, only new generated controller should
   * be included on it. Must be fixed on future versions.
   * 
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void updateMenuView(ViewContext ctx);

  /**
   * This operation will install all necessary templates on generated project.
   * With that, Spring Roo users will be able to customize the Spring Roo templates 
   * to be able to generate views with their custom code.
   */
  void installTemplates();

}
