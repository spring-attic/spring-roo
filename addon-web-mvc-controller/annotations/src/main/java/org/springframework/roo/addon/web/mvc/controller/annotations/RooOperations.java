package org.springframework.roo.addon.web.mvc.controller.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a type that requires ROO operations support.
 * <p>
 * This annotation will cause ROO to produce code that lets publish the methods of the service that were created by user.
 *
 * @author Manuel Iborra
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooOperations {

  /**
   * Service operations to be publish.
   */
  String[] operations();


}
