package org.springframework.roo.addon.web.mvc.controller.addon;

import java.util.List;

import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.model.JavaType;

/**
 * Provides an API with the available Operations to include Spring MVC on generated
 * project and generate new controllers.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public interface ControllerMVCService {

  /**
   * This operation will obtain an existing method with the provided
   * @RequestMapping attributes.
   * 
   * @param controller
   * @param method
   * @param path
   * @param params
   * @param accept
   * @param consumes
   * @param produces
   * @param headers
   * 
   * @return MethodMetadata if exists some method that has @RequestMapping annotation
   *           with the provided attributes. Will return null if doesn't exists any method that match
   *           with the provided parameters.
   */
  MethodMetadata getMVCMethodByRequestMapping(JavaType controller, String method, String path,
      List<String> params, String accept, String consumes, String produces, String headers);

}
