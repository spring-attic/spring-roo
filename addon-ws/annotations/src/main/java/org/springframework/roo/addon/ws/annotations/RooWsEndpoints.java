package org.springframework.roo.addon.ws.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation indicates that the annotated class is a @Configuration 
 * class that manages the Web Service endpoints defined in the application.
 * <p>
 * It has one parameter that contains an array of endpoint classes.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooWsEndpoints {

  /**
   * The array of Classes with the endpoints to be included in
   * current configuration class
   *
   * @return a non empty array with one or more endpoint class
   */
  Class<?>[] endpoints() default {};

  /**
   * The profile that should be used in the annotated
   * configuration class.
   * 
   * @return String with the provided profile
   */
  String profile() default "";


}
