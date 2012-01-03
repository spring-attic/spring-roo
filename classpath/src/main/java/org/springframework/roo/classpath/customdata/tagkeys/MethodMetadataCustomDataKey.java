package org.springframework.roo.classpath.customdata.tagkeys;

import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.support.util.Assert;

/**
 * {@link MethodMetadata}-specific implementation of
 * {@link InvocableMemberMetadataCustomDataKey}.
 * 
 * @author James Tyrrell
 * @since 1.1.3
 */
public class MethodMetadataCustomDataKey extends
        InvocableMemberMetadataCustomDataKey<MethodMetadata> {

    // Fields
    private final String tag;

    /**
     * Constructor
     * 
     * @param tag
     */
    public MethodMetadataCustomDataKey(final String tag) {
        Assert.hasText(tag, "Invalid tag '" + tag + "'");
        this.tag = tag;
    }

    @Override
    public String toString() {
        return tag;
    }

    public String name() {
        return tag;
    }
}
