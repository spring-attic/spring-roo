package org.springframework.roo.addon.dto.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a type that is an Entity Projection.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooEntityProjection {

  /**
   * Indicates the entity which is associated with the Projection.
   * 
   * @return entity the Class associated with the current Projection.
   */
  Class<?> entity();

  /**
   * Indicates the field names which are present in the Projection.
   * 
   * @return
   */
  String[] fields();

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
