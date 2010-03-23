package org.springframework.roo.metadata;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.metadata.internal.AbstractMetadataCache;
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
@Component
@Service
@Reference(name="metadataProvider", strategy=ReferenceStrategy.LOOKUP, policy=ReferencePolicy.DYNAMIC, referenceInterface=MetadataProvider.class, cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE)
public class DefaultMetadataService extends AbstractMetadataCache implements MetadataService {

	@Reference
	private MetadataDependencyRegistry metadataDependencyRegistry;
	private ComponentContext context;
	
	protected void activate(ComponentContext context) {
		this.context = context;
		metadataDependencyRegistry.addNotificationListener(this);
	}
	
	private int validGets = 0;
	private int cachePuts = 0;
	private int cacheHits = 0;
	private int cacheMisses = 0;
	private int cacheEvictions = 0; 
	
	/**
	 * Registers the provider into either the passed providersMap or sortedProviders set (or both).
	 * 
	 * @param provider
	 * @param providerMap
	 * @param sortedProviders
	 */
	private void register(MetadataProvider provider, Map<String, MetadataProvider> providerMap, SortedSet<MetadataProvider> sortedProviders) {
		Assert.notNull(provider, "Metadata provider required");
		String mid = provider.getProvidesType();
		Assert.isTrue(MetadataIdentificationUtils.isIdentifyingClass(mid), "Metadata provider '" + provider + "' violated interface contract by returning '" + mid + "'");
		if (providerMap != null) {
			Assert.isTrue(!providerMap.containsKey(mid), "Metadata provider '" + providerMap.get(mid) + "' already is providing metadata for '" + mid + "'");
		}
		if (providerMap != null) {
			providerMap.put(mid, provider);
		}
		if (sortedProviders != null) {
			sortedProviders.add(provider);
		}
	}
	
	public SortedSet<MetadataProvider> getRegisteredProviders() {
		SortedSet<MetadataProvider> sortedProviders = new TreeSet<MetadataProvider>(new Comparator<MetadataProvider>() {
			public int compare(MetadataProvider o1, MetadataProvider o2) {
				return o1.getClass().getName().compareTo(o2.getClass().getName());
			}
		});

		Object[] objs = context.locateServices("metadataProvider");
		if (objs != null) {
			for (Object found : objs) {
				register((MetadataProvider) found, null, sortedProviders);
			}
		}
		
		return Collections.unmodifiableSortedSet(sortedProviders);
	}
	
	public MetadataProvider getRegisteredProvider(String metadataIdentificationString) {
		Assert.isTrue(MetadataIdentificationUtils.isValid(metadataIdentificationString), "Metadata identification string '" + metadataIdentificationString + "' is not valid");
		Assert.isTrue(MetadataIdentificationUtils.isIdentifyingClass(metadataIdentificationString), "Metadata identification string '" + metadataIdentificationString + "' does not identify a class");
		Map<String, MetadataProvider> providerMap = new HashMap<String, MetadataProvider>();

		Object[] objs = context.locateServices("metadataProvider");
		if (objs != null) {
			for (Object found : objs) {
				register((MetadataProvider) found, providerMap, null);
			}
		}
		return providerMap.get(metadataIdentificationString);
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
		MetadataProvider p = getRegisteredProvider(mdClassId);
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

	public void notify(String upstreamDependency, String downstreamDependency) {
		Assert.isTrue(MetadataIdentificationUtils.isValid(upstreamDependency), "Upstream dependency is an invalid metadata identification string ('" + upstreamDependency + "')");
		Assert.isTrue(MetadataIdentificationUtils.isValid(downstreamDependency), "Downstream dependency is an invalid metadata identification string ('" + downstreamDependency + "')");
		
		// Get the destination
		String mdClassId = MetadataIdentificationUtils.create(MetadataIdentificationUtils.getMetadataClass(downstreamDependency));
		MetadataProvider p = getRegisteredProvider(mdClassId);
		
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
		String mdClassId = MetadataIdentificationUtils.create(MetadataIdentificationUtils.getMetadataClass(metadataIdentificationString));
		MetadataProvider p = getRegisteredProvider(mdClassId);
		
		if (p != null && p instanceof MetadataCache) {
			((MetadataCache)p).evict(metadataIdentificationString);
		}
		
		// Finally, evict downstream dependencies (ie metadata that previously depended on this now-evicted metadata)
		for (String downstream : metadataDependencyRegistry.getDownstream(metadataIdentificationString)) {
			evict(downstream);
		}
	}

	public void evictAll() {
		// Clear my own cache
		super.evictAll();
		
		// Clear the caches of any metadata providers which support the interface
		for (MetadataProvider p : getRegisteredProviders()) {
			if (p instanceof MetadataCache) {
				((MetadataCache)p).evictAll();
			}
		}
	}

	public final String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("providers", getRegisteredProviders());
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
