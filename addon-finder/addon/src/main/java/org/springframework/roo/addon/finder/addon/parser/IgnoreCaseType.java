package org.springframework.roo.addon.finder.addon.parser;

/**
 * The various types of ignore case that are supported.
 * 
 * @author Paula Navarro
 * @since 2.0
 */
public enum IgnoreCaseType {

  /**
   * Should not ignore the sentence case.
   */
  NEVER,

  /**
   * Should ignore the sentence case, throwing an exception if this is not
   * possible.
   */
  ALWAYS
}
