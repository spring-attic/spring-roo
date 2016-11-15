package org.springframework.roo.addon.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation indicates that the annotated service has methods where the
 * annotations {@link Prefilter} or {@link PostFilter} should be applied
 * <p>
 * It has one parameter that contains an array of {@link RooSecurityFilter}
 * annotations. These annotations includes information of the individual
 * methods to apply the annotations {@link Prefilter} or {@link PostFilter}.
 *
 * @author Manuel Iborra
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooSecurityFilters {

  /**
   * TODO
   * The array of {@link RooSecurityFilter} with the clients to be included in
   * current configuration class
   *
   * @return a non empty array with one or more {@link RooSecurityFilter}
   */
  RooSecurityFilter[] filters() default {};

}
