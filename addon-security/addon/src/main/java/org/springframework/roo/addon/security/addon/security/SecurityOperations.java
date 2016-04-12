package org.springframework.roo.addon.security.addon.security;

import org.springframework.roo.project.Feature;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.maven.Pom;

/**
 * Interface for {@link SecurityOperationsImpl}.
 * 
 * @author Ben Alex
 * @author Sergio Clares
 * @since 1.0
 */
public interface SecurityOperations extends Feature {

  String SECURITY_FEATURE_NAME = FeatureNames.SECURITY;

  boolean isSecurityInstallationPossible();

  void installSecurity(Pom module);
}
