package org.springframework.roo.classpath.customdata.tagkeys;

import org.springframework.roo.classpath.details.ConstructorMetadata;

/**
 * {@link ConstructorMetadata}-specific implementation of
 * {@link InvocableMemberMetadataCustomDataKey}.
 * 
 * @author James Tyrrell
 * @since 1.1.3
 */
public class ConstructorMetadataCustomDataKey extends
        InvocableMemberMetadataCustomDataKey<ConstructorMetadata> {
    private final String name;

    public ConstructorMetadataCustomDataKey(final String name) {
        this.name = name;
    }

    @Override
    public boolean meets(final ConstructorMetadata constructor) {
        return super.meets(constructor);
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
