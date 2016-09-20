package org.springframework.roo.addon.finder.addon;

import java.util.SortedSet;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Provides Finder add-on operations.
 * 
 * @author Ben Alex
 * @author Sergio Clares
 * @since 1.0
 */
public interface FinderOperations {

  /**
   * Creates a finder in an entity repository.
   * 
   * @param typeName the entity for which the finders are generated.
   * @param finderName the finder string defined as a Spring Data query
   * @param formBean the finder's search parameter. Should be a DTO.
   * @param defaultReturnType the finder's results return type. Should be a Projection.
   */
  void installFinder(JavaType typeName, JavaSymbolName finderName, JavaType formBean,
      JavaType defaultReturnType);

  boolean isFinderInstallationPossible();

  SortedSet<String> listFindersFor(JavaType typeName, Integer depth);
}
