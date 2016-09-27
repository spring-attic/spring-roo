package org.springframework.roo.addon.web.mvc.controller.annotations.finder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a type that requires ROO controller support.
 * <p>
 * This annotation will cause ROO to produce code that will expose dynamic
 * finders to the Web UI.
 * 
 * @author Stefan Schmidt
 * @author Sergio Clares
 * @since 1.2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooSearch {

  /**
   * The list with the name of finders to expose to web layer.
   * 
   * @return
   */
  String[] finders();

}
