package org.springframework.roo.addon.jpa.annotations.entity;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a type that is a JPA entity.
 * 
 * @author Andrew Swan
 * @author Juan Carlos García
 * @author Sergio Clares
 * @since 1.2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooJpaEntity {

  /**
   * Specifies the database catalog name that should be used for the entity.
   * 
   * @return the name of the catalog to use (defaults to "")
   */
  String catalog() default "";

  /**
   * Specifies the name used to refer to the entity in queries.
   * <p>
   * The name must not be a reserved literal in JPQL.
   * 
   * @return the name given to the entity (defaults to "")
   */
  String entityName() default "";

  /**
   * Specifies the JPA inheritance type that should be used for the entity.
   * 
   * @return the inheritance type to use (defaults to "")
   */
  String inheritanceType() default "";

  /**
   * @return whether to generated a @MappedSuperclass type annotation instead
   *         of @Entity (defaults to false).
   */
  boolean mappedSuperclass() default false;

  /**
   * Specifies the database schema name that should be used for the entity.
   * 
   * @return the name of the schema to use (defaults to "")
   */
  String schema() default "";

  /**
   * Specifies the table name that should be used for the entity.
   * 
   * @return the name of the table to use (defaults to "")
   */
  String table() default "";

  /**
   * Specifies if current entity should be used for read only operations. This
   * param will be taken in mind to generate readOnly repositories and
   * services.
   * 
   * @return true if entity should be used for read only operations and false
   *         if this entity should be used for CRUD operations.
   */
  boolean readOnly() default false;

  /**
   * Specifies the localization message used to obtain a localized Spring 
   * Expression Language expression to format the entity when showing 
   * it in presentation layer.
   * 
   * @return the key of the message with the SpEL (defaults to "").
   */
  String entityFormatMessage() default "";

  /**
   * Specifies the Spring Expression Language expression to use for formatting 
   * the entity in presentation layer.
   * 
   * @return the SpEL (defaults to "").
   */
  String entityFormatExpression() default "";
}
