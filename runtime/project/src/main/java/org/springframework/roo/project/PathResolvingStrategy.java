package org.springframework.roo.project;

import java.util.Collection;

import org.springframework.roo.model.JavaType;

/**
 * A strategy for resolving logical {@link Path}s to physical file system
 * locations.
 * 
 * @author James Tyrrell
 * @since 1.2.0
 */
public interface PathResolvingStrategy {

    /**
     * @param path the focus path
     * @param javaType the type t
     * @return
     */
    String getCanonicalPath(LogicalPath path, JavaType javaType);

    /**
     * @param path
     * @param javaType
     * @return
     */
    String getFocusedCanonicalPath(Path path, JavaType javaType);

    /**
     * @param path
     * @param relativePath
     * @return
     */
    String getFocusedIdentifier(Path path, String relativePath);

    /**
     * @see PathResolver#getFocusedPath(Path)
     */
    LogicalPath getFocusedPath(Path path);

    /**
     * @param path
     * @return
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
     * Produces a canonical path for the presented {@link LogicalPath} and
     * relative path.
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
     * @see PathResolver#getPaths()
     */
    Collection<LogicalPath> getPaths();

    /**
     * Attempts to determine which {@link Path} the specified canonical path
     * falls under, and then returns the relative portion of the file name.
     * <p>
     * See
     * {@link org.springframework.roo.file.monitor.event.FileDetails#getRelativeSegment(String)}
     * for related information.
     * 
     * @param identifier to resolve (required)
     * @return the relative segment (which may be an empty string if the
     *         identifier referred to the {@link Path} directly), or null if the
     *         identifier does not have a corresponding {@link Path}
     */
    String getRelativeSegment(String identifier);

    /**
     * @return the directory where Roo was launched
     */
    String getRoot();

    /**
     * @see PathResolver#getRoot(LogicalPath)
     */
    String getRoot(LogicalPath path);

    /**
     * @see PathResolver#getSourcePaths()
     */
    Collection<LogicalPath> getSourcePaths();

    /**
     * Indicates whether this strategy is active. The {@link PathResolver} will
     * typically expect exactly one strategy to be active at a time.
     * 
     * @return see above
     */
    boolean isActive();
}
