package org.springframework.roo.addon.jpa.annotations.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates to produce an unit test class for JPA entity classes.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooJpaUnitTest {

  /**
   * The target class from which unit test class is created.
   */
  Class<?> targetClass();

}
