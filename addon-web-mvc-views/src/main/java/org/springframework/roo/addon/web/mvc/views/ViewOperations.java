package org.springframework.roo.addon.web.mvc.views;

import org.springframework.roo.addon.web.mvc.controller.addon.responses.ControllerMVCResponseService;
import org.springframework.roo.project.maven.Pom;

/**
 * Provides an API with the available Operations to include views on generated
 * project 
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public interface ViewOperations {

  /**
   * This operation will setup provided responseType 
   * on generated project.
   * 
   * @param viewType ControllerMVCResponseService 
   * @param module 
   *            Pom module where responseType components  should be included
   */
  void setup(ControllerMVCResponseService viewType, Pom module);

}
