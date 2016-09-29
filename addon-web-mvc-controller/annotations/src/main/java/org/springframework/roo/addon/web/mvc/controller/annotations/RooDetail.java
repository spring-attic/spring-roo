package org.springframework.roo.addon.web.mvc.controller.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a type that requires ROO detail controller support.
 * <p>
 * This annotation will cause ROO to produce code that would typically appear in
 * MVC detail controllers. Importantly, such code does NOT depend on any singletons and
 * is intended to safely serialise. In the current release this code will be
 * emitted to an ITD.
 *
 * @author Manuel Iborra
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooDetail {

  /**
   * This parameter defines the relation field
   *
   * @return The relation field name
   */
  String relationField();

}
