package org.springframework.roo.addon.web.mvc.controller.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a type that requires ROO controller support.
 * <p>
 * This annotation will cause ROO to produce code that would typically appear in
 * MVC controllers. Importantly, such code does NOT depend on any singletons and
 * is intended to safely serialise. In the current release this code will be
 * emitted to an ITD.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooController {

  /**
   * Every controller is responsible for a single entity. 
   */
  Class<?> entity();

  /**
   * Every controller should be related with a service, to be able to work with 
   * negotiation layer. 
   */
  Class<?> service();

  /**
   * This parameter defines the @RequestMapping of annotated Controller
   * 
   * @return The view path.
   */
  String path();

}
