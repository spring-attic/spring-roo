package org.springframework.roo.addon.web.mvc.exceptions.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation indicates that annotated class contains exception handler methods
 *  annotated with {@link RooExceptionHandler}
 *
 * @author Fran Cardoso
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooExceptionHandlers {

  /**
   * The array of {@link RooExceptionHandler} with the exception handlers to be
   * included in current class.
   */
  RooExceptionHandler[] value() default {};

}
