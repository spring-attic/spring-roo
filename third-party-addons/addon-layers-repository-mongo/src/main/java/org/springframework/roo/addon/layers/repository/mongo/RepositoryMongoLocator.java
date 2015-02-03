package org.springframework.roo.addon.layers.repository.mongo;

import java.util.Collection;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaType;

/**
 * Locates Spring Data Mongo Repositories within the user's project
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public interface RepositoryMongoLocator {

    /**
     * Returns the repositories that support the given domain type
     * 
     * @param domainType the domain type for which to find the repositories; can
     *            be <code>null</code>
     * @return a non-<code>null</code> collection
     */
    Collection<ClassOrInterfaceTypeDetails> getRepositories(
            final JavaType domainType);
}
