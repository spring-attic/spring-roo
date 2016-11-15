package org.springframework.roo.addon.security.addon.security;

import org.springframework.roo.addon.security.addon.security.providers.SecurityProvider;
import org.springframework.roo.addon.security.annotations.RooSecurityAuthorizations;
import org.springframework.roo.addon.security.annotations.RooSecurityFilter;
import org.springframework.roo.addon.security.annotations.RooSecurityFilters;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Feature;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.maven.Pom;

import java.util.List;

/**
 * Interface for {@link SecurityOperationsImpl}.
 *
 * @author Ben Alex
 * @author Sergio Clares
 * @author Juan Carlos García
 * @since 1.0
 */
public interface SecurityOperations extends Feature {

  String SECURITY_FEATURE_NAME = FeatureNames.SECURITY;

  /**
   * Defines install operation that will be used by implementations to install
   * the necessary components of Spring Security.
   *
   * @param type
   *            SecurityProvider type that will be install
   * @param module
   *            Pom of the application module where Spring Security will be
   *            installed.
   */
  void installSecurity(SecurityProvider type, Pom module);

  /**
   * Defines getAllSecurityProviders operation that will be used by
   * implementations to get all security providers registered on the Spring
   * Roo Shell.
   *
   * @return List with the registered security providers on the Spring Roo
   *         Shell.
   */
  List<SecurityProvider> getAllSecurityProviders();

  /**
   * Defines getSpringSecurityAnnotationValue method that will be used by
   * implementations to calculate the value of the Spring Security Annotations
   *
   *
   * @param roles separated comma list with the roles to include in Spring Security annotation.
   * @param usernames separated comma list with the usernames to include in Spring Security annotation
   *
   * @return String with the value of the Spring Security annotation
   */
  String getSpringSecurityAnnotationValue(String roles, String usernames);

  /**
   * Defines {@link RooSecurityFilters} annotation that include one or many
   * {@link RooSecurityFilter} annotations which will be used by
   * implementations to include {@link PreFilter} or {@link PostFilter} annotations
   * in service methods.
   *
   * @param klass Class where include the annotations
   * @param methodName Method where apply the filter
   * @param roles Roles to apply by the filter
   * @param usernames Usernames to apply by the filter
   * @param when Indicate the type of filter 'PRE' (@PreFilter) or 'POST' (@PostFilter)
   */
  void generateFilterAnnotations(JavaType klass, String methodName, String roles, String usernames,
      String when);

  /**
   * Defines {@link RooSecurityAuthorizations} annotation that include one or many
   * {@link RooSecurityAuthorization} annotations which will be used by
   * implementations to include {@link PreAuthorize} annotations
   * in service methods.
   *
   * @param klass Class where include the annotations
   * @param methodName Method where apply the authorization
   * @param roles Roles to apply by the authorization
   * @param usernames Usernames that have authorization
   */
  void generateAuthorizeAnnotations(JavaType klass, String methodName, String roles,
      String usernames);
}
