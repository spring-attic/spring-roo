package org.springframework.roo.addon.dto.addon;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaType;

import java.util.Collection;

/**
 * = EntityProjectionLocator
 * 
 * Locates RooEntityProjection within the user's project
 *
 * @author Sergio Clares
 * @since 2.0.0
 */
public interface EntityProjectionLocator {

  /**
   * Returns the projections related the given domain type
   *
   * @param domainType the domain type for which to find the Projection
   * @return a non-`null` collection
   */
  Collection<ClassOrInterfaceTypeDetails> getEntityProjectionsForEntity(final JavaType domainType);

  /**
   * Returns the projection metadata for the given projection.
   * 
   * @param entityProjection the projection JavaType for which to find the 
   *            Projection metadata.
   * @return the EntityProjectionMetadata for the given projection or `null` if 
   *            the provided projection was `null`.
   */
  EntityProjectionMetadata getEntityProjectionMetadata(final JavaType entityProjectionDetails);
}
