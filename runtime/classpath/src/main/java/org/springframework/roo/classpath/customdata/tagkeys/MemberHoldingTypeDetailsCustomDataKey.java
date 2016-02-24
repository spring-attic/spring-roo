package org.springframework.roo.classpath.customdata.tagkeys;

import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.model.CustomDataKey;

/**
 * {@link MemberHoldingTypeDetails}-specific implementation of
 * {@link org.springframework.roo.model.CustomDataKey}.
 * 
 * @author James Tyrrell
 * @since 1.1.3
 */
public class MemberHoldingTypeDetailsCustomDataKey implements
        CustomDataKey<MemberHoldingTypeDetails> {
    private final String name;

    public MemberHoldingTypeDetailsCustomDataKey(final String name) {
        this.name = name;
    }

    public boolean meets(final MemberHoldingTypeDetails memberHoldingTypeDetails)
            throws IllegalStateException {
        return true;
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
