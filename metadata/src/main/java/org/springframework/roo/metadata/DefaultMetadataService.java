package org.springframework.roo.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.metadata.internal.AbstractMetadataCache;

/**
 * Default implementation of {@link MetadataService}.
 * <p>
 * This implementation is not thread safe. It should only be accessed by a
 * single thread at a time. This is enforced by the process manager semantics,
 * so we avoid the cost of re-synchronization here.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true)
@Service
@Reference(name = "metadataProvider", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = MetadataProvider.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
public class DefaultMetadataService extends AbstractMetadataCache implements
        MetadataService {

    @Reference private MetadataDependencyRegistry metadataDependencyRegistry;
    @Reference private MetadataLogger metadataLogger;

    // Request control
    // List to assist output "stacks"which show the order of requests
    private final List<String> activeRequests = new ArrayList<String>();
    private int cacheEvictions = 0;
    private int cacheHits = 0;
    private int cacheMisses = 0;
    private int cachePuts = 0;
    // List to help us verify correct operation through logs (predictable
    // ordering)
    private final List<String> keysToRetry = new ArrayList<String>();
    // Mutex
    private final Object lock = new Object();
    private final Map<String, MetadataProvider> providerMap = new HashMap<String, MetadataProvider>();
    private final Set<MetadataProvider> providers = new HashSet<MetadataProvider>();
    private int recursiveGets = 0;
    private int validGets = 0;

    protected void activate(final ComponentContext context) {
        metadataDependencyRegistry.addNotificationListener(this);
    }

    protected void bindMetadataProvider(final MetadataProvider mp) {
        synchronized (lock) {
            Validate.notNull(mp, "Metadata provider required");
            final String mid = mp.getProvidesType();
            Validate.isTrue(
                    MetadataIdentificationUtils.isIdentifyingClass(mid),
                    "Metadata provider '%s' violated interface contract by returning '%s'",
                    mp, mid);
            Validate.isTrue(
                    !providerMap.containsKey(mid),
                    "Metadata provider '%s' already is providing metadata for '%s'",
                    providerMap.get(mid), mid);
            providers.add(mp);
            providerMap.put(mid, mp);
        }
    }

    protected void deactivate(final ComponentContext context) {
        metadataDependencyRegistry.removeNotificationListener(this);
    }

    @Override
    public void evict(final String metadataIdentificationString) {
        synchronized (lock) {
            // Clear my own cache (which also verifies the argument is valid at
            // the same time)
            super.evict(metadataIdentificationString);

            // Finally, evict downstream dependencies (ie metadata that
            // previously depended on this now-evicted metadata)
            for (final String downstream : metadataDependencyRegistry
                    .getDownstream(metadataIdentificationString)) {
                // We only need to evict if it is an instance, as only an
                // instance will ever go into the cache
                if (MetadataIdentificationUtils
                        .isIdentifyingInstance(downstream)) {
                    evict(downstream);
                }
            }
        }
    }

    @Override
    public void evictAll() {
        synchronized (lock) {
            // Clear my own cache
            super.evictAll();

            // Clear the caches of any metadata providers which support the
            // interface
            for (final MetadataProvider p : providers) {
                if (p instanceof MetadataCache) {
                    ((MetadataCache) p).evictAll();
                }
            }
        }
    }

    public MetadataItem evictAndGet(final String metadataIdentificationString) {
        return getInternal(metadataIdentificationString, true, false);
    }

    public MetadataItem get(final String metadataIdentificationString) {
        return get(metadataIdentificationString, false);
    }

    public MetadataItem get(final String metadataIdentificationString,
            final boolean evictCache) {
        return getInternal(metadataIdentificationString, evictCache, true);
    }

    private MetadataItem getInternal(final String metadataIdentificationString,
            final boolean evictCache, final boolean cacheRetrievalAllowed) {
        Validate.isTrue(
                MetadataIdentificationUtils
                        .isIdentifyingInstance(metadataIdentificationString),
                "Metadata identification string '%s' does not identify a metadata instance",
                metadataIdentificationString);

        synchronized (lock) {
            validGets++;

            try {
                metadataLogger.startEvent();

                // Do some cache eviction if the caller requested it
                if (evictCache) {
                    evict(metadataIdentificationString);
                    if (metadataLogger.getTraceLevel() > 0) {
                        metadataLogger.log("Evicting "
                                + metadataIdentificationString);
                    }
                    cacheEvictions++;
                }

                // We can use the cache even for a recursive get (unless of
                // course the caller has prevented it)
                if (cacheRetrievalAllowed) {
                    // Try the cache first
                    final MetadataItem result = getFromCache(metadataIdentificationString);
                    if (result != null) {
                        cacheHits++;
                        if (metadataLogger.getTraceLevel() > 0) {
                            metadataLogger.log("Cache hit "
                                    + metadataIdentificationString);
                        }
                        return result;
                    }
                }

                if (metadataLogger.getTraceLevel() > 0) {
                    metadataLogger.log("Cache miss "
                            + metadataIdentificationString);
                }
                cacheMisses++;

                // Determine if this MID was already requested earlier. We need
                // to stop these infinite requests from occurring.
                if (activeRequests.contains(metadataIdentificationString)) {
                    recursiveGets++;
                    if (!keysToRetry.contains(metadataIdentificationString)) {
                        if (metadataLogger.getTraceLevel() > 0) {
                            metadataLogger.log("Blocked recursive request for "
                                    + metadataIdentificationString);
                        }
                        keysToRetry.add(metadataIdentificationString);
                    }
                    return null;
                }

                // Get the destination
                final String mdClassId = MetadataIdentificationUtils
                        .getMetadataClassId(metadataIdentificationString);
                final MetadataProvider p = providerMap.get(mdClassId);
                Validate.notNull(
                        p,
                        "No metadata provider is currently registered to provide metadata for identifier '%s' (class '%s')",
                        metadataIdentificationString, mdClassId);

                // Infinite loop management
                activeRequests.add(metadataIdentificationString);

                // Obtain the item
                if (metadataLogger.getTraceLevel() > 0) {
                    metadataLogger.log("Get " + metadataIdentificationString
                            + " from " + p.getClass().getName());
                }
                MetadataItem result = null;
                try {
                    metadataLogger.startTimer(p.getClass().getName());
                    result = p.get(metadataIdentificationString);
                }
                finally {
                    metadataLogger.stopTimer();
                }

                // If the item isn't available, evict it from the cache (unless
                // we did so at the start of the method already)
                if (result == null && !evictCache) {
                    if (metadataLogger.getTraceLevel() > 0) {
                        metadataLogger.log("Evicting unavailable item "
                                + metadataIdentificationString);
                    }
                    evict(metadataIdentificationString);
                    cacheEvictions++;
                }

                // Put into the cache, provided it isn't null
                if (result != null) {
                    if (metadataLogger.getTraceLevel() > 0) {
                        metadataLogger.log("Caching "
                                + metadataIdentificationString);
                    }
                    super.put(result);
                    cachePuts++;
                }

                activeRequests.remove(metadataIdentificationString);

                if (metadataLogger.getTraceLevel() > 0) {
                    metadataLogger.log("Returning "
                            + metadataIdentificationString);
                }

                return result;
            }
            catch (final Exception e) {
                activeRequests.remove(metadataIdentificationString);
                throw new IllegalStateException(e);
            }
            finally {
                // We use another try..finally block as we want to ensure
                // exceptions don't prevent our metadataLogger.stopEvent()
                try {
                    // Have we processed all requests? If so, handle any retries
                    // we recorded
                    if (activeRequests.isEmpty()) {
                        final List<String> thisRetry = new ArrayList<String>();
                        thisRetry.addAll(keysToRetry);
                        keysToRetry.clear();
                        if (metadataLogger.getTraceLevel() > 0
                                && thisRetry.size() > 0) {
                            metadataLogger.log(thisRetry.size()
                                    + " keys to retry: " + thisRetry);
                        }
                        for (final String retryMid : thisRetry) {
                            // Important: we should not evict any prior version
                            // from the cache (an interim version is
                            // acceptable).
                            // We discard the result of the get; this is purely
                            // to facilitate updating metadata stored in memory
                            // and on-disk
                            if (metadataLogger.getTraceLevel() > 0) {
                                metadataLogger.log("Retrying " + retryMid);
                            }
                            getInternal(retryMid, false, false);
                        }
                        if (metadataLogger.getTraceLevel() > 0
                                && thisRetry.size() > 0) {
                            metadataLogger.log("Retry group completed "
                                    + metadataIdentificationString);
                        }
                    }
                }
                finally {
                    metadataLogger.stopEvent();
                }
            }
        }
    }

    public void notify(final String upstreamDependency,
            final String downstreamDependency) {
        Validate.isTrue(
                MetadataIdentificationUtils.isValid(upstreamDependency),
                "Upstream dependency is an invalid metadata identification string ('%s')",
                upstreamDependency);
        Validate.isTrue(
                MetadataIdentificationUtils.isValid(downstreamDependency),
                "Downstream dependency is an invalid metadata identification string ('%s')",
                downstreamDependency);

        synchronized (lock) {
            // Get the destination
            final String mdClassId = MetadataIdentificationUtils
                    .getMetadataClassId(downstreamDependency);
            final MetadataProvider p = providerMap.get(mdClassId);

            if (p == null) {
                // No known provider that can consume this notification, so just
                // return as per the interface contract
                return;
            }

            if (p instanceof MetadataNotificationListener) {
                // The provider can directly handle this notification, so we
                // just need to delegate directly to it.
                // We rely on the provider to evict items from the cache if
                // applicable.
                ((MetadataNotificationListener) p).notify(upstreamDependency,
                        downstreamDependency);
            }
            else {
                // As per interface contract, we just ensure we evict the item
                // and recreate it
                // However, we only do this if the destination is an instance -
                // if it's a class, "get" is not a meaningful operation.
                if (MetadataIdentificationUtils
                        .isIdentifyingInstance(downstreamDependency)) {
                    get(downstreamDependency, true);
                }
                // As per interface contract, we now notify any listeners this
                // downstream instance has probably now changed
                metadataDependencyRegistry
                        .notifyDownstream(downstreamDependency);
            }
        }
    }

    @Override
    public void put(final MetadataItem metadataItem) {
        super.put(metadataItem);
        cachePuts++;
    }

    @Override
    public final String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("validGets", validGets);
        builder.append("recursiveGets", recursiveGets);
        builder.append("cachePuts", cachePuts);
        builder.append("cacheHits", cacheHits);
        builder.append("cacheMisses", cacheMisses);
        builder.append("cacheEvictions", cacheEvictions);
        builder.append("cacheCurrentSize", getCacheSize());
        builder.append("cacheMaximumSize", getMaxCapacity());
        return builder.toString().replaceFirst("@[0-9a-f]+", ":");
    }

    protected void unbindMetadataProvider(final MetadataProvider mp) {
        synchronized (lock) {
            final String mid = mp.getProvidesType();
            providers.remove(mp);
            providerMap.remove(mid);
        }
    }
}