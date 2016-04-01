package org.springframework.roo.addon.security.addon.audit;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Reference;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.shell.*;

/**
 * Commands to be used by the ROO shell for adding audit support.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class AuditCommands implements CommandMarker {

  @Reference
  AuditOperations auditOperations;

  @CliAvailabilityIndicator("audit setup")
  public boolean isSetupAuditAvailable() {
    return auditOperations.isAuditSetupPossible();
  }

  @CliCommand(value = "audit setup", help = "Install audit support into your project")
  public void setupAudit(
      @CliOption(key = "package", mandatory = true,
          help = "The package in which new classes needed for audit will be placed") final JavaPackage javaPackage) {
    auditOperations.setupAudit(javaPackage);
  }
}
