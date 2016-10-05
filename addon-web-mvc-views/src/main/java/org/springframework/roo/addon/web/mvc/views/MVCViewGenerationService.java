package org.springframework.roo.addon.web.mvc.views;

import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.scanner.MemberDetails;

import java.util.List;

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
   * This operation returns the view folder of the specified module, where views
   * will be included.
   * 
   * @param moduleName module where view folder is located
   * @return String with the views folder path
   */
  String getViewsFolder(String moduleName);

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
  String getLayoutsFolder(String moduleName);

  /**
   * This operation returns the folder where fragment will be included.
   * If needed.
   * 
   * @return String with the views folder path
   */
  String getFragmentsFolder(String moduleName);

  /**
   * This operation will add a list view using entityDetails 
   * and the provided context
   * 
   * @param moduleName module where list view will be added
   * @param entity Details of an entity to be able to generate view
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addListView(String moduleName, MemberDetails entity, ViewContext ctx);

  /**
   * This operation will add a show view using entityDetails 
   * and the provided context
   * 
   * @param moduleName module where show view will be added
   * @param entity Details of an entity to be able to generate view
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addShowView(String moduleName, MemberDetails entity, ViewContext ctx);

  /**
   * This operation will add a create view using entityDetails 
   * and the provided context
   * 
   * @param moduleName module where create view will be added
   * @param entity Details of an entity to be able to generate view
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addCreateView(String moduleName, MemberDetails entity, ViewContext ctx);

  /**
   * This operation will add an update view using entityDetails 
   * and the provided context
   * 
   * @param moduleName module where update view will be added
   * @param entity Details of an entity to be able to generate view
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addUpdateView(String moduleName, MemberDetails entity, ViewContext ctx);

  /**
   * This operation will add a finder form view using entityDetails and the 
   * provided context
   * 
   * @param moduleName the module where finder form view will be added
   * @param entity the details of an entity to be able to generate view
   * @param finderName the name of the finder for which this form will be created
   * @param fieldsToAdd 
   * @param ctx the ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addFinderFormView(String moduleName, MemberDetails entity, String finderName,
      List<FieldMetadata> fieldsToAdd, ViewContext ctx);

  /**
   * This operation will add a finder list view using entityDetails and the 
   * provided context. This view will show finder result list
   * 
   * @param moduleName the module where finder list view will be added
   * @param entity the details of an entity to be able to generate view
   * @param finderName the name of the finder for which this form will be created
   * @param ctx the ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addFinderListView(String moduleName, MemberDetails entity, String finderName, ViewContext ctx);

  /**
   * This operation will add the application index view using 
   * the provided context
   * 
   * @param moduleName module where index view will be added
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addIndexView(String moduleName, ViewContext ctx);

  /**
   * This operation will add the application error view using 
   * the provided context
   * 
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addErrorView(String moduleName, ViewContext ctx);


  /**
   * This operation will add the default-layout view using 
   * the provided context
   * 
   * @param moduleName module where default view will be added
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addDefaultLayout(String moduleName, ViewContext ctx);

  /**
   * This operation will add the footer fragment using 
   * the provided context
   * 
   * @param moduleName module where footer fragment will be added
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addFooter(String moduleName, ViewContext ctx);

  /**
   * This operation will add the header fragment using 
   * the provided context
   * 
   * @param moduleName module where header fragment will be added
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addHeader(String moduleName, ViewContext ctx);

  /**
   * This operation will add the menu fragment using 
   * the provided context
   * 
   * @param moduleName module where menu fragment will be added
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addMenu(String moduleName, ViewContext ctx);

  /**
   * This operation will add the modal fragment using 
   * the provided context
   * 
   * @param moduleName module where session fragment will be added
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addModal(String moduleName, ViewContext ctx);

  /**
   * This operation will add the session fragment using 
   * the provided context
   * 
   * @param moduleName module where session fragment will be added
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addSession(String moduleName, ViewContext ctx);

  /**
   * This operation will add the session-links fragment using 
   * the provided context
   * 
   * @param moduleName module where session fragment will be added
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addSessionLinks(String moduleName, ViewContext ctx);

  /**
   * This operation will add the languages fragment using 
   * the provided context
   * 
   * @param moduleName module where session fragment will be added
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void addLanguages(String moduleName, ViewContext ctx);

  /**
   * This operation will updates menu view.
   * 
   * TODO: Maybe, instead of modify all menu view, only new generated controller should
   * be included on it. Must be fixed on future versions.
   * 
   * @param moduleName module where menu view is located
   * @param ctx ViewContext that contains necessary information about
   *            the controller, the project, etc...
   */
  void updateMenuView(String moduleName, ViewContext ctx);

  /**
   * This operation will install all necessary templates on generated project.
   * With that, Spring Roo users will be able to customize the Spring Roo templates 
   * to be able to generate views with their custom code.
   */
  void installTemplates();

}
