package org.springframework.roo.metadata.internal;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.roo.metadata.MetadataCache;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.support.util.Assert;

/**
 * Basic {@link MetadataCache} that stores elements on a least recently used (LRU) basis.
 *
 * @author Ben Alex
 * @since 1.0
 */
public abstract class AbstractMetadataCache implements MetadataCache {

	// Constants
	private static final float hashTableLoadFactor = 0.75f;

	// Fields
	private LinkedHashMap<String,MetadataItem> map;
	private int maxCapacity = 100000;

	protected AbstractMetadataCache() {
		init();
	}

	protected int getCacheSize() {
		return map.size();
	}

	public void setMaxCapacity(int maxCapacity) {
		if (maxCapacity < 100) {
			maxCapacity = 100;
		}
		this.maxCapacity = maxCapacity;
		init();
	}

	public int getMaxCapacity() {
		return maxCapacity;
	}

	public void put(final MetadataItem metadataItem) {
		Assert.notNull(metadataItem, "A metadata item is required");
		this.map.put(metadataItem.getId(), metadataItem);
	}

	protected MetadataItem getFromCache(final String metadataIdentificationString) {
		Assert.isTrue(MetadataIdentificationUtils.isIdentifyingInstance(metadataIdentificationString), "Only metadata instances can be cached (not '" + metadataIdentificationString + "')");
		return this.map.get(metadataIdentificationString);
	}

	public void evict(final String metadataIdentificationString) {
		Assert.isTrue(MetadataIdentificationUtils.isIdentifyingInstance(metadataIdentificationString), "Only metadata instances can be cached (not '" + metadataIdentificationString + "')");
		this.map.remove(metadataIdentificationString);
	}

	public void evictAll() {
		init();
	}

	private void init() {
		int hashTableCapacity = (int) Math.ceil(maxCapacity / hashTableLoadFactor) + 1;
		map = new LinkedHashMap<String,MetadataItem>(hashTableCapacity, hashTableLoadFactor, true) {
			private static final long serialVersionUID = 1;
			@Override
			protected boolean removeEldestEntry(final Map.Entry<String,MetadataItem> eldest) {
				return size() > maxCapacity;
			}
		};
	}
}
