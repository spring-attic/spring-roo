package org.springframework.roo.addon.test.addon.providers;

import org.springframework.roo.model.JavaType;

/**
 * Provides a "data on demand" creation API which can be implemented by each 
 * add-on that needs (usually for using with tests).
 * 
 * This interface permits data on demand creation via Roo commands with 
 * different configurations depending on the type of persistent class 
 * to generate data for.
 *
 * @author Sergio Clares
 * @since 2.0
 */
public interface DataOnDemandCreatorProvider {

  /**
   * Whether an implementation of this interface is valid for the class type
   * which the data is going to be needed for.
   *
   * @return `true` if the implementation is valid, `false` otherwise.
   */
  boolean isValid(JavaType javaType);

  /**
   * Creates a new data-on-demand provider for an entity, with all its needed 
   * setup. Silently returns if the needed components already exist.
   * 
   * @param entity to produce a DoD provider for
   */
  void createDataOnDemand(JavaType entity);

}
