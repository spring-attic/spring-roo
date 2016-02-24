package org.springframework.roo.addon.backup;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CommandMarker;

/**
 * Commands for the 'backup' add-on to be used by the ROO shell.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component
@Service
public class BackupCommands implements CommandMarker {

    @Reference private BackupOperations backupOperations;

    @CliCommand(value = "backup", help = "Backup your project to a zip file")
    public String backup() {
        return backupOperations.backup();
    }

    @CliAvailabilityIndicator("backup")
    public boolean isBackupCommandAvailable() {
        return backupOperations.isBackupPossible();
    }
}