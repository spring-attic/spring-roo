package org.springframework.roo.addon.web.mvc.thymeleaf.annotations.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates to produce an integration test class Thymeleaf controllers.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooThymeleafControllerIntegrationTest {

  /**
   * The target class from which integration test class is created.
   */
  Class<?> targetClass();

}
