package org.springframework.roo.addon.layers.repository.jpa.addon;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Feature;

/**
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @since 1.2.0
 */
public interface RepositoryJpaOperations extends Feature {

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
   */
  void addRepository(JavaType interfaceType, JavaType domainType, JavaType defaultReturnType);

  /**
   * Add new repository for all existing entities.
   * 
   * @param repositoriesPackage package where repositories will be generated
   */
  void generateAllRepositories(JavaPackage repositoriesPackage);
}
