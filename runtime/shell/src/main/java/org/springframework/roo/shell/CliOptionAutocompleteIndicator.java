package org.springframework.roo.shell;

import java.lang.annotation.*;

/**
 * Annotates a method which returns possible values for an option, replacing 
 * other Converters. These possible values could depend on other options in the 
 * same command.
 * <p>
 * This annotation must only be applied to a public method which receives 
 * ShellContext and String parameters and returns a String List.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CliOptionAutocompleteIndicator {

  /**
   * @return the name of the command that contains the option to check 
   * values.
   * 
   */
  String command();

  /**
   * @return the param (option) which needs to return its possible values.
   * 
   */
  String param();

  /**
   * @return the error message if the command is executed breaking this 
   * dependency 
   * 
   */
  String help();

  /**
   * @return <code>true</code> if autocomplete operation should include an space on finish when only
   * one result has been returned.
   * 
   */
  boolean includeSpaceOnFinish() default true;

  /**
   * @return <code>true</code> if values introduced should be validated on command execution.
   * 
   */
  boolean validate() default true;
}
