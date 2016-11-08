package org.springframework.roo.addon.ws.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation indicates that the annotated class 
 * is an implementation of a Service Endpoint Interface. In other
 * words, the Endpoint.
 * 
 * It includes information about the implemented SEI.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooSeiImpl {

  /**
   * Indicates the sei that this class implements.
   * 
   * @return class with the implemented SEI.
   */
  Class<?> sei();

}
