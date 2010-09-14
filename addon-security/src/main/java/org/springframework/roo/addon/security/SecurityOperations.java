package org.springframework.roo.addon.security;

/**
 * Interface for {@link SecurityOperationsImpl}.
 * 
 * @author Ben Alex
 *
 */
public interface SecurityOperations {
	
	String SECURITY_FILTER_NAME = "springSecurityFilterChain";

	boolean isInstallSecurityAvailable();

	void installSecurity();
}