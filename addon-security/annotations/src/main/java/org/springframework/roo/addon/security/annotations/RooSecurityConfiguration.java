package org.springframework.roo.addon.security.annotations;

/**
 * Indicates a type that is a security config class
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public @interface RooSecurityConfiguration {

  /**
   * Indicates if method-level security is enabled. Default is false.
   */
  boolean enableGlobalMethodSecurity() default false;

  /**
   * Indicates if audit support is enabled. Default is false.
   */
  boolean enableJpaAuditing() default false;
}
