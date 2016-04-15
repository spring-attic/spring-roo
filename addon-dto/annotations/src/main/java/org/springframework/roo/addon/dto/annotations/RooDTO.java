package org.springframework.roo.addon.dto.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a type that is a DTO.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooDTO {

  /**
   * Specifies if current DTO should be immutable. That is, all its fields should
   * be final and it should have a constructor for initializing these fields.
   * 
   * @return true if DTO should be immutable, false otherwise.
   */
  boolean immutable() default false;
}
