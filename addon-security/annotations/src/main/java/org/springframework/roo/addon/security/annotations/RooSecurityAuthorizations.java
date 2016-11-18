package org.springframework.roo.addon.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation indicates that the annotated service has methods where the
 * annotations {@link PreAuthorize} should be applied.
 * <p>
 * It has one parameter that contains an array of {@link RooSecurityAuthorization}
 * annotations. These annotations includes information of the individual
 * methods to apply the annotation {@link PreAuthorize}.
 *
 * @author Manuel Iborra
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooSecurityAuthorizations {

  /**
   * Array of {@link RooSecurityAuthorization}
   *
   * @return a non empty array with one or more {@link RooSecurityAuthorization}
   */
  RooSecurityAuthorization[] authorizations() default {};

}
