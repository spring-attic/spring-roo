package org.springframework.roo.addon.web.mvc.controller.annotations.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates to produce an integration test class for a JSON controller class.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooJsonControllerIntegrationTest {

  /**
   * The target class from which integration test class is created.
   */
  Class<?> targetClass();

}
