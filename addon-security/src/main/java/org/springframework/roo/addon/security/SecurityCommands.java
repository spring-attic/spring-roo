package org.springframework.roo.addon.security;

import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.lifecycle.ScopeDevelopmentShell;
import org.springframework.roo.support.util.Assert;

/**
 * Commands for the security add-on to be used by the ROO shell.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopmentShell
public class SecurityCommands implements CommandMarker {
	
	private SecurityOperations securityOperations;
	
	public SecurityCommands(SecurityOperations securityOperations) {
		Assert.notNull(securityOperations, "Security operations required");
		this.securityOperations = securityOperations;
	}
	
	/**
	 * @return true if the "install security" command is available at this moment
	 */
	@CliAvailabilityIndicator("install security")
	public boolean isInstallSecurityAvailable() {
		return securityOperations.isInstallSecurityAvailable();
	}
	
	@CliCommand(value="install security", help="Install Spring Security into your project")
	public void installSecurity() {
		securityOperations.installSecurity();
	}
	
}