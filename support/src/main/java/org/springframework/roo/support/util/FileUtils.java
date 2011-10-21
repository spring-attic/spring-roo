package org.springframework.roo.support.util;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Utilities for handling {@link File} instances.
 *
 * @author Ben Alex
 * @since 1.0
 */
public abstract class FileUtils {

	// Doesn't check for backslash after the colon, since Java has no issues with paths like c:/Windows
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
	public static boolean deleteRecursively(final File file) {
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
	public static boolean copyRecursively(final File source, final File destination, final boolean deleteDestinationOnExit) {
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
	public static boolean denotesAbsolutePath(final String fileName) {
		if (OsUtils.isWindows()) {
			// first check for drive letter
			if (WINDOWS_DRIVE_PATH.matcher(fileName).matches()) {
				return true;
			}
		}
		return fileName.startsWith(File.separator);
	}

	/**
	 * TODO
	 * 
	 * @param fileIdentifier
	 * @return
	 * @since 1.2.0
	 */
	public static String getFirstDirectory(String fileIdentifier) {
		fileIdentifier = removeTrailingSeparator(fileIdentifier);
		File file = new File(fileIdentifier);
		if (file.isDirectory()) {
			return fileIdentifier;
		}
		return backOneDirectory(fileIdentifier);
	}

	/**
	 * TODO
	 * 
	 * @param fileIdentifier
	 * @return
	 * @since 1.2.0
	 */
	public static String backOneDirectory(String fileIdentifier) {
		fileIdentifier = removeTrailingSeparator(fileIdentifier);
		fileIdentifier = fileIdentifier.substring(0, fileIdentifier.lastIndexOf(File.separator));
		return removeTrailingSeparator(fileIdentifier);
	}

	/**
	 * TODO
	 * 
	 * @param pomPath
	 * @return
	 * @since 1.2.0
	 */
	public static String removeTrailingSeparator(String pomPath) {
		while (pomPath.endsWith(File.separator)) {
			pomPath = pomPath.substring(0, pomPath.length() - 1);
		}
		return pomPath;
	}

	/**
	 * TODO
	 * 
	 * @param pomPath
	 * @return
	 * @since 1.2.0
	 */
	public static String removePrePostSeparator(String pomPath) {
		while (pomPath.endsWith(File.separator)) {
			pomPath = pomPath.substring(0, pomPath.length() - 1);
		}
		while (pomPath.startsWith(File.separator)) {
			pomPath = pomPath.substring(1, pomPath.length());
		}
		return pomPath;
	}

	/**
	 * TODO
	 * 
	 * @param pomPath
	 * @return
	 * @since 1.2.0
	 */
	public static String normalise(String pomPath) {
		return removeTrailingSeparator(pomPath) + File.separatorChar;
	}
	
	/**
	 * Returns an operating-system-dependent path consisting of the given
	 * elements, separated by {@link File#separator}.
	 * 
	 * @param pathElements the path elements from uppermost downwards (can't be empty)
	 * @return a non-blank string
	 * @since 1.2.0
	 */
	public static String getSystemDependentPath(final String... pathElements) {
		Assert.notEmpty(pathElements);
		return StringUtils.arrayToDelimitedString(pathElements, File.separator);
	}
	
	/**
	 * Constructor is private to prevent instantiation
	 * 
	 * @since 1.2.0
	 */
	private FileUtils() {}
}
