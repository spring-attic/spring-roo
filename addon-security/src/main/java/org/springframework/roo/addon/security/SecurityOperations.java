package org.springframework.roo.addon.security;

/**
 * Interface for {@link SecurityOperationsImpl}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface SecurityOperations {

	String SECURITY_FILTER_NAME = "springSecurityFilterChain";

	boolean isInstallSecurityAvailable();

	void installSecurity();
}