package org.springframework.roo.metadata;

/**
 * Indicates a service which is aware of all {@link MetadataProvider}s in the
 * system and can provide access to their respective capabilities.
 * <p>
 * A {@link MetadataService} provides a convenient way for any object that
 * requires metadata information to consult a single source that can provide
 * that metadata. An implementation can act as the single source because it can
 * identify which {@link MetadataProvider} is applicable to a given metadata
 * identification string and delegate to that provider.
 * <p>
 * An instance of {@link MetadataService} becomes aware of candidate
 * {@link MetadataProvider} instances by way of "registration". An
 * implementation is required to use OSGi declarative services to detect the
 * presence of {@link MetadataProvider} instances.
 * <p>
 * As indicated by the {@link MetadataService} interface extending
 * {@link MetadataCache}, all implementations must provide caching support.
 * <p>
 * Also as indicated by {@link MetadataService} extending
 * {@link MetadataNotificationListener}, an implementation is required to
 * receive notification events and pass these notification events through to the
 * relevant {@link MetadataProvider}. If there is no registered
 * {@link MetadataProvider}, the notification method should simply return
 * without error. If passing through to a {@link MetadataProvider} that
 * implements {@link MetadataNotificationListener}, any cache modification
 * responsibilities are that of the delegate. If the {@link MetadataProvider}
 * does not implement {@link MetadataNotificationListener}, the fallback
 * behaviour of the {@link MetadataService} implementation is to invoke
 * {@link #get(String, boolean)}, passing a true value to the evict from cache
 * boolean parameter. It must also call
 * {@link MetadataDependencyRegistry#notifyDownstream(String)} in case any other
 * metadata was monitoring the metadata.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface MetadataService extends MetadataNotificationListener,
        MetadataCache {

    /**
     * Returns the {@link MetadataItem} with the given ID, generating it from
     * scratch and caching the result. For performance reasons it's preferable
     * to call {@link #get(String)} if possible, to take advantage of the cache.
     * 
     * @param metadataIdentificationString the ID of the {@link MetadataItem} to
     *            acquire; must identify a metadata instance, i.e. return
     *            <code>true</code> when passed to
     *            {@link MetadataIdentificationUtils#isIdentifyingInstance(String)}
     * @return the metadata, or <code>null</code> if the ID was valid but the
     *         metadata is not currently available
     * @throws an exception if the given type of metadata is not supported
     */
    MetadataItem evictAndGet(String metadataIdentificationString);

    /**
     * Returns the {@link MetadataItem} with the given ID, from the cache if
     * possible.
     * 
     * @param metadataIdentificationString the ID of the {@link MetadataItem} to
     *            acquire; must identify a metadata instance, i.e. return
     *            <code>true</code> when passed to
     *            {@link MetadataIdentificationUtils#isIdentifyingInstance(String)}
     * @return the metadata, or <code>null</code> if the ID was valid but the
     *         metadata is not currently available
     * @throws an exception if the given type of metadata is not supported
     */
    MetadataItem get(String metadataIdentificationString);

    /**
     * Creates the requested {@link MetadataItem} if possible, returning null if
     * the item cannot be created or found. Implementations will delegate
     * creation events to the respective registered {@link MetadataProvider},
     * and may at their option retrieve a cached instance.
     * <p>
     * This method will throw an exception if the caller has provided an invalid
     * input argument. This would be the case if the input argument is null,
     * empty, does not return true from
     * {@link MetadataIdentificationUtils#isIdentifyingInstance(String)}, or the
     * requested metadata identifier is not of the same class as indicated by
     * getProvidesType()).
     * <p>
     * An exception will also be thrown if the identification string is related
     * to a provider that is not registered.
     * 
     * @param metadataIdentificationString to acquire (required and must be
     *            supported by this provider)
     * @param evictCache forces eviction of the instance from any caches before
     *            attempting retrieval
     * @return the metadata, or null if the identification was valid and a
     *         provider was found, but the metadata is unavailable
     * @deprecated use {@link #evictAndGet(String)} instead of calling
     *             {@link #get(String, true)} or {@link #get(String)} instead of
     *             calling {@link #get(String, false)}
     */
    @Deprecated
    MetadataItem get(String metadataIdentificationString, boolean evictCache);
}
