package org.springframework.roo.addon.layers.repository.jpa.annotations.finder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a single finder in {@link RooFinders} with its name and return type.
 *
 * @author Sergio Clares
 * @author Jose Manuel Vivó
 * @since 2.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooFinder {

  /**
   * The name of the finder with Spring Data nomenclature
   *
   * @return
   */
  String value();

  /**
   * The class which the finder will return as result.
   */
  Class<?> returnType() default Class.class;

  /**
   * The class which the finder will receive as argument.
   */
  Class<?> formBean() default Class.class;

}
