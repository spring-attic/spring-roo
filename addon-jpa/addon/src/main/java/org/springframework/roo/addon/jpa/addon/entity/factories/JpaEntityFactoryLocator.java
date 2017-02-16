package org.springframework.roo.addon.jpa.addon.entity.factories;

import java.util.Collection;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaType;

/**
 * JpaEntityFactoryLocator
 * 
 * Locates RooJpaEntityFactory within the user's project
 *
 * @author Sergio Clares
 * @since 2.0.0
 */
public interface JpaEntityFactoryLocator {

  /**
   * Returns the factory related the given domain type
   *
   * @param entity the entity for which to find the factory
   * @return a @{@link Collection} with the {@link ClassOrInterfaceTypeDetails} 
   *            with the related factories
   */
  Collection<ClassOrInterfaceTypeDetails> getJpaEntityFactoriesForEntity(final JavaType entity);

  /**
   * Returns the first factory found, related the given domain type
   *
   * @param entity the entity for which to find the factory
   * @return a @{@link JavaType} with the first related factory (usually the 
   *            only one related), or <code>null</code> if none could be found.
   */
  JavaType getFirstJpaEntityFactoryForEntity(final JavaType entity);

  /**
   * Returns the factory metadata for the given factory.
   * 
   * @param entityFactory the factory {@link JavaType} for which to find the 
   *            factory metadata.
   * @return the {@link JpaEntityFactoryMetadata} for the given factory or `null` if 
   *            the provided factory was `null`.
   */
  JpaEntityFactoryMetadata getJpaEntityFactoryMetadata(final JavaType entityFactory);
}
