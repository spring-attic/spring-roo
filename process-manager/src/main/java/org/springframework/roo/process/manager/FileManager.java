package org.springframework.roo.process.manager;

import java.io.InputStream;
import java.util.SortedSet;

import org.springframework.roo.file.monitor.FileMonitorService;
import org.springframework.roo.file.monitor.NotifiableFileMonitorService;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.file.undo.UndoManager;

/**
 * Represents the primary means for add-ons to modify the underlying disk storage.
 * 
 * <p>
 * A {@link FileManager} instance is acquired from the {@link ProcessManager}. A {@link FileManager} implementation must guarantee to use an {@link UndoManager} that is available to the
 * {@link ProcessManager}, such that {@link ProcessManager} can undo or reset as required.
 * 
 * <p>
 * An implementation may elect to defer writes to disk or discard them until {@link #commit()} or {@link #clear()} respectively is invoked.
 * 
 * @author Ben Alex
 * @since 1.0
 * 
 */
public interface FileManager {

	/**
	 * Indicates whether the file identified by the passed canonical path exists.
	 * 
	 * @param fileIdentifier to locate (required, in canonical path format)
	 * @return true if the file or directory exists
	 */
	boolean exists(String fileIdentifier);

	/**
	 * Obtains an input stream for the indicated file identifier, which must be a file (not a directory) and must exist at the time the method is called. This method is useful if read-only access to a
	 * file is required. For read-write access, use one of the other methods on {@link FileManager}.
	 * 
	 * @param fileIdentifier to read (required, in canonical path format)
	 * @return the input stream (never null)
	 */
	InputStream getInputStream(String fileIdentifier);

	/**
	 * Attempts to create a new directory on the disk.
	 * 
	 * <p>
	 * The requested file identifier path must not already exist. It should be in canonical file name format.
	 * 
	 * <p>
	 * Parent directories must also be created automatically by this method. Any created parent directories should be removed as part of the undo behaviour.
	 * 
	 * <p>
	 * An exception will be thrown if the path already exists.
	 * 
	 * @param fileIdentifier a path to be created that does not already exist (required)
	 * @return a representation of the directory (or null if the creation failed)
	 */
	FileDetails createDirectory(String fileIdentifier);

	/**
	 * Attempts to create a zero-byte file on the disk.
	 * 
	 * <p>
	 * The requested fileIdentifier path must not already exist. It should be in canonical file name format.
	 * 
	 * <p>
	 * Implementations guarantee to {@link #createDirectory(String)} as required to create any required parent directories.
	 * 
	 * <p>
	 * An exception will be thrown if the path already exists.
	 * 
	 * @param fileIdentifier a path to be created that does not already exist (required)
	 * @return a representation of the file (or null if the creation failed)
	 */
	MutableFile createFile(String fileIdentifier);

	/**
	 * Attempts to delete a file or directory on the disk. The path should be in canonical file name format.
	 * 
	 * <p>
	 * An exception will be thrown if the path does not exist.
	 * 
	 * <p>
	 * If the path refers to a directory, contents of the directory will be recursively deleted.
	 * 
	 * <p>
	 * If a delete fails, an exception will be thrown.
	 * 
	 * @param fileIdentifier to delete (required)
	 */
	void delete(String fileIdentifier);

	/**
	 * Provides an updatable representation of a file on the disk.
	 * 
	 * <p>
	 * The file identifier must refer to a file (not directory) that already exists. A violation of this requirement will result in an exception. The identifier should be in canonical file name
	 * format.
	 * 
	 * <p>
	 * Refer to the documentation for {@link MutableFile} for important restrictions on usage.
	 * 
	 * @param fileIdentifier to update (must be a file that already exists, required)
	 * @return a mutable presentation (never null)
	 */
	MutableFile updateFile(String fileIdentifier);

	/**
	 * Provides a simple way to create or update a file, skipping any modification if the file's contents match the proposed contents. This should only
	 * be called for text files.
	 * 
	 * <p>
	 * This mechanism also automatically deletes an unwanted file if the new contents are zero bytes. If deleting, the existence of
	 * the file need not be considered in advance (it will only delete if the file is present, but it will not fail if the file does not
	 * exist or has been separately deleted).
	 * 
	 * <p>
	 * Implementations guarantee to {@link #createDirectory(String)} as required to create any required parent directories.
	 * 
	 * <p>
	 * Implementations are required to observe the {@link #commit()} and {@link #clear()} semantics defined in the type-level JavaDocs.
	 * 
	 * @param fileIdentifier to create or update as appropriate (required)
	 * @param newContents the replacement contents (required, but can be zero bytes if the file should be deleted)
	 * @param writeImmediately forces immediate write of the file to disk (false means it can be deferred, as recommended)
	 */
	void createOrUpdateTextFileIfRequired(String fileIdentifier, String newContents, boolean writeImmediately);

	/**
	 * Commits actual changes to the disk that an implementation may have elected to defer.
	 */
	void commit();
	
	/**
	 * Discards proposed changes to the disk that an implementation may have elected to defer.
	 */
	void clear();
	
	/**
	 * Obtains an already-existing file for reading. The path should be in canonical file name format.
	 * 
	 * @param fileIdentifier to read that already exists (required)
	 * @return a representation of the file (or null if the file does not exist)
	 */
	FileDetails readFile(String fileIdentifier);

	/**
	 * Delegates to {@link FileMonitorService#scanAll()} or {@link NotifiableFileMonitorService#scanNotified()} if available.
	 * 
	 * @return the number of changes detected (can be 0 or above)
	 */
	int scan();

	/**
	 * Delegates to {@link FileMonitorService#findMatchingAntPath(String)}.
	 * 
	 * @param antPath the Ant path to evaluate, as per the canonical file path format (required)
	 * @return all matching identifiers (may be empty, but never null)
	 */
	SortedSet<FileDetails> findMatchingAntPath(String antPath);

}