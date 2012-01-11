package org.springframework.roo.addon.backup;

/**
 * Interface to {@link BackupOperationsImpl}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface BackupOperations {

    String backup();

    boolean isBackupPossible();
}