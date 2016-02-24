package org.springframework.roo.shell;

import java.lang.annotation.*;

/**
 * Annotates a method which indicates when a command option is visible depending
 * on other options in the same command.
 * <p>
 * This annotation must only be applied to a public method which receives a 
 * ShellContext parameter and returns primitive boolean.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CliOptionVisibilityIndicator {

  /**
   * @return the name of the command that contains the option to check 
   * visibility.
   * 
   */
  String command();

  /**
   * @return the list of params (options) which affect the visibility of this 
   * command option and thus we have to check.
   * 
   */
  String[] params();

  /**
   * @return the error message if the command is executed breaking this 
   * dependency 
   * 
   */
  String help();

}
