package org.springframework.roo.metadata.internal;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.roo.metadata.MetadataCache;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataNotificationListener;
import org.springframework.roo.metadata.MetadataProvider;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Default implementation of {@link MetadataService}.
 * 
 * <p>
 * This implementation is not thread safe. It should only be accessed by a single thread at a time.
 * This is enforced by the process manager semantics, so we avoid the cost of re-synchronization here.
 *
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public final class DefaultMetadataService extends AbstractMetadataCache implements MetadataService {

	/** key: metadata identification string for a class; value: the provider */
	private Map<String, MetadataProvider> providers = new HashMap<String, MetadataProvider>();
	private SortedSet<MetadataProvider> sortedProviders = new TreeSet<MetadataProvider>(new Comparator<MetadataProvider>() {
		public int compare(MetadataProvider o1, MetadataProvider o2) {
			return o1.getClass().getName().compareTo(o2.getClass().getName());
		}
	});
	
	private MetadataDependencyRegistry metadataDependencyRegistry;
	
	private int validGets = 0;
	private int cachePuts = 0;
	private int cacheHits = 0;
	private int cacheMisses = 0;
	private int cacheEvictions = 0;
	
	public DefaultMetadataService(MetadataDependencyRegistry metadataDependencyRegistry) {
		Assert.notNull(metadataDependencyRegistry, "Metadata dependency registry required");
		metadataDependencyRegistry.addNotificationListener(this);
		this.metadataDependencyRegistry = metadataDependencyRegistry;
	}
	
	public void register(MetadataProvider provider) {
		Assert.notNull(provider, "Metadata provider required");
		String mid = provider.getProvidesType();
		Assert.isTrue(MetadataIdentificationUtils.isIdentifyingClass(mid), "Metadata provider '" + provider + "' violated interface contract by returning '" + mid + "'");
		Assert.isTrue(!providers.containsKey(mid), "Metadata provider '" + providers.get(mid) + "' already is providing metadata for '" + mid + "'");
		providers.put(mid, provider);
		sortedProviders.add(provider);
	}

	public void deregister(String metadataIdentificationString) {
		Assert.isTrue(MetadataIdentificationUtils.isIdentifyingClass(metadataIdentificationString), "Metadata identification string '" + metadataIdentificationString + "' does not identify a class");
		MetadataProvider existing = providers.get(metadataIdentificationString);
		if (existing != null) {
			providers.remove(metadataIdentificationString);
			sortedProviders.remove(existing);
		}
	}
	
	public SortedSet<MetadataProvider> getRegisteredProviders() {
		return Collections.unmodifiableSortedSet(sortedProviders);
	}
	
	public MetadataItem get(String metadataIdentificationString, boolean evictCache) {
		Assert.isTrue(MetadataIdentificationUtils.isIdentifyingInstance(metadataIdentificationString), "Metadata identification string '" + metadataIdentificationString + "' does not identify a metadata instance");
		validGets++;
		
		if (!evictCache) {
			// Try the cache first
			MetadataItem result = getFromCache(metadataIdentificationString);
			if (result != null) {
				cacheHits++;
				return result;
			}
		}
		
		cacheMisses++;

		// Get the destination
		String mdClassId = MetadataIdentificationUtils.create(MetadataIdentificationUtils.getMetadataClass(metadataIdentificationString));
		MetadataProvider p = providers.get(mdClassId);
		Assert.notNull(p, "No metadata provider is currently registered to provide metadata for identifier '" + metadataIdentificationString + "'");
		
		// Appears to be a valid key with a valid provider, so let's do some eviction if requested
		if (evictCache) {
			evict(metadataIdentificationString);
			cacheEvictions++;
		}
		
		// Obtain the item
		MetadataItem result = p.get(metadataIdentificationString);
		
		// If the item isn't available, evict it from the cache unless we did so already
		if (result == null && !evictCache) {
			evict(metadataIdentificationString);
			cacheEvictions++;
		}
		
		// Put into the cache, provided it isn't null
		if (result != null) {
			super.putInCache(metadataIdentificationString, result);
			cachePuts++;
		}
		
		return result;
	}

	public MetadataItem get(String metadataIdentificationString) {
		return get(metadataIdentificationString, false);
	}

	public MetadataProvider getRegisteredProvider(String metadataIdentificationString) {
		Assert.isTrue(MetadataIdentificationUtils.isIdentifyingClass(metadataIdentificationString), "Metadata identification string '" + metadataIdentificationString + "' does not identify a class");
		return providers.get(metadataIdentificationString);
	}

	public void notify(String upstreamDependency, String downstreamDependency) {
		Assert.isTrue(MetadataIdentificationUtils.isValid(upstreamDependency), "Upstream dependency is an invalid metadata identification string ('" + upstreamDependency + "')");
		Assert.isTrue(MetadataIdentificationUtils.isValid(downstreamDependency), "Downstream dependency is an invalid metadata identification string ('" + downstreamDependency + "')");
		
		// Get the destination
		String mdClassId = MetadataIdentificationUtils.create(MetadataIdentificationUtils.getMetadataClass(downstreamDependency));
		MetadataProvider p = providers.get(mdClassId);
		
		if (p == null) {
			// No known provider that can consume this notification, so just return as per the interface contract
			return;
		}
		
		if (p instanceof MetadataNotificationListener) {
			// The provider can directly handle this notification, so we just need to delegate directly to it.
			// We rely on the provider to evict items from the cache if applicable.
			((MetadataNotificationListener)p).notify(upstreamDependency, downstreamDependency);
		} else {
			// As per interface contract, we just ensure we evict the item and recreate it
			// However, we only do this if the destination is an instance - if it's a class, "get" is not a meaningful operation.
			if (MetadataIdentificationUtils.isIdentifyingInstance(downstreamDependency)) {
				get(downstreamDependency, true);
			}
			// As per interface contract, we now notify any listeners this downstream instance has probably now changed
			metadataDependencyRegistry.notifyDownstream(downstreamDependency);
		}
	}

	public void evict(String metadataIdentificationString) {
		// Clear my own cache (which also verifies the argument is valid at the same time)
		super.evict(metadataIdentificationString);
		
		// Delegate to the relevant metadata provider, if available
		String mdClassId = MetadataIdentificationUtils.getMetadataClass(metadataIdentificationString);
		MetadataProvider p = providers.get(mdClassId);
		
		if (p != null && p instanceof MetadataCache) {
			((MetadataCache)p).evict(metadataIdentificationString);
		}
	}

	public void evictAll() {
		// Clear my own cache
		super.evictAll();
		
		// Clear the caches of any metadata providers which support the interface
		for (MetadataProvider p : providers.values()) {
			if (p instanceof MetadataCache) {
				((MetadataCache)p).evictAll();
			}
		}
	}

	public final String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("providers", providers.size());
		tsc.append("validGets", validGets);
		tsc.append("cachePuts", cachePuts);
		tsc.append("cacheHits", cacheHits);
		tsc.append("cacheMisses", cacheMisses);
		tsc.append("cacheEvictions", cacheEvictions);
		tsc.append("cacheCurrentSize", getCacheSize());
		tsc.append("cacheMaximumSize", getMaxCapacity());
		return tsc.toString();
	}
	
}
