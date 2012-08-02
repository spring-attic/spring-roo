package org.springframework.roo.addon.security;

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

    boolean isSecurityInstallationPossible();
}