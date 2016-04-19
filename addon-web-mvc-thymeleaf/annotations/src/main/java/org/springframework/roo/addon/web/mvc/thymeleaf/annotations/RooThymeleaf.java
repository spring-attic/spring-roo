package org.springframework.roo.addon.web.mvc.thymeleaf.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a type that requires ROO THYMELEAF controller support.
 * <p>
 * This annotation will cause ROO to produce code that would typically appear in
 * MVC controllers with Thymeleaf methods. In the current release this code will be
 * emitted to an ITD.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooThymeleaf {

  /**
   * This parameter defines the finders that should be published on 
   * annotated Controller
   * 
   * @return The finders names list
   */
  String[] finders() default "";

}
