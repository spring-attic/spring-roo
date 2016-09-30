package org.springframework.roo.addon.web.mvc.controller.addon;

import org.springframework.roo.addon.web.mvc.controller.addon.responses.ControllerMVCResponseService;
import org.springframework.roo.addon.web.mvc.controller.addon.servers.ServerProvider;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Feature;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.maven.Pom;

/**
 * Provides an API with the available Operations to include Spring MVC on
 * generated project and generate new controllers.
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
   * This operation will check if add controllers operation is available
   *
   * @return true if add controller operation is available. false if not.
   */
  boolean isAddControllerAvailable();

  /**
   * This operation will generate or update a controller for every class
   * annotated with @RooJpaEntity
   *
   * @param responseType
   *            View provider to use
   * @param controllerPackage
   *            Package where is situated the controller
   * @param pathPrefix
   *            Prefix to use in RequestMapping
   */
  void createOrUpdateControllerForAllEntities(ControllerMVCResponseService responseType,
      JavaPackage controllerPackage, String pathPrefix);

  /**
   * This operation will generate or update a controller for a specified
   * entity
   *
   * @param entity
   *            Entity over which create the controller
   * @param responseType
   *            View provider to use
   * @param controllerPackage
   *            Package where is situated the controller
   * @param pathPrefix
   *            Prefix to use in RequestMapping
   */
  void createOrUpdateControllerForEntity(JavaType entity,
      ControllerMVCResponseService responseType, JavaPackage controllerPackage, String pathPrefix);

  /**
   * This operation will generate or update a first level detail controller
   * for every class annotated with @RooJpaEntity that has defined a @RooController
   *
   * @param responseType
   *            View provider to use
   * @param controllerPackage
   *            Package where is situated the controller
   */
  void createOrUpdateDetailControllersForAllEntities(ControllerMVCResponseService responseType,
      JavaPackage controllerPackage);

  /**
   * This operation will generate or update a level detail controller for a
   * specified field located in specified entity
   *
   * @param entity
   *            Entity over which create the controller
   * @param relationField
   * 			Field that set the relationship
   * @param responseType
   *            View provider to use
   * @param controllerPackage
   *            Package where is situated the controller
   */
  void createOrUpdateDetailControllerForEntity(JavaType entity, String relationField,
      ControllerMVCResponseService responseType, JavaPackage controllerPackage);

}
