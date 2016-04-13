package org.springframework.roo.addon.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a type that is a security config class
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
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
