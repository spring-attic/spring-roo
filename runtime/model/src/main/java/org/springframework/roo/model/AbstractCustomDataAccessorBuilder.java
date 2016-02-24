package org.springframework.roo.model;

import org.apache.commons.lang3.Validate;

/**
 * Assists in the creation of a {@link Builder} for types that eventually
 * implement {@link CustomDataAccessor}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public abstract class AbstractCustomDataAccessorBuilder<T extends CustomDataAccessor>
        implements Builder<T> {

    private CustomDataBuilder customDataBuilder;

    /**
     * Constructor for an empty builder
     */
    protected AbstractCustomDataAccessorBuilder() {
        this.customDataBuilder = new CustomDataBuilder();
    }

    /**
     * Constructor for a builder initialised with the given custom data
     * 
     * @param existing can't be <code>null</code>
     */
    protected AbstractCustomDataAccessorBuilder(
            final CustomDataAccessor existing) {
        Validate.notNull(existing, "Custom data accessor required");
        this.customDataBuilder = new CustomDataBuilder(existing.getCustomData());
    }

    /**
     * Appends the given custom data to this builder
     * 
     * @param customDataBuilder the custom data to append; can be
     *            <code>null</code> to make no changes
     */
    public void append(final CustomData customData) {
        if (customData != null) {
            // Set the custom data builder to a new instance containing both
            // builders' values
            final CustomDataBuilder customDataBuilder = new CustomDataBuilder(
                    customData);
            customDataBuilder.append(this.customDataBuilder.build());
            this.customDataBuilder = customDataBuilder;
        }
    }

    public CustomDataBuilder getCustomData() {
        return this.customDataBuilder;
    }

    public Object putCustomData(final Object key, final Object value) {
        return customDataBuilder.put(key, value);
    }

    /**
     * Sets this builder's {@link CustomDataBuilder} to the given one (does not
     * take a copy)
     * 
     * @param customDataBuilder the builder to set (required)
     */
    public void setCustomData(final CustomDataBuilder customDataBuilder) {
        Validate.notNull(customDataBuilder, "Custom data builder required");
        this.customDataBuilder = customDataBuilder;
    }
}
