package org.springframework.roo.shell;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a method that can indicate if a particular command option is
 * mandatory or not.
 * <p>
 * This annotation must only be applied to a public no-argument method that
 * returns primitive boolean.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CliOptionMandatoryIndicator {

  /**
   * @return the name of the command that contains the option to set mandatory
   *         dynamically.
   */
  String command();

  /**
   * @return the name of the option to set mandatory dynamically.
   * 
   * <b>IMPORTANT</b>: 
   * - Parameters should be setted as mandatory on @CliOption to use them on params 
   *   attribute.
   * - On the other hand, if @CliOption has more than one key, this attribute must be
   *   setted with the first value
   */
  String[] params();
}
