package org.springframework.roo.addon.layers.repository.jpa.annotations.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates to produce an integration test class for JPA repository classes.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooRepositoryJpaIntegrationTest {

  /**
   * The target class from which integration test class is created.
   */
  Class<?> targetClass();

  /**
   * The Data On Demand configuration class.
   */
  Class<?> dodConfigurationClass();

  /**
   * The related JPA entity Data On Demand class used for testing.
   */
  Class<?> dodClass();

}
