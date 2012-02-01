package org.springframework.roo.model;

import org.apache.commons.lang3.Validate;

/**
 * Convenience superclass for {@link CustomDataAccessor} implementations.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public abstract class AbstractCustomDataAccessorProvider implements
        CustomDataAccessor {

    private final CustomData customData;

    /**
     * Constructor
     * 
     * @param customData
     */
    protected AbstractCustomDataAccessorProvider(final CustomData customData) {
        Validate.notNull(customData, "Custom data required");
        this.customData = customData;
    }

    public final CustomData getCustomData() {
        return customData;
    }
}
