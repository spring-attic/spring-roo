package org.springframework.roo.addon.jpa.activerecord;

import static org.springframework.roo.addon.jpa.entity.RooJpaEntity.ID_FIELD_DEFAULT;
import static org.springframework.roo.addon.jpa.entity.RooJpaEntity.VERSION_COLUMN_DEFAULT;
import static org.springframework.roo.addon.jpa.entity.RooJpaEntity.VERSION_FIELD_DEFAULT;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.roo.addon.jpa.entity.RooJpaEntity;

/**
 * Provides services related to JPA, as a superset of those provided by
 * {@link RooJpaEntity}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooJpaActiveRecord {

    String CLEAR_METHOD_DEFAULT = "clear";
    String COUNT_METHOD_DEFAULT = "count";
    String FIND_ALL_METHOD_DEFAULT = "findAll";
    String FIND_ENTRIES_METHOD_DEFAULT = "find";
    String FIND_METHOD_DEFAULT = "find";
    String FLUSH_METHOD_DEFAULT = "flush";
    String MERGE_METHOD_DEFAULT = "merge";
    String PERSIST_METHOD_DEFAULT = "persist";
    String REMOVE_METHOD_DEFAULT = "remove";

    /**
     * Specifies the database catalog name that should be used for the entity.
     * 
     * @return the name of the catalog to use (defaults to "")
     */
    String catalog() default "";

    /**
     * @return the name of the "clear" method to generate (defaults to
     *         {@value #CLEAR_METHOD_DEFAULT}; mandatory)
     */
    String clearMethod() default CLEAR_METHOD_DEFAULT;

    /**
     * @return the prefix of the "count" method to generate (defaults to
     *         {@value #COUNT_METHOD_DEFAULT}, with the plural of the entity
     *         appended after the specified method name; mandatory)
     */
    String countMethod() default COUNT_METHOD_DEFAULT;

    /**
     * Specifies the name used to refer to the entity in queries.
     * <p>
     * The name must not be a reserved literal in JPQL.
     * 
     * @return the name given to the entity (defaults to "")
     */
    String entityName() default "";

    /**
     * @return the prefix of the "findAll" method to generate (defaults to
     *         {@value #FIND_ALL_METHOD_DEFAULT}, with the plural of the entity
     *         appended after the specified method name; if empty, does not
     *         create a "find all" method)
     */
    String findAllMethod() default FIND_ALL_METHOD_DEFAULT;

    /**
     * @return the prefix of the "find[Name]Entries" method to generate
     *         (defaults to {@value #FIND_ENTRIES_METHOD_DEFAULT}, with the
     *         simple name of the entity appended after the specified method
     *         name, followed by "Entries"; mandatory)
     */
    String findEntriesMethod() default FIND_ENTRIES_METHOD_DEFAULT;

    /**
     * @return an array of strings, with each string being the full name of a
     *         method that should be created as a "dynamic finder" by an
     *         additional add-on that can provide implementations of such
     *         methods (optional)
     */
    String[] finders() default "";

    /**
     * @return the prefix of the "find" (by identifier) method to generate
     *         (defaults to {@value #FIND_METHOD_DEFAULT}, with the simple name
     *         of the entity appended after the specified method name;
     *         mandatory)
     */
    String findMethod() default FIND_METHOD_DEFAULT;

    /**
     * @return the name of the "flush" method to generate (defaults to
     *         {@value #FLUSH_METHOD_DEFAULT}; mandatory)
     */
    String flushMethod() default FLUSH_METHOD_DEFAULT;

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
     * in a superclass (either written in normal Java source or introduced by a
     * superclass that has the {@link RooJpaActiveRecord} or
     * {@link RooJpaEntity} annotation.
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
     * @return the name of the "merge" method to generate (defaults to
     *         {@value #MERGE_METHOD_DEFAULT}; mandatory)
     */
    String mergeMethod() default MERGE_METHOD_DEFAULT;

    /**
     * @return the name of the persistence unit defined in the persistence.xml
     *         file (optional)
     */
    String persistenceUnit() default "";

    /**
     * @return the name of the "persist" method to generate (defaults to
     *         {@value #PERSIST_METHOD_DEFAULT}; mandatory)
     */
    String persistMethod() default PERSIST_METHOD_DEFAULT;

    /**
     * @return the name of the "remove" method to generate (defaults to
     *         {@value #REMOVE_METHOD_DEFAULT}; mandatory)
     */
    String removeMethod() default REMOVE_METHOD_DEFAULT;

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
     * @return the name of the sequence
     */
    String sequenceName() default "";

    /**
     * Specifies the table name that should be used for the entity.
     * 
     * @return the name of the table to use (defaults to "")
     */
    String table() default "";

    /**
     * @return the name of the transaction manager (optional)
     */
    String transactionManager() default "";

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
     * JPA @Version field annotation on a superclass (either written in normal
     * Java source or introduced by a superclass that uses the
     * {@link RooJpaActiveRecord} or {@link RooJpaEntity} annotation. The
     * produced field will be of the type specified by {@link #versionType()}.
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
