package org.springframework.roo.addon.security;

import static org.springframework.roo.shell.OptionContexts.PROJECT;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
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

    @CliAvailabilityIndicator("permissionEvaluator")
    public boolean isPermissionEvaluatorCommandAvailable() {
        return securityOperations
                .isServicePermissionEvaluatorInstallationPossible();
    }

    @CliCommand(value = "permissionEvaluator", help = "Create a permission evaluator")
    public void setupPermissionEvaluator(
            @CliOption(key = "package", mandatory = true, optionContext = PROJECT, help = "The package to add the permission evaluator to") final JavaPackage evaluatorPackage) {
        securityOperations.installPermissionEvaluator(evaluatorPackage);
    }
}