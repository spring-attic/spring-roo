package org.springframework.roo.addon.backup;

import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.lifecycle.ScopeDevelopmentShell;
import org.springframework.roo.support.util.Assert;

/**
 * Commands for the 'backup' add-on to be used by the ROO shell.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
@ScopeDevelopmentShell
public class BackupCommands implements CommandMarker {	

	private BackupOperations backupOperations;

	public BackupCommands(BackupOperations backupOperations) {
		Assert.notNull(backupOperations, "Backup operations required");
		this.backupOperations = backupOperations;
	}
	
	@CliAvailabilityIndicator("backup")
	public boolean isBackupCommandAvailable() {		
		return backupOperations.isBackupAvailable();
	}
	
	@CliCommand(value="backup", help="Backup your project to a zip file")
	public String backup() {
		return backupOperations.backup();
	}	
}