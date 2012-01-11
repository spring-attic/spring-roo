package org.springframework.roo.addon.security;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CommandMarker;

/**
 * Commands for the security add-on to be used by the ROO shell.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
public class SecurityCommands implements CommandMarker {

    @Reference private SecurityOperations securityOperations;

    @CliCommand(value = "security setup", help = "Install Spring Security into your project")
    public void installSecurity() {
        securityOperations.installSecurity();
    }

    @CliAvailabilityIndicator("security setup")
    public boolean isInstallSecurityAvailable() {
        return securityOperations.isSecurityInstallationPossible();
    }
}