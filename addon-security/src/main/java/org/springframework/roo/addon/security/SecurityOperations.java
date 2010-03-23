package org.springframework.roo.addon.security;

/**
 * Interface for {@link SecurityOperationsImpl}.
 * 
 * @author Ben Alex
 *
 */
public interface SecurityOperations {

	boolean isInstallSecurityAvailable();

	void installSecurity();

}