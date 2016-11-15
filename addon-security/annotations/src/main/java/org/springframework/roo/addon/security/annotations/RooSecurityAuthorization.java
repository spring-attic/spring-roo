package org.springframework.roo.addon.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * This annotation should be used as attribute of the {@link RooSecurityAuthorizations}
 * annotation.
 *
 * It includes necessary information to apply {@link PreAuthorize} annotation
 *
 * @author Manuel Iborra
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooSecurityAuthorization {

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


}
