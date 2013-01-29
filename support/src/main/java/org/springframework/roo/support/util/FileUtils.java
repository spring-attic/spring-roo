package org.springframework.roo.support.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.support.ant.AntPathMatcher;
import org.springframework.roo.support.ant.PathMatcher;

/**
 * Utilities for handling {@link File} instances.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public final class FileUtils {

    private static final String BACKSLASH = "\\";

    /**
     * The relative file path to the current directory. Should be valid on all
     * platforms that Roo supports.
     */
    public static final String CURRENT_DIRECTORY = ".";

    private static final String ESCAPED_BACKSLASH = "\\\\";
    private static final PathMatcher PATH_MATCHER;

    static {
        PATH_MATCHER = new AntPathMatcher();
        ((AntPathMatcher) PATH_MATCHER).setPathSeparator(File.separator);
    }

    /**
     * Returns the given file system path minus its last element
     * 
     * @param fileIdentifier
     * @return
     * @since 1.2.0
     */
    public static String backOneDirectory(String fileIdentifier) {
        fileIdentifier = StringUtils.removeEnd(fileIdentifier, File.separator);
        fileIdentifier = fileIdentifier.substring(0,
                fileIdentifier.lastIndexOf(File.separator));
        return StringUtils.removeEnd(fileIdentifier, File.separator);
    }

    /**
     * Copies the specified source directory to the destination.
     * <p>
     * Both the source must exist. If the destination does not already exist, it
     * will be created. If the destination does exist, it must be a directory
     * (not a file).
     * 
     * @param source the already-existing source directory (required)
     * @param destination the destination directory (required)
     * @param deleteDestinationOnExit indicates whether to mark any created
     *            destinations for deletion on exit
     * @return true if the copy was successful
     */
    public static boolean copyRecursively(final File source,
            final File destination, final boolean deleteDestinationOnExit) {
        Validate.notNull(source, "Source directory required");
        Validate.notNull(destination, "Destination directory required");
        Validate.isTrue(source.exists(), "Source directory '%s' must exist",
                source);
        Validate.isTrue(source.isDirectory(),
                "Source directory '%s' must be a directory", source);
        if (destination.exists()) {
            Validate.isTrue(destination.isDirectory(),
                    "Destination directory '%s' must be a directory",
                    destination);
        }
        else {
            destination.mkdirs();
            if (deleteDestinationOnExit) {
                destination.deleteOnExit();
            }
        }
        for (final File s : source.listFiles()) {
            final File d = new File(destination, s.getName());
            if (deleteDestinationOnExit) {
                d.deleteOnExit();
            }
            if (s.isFile()) {
                try {
                    org.apache.commons.io.FileUtils.copyFile(s, d);
                }
                catch (final IOException ioe) {
                    return false;
                }
            }
            else {
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
     * Ensures that the given path has exactly one trailing
     * {@link File#separator}
     * 
     * @param path the path to modify (can't be <code>null</code>)
     * @return the normalised path
     * @since 1.2.0
     */
    public static String ensureTrailingSeparator(final String path) {
        Validate.notNull(path);
        return StringUtils.stripEnd(path, File.separator) + File.separatorChar;
    }

    /**
     * Returns the canonical path of the given {@link File}.
     * 
     * @param file the file for which to find the canonical path (can be
     *            <code>null</code>)
     * @return the canonical path, or <code>null</code> if a <code>null</code>
     *         file is given
     * @since 1.2.0
     */
    public static String getCanonicalPath(final File file) {
        if (file == null) {
            return null;
        }
        try {
            return file.getCanonicalPath();
        }
        catch (final IOException ioe) {
            throw new IllegalStateException(
                    "Cannot determine canonical path for '" + file + "'", ioe);
        }
    }

    /**
     * Returns the platform-specific file separator as a regular expression.
     * 
     * @return a non-blank regex
     * @since 1.2.0
     */
    public static String getFileSeparatorAsRegex() {
        final String fileSeparator = File.separator;
        if (fileSeparator.contains(BACKSLASH)) {
            // Escape the backslashes
            return fileSeparator.replace(BACKSLASH, ESCAPED_BACKSLASH);
        }
        return fileSeparator;
    }

    /**
     * Returns the part of the given path that represents a directory, in other
     * words the given path if it's already a directory, or the parent directory
     * if it's a file.
     * 
     * @param fileIdentifier the path to parse (required)
     * @return see above
     * @since 1.2.0
     */
    public static String getFirstDirectory(String fileIdentifier) {
        fileIdentifier = StringUtils.stripEnd(fileIdentifier, File.separator);
        if (new File(fileIdentifier).isDirectory()) {
            return fileIdentifier;
        }
        return backOneDirectory(fileIdentifier);
    }

    //
    /**
     * Loads the given file from the classpath.
     * 
     * @param loadingClass the class from whose package to load the file
     *            (required)
     * @param filename the name of the file to load, relative to that package
     *            (required)
     * @return the file's input stream (never <code>null</code>)
     * @throws NullPointerException if the given file cannot be found
     */
    public static InputStream getInputStream(final Class<?> loadingClass,
            final String filename) {
        final InputStream inputStream = loadingClass
                .getResourceAsStream(filename);
        Validate.notNull(inputStream,
                "Could not locate '%s' in classpath of %s", filename,
                loadingClass.getName());
        return inputStream;
    }

    /**
     * Determines the path to the requested file, relative to the given class.
     * 
     * @param loadingClass the class to whose package the given file is relative
     *            (required)
     * @param relativeFilename the name of the file relative to that package
     *            (required)
     * @return the full classloader-specific path to the file (never
     *         <code>null</code>)
     * @since 1.2.0
     */
    public static String getPath(final Class<?> loadingClass,
            final String relativeFilename) {
        Validate.notNull(loadingClass, "Loading class required");
        Validate.notBlank(relativeFilename, "Filename required");
        Validate.isTrue(!relativeFilename.startsWith("/"),
                "Filename shouldn't start with a slash");
        // Slashes instead of File.separatorChar is correct here, as these are
        // classloader paths (not file system paths)
        return "/" + loadingClass.getPackage().getName().replace('.', '/')
                + "/" + relativeFilename;
    }

    /**
     * Returns an operating-system-dependent path consisting of the given
     * elements, separated by {@link File#separator}.
     * 
     * @param pathElements the path elements from uppermost downwards (can't be
     *            empty)
     * @return a non-blank string
     * @since 1.2.0
     */
    public static String getSystemDependentPath(
            final Collection<String> pathElements) {
        Validate.notEmpty(pathElements);
        return StringUtils.join(pathElements, File.separator);
    }

    /**
     * Returns an operating-system-dependent path consisting of the given
     * elements, separated by {@link File#separator}.
     * 
     * @param pathElements the path elements from uppermost downwards (can't be
     *            empty)
     * @return a non-blank string
     * @since 1.2.0
     */
    public static String getSystemDependentPath(final String... pathElements) {
        Validate.notEmpty(pathElements);
        return getSystemDependentPath(Arrays.asList(pathElements));
    }

    //
    /**
     * Indicates whether the given canonical path matches the given Ant-style
     * pattern
     * 
     * @param antPattern the pattern to check against (can't be blank)
     * @param canonicalPath the path to check (can't be blank)
     * @return see above
     * @since 1.2.0
     */
    public static boolean matchesAntPath(final String antPattern,
            final String canonicalPath) {
        Validate.notBlank(antPattern, "Ant pattern required");
        Validate.notBlank(canonicalPath, "Canonical path required");
        return PATH_MATCHER.match(antPattern, canonicalPath);
    }

    /**
     * Constructor is private to prevent instantiation
     * 
     * @since 1.2.0
     */
    private FileUtils() {
    }
}
