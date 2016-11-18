package org.springframework.roo.addon.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * This annotation should be used as attribute of the
 * {@link RooSecurityFilters} annotation.
 *
 * It includes necessary information to apply {@link PreFilter} or {@link PostFilter} annotation
 *
 * @author Manuel Iborra
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooSecurityFilter {

  /**
   * Indicates the method name
   *
   * @return String with the method name
   */
  String method() default "";

  /**
   * Indicates the parameter types associated to the method
   *
   * @return Array of parameters types
   */
  Class<?>[] parameters() default {};

  /**
   * Indicates the roles to apply authorization
   *
   * @return Array of roles
   */
  String[] roles() default {};

  /**
   * Indicates the usernames to apply authorization
   *
   * @return Array of usernames
   */
  String[] usernames() default {};



  /**
   * Indicates the type of filtered (pre or post)
   *
   * @return String with the type
   */
  String when() default "";

}
