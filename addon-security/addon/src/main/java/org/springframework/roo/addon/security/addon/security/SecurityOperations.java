package org.springframework.roo.addon.security.addon.security;

import java.util.List;

import org.springframework.roo.addon.security.addon.security.providers.SecurityProvider;
import org.springframework.roo.project.Feature;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.maven.Pom;

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
   * Defines install operation that will be used by implementations to install the necessary
   * components of Spring Security.
   * 
   * @param type SecurityProvider type that will be install
   * @param module Pom of the application module where Spring Security will be installed.
   */
  void installSecurity(SecurityProvider type, Pom module);

  /**
   * Defines getAllSecurityProviders operation that will be used by implementations to
   * get all security providers registered on the Spring Roo Shell.
   * 
   * @return List with the registered security providers on the Spring Roo Shell.
   */
  List<SecurityProvider> getAllSecurityProviders();
}
