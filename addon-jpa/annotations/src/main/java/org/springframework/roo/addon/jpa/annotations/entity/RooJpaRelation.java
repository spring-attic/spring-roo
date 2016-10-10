package org.springframework.roo.addon.jpa.annotations.entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * = _RooJpaRelation_
 *
 * Includes additional information about JPA relation fields on the _parent_ side.
 *
 * @author Jose Manuel Vivó
 * @since 2.0.0
 *
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface RooJpaRelation {

  /**
   * Specifies relation type of a JPA relation field.
   *
   * Cardinality of field should be OneToOne, OneToMany or ManyToMany.
   *
   * @return relation type
   */
  JpaRelationType type() default JpaRelationType.AGGREGATION;

}
