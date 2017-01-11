package org.springframework.roo.addon.dto.addon;

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
   * Indicates whether an entity projection can be created in the project. The project 
   * should be available and contain at least one entity.
   * 
   * @return <code>false</code> if entity projection creation is not possible.
   */
  boolean isEntityProjectionPossible();

  /**
   * Creates a DTO in the project with the specified options.
   * 
   * @param name the name of the DTO
   * @param immutable whether the DTO should be immutable
   * @param utilityMethods whether the DTO should have utility methods
   * @param serializable whether the DTO should implement Serializable
   * @param formatMessage the localization message key to use to format the dto
   * @param formatExpression the SpEL expression to use to format the dto
   */
  void createDto(JavaType name, boolean immutable, boolean utilityMethods, boolean serializable,
      String formatExpression, String formatMessage);

  /**
   * Creates a Projection class from an entity.
   * 
   * @param entity the entity related with this Projection.
   * @param name the name of the Projection.
   * @param fields the related entity fields to include into the Projection.
   * @param suffix the suffix to add to Projection name. 
   * @param formatMessage the localization message key to use to format the projection
   * @param formatExpression the SpEL expression to use to format the projection
   */
  void createProjection(JavaType entity, JavaType name, String fields, String suffix,
      String formatExpression, String formatMessage);

  /**
   * Creates one entity Projection for each entity in the project.
   * 
   * @param suffix the suffix added to each projection class name, builded from each 
   * associated entity name.
   * @param shellContext the ShellContext for checking global parameters
   */
  void createAllProjections(String suffix, ShellContext shellContext);

}
