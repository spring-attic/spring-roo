package org.springframework.roo.addon.web.mvc.thymeleaf.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * = RooLinkFactory
 * 
 * Indicates a type that requires ROO LINK FACTORY support.
 * <p>
 * This annotation will cause ROO to produce code used for replacing current 
 * Spring URI generation used by controllers and Thymeleaf views. In the 
 * current release this code will be emitted to an ITD.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooLinkFactory {

  /**
   * The controller for which to generate the code used for URI generation.
   *
   * @return a non `null` controller.
   */
  Class<?> controller(); // No default => mandatory

}
