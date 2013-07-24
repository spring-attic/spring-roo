package org.springframework.roo.addon.security;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.project.Feature;

/**
 * Interface for {@link SecurityOperationsImpl}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface SecurityOperations extends Feature {

    String SECURITY_FILTER_NAME = "springSecurityFilterChain";

    void installSecurity();

    void installPermissionEvaluator(JavaPackage permissionEvaluatorPackage);

    boolean isSecurityInstallationPossible();

    boolean isServicePermissionEvaluatorInstallationPossible();
}