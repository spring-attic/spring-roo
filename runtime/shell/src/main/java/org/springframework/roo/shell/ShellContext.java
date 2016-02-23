package org.springframework.roo.shell;

import java.util.Map;

/**
 * ShellContext interface defines operations that will be used to get Spring Roo
 * Shell Context. This could be useful to check current defined parameters, get
 * global parameters...
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public interface ShellContext {

  /**
   * Check if --force global parameter has been defined with 'true' value.
   * 
   * @return true if --force global parameter has been defined with 'true'
   *         value
   */
  boolean isForce();

  /**
   * Returns value of --profile global parameter
   * 
   * @return String with profile name
   */
  String getProfile();

  /**
   * Returns last executed command
   * 
   * @return String with last executed command
   */
  String getExecutedCommand();

  /**
   * Returns a map with current parameters written on shell
   * 
   * @return Map<String, String> where key is parameter name and value is the
   *         defined value on current shell
   */
  Map<String, String> getParameters();

}
