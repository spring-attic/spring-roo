package org.springframework.roo.project;

import java.io.File;
import java.util.List;

import org.springframework.roo.file.monitor.event.FileDetails;

/**
 * Allows resolution between {@link File}, {@link Path} and canonical path {@link String}s.
 * 
 * <p>
 * Add-ons should use this class as their primary mechanism to resolve paths in order to maximize
 * future compatibility with any design refactoring, project structural enhancements or alternate
 * build systems. Add-ons should generally avoid using {@link File} directly.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface PathResolver {

	/**
	 * Produces a canonical path for the presented {@link Path} and relative path.
	 * 
	 * @param path to use (required)
	 * @param relativePath to use (cannot be null, but may be empty if referring to the path itself)
	 * @return the canonical path to the file (never null)
	 */
	String getIdentifier(Path path, String relativePath);
	
	/**
	 * Attempts to determine which {@link Path} the specified canonical path falls under.
	 * 
	 * @param identifier to lookup (required)
	 * @return the {@link Path}, or null if the identifier refers to a location not under a know path.
	 */
	Path getPath(String identifier);
	
	/**
	 * Returns a canonical path that represents the root of the presented {@link Path}.
	 * 
	 * @param path to lookup (required)
	 * @return the root of the path (never null or empty)
	 */
	String getRoot(Path path);
	
	/**
	 * Attempts to determine which {@link Path} the specified canonical path falls under,
	 * and then returns the relative portion of the file name.
	 * 
	 * <p>
	 * See {@link FileDetails#getRelativeSegment(String)} for related information.
	 * 
	 * @param identifier to resolve (required)
	 * @return the relative segment (which may be an empty string if the identifier referred to the
	 * {@link Path} directly), or null if the identifier does not have a corresponding {@link Path}
	 */
	String getRelativeSegment(String identifier);
	
	/**
	 * Converts the presented canonical path into a human-friendly name.
	 * 
	 * @param identifier to resolve (required)
	 * @return a human-friendly name for the identifier (required)
	 */
	String getFriendlyName(String identifier);
	
	/**
	 * Indicates all the source code {@link Path}s known to this {@link PathResolver}.
	 * 
	 * <p>
	 * Whilst generally add-ons should know which paths contain source and which do not, this method
	 * abstracts add-ons from direct knowledge of available paths.
	 * 
	 * <p>
	 * By default this method will return, in the following order:
	 * <ul>
	 * <li>{@link Path#SRC_MAIN_JAVA}</li>
	 * <li>{@link Path#SRC_MAIN_RESOURCES}</li>
	 * <li>{@link Path#SRC_TEST_JAVA}</li>
	 * <li>{@link Path#SRC_TEST_RESOURCES}</li>
	 * </ul>
	 * 
	 * @return the paths, in order of compilation priority (never null and never empty)
	 */
	List<Path> getSourcePaths();
	
	/**
	 * Similar to {@link #getSourcePaths()}, but only returns {@link Path}s which are not compiled.
	 * 
	 * <p>
	 * By default this method will return, in the following order:
	 * <ul>
	 * <li>{@link Path#SRC_MAIN_WEBAPP}</li>
	 * <li>{@link Path#ROOT}</li>
	 * </ul>
	 * 
	 * @return the paths which are not compiled, in an order defined by the implementation (never null
	 * and never empty).
	 */
	List<Path> getNonSourcePaths();
	
	/**
	 * Returns all paths supported by the implementation, in an order defined by the implementation.
	 * 
	 * <p>
	 * By default this method will return, in the following order:
	 * <ul>
	 * <li>{@link Path#SRC_MAIN_JAVA}</li>
	 * <li>{@link Path#SRC_MAIN_RESOURCES}</li>
	 * <li>{@link Path#SRC_MAIN_WEBAPP}</li>
	 * <li>{@link Path#SRC_TEST_JAVA}</li>
	 * <li>{@link Path#SRC_TEST_RESOURCES}</li>
	 * <li>{@link Path#ROOT}</li>
	 * <li>{@link Path#SPRING_CONFIG_ROOT}</li>
	 * </ul>
	 * 
	 * @return all supported paths (never null and never empty)
	 */
	List<Path> getPaths();
}
