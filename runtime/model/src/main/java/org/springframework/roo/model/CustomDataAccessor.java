package org.springframework.roo.model;

/**
 * Allows custom data to be stored against various Spring Roo types.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public interface CustomDataAccessor {

    /**
     * Provides immutable access to the custom data stored against the instance.
     * 
     * @return the custom data (never returns null)
     */
    CustomData getCustomData();
}
