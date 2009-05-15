package org.springframework.roo.support.util;

import java.io.File;
import java.io.IOException;

/**
 * Utilities for handling {@link File} instances.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public abstract class FileUtils {

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
	
}
