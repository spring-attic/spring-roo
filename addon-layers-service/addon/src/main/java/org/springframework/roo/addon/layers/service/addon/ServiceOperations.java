package org.springframework.roo.addon.layers.service.addon;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;

/**
 * API that defines all available operations for service layer management.
 * 
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @since 1.2.0
 */
public interface ServiceOperations {

  /**
   * Check if developer is able to use 'service' commands
   * 
   * @return true if service commands are available
   */
  boolean areServiceCommandsAvailable();

  /**
   * Generates new service interface and its implementation for some specific
   * domain entity and repository.
   * 
   * @param domainType entity related with service
   * @param repositoryType repository related with service
   * @param interfaceType service interface to generate
   * @param implType service implementation to generate. 
   */
  void addService(JavaType domainType, JavaType repositoryType, JavaType interfaceType,
      JavaType implType);

  /**
   * Generates new service interface and its implementation for some specific
   * domain entity.
   * 
   * @param domainType entity related with service
   * @param interfaceType service interface to generate
   * @param implType service implementation to generate. 
   */
  void addService(JavaType domainType, JavaType interfaceType, JavaType implType);

  /**
   * Generates new services interface and its implementations for every domain
   * entity of generated project
   * 
   * @param apiPackage
   * @param implPackage
   */
  void addAllServices(JavaPackage apiPackage, JavaPackage implPackage);

}
