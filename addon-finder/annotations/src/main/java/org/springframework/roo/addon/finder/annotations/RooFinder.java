package org.springframework.roo.addon.finder.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a single finder in {@link RooFinders} with its name and return type.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooFinder {

  /**
   * The name of the finder with Spring Data nomenclature
   * 
   * @return
   */
  String finder();

  /**
   * The class which the finder will return as result.
   */
  Class<?> defaultReturnType();

  /**
   * The class which the finder will receive as argument.
   */
  Class<?> formBean();

}
