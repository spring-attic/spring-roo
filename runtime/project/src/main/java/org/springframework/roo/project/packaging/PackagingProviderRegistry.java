package org.springframework.roo.project.packaging;

import java.util.Collection;

/**
 * A registry for {@link PackagingProvider}s.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public interface PackagingProviderRegistry {

    /**
     * Returns all known {@link PackagingProvider}s
     * 
     * @return a non-<code>null</code> list (might be empty)
     */
    Collection<PackagingProvider> getAllPackagingProviders();

    /**
     * Returns the {@link PackagingProvider} to be used when the user doesn't
     * specify one.
     * 
     * @return a non-<code>null</code> instance
     */
    PackagingProvider getDefaultPackagingProvider();

    /**
     * Returns the {@link PackagingProvider} with the given ID.
     * 
     * @param id the ID to look for; see {@link PackagingProvider#getId()}
     * @return <code>null</code> if there's no such instance
     */
    PackagingProvider getPackagingProvider(String id);
}