package org.springframework.roo.addon.layers.repository.jpa.addon;

import java.util.Collection;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaType;

/**
 * Locates Spring Data JPA Repositories within the user's project
 *
 * @author Andrew Swan
 * @author Jose Manuel Vivó
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
  Collection<ClassOrInterfaceTypeDetails> getRepositories(final JavaType domainType);

  /**
   * Returns the repository that support the given domain type
   *
   * @param domainType the domain type for which to find the repositories; not
   *            <code>null</code>
   * @return a repository or null if not found
   * @throws NullPointerException if domainType is null
   * @throws IllegalStateException if more than one repository found
   */
  ClassOrInterfaceTypeDetails getRepository(final JavaType domainType);

  /**
   * Returns the repository metadata that support the given domain type
   *
   * @param domainType the domain type for which to find the repositories; not
   *            <code>null</code>
   * @return a repository or null if not found
   * @throws NullPointerException if domainType is null
   * @throws IllegalStateException if more than one repository found
   */
  RepositoryJpaMetadata getRepositoryMetadata(final JavaType domainType);

  /**
   * Returns first repository that support the given domain type
   *
   * @param domainType the domain type for which to find the repositories; not
   *            <code>null</code>
   * @return a repository (first found) or null if not found
   * @throws NullPointerException if domainType is null
   */
  ClassOrInterfaceTypeDetails getFirstRepository(final JavaType domainType);

  /**
   * Returns first repository Metadata that support the given domain type
   *
   * @param domainType the domain type for which to find the repositories; not
   *            <code>null</code>
   * @return a repository (first found) or null if not found
   * @throws NullPointerException if domainType is null
   */
  RepositoryJpaMetadata getFirstRepositoryMetadata(final JavaType domainType);


}
