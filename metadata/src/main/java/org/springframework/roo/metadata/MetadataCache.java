package org.springframework.roo.metadata;

/**
 * Indicates a cache is maintained by the implementation.
 * <p>
 * A cache is a "best effort" cache and does not need to guarantee to include
 * every item of metadata. Implementations should take care to ensure excessive
 * memory consumption does not occur as a result of their operation. It is
 * recommended that least recently used metadata instances are automatically
 * removed should consumption exceed an implementation-defined threshold.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface MetadataCache {

    /**
     * Evicts the specified metadata instance from the cache.
     * <p>
     * The identification string must return true if presented to
     * {@link MetadataIdentificationUtils#isIdentifyingInstance(String)}.
     * <p>
     * If the metadata instance does not presently exist in the cache, this is
     * considered a non-fatal event and the method will simply return.
     * 
     * @param metadataIdentificationString to evict (mandatory and must refer to
     *            a specific item instance)
     */
    void evict(String metadataIdentificationString);

    /**
     * Evicts every item from the cache.
     * <p>
     * This method can be used during reload/refresh-style operations or where
     * there is a requirement to guarantee cache consistency.
     */
    void evictAll();

    /**
     * Eagerly inserts an item into the cache. ONLY SPRING ROO INFRASTRUCTURE
     * SHOULD INVOKE THIS METHOD. Do not invoke this method from add-ons, as the
     * caching semantics are likely to be modified in the future and you should
     * not rely on this method remaining.
     * 
     * @param metadataItem an instance-identifying metadata item to insert
     *            (required)
     */
    void put(MetadataItem metadataItem);

    /**
     * Modifies the metadata cache maximum capacity.
     * 
     * @param maxCapacity the new maximum capacity
     */
    void setMaxCapacity(int maxCapacity);
}
