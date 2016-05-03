package org.springframework.roo.addon.web.mvc.controller.addon;

import org.springframework.roo.addon.web.mvc.controller.addon.responses.ControllerMVCResponseService;
import org.springframework.roo.addon.web.mvc.controller.addon.servers.ServerProvider;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Feature;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.maven.Pom;

/**
 * Provides an API with the available Operations to include Spring MVC on generated
 * project and generate new controllers.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @author Paula Navarro
 */
public interface ControllerOperations extends Feature {

  public static final String FEATURE_NAME = FeatureNames.MVC;

  /**
   * This operation will check if setup operation is available
   * 
   * @return true if setup operation is available. false if not.
   */
  boolean isSetupAvailable();

  /**
   * This operation will setup Spring MVC on generated project.
   * 
   * @param module 
   *            Pom module where Spring MVC should be included
   * @param appServer
   *            Server where application should be deployed
   */
  void setup(Pom module, ServerProvider appServer);

  /**
   * This operation will check if add controllers operation is 
   * available
   * 
   * @return true if add controller operation is available. 
   * false if not.
   */
  boolean isAddControllerAvailable();

  /**
   * This operation will generate a new controller for every class annotated
   * with @RooJpaEntity on current project.
   * 
   * @param controllersPackage
   * @param responseType
   * @param formattersPackage
   */
  void createControllerForAllEntities(JavaPackage controllersPackage,
      ControllerMVCResponseService responseType, JavaPackage formattersPackage);

  /**
   * This operation will generate a new controller with the specified information 
   * 
   * @param controller
   * @param entity
   * @param service
   * @param path
   * @param responseType
   * @param formattersPackage
   */
  void createController(JavaType controller, JavaType entity, JavaType service, String path,
      ControllerMVCResponseService responseType, JavaPackage formattersPackage);

  /**
   * Adds provided response type to existing controller
   * 
   * @param controller
   * @param controllerMVCResponseService
   */
  void updateController(JavaType controller, ControllerMVCResponseService responseType);

}
