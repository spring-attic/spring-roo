package org.springframework.roo.support.util;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Utilities for handling {@link File} instances.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public abstract class FileUtils {
	// doesn't check for backslash after the colon, since Java has no issues with paths like c:/Windows
	private static final Pattern WINDOWS_DRIVE_PATH= Pattern.compile("^[A-Za-z]:.*");

	/**
	 * Deletes the specified {@link File}.
	 * 
	 * <p>
	 * If the {@link File} refers to a directory, any contents of that directory (including other directories)
	 * are also deleted.
	 * 
	 * <p>
	 * If the {@link File} does not already exist, this method immediately returns true.
	 * 
	 * @param file to delete (required; the file may or may not exist)
	 * @return true if the file is fully deleted, or false if there was a failure when deleting
	 */
	public static final boolean deleteRecursively(File file) {
		Assert.notNull(file, "File to delete required");
		if (!file.exists()) {
			return true;
		}
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				if (!deleteRecursively(f)) {
					return false;
				}
			}
		}
		file.delete();
		return true;
	}
	
	/**
	 * Copies the specified source directory to the destination.
	 * 
	 * <p>
	 * Both the source must exist. If the destination does not already exist, it will be created. If the destination
	 * does exist, it must be a directory (not a file).
	 * 
	 * @param source the already-existing source directory (required)
	 * @param destination the destination directory (required)
	 * @param deleteDestinationOnExit indicates whether to mark any created destinations for deletion on exit
	 * @return true if the copy was successful
	 */
	public static final boolean copyRecursively(File source, File destination, boolean deleteDestinationOnExit) {
		Assert.notNull(source, "Source directory required");
		Assert.notNull(destination, "Destination directory required");
		Assert.isTrue(source.exists(), "Source directory '" + source + "' must exist");
		Assert.isTrue(source.isDirectory(), "Source directory '" + source + "' must be a directory");
		if (destination.exists()) {
			Assert.isTrue(destination.isDirectory(), "Destination directory '" + destination + "' must be a directory");
		} else {
			destination.mkdirs();
			if (deleteDestinationOnExit) {
				destination.deleteOnExit();
			}
		}
		for (File s : source.listFiles()) {
			File d = new File(destination, s.getName());
			if (deleteDestinationOnExit) {
				d.deleteOnExit();
			}
			if (s.isFile()) {
				try {
					FileCopyUtils.copy(s, d);
				} catch (IOException ioe) {
					return false;
				}
			} else {
				// It's a sub-directory, so copy it
				d.mkdir();
				if (!copyRecursively(s, d, deleteDestinationOnExit)) {
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Checks if the provided fileName denotes an absolute path on the file system. 
	 * On Windows, this includes both paths with and without drive letters, where the latter have to start with '\'.
	 * No check is performed to see if the file actually exists!
	 * 
	 * @param fileName name of a file, which could be an absolute path
	 * @return true if the fileName looks like an absolute path for the current OS
	 */
	public static boolean denotesAbsolutePath(String fileName) {
		if (OsUtils.isWindows()) {
			// first check for drive letter
			if (WINDOWS_DRIVE_PATH.matcher(fileName).matches()) {
				return true;
			}
		} 
		return fileName.startsWith(File.separator);
	}
	
}
