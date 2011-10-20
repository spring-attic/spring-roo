package org.springframework.roo.project;

import java.util.List;

import org.springframework.roo.model.JavaType;

public interface PathResolvingStrategy {

	/**
	 * Produces a canonical path for the presented {@link Path} and relative path.
	 *
	 * @param path to use (required)
	 * @param relativePath to use (cannot be null, but may be empty if referring to the path itself)
	 * @return the canonical path to the file (never null)
	 */
	String getIdentifier(ContextualPath path, String relativePath);

	/**
	 * Attempts to determine which {@link Path} the specified canonical path falls under.
	 *
	 * @param identifier to lookup (required)
	 * @return the {@link Path}, or null if the identifier refers to a location not under a know path.
	 */
	ContextualPath getPath(String identifier);

	/**
	 * Returns a canonical path that represents the root of the presented {@link Path}.
	 *
	 * @param path to lookup (required)
	 * @return the root of the path (never null or empty)
	 */
	String getRoot(ContextualPath path);

	/**
	 * Attempts to determine which {@link Path} the specified canonical path falls under,
	 * and then returns the relative portion of the file name.
	 *
	 * <p>
	 * See {@link org.springframework.roo.file.monitor.event.FileDetails#getRelativeSegment(String)} for related information.
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
	List<ContextualPath> getSourcePaths();

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
	List<ContextualPath> getNonSourcePaths();

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
	List<ContextualPath> getPaths();

	/**
	 *
	 * @return true if this strategy is not active
	 */
	boolean inactive();

	/**
	 * @return the directory where Roo was launched
	 */
	String getRoot();

	/**
	 *
	 * @param path the focus path
	 * @param javaType the type t
	 * @return
	 */
	String getCanonicalPath(ContextualPath path, JavaType javaType);

	/**
	 *
	 * @param path
	 * @param relativePath
	 * @return
	 */
	String getFocusedIdentifier(Path path, String relativePath);

	/**
	 *
	 * @param path
	 * @return
	 */
	String getFocusedRoot(Path path);

	/**
	 *
	 * @param path
	 * @return
	 */
	ContextualPath getFocusedPath(Path path);

	/**
	 *
	 * @param path
	 * @param javaType
	 * @return
	 */
	String getFocusedCanonicalPath(Path path, JavaType javaType);
}
