package org.springframework.roo.addon.security.addon.security;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.*;

/**
 * Commands for the security add-on to be used by the ROO shell.
 * 
 * @author Ben Alex
 * @author Sergio Clares
 * @since 1.0
 */
@Component
@Service
public class SecurityCommands implements CommandMarker {

  @Reference
  private SecurityOperations securityOperations;

  @CliAvailabilityIndicator("security setup")
  public boolean isInstallSecurityAvailable() {
    return securityOperations.isSecurityInstallationPossible();
  }

  @CliCommand(value = "security setup", help = "Install Spring Security into your project")
  public void installSecurity() {
    securityOperations.installSecurity();
  }

}
