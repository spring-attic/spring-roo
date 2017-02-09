package org.springframework.roo.addon.jpa.annotations.entity.factory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a factory used to create transient entity instances of the
 * annotated entity for testing purposes.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooJpaEntityFactory {

  /**
   * @return the type of the entity related to this factory annotated type.
   */
  Class<?> entity();

}
