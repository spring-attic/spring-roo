package org.springframework.roo.shell;

/**
 * Common constants used in the option contexts of {@link CliOption}s.
 * 
 * @author Alan Stewart
 */
public final class OptionContexts {

  /**
   * If this string appears in an option context, a {@link Converter} will
   * return only interface types appearing in any module of the user's
   * project.
   */
  public static final String INTERFACE = "interface";

  /**
   * If this string appears in an option context, a {@link Converter} will
   * return only enum types appearing in any module of the user's
   * project.
   */
  public static final String ENUMERATION = "enumeration";

  /**
   * If this string appears in an option context, a {@link Converter} will
   * return types appearing in any module of the user's project.
   */
  public static final String PROJECT = "project";

  /**
   * If this string appears in an option context, this converter will return
   * non-final types appearing in any module of the user's project.
   */
  public static final String SUPERCLASS = "superclass";

  /**
   * If this string appears in an option context, this converter will update
   * the last used type and the focused module as applicable.
   */
  public static final String UPDATE = "update";

  /**
   * If this string appears in an option context, this converter will return values that have the specified feature, e.g. feature[APPLICATION]
   */
  public static final String FEATURE = "feature";

  /**
   * If this string appears in an option context, this converter will update
   * the last used type.
   */
  public static final String UPDATELAST = "lastused";

  /**
   * An option context value indicating that the currently focused module
   * should be included when this {@link Converter} generates completions.
   */
  public static final String INCLUDE_CURRENT_MODULE = "includeCurrent";

  public static final String UPDATE_PROJECT = "update,project";

  public static final String UPDATELAST_INTERFACE = "lastused,interface";

  public static final String UPDATELAST_PROJECT = "lastused,project";

  public static final String APPLICATION_FEATURE = FEATURE + "[APPLICATION]";

  public static final String APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE = APPLICATION_FEATURE + ","
      + INCLUDE_CURRENT_MODULE;


  private OptionContexts() {}
}
