package org.springframework.roo.addon.layers.repository.jpa.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.roo.addon.layers.repository.jpa.annotations.finder.RooFinder;

/**
 * Marks the annotated type as a Spring Data JPA repository interface. For the
 * time being, we don't allow users to customise the names of repository methods
 * like we do for service interfaces, because Spring Data JPA provides a
 * complete pre-named set of CRUD methods out of the box.
 *
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @author Juan Carlos García
 * @since 1.2.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooJpaRepository {

  /**
   * The name of this annotation's attribute that specifies the managed
   * entity.
   */
  String ENTITY_ATTRIBUTE = "entity";

  /**
   * The entity managed by the annotated repository
   *
   * @return a non-<code>null</code> entity type
   */
  Class<?> entity(); // No default => mandatory

  /**
   * The name of this annotation's attribute that specifies the finders
   */
  String FINDERS_ATTRIBUTE = "finders";

  /**
   * The array of {@link RooFinder} with the finders to be included in
   * current Spring Data JPA Repository
   *
   * @return a non empty array with one or more {@link RooFinder}
   */
  RooFinder[] finders() default {};


  /**
   * The type of the results returned by the findAll search of annotated repository
   *
   * @return a non-<code>null</code> type
   */
  Class<?> defaultReturnType() default Class.class;


  /**
   * The name of this annotation's attribute that specifies the findAll search results
   * type.
   */
  String DEFAULT_RETURN_TYPE_ATTRIBUTE = "defaultReturnType";
}
