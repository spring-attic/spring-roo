package org.springframework.roo.addon.security.addon;

import org.springframework.roo.project.Feature;
import org.springframework.roo.project.FeatureNames;

/**
 * Interface for {@link SecurityOperationsImpl}.
 * 
 * @author Ben Alex
 * @author Sergio Clares
 * @since 1.0
 */
public interface SecurityOperations extends Feature {

  String SECURITY_FEATURE_NAME = FeatureNames.SECURITY;

  void installSecurity();

  boolean isSecurityInstallationPossible();

}
