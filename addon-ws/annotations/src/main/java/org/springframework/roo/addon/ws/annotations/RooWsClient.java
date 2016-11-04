package org.springframework.roo.addon.ws.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation should be used as attribute of the {@link RooWsClients}
 * annotation.
 * 
 * It includes information about the client to generate
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooWsClient {

  /**
   * Indicates the endpoint name 
   * 
   * @return String with the endpoint name
   */
  String endpoint() default "";

  /**
   * Indicates the targetNamespace associated to 
   * this endpoint
   * 
   * @return String with the targetNamespace
   */
  String targetNamespace() default "";

  /**
   * Indicates the bindingType associated to
   * this endpoint
   * 
   * @return
   */
  SoapBindingType binding();

}
