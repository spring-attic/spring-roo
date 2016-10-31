package org.springframework.roo.addon.web.mvc.controller.addon;

import org.springframework.roo.addon.web.mvc.controller.annotations.ControllerType;
import org.springframework.roo.addon.web.mvc.controller.annotations.RooController;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

import java.util.Collection;

/**
 * Locates {@link RooController} within the user's project
 *
 * @author Jose Manuel Vivó
 * @since 2.0.0
 */
public interface ControllerLocator {

  /**
   * Returns the controllers related the given domain type
   *
   * @param domainType the domain type for which to find the Controller
   * @return a non-<code>null</code> collection
   */
  Collection<ClassOrInterfaceTypeDetails> getControllers(final JavaType domainType);

  /**
   * Returns the controllers related the given domain type of a concrete type
   *
   * @param domainType the domain type for which to find the Controller
   * @param type controller to locate
   * @return a non-<code>null</code> collection
   */
  Collection<ClassOrInterfaceTypeDetails> getControllers(final JavaType domainType,
      final ControllerType type);


  /**
   * Returns the controllers related the given domain type of a concrete type and
   * view
   *
   * @param domainType the domain type for which to find the Controller
   * @param type controller to locate
   * @param viewType View type annotation javaType (by example {@link RooJavaType#ROO_JSON})
   * @return a non-<code>null</code> collection
   */
  Collection<ClassOrInterfaceTypeDetails> getControllers(final JavaType domainType,
      final ControllerType type, final JavaType viewType);

}
