package org.springframework.roo.metadata;

import org.apache.commons.lang3.Validate;

/**
 * Abstract implementation of {@link MetadataItem}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public abstract class AbstractMetadataItem implements MetadataItem {

    /**
     * Private to reinforce contractual immutability and formatting requirements
     */
    private final String id;

    /**
     * Defaults to true; protected to simplify superclass direct field
     * modification
     */
    protected boolean valid = true;

    /**
     * Constructs an {@link AbstractMetadataItem} with the specified identifier,
     * defaulting the {@link #isValid()} to true.
     * 
     * @param id the metadata identification string for a particular instance
     *            (must return true when presented to
     *            {@link MetadataIdentificationUtils#isIdentifyingInstance(String)}
     *            )
     */
    protected AbstractMetadataItem(final String id) {
        Validate.isTrue(MetadataIdentificationUtils.isIdentifyingInstance(id),
                "Metadata identification string '" + id
                        + "' does not identify a metadata instance");
        this.id = id;
    }

    public final String getId() {
        return id;
    }

    public final boolean isValid() {
        return valid;
    }
}
