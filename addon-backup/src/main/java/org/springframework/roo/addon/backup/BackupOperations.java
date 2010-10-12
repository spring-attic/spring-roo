package org.springframework.roo.addon.backup;

/**
 * Interface to {@link BackupOperationsImpl}.
 * 
 * @author Ben Alex
 */
public interface BackupOperations {

	boolean isBackupAvailable();

	String backup();
}