package org.springframework.roo.addon.web.mvc.controller.annotations.responses.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a type that requires ROO JSON controller support.
 * <p>
 * This annotation will cause ROO to produce code that would typically appear in
 * MVC controllers with JSON methods. In the current release this code will be
 * emitted to an ITD.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooJSON {

  /**
   * This parameter defines the finders that should be published on 
   * annotated Controller
   * 
   * @return The finders names list
   */
  String[] finders() default "";

}
