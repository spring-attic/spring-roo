package org.springframework.roo.felix.help;


/**
 * Provides Help Operations
 * 
 * @author Juan Carlos García
 * @since 1.3
 */
public interface HelpService {

  /**
   * Writes the command index as reference guid in DocBook format, into the 
   * current working directory.
   */
  void helpReferenceGuide();

  /**
   * Shows Spring Roo help
   */
  void obtainHelp(String buffer);

}
