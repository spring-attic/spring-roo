package org.springframework.roo.metadata.internal;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.metadata.MetadataCache;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataItem;

/**
 * Basic {@link MetadataCache} that stores elements on a least recently used
 * (LRU) basis.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public abstract class AbstractMetadataCache implements MetadataCache {

    private static final float hashTableLoadFactor = 0.75f;

    private LinkedHashMap<String, MetadataItem> map;
    private int maxCapacity = 100000;

    protected AbstractMetadataCache() {
        init();
    }

    public void evict(final String metadataIdentificationString) {
        Validate.isTrue(MetadataIdentificationUtils
                .isIdentifyingInstance(metadataIdentificationString),
                "Only metadata instances can be cached (not '%s')",
                metadataIdentificationString);
        map.remove(metadataIdentificationString);
    }

    public void evictAll() {
        init();
    }

    protected int getCacheSize() {
        return map.size();
    }

    protected MetadataItem getFromCache(
            final String metadataIdentificationString) {
        Validate.isTrue(MetadataIdentificationUtils
                .isIdentifyingInstance(metadataIdentificationString),
                "Only metadata instances can be cached (not '%s')",
                metadataIdentificationString);
        return map.get(metadataIdentificationString);
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    private void init() {
        final int hashTableCapacity = (int) Math.ceil(maxCapacity
                / hashTableLoadFactor) + 1;
        map = new LinkedHashMap<String, MetadataItem>(hashTableCapacity,
                hashTableLoadFactor, true) {
            private static final long serialVersionUID = 1;

            @Override
            protected boolean removeEldestEntry(
                    final Map.Entry<String, MetadataItem> eldest) {
                return size() > maxCapacity;
            }
        };
    }

    public void put(final MetadataItem metadataItem) {
        Validate.notNull(metadataItem, "A metadata item is required");
        map.put(metadataItem.getId(), metadataItem);
    }

    public void setMaxCapacity(int maxCapacity) {
        if (maxCapacity < 100) {
            maxCapacity = 100;
        }
        this.maxCapacity = maxCapacity;
        init();
    }
}
