package org.springframework.roo.addon.test.providers;

import org.springframework.roo.model.JavaType;

/**
 * Provides a "data on demand" creation API which can be implemented by each 
 * add-on that needs it (usually for using with tests).
 * 
 * This interface permits data on demand creation with different configurations 
 * depending on the type of persistent class to generate data for.
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
   * Creates a new data-on-demand provider class for a persistent class, with 
   * all its needed setup, like data-on-demand configuration and entity factory. 
   * It returns the recently created or already existent data-on-demand class.   
   * 
   * @param persistentType to produce a DoD provider class for.
   * @return {@link JavaType} the data-on-demand class created for the provided 
   *            persistent type.
   */
  JavaType createDataOnDemand(JavaType persistentType);

  /**
   * Creates a DoD configuration class if it still doesn't exist in the project. 
   * This class will be in carry of injecting every DataOnDemand class into the 
   * Spring context when required.
   * 
   * @param {@link String} moduleName the module name where the class should be 
   *            created.
   * @return {@link JavaType} the data-on-demand configuration class.
   */
  JavaType createDataOnDemandConfiguration(String moduleName);


  /**
   * Creates a new entity factory class for the provided persistent type and 
   * its related types (one factory for each type). These factories are used 
   * for creating transient instances to use in tests.
   * 
   * @param persistentType the {@link JavaType} to produce a factory for.
   * @return {@link JavaType} the entity factory created for provided 
   *            persistent type.
   */
  JavaType createEntityFactory(JavaType persistentType);

  /**
   * Seeks the project for the data-on-demand class for the provided persistent 
   * type and retrieves it if exists.
   * 
   * @param persistentType to get its DoD provider class for.
   * @return {@link JavaType} the data-on-demand class of the provided 
   *            persistent type or `null` if the class doesn't exist.
   */
  JavaType getDataOnDemand(JavaType persistentType);

  /**
   * Seeks the project for a data-on-demand configuration class and retrieves 
   * it if exists.
   * 
   * @return {@link JavaType} the data-on-demand configuration class in the 
   *            project, or `null` if the class doesn't exist.
   */
  JavaType getDataOnDemandConfiguration();

  /**
   * Seeks the provided module for a data-on-demand configuration class and 
   * retrieves it if exists.
   * 
   * @param {@link String} moduleName the module name where the class should 
   *            be searched for. 
   * @return {@link JavaType} the data-on-demand configuration class in the 
   *            module, or `null` if the class doesn't exist in the specified 
   *            module name.
   */
  JavaType getDataOnDemandConfiguration(String moduleName);

  /**
   * Seeks the project for an entity factory class for the provided persistent 
   * type and retrieves it if exists.
   * 
   * @param persistentType to get its entity factory class for.
   * @return {@link JavaType} the entity factory class of the provided 
   *            persistent type or `null` if the class doesn't exist.
   */
  JavaType getEntityFactory(JavaType persistentType);

}
