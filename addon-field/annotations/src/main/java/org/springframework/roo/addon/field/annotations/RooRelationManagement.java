package org.springframework.roo.addon.field.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * = _RooRelationManagement_
 *
 * Indicates which fields should be handle as relationship parent.
 *
 * @author Manuel Iborra
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooRelationManagement {

  /**
   * Indicates a list of the relation fields that the annotated
   * parent entity contains.
   */
  String[] relationFields();

}
