package org.springframework.roo.project;

import java.io.File;
import java.util.Collection;

import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.model.JavaType;

/**
 * Allows resolution between {@link File}, {@link Path} and canonical path
 * {@link String}s.
 * <p>
 * Add-ons should use this class as their primary mechanism to resolve paths in
 * order to maximize future compatibility with any design refactoring, project
 * structural enhancements or alternate build systems. Add-ons should generally
 * avoid using {@link File} directly.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface PathResolver {

    /**
     * Returns the canonical path to the given {@link JavaType} within the given
     * {@link LogicalPath}
     * 
     * @param path the path to the type's base package (required)
     * @param javaType the type for which to get the path (required)
     * @return a valid canonical path (N.B. this file might not exist)
     * @since 1.2.0
     */
    String getCanonicalPath(LogicalPath path, JavaType javaType);

    /**
     * Returns the canonical path of the given {@link JavaType} in the given
     * {@link Path} of the currently focused module.
     * 
     * @param path
     * @param javaType
     * @return
     * @since 1.2.0
     */
    String getFocusedCanonicalPath(Path path, JavaType javaType);

    /**
     * Returns the canonical path of the given path relative to the given
     * {@link Path} of the currently focused module.
     * 
     * @param path
     * @param relativePath
     * @return a canonical path
     * @since 1.2.0
     */
    String getFocusedIdentifier(Path path, String relativePath);

    /**
     * Returns the {@link LogicalPath} for the given {@link Path} within the
     * currently focused module.
     * 
     * @param path the path within the currently focused module (required)
     * @return a non-<code>null</code> instance
     * @since 1.2.0
     */
    LogicalPath getFocusedPath(Path path);

    /**
     * @param path
     * @return
     * @since 1.2.0
     */
    String getFocusedRoot(Path path);

    /**
     * Converts the presented canonical path into a human-friendly name.
     * 
     * @param identifier to resolve (required)
     * @return a human-friendly name for the identifier (required)
     */
    String getFriendlyName(String identifier);

    /**
     * Produces a canonical path for the presented {@link Path} and relative
     * path.
     * 
     * @param path to use (required)
     * @param relativePath to use (cannot be null, but may be empty if referring
     *            to the path itself)
     * @return the canonical path to the file (never null)
     */
    String getIdentifier(LogicalPath path, String relativePath);

    /**
     * Attempts to determine which {@link Path} the specified canonical path
     * falls under.
     * 
     * @param identifier to lookup (required)
     * @return the {@link Path}, or null if the identifier refers to a location
     *         not under a know path.
     */
    LogicalPath getPath(String identifier);

    /**
     * Returns all known paths within the user project.
     * 
     * @return a non-<code>null</code> list
     */
    Collection<LogicalPath> getPaths();

    /**
     * Attempts to determine which {@link Path} the specified canonical path
     * falls under, and then returns the relative portion of the file name.
     * <p>
     * See {@link FileDetails#getRelativeSegment(String)} for related
     * information.
     * 
     * @param identifier to resolve (required)
     * @return the relative segment (which may be an empty string if the
     *         identifier referred to the {@link Path} directly), or null if the
     *         identifier does not have a corresponding {@link Path}
     */
    String getRelativeSegment(String identifier);

    /**
     * Returns the canonical path of the user project's root directory
     * 
     * @return a valid directory path
     */
    String getRoot();

    /**
     * Returns the canonical path of the root of the given {@link LogicalPath}.
     * 
     * @param path to lookup (required)
     * @return <code>null</code> if the root path cannot be determined
     */
    String getRoot(LogicalPath path);

    /**
     * Returns the {@link LogicalPath}s of user project directories that can
     * contain Java source code.
     * 
     * @return a non-<code>null</code> list
     */
    Collection<LogicalPath> getSourcePaths();
}
