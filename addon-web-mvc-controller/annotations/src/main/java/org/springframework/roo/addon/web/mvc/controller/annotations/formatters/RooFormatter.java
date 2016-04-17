package org.springframework.roo.addon.web.mvc.controller.annotations.formatters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a type that requires ROO Formatter support.
 * <p>
 * This annotation will cause ROO to produce code that would typically appear in
 * formatters. Importantly, such code does NOT depend on any singletons and
 * is intended to safely serialise. In the current release this code will be
 * emitted to an ITD.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooFormatter {

  /**
   * Every formatter is responsible for a single entity. 
   */
  Class<?> entity();

  /**
   * Every formatter uses a single service.
   */
  Class<?> service();

}
