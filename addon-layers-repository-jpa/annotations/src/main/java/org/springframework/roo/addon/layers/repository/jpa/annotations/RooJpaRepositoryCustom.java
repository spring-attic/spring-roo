package org.springframework.roo.addon.layers.repository.jpa.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the annotated type as a Custom Spring Data JPA repository interface.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooJpaRepositoryCustom {

  /**
   * The name of this annotation's attribute that specifies the managed
   * entity.
   */
  String ENTITY_ATTRIBUTE = "entity";

  /**
   * The name of this annotation's attribute that specifies the findAll search results
   * type.
   */
  String DEFAULT_RETURN_TYPE_ATTRIBUTE = "defaultReturnType";

  /**
   * The entity managed by the annotated repository
   * 
   * @return a non-<code>null</code> entity type
   */
  Class<?> entity(); // No default => mandatory

  /**
   * The type of the results returned by the findAll search of annotated repository
   * 
   * @return a non-<code>null</code> type
   */
  Class<?> defaultReturnType(); // No default => mandatory

}
