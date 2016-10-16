package org.springframework.roo.addon.layers.repository.jpa.addon;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;

/**
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @author Jose Manuel Vivó
 * @since 1.2.0
 */
public interface RepositoryJpaOperations {

  /**
   * Checks if it's possible to generate new repositories on current project.
   *
   * @return true if is possible to generate new repositories. If not, return
   *         false
   */
  boolean isRepositoryInstallationPossible();

  /**
   * Add new repository related with some existing entity.
   *
   * @param interfaceType new JavaType representing the interface that will be generated.
   * @param domainType the JavaType representing the domain entity this repository should expose.
   * @param defaultReturnType the JavaType of the findAll search results.
   * @param failOnComposition whatever should fail if a should-not-generate-repository-entity is received as parameter
   */
  void addRepository(JavaType interfaceType, JavaType domainType, JavaType defaultReturnType,
      boolean failOnComposition);

  /**
   * Add new repository for all existing entities.
   *
   * @param repositoriesPackage package where repositories will be generated
   */
  void generateAllRepositories(JavaPackage repositoriesPackage);

  /**
   * Informs a DomainType (entity) should or shouldn't has a repository.
   *
   * Entity shouldn't generate repository when is the child part of
   * one-to-one composition relation (as it's managed by parent).
   *
   * @param domainType
   * @return
   */
  boolean shouldGenerateRepository(JavaType domainType);
}
