package org.springframework.roo.addon.web.mvc.controller.addon;

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

}
