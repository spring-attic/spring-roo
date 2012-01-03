package org.springframework.roo.addon.layers.repository.jpa;

import java.util.Collection;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaType;

/**
 * Locates Spring Data JPA Repositories within the user's project
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public interface RepositoryJpaLocator {

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
