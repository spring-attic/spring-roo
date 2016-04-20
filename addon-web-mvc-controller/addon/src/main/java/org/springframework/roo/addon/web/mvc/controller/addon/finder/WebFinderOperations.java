package org.springframework.roo.addon.web.mvc.controller.addon.finder;

import java.util.List;

import org.springframework.roo.addon.web.mvc.controller.addon.responses.ControllerMVCResponseService;
import org.springframework.roo.model.JavaType;

/**
 * Provides operations for Web MVC finder functionality.
 * 
 * @author Stefan Schmidt
 * @author Paula Navarro
 * @since 1.2.0
 */
public interface WebFinderOperations {

  /**
   * Add finders to the specified controller
   * 
   * @param controller the controller to be updated with the finders.
   * @param finderMethods list with finder names to be added.
   * @param responseType
   */
  void addFinders(JavaType controller, List<String> finderMethods,
      ControllerMVCResponseService responseType);

  boolean isWebFinderInstallationPossible();
}
