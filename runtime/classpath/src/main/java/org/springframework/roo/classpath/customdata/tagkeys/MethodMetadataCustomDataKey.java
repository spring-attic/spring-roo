package org.springframework.roo.classpath.customdata.tagkeys;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.details.MethodMetadata;

/**
 * {@link MethodMetadata}-specific implementation of
 * {@link InvocableMemberMetadataCustomDataKey}.
 * 
 * @author James Tyrrell
 * @since 1.1.3
 */
public class MethodMetadataCustomDataKey extends
        InvocableMemberMetadataCustomDataKey<MethodMetadata> {

    private final String tag;

    /**
     * Constructor
     * 
     * @param tag
     */
    public MethodMetadataCustomDataKey(final String tag) {
        Validate.notBlank(tag, "Invalid tag '%s'", tag);
        this.tag = tag;
    }

    public String name() {
        return tag;
    }

    @Override
    public String toString() {
        return tag;
    }
}
