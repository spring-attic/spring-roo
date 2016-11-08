package org.springframework.roo.addon.ws.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation indicates that the annotated interface 
 * is a Service Endpoint Interface.
 * 
 * It includes information about the based service
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooSei {

  /**
   * Indicates the service that will provide
   * the operations for this endpoint.
   * 
   * @return class with the based service.
   */
  Class<?> service();

}
