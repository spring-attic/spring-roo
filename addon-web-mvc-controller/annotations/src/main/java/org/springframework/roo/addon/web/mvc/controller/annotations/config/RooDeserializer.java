package org.springframework.roo.addon.web.mvc.controller.annotations.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This class provides annotation to indicate that some class
 * provides configuration about deserialization of a entity with JSON
 *
 * @author Jose Manuel Vivó
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooDeserializer {

  /**
   * Specifies the entity which is configured by this class
   *
   * @return target entity
   */
  Class<?> entity();

}
