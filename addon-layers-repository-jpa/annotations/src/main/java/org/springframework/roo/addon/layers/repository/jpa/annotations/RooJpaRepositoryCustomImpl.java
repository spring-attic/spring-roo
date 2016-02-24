package org.springframework.roo.addon.layers.repository.jpa.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the annotated type as a Custom Spring Data JPA repository
 * implementation.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooJpaRepositoryCustomImpl {

  /**
   * The name of this annotation's attribute that specifies The interface
   * implemented by the annotated type
   */
  String REPOSITORY_ATTRIBUTE = "repository";

  /**
   * The interface implemented by the annotated type
   * 
   * @return a non-<code>null</code> entity type
   */
  Class<?> repository(); // No default => mandatory

}
