package org.springframework.roo.addon.backup;

/**
 * Interface to {@link BackupOperationsImpl}.
 * 
 * @author Ben Alex
 *
 */
public interface BackupOperations {

	public abstract boolean isBackupAvailable();

	public abstract String backup();

}