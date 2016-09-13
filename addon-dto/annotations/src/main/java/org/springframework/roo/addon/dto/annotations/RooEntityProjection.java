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
}
