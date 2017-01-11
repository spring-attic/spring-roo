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

  /**
   * Specifies the localization message used to obtain a localized Spring 
   * Expression Language expression to format the dto when showing 
   * it in presentation layer.
   * 
   * @return the key of the message with the SpEL (defaults to "").
   */
  String formatMessage() default "";

  /**
   * Specifies the Spring Expression Language expression to use for formatting 
   * the dto in presentation layer.
   * 
   * @return the SpEL (defaults to "").
   */
  String formatExpression() default "";

}
