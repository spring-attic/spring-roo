package org.springframework.roo.addon.security;

/**
 * Interface for {@link SecurityOperationsImpl}.
 * 
 * @author Ben Alex
 *
 */
public interface SecurityOperations {
	
	public static final String SECURITY_FILTER_NAME = "springSecurityFilterChain";

	boolean isInstallSecurityAvailable();

	void installSecurity();

}