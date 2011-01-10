package org.springframework.roo.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 */
@Component
@Service
@Reference(name = "metadataProvider", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = MetadataProvider.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
public class DefaultMetadataService extends AbstractMetadataCache implements MetadataService {
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference private MetadataLogger metadataLogger;
	private int validGets = 0;
	private int recursiveGets = 0;
	private int cachePuts = 0;
	private int cacheHits = 0;
	private int cacheMisses = 0;
	private int cacheEvictions = 0; 
	private Set<MetadataProvider> providers = new HashSet<MetadataProvider>();
	private Map<String, MetadataProvider> providerMap = new HashMap<String, MetadataProvider>();

	// Mutex
	private Boolean lock = new Boolean(true);

	// Request control
	private List<String> activeRequests = new ArrayList<String>(); // list to assist output "stacks" which show the order of requests
	private List<String> keysToRetry = new ArrayList<String>();  // list to help us verify correct operation through logs (predictable ordering)
	
	protected void bindMetadataProvider(MetadataProvider mp) {
		synchronized (lock) {
			Assert.notNull(mp, "Metadata provider required");
			String mid = mp.getProvidesType();
			Assert.isTrue(MetadataIdentificationUtils.isIdentifyingClass(mid), "Metadata provider '" + mp + "' violated interface contract by returning '" + mid + "'");
			Assert.isTrue(!providerMap.containsKey(mid), "Metadata provider '" + providerMap.get(mid) + "' already is providing metadata for '" + mid + "'");
			providers.add(mp);
			providerMap.put(mid, mp);
		}
	}
	
	protected void unbindMetadataProvider(MetadataProvider mp) {
		synchronized (lock) {
			String mid = mp.getProvidesType();
			providers.remove(mp);
			providerMap.remove(mid);
		}
	}
	
	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
	}
	
	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
	}

	public MetadataItem get(String metadataIdentificationString, boolean evictCache) {
		return getInternal(metadataIdentificationString, evictCache, true);
	}
	
	public MetadataItem getInternal(String metadataIdentificationString, boolean evictCache, boolean cacheRetrievalAllowed) {
		Assert.isTrue(MetadataIdentificationUtils.isIdentifyingInstance(metadataIdentificationString), "Metadata identification string '" + metadataIdentificationString + "' does not identify a metadata instance");
		
		synchronized (lock) {
			validGets++;

			try {
				metadataLogger.startEvent();
				
				// Do some cache eviction if the caller requested it
				if (evictCache) {
					evict(metadataIdentificationString);
					if (metadataLogger.getTraceLevel() > 0) {
						metadataLogger.log("Evicting " + metadataIdentificationString);
					}
					cacheEvictions++;
				}
				
				// We can use the cache even for a recursive get (unless of course the caller has prevented it)
				if (cacheRetrievalAllowed) {
					// Try the cache first
					MetadataItem result = getFromCache(metadataIdentificationString);
					if (result != null) {
						cacheHits++;
						if (metadataLogger.getTraceLevel() > 0) {
							metadataLogger.log("Cache hit " + metadataIdentificationString);
						}
						return result;
					}
				}
				
				if (metadataLogger.getTraceLevel() > 0) {
					metadataLogger.log("Cache miss " + metadataIdentificationString);
				}
				cacheMisses++;

				// Determine if this MID was already requested earlier. We need to stop these infinite requests from occurring.
				if (activeRequests.contains(metadataIdentificationString)) {
					recursiveGets++;
					if (!keysToRetry.contains(metadataIdentificationString)) {
						if (metadataLogger.getTraceLevel() > 0) {
							metadataLogger.log("Blocked recursive request for " + metadataIdentificationString);
						}
						keysToRetry.add(metadataIdentificationString);
					}
					return null;
				}
				
				// Infinite loop management
				activeRequests.add(metadataIdentificationString);

				// Get the destination
				String mdClassId = MetadataIdentificationUtils.create(MetadataIdentificationUtils.getMetadataClass(metadataIdentificationString));
				MetadataProvider p = providerMap.get(mdClassId);
				Assert.notNull(p, "No metadata provider is currently registered to provide metadata for identifier '" + metadataIdentificationString + "' (class '" + mdClassId + "')");
				
				// Obtain the item
				if (metadataLogger.getTraceLevel() > 0) {
					metadataLogger.log("Get " + metadataIdentificationString + " from " + p.getClass().getName());
				}
				MetadataItem result = null;
				try {
					metadataLogger.startTimer(p.getClass().getName());
					result = p.get(metadataIdentificationString);
				} finally {
					metadataLogger.stopTimer();
				}
				
				// If the item isn't available, evict it from the cache (unless we did so at the start of the method already)
				if (result == null && !evictCache) {
					if (metadataLogger.getTraceLevel() > 0) {
						metadataLogger.log("Evicting unavailable item " + metadataIdentificationString);
					}
					evict(metadataIdentificationString);
					cacheEvictions++;
				}
				
				// Put into the cache, provided it isn't null
				if (result != null) {
					if (metadataLogger.getTraceLevel() > 0) {
						metadataLogger.log("Caching " + metadataIdentificationString);
					}
					super.put(result);
					cachePuts++;
				}
				
				activeRequests.remove(metadataIdentificationString);

				if (metadataLogger.getTraceLevel() > 0) {
					metadataLogger.log("Returning " + metadataIdentificationString);
				}
				return result;

			} finally {
				// We use another try..finally block as we want to ensure exceptions don't prevent our metadataLogger.stopEvent()
				try {
					// Have we processed all requests? If so, handle any retries we recorded
					if (activeRequests.size() == 0) {
						List<String> thisRetry = new ArrayList<String>();
						thisRetry.addAll(keysToRetry);
						keysToRetry.clear();
						if (metadataLogger.getTraceLevel() > 0 && thisRetry.size() > 0) {
							metadataLogger.log(thisRetry.size() + " keys to retry: " + thisRetry);
						}
						for (String retryMid : thisRetry) {
							// Important: we should not evict any prior version from the cache (an interim version is acceptable).
							// We discard the result of the get; this is purely to facilitate updating metadata stored in memory and on-disk
							if (metadataLogger.getTraceLevel() > 0) {
								metadataLogger.log("Retrying " + retryMid);
							}
							getInternal(retryMid, false, false);
						}
						if (metadataLogger.getTraceLevel() > 0 && thisRetry.size() > 0) {
							metadataLogger.log("Retry group completed " + metadataIdentificationString);
						}
					}
				} finally {
					metadataLogger.stopEvent();
				}
			}
		}
	}
	
	@Override
	public void put(MetadataItem metadataItem) {
		super.put(metadataItem);
		cachePuts++;
	}

	public MetadataItem get(String metadataIdentificationString) {
		return get(metadataIdentificationString, false);
	}

	public void notify(String upstreamDependency, String downstreamDependency) {
		Assert.isTrue(MetadataIdentificationUtils.isValid(upstreamDependency), "Upstream dependency is an invalid metadata identification string ('" + upstreamDependency + "')");
		Assert.isTrue(MetadataIdentificationUtils.isValid(downstreamDependency), "Downstream dependency is an invalid metadata identification string ('" + downstreamDependency + "')");
		
		synchronized (lock) {
			// Get the destination
			String mdClassId = MetadataIdentificationUtils.create(MetadataIdentificationUtils.getMetadataClass(downstreamDependency));
			MetadataProvider p = providerMap.get(mdClassId);
			
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
	}

	public void evict(String metadataIdentificationString) {
		synchronized (lock) {
			// Clear my own cache (which also verifies the argument is valid at the same time)
			super.evict(metadataIdentificationString);
			
			// Finally, evict downstream dependencies (ie metadata that previously depended on this now-evicted metadata)
			for (String downstream : metadataDependencyRegistry.getDownstream(metadataIdentificationString)) {
				// We only need to evict if it is an instance, as only an instance will ever go into the cache
				if (MetadataIdentificationUtils.isIdentifyingInstance(downstream)) {
					evict(downstream);
				}
			}
		}
	}

	public void evictAll() {
		synchronized (lock) {
			// Clear my own cache
			super.evictAll();
			
			// Clear the caches of any metadata providers which support the interface
			for (MetadataProvider p : providers) {
				if (p instanceof MetadataCache) {
					((MetadataCache)p).evictAll();
				}
			}
		}
	}

	public final String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("validGets", validGets);
		tsc.append("recursiveGets", recursiveGets);
		tsc.append("cachePuts", cachePuts);
		tsc.append("cacheHits", cacheHits);
		tsc.append("cacheMisses", cacheMisses);
		tsc.append("cacheEvictions", cacheEvictions);
		tsc.append("cacheCurrentSize", getCacheSize());
		tsc.append("cacheMaximumSize", getMaxCapacity());
		return tsc.toString();
	}
}