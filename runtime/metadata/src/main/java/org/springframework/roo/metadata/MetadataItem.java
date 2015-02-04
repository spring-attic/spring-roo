package org.springframework.roo.metadata;

/**
 * A piece of information about the user's project, typically obtained via the
 * {@link MetadataService}.
 * <p>
 * Implementations should be immutable.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface MetadataItem {

    /**
     * Returns the unique ID of this piece of metadata within the user's
     * project.
     * 
     * @return a non-blank ID that satisfies
     *         {@link MetadataIdentificationUtils#isIdentifyingInstance(String)}
     *         )
     */
    String getId();

    /**
     * Indicates whether this piece of metadata was successfully produced.
     * <p>
     * TODO Ben has suggested deprecating this method and having
     * {@link MetadataProvider}s simply return <code>null</code> if they can't
     * produce the metadata for some reason (some already do this). Callers
     * already have to check for null anyway, so requiring them to call this
     * method as well imposes an extra step that's easily missed.
     * 
     * @return <code>false</code> if for example some metadata on which it
     *         depends was unavailable.
     */
    boolean isValid();
}
