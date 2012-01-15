package org.springframework.roo.addon.jpa.entity;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord;

/**
 * Indicates a type that is a JPA entity. Created to reduce the number of
 * concerns managed by {@link RooJpaActiveRecord}.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooJpaEntity {

    String ID_FIELD_DEFAULT = "id";
    String VERSION_COLUMN_DEFAULT = "version";
    String VERSION_FIELD_DEFAULT = "version";

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
     * Specifies the column name that should be used for the identifier field.
     * By default this is generally made identical to the
     * {@link #identifierField()}, although it will be made unique as required
     * for the particular entity fields present.
     * 
     * @return the name of the identifier column to use (defaults to ""; in this
     *         case it is automatic)
     */
    String identifierColumn() default "";

    /**
     * Creates an identifier, unless there is already a JPA @Id field annotation
     * in a superclass (either written in normal Java source ,or introduced by a
     * superclass that is annotated with either {@link RooJpaActiveRecord} or
     * {@link RooJpaEntity}.
     * <p>
     * If you annotate a field with JPA's @Id annotation, it is required that
     * you provide a public accessor for that field.
     * 
     * @return the name of the identifier field to use (defaults to
     *         {@value #ID_FIELD_DEFAULT}; must be provided)
     */
    String identifierField() default ID_FIELD_DEFAULT;

    /**
     * @return the class of identifier that should be used (defaults to
     *         {@link Long}; must be provided)
     */
    Class<? extends Serializable> identifierType() default Long.class;

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
     * Specifies the name of the sequence to use for incrementing
     * sequence-driven primary keys.
     * 
     * @return the name of the sequence (defaults to "")
     */
    String sequenceName() default "";

    /**
     * Specifies the table name that should be used for the entity.
     * 
     * @return the name of the table to use (defaults to "")
     */
    String table() default "";

    /**
     * Specifies the column name that should be used for the version field. By
     * default this is generally made identical to the {@link #versionField()},
     * although it will be made unique as required for the particular entity
     * fields present.
     * 
     * @return the name of the version column to use (defaults to
     *         {@value #VERSION_COLUMN_DEFAULT}; in this case it is automatic)
     */
    String versionColumn() default VERSION_COLUMN_DEFAULT;

    /**
     * Creates an optimistic locking version field, unless there is already a
     * JPA @Version field annotation in a superclass (either written in normal
     * Java source, or introduced by a superclass annotated with
     * {@link RooJpaActiveRecord} or {@link RooJpaEntity}. The produced field
     * will be of the type specified by {@link #versionType()}.
     * <p>
     * If you annotate a field with JPA's @Version annotation, it is required
     * that you provide a public accessor for that field.
     * 
     * @return the name of the version field to use (defaults to
     *         {@value #VERSION_FIELD_DEFAULT}; must be provided)
     */
    String versionField() default VERSION_FIELD_DEFAULT;

    /**
     * @return the class of version that should be used (defaults to
     *         {@link Integer}; must be provided)
     */
    Class<? extends Serializable> versionType() default Integer.class;
}
