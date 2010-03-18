package org.springframework.roo.file.undo;

import java.io.File;

/**
 * Interface to support {@link UndoableOperation} implementations rendering log messages with filename
 * conventions applicable to the caller.
 * 
 * <p>
 * This interface is primarily intended to allow more meaningful paths to be displayed than those
 * available directly via {@link File}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface FilenameResolver {
	/**
	 * Resolves the presented {@link File} into a meaningful name for display purposes.
	 * 
	 * @param file to resolve (required)
	 * @return a string-based representation of the file name (never null or empty)
	 */
	String getMeaningfulName(File file);
}
