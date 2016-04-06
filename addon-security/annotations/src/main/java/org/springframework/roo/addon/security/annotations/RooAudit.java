package org.springframework.roo.addon.security.annotations;

/**
 * Indicates an entity class which must be audited
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public @interface RooAudit {

  /**
   * Indicates the name of the Data Base column to store createdDate field data.
   */
  String createdDateColumn() default "";

  /**
   * Indicates the name of the Data Base column to store modifiedDate field data.
   */
  String modifiedDateColumn() default "";

  /**
   * Indicates the name of the Data Base column to store createdBy field data.
   */
  String createdByColumn() default "";

  /**
   * Indicates the name of the Data Base column to store modifiedBy field data.
   */
  String modifiedByColumn() default "";

}
