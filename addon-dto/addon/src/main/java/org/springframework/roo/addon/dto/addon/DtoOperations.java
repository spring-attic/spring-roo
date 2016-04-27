package org.springframework.roo.addon.dto.addon;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.ShellContext;

/**
 * Provides DTO configuration and operations.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public interface DtoOperations {

  /**
   * Indicates whether a DTO can be created in the project. The project should be available.
   * 
   * @return <code>false</code> if DTO creation is not possible.
   */
  boolean isDtoCreationPossible();

  /**
   * Creates a DTO in the project with the specified options.
   * 
   * @param name the name of the DTO
   * @param immutable whether the DTO should be immutable
   * @param utilityMethods whether the DTO should have utility methods
   * @param serializable whether the DTO should implement Serializable
   * @param fromEntity whether the DTO is created from an entity and still shouldn't be built
   * @return ClassOrInterfaceTypeDetailsBuilder for building the class file 
   */
  ClassOrInterfaceTypeDetailsBuilder createDto(JavaType name, boolean immutable,
      boolean utilityMethods, boolean serializable, boolean fromEntity);

  /**
   * Creates one DTO for each entity in the project. 
   * 
   * @param immutable whether the DTO's should be immutable
   * @param utilityMethods whether the DTO's should have utility methods
   * @param serializable whether the DTO's should implement Serializable
   * @param shellContext 
   */
  void createDtoFromAll(boolean immutable, boolean utilityMethods, boolean serializable,
      ShellContext shellContext);

  /**
   * Creates a DTO from the specified fields of an entity.
   * 
   * @param name the name of the DTO
   * @param entity the name of the entity from which create the DTO
   * @param fields the entity fields to include in the DTO
   * @param excludeFields the entity fields to exclude in the DTO
   * @param immutable whether the DTO should be immutable
   * @param utilityMethods whether the DTO should have utility methods
   * @param serializable whether the DTO should implement Serializable
   * @param shellContext
   */
  void createDtoFromEntity(JavaType name, JavaType entity, String fields, String excludeFields,
      boolean immutable, boolean utilityMethods, boolean serializable, ShellContext shellContext);

}
