package org.springframework.roo.model;

/**
 * = _SpringletsJavaType_
 *
 * Constants for Springlets-specific {@link JavaType}s. Use them in preference to
 * creating new instances of these types.
 *
 * @author Jose Manuel Vivó
 * @author Juan Carlos García
 * @since 2.0.0
 */
public class SpringletsJavaType {

  public static final JavaType SPRINGLETS_GLOBAL_SEARCH = new JavaType(
      "io.springlets.data.domain.GlobalSearch");
  public static final JavaType SPRINGLETS_GLOBAL_SEARCH_ARGUMENT_RESOLVER = new JavaType(
      "io.springlets.data.web.GlobalSearchHandlerMethodArgumentResolver");
  public static final JavaType SPRINGLETS_QUERYDSL_REPOSITORY_SUPPORT_EXT = new JavaType(
      "io.springlets.data.jpa.repository.support.QueryDslRepositorySupportExt");
  public static final JavaType SPRINGLETS_QUERYDSL_REPOSITORY_SUPPORT_ATTRIBUTE_BUILDER =
      new JavaType(
          "io.springlets.data.jpa.repository.support.QueryDslRepositorySupportExt.AttributeMappingBuilder");

  /**
   * Constructor is private to prevent instantiation
   */
  private SpringletsJavaType() {}

}
