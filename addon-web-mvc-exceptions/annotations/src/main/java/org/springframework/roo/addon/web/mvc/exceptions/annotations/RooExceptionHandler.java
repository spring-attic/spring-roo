package org.springframework.roo.addon.web.mvc.exceptions.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation indicates that annotated method contains the code necessary
 * to handle the specified exception.
 *
 * Allows to specify a view to return when specified exception is thrown.
 *
 * @author Fran Cardoso
 * @since 2.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooExceptionHandler {

  /**
   * Exception to handle.
   */
  Class<? extends Exception> exception();

  /**
   * View to return when defined exception is thrown.
   */
  String errorView() default "";
}
