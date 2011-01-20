package org.springframework.roo.addon.entity;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides services related to JPA.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooEntity {
	
	/**
	 * 
	 * @return the class of identifier that should be used (defaults to {@link Long}; must be provided)
	 */
	Class<? extends Serializable> identifierType() default Long.class;
	
	/**
	 * Creates an identifier, unless there is already a JPA @Id field annotation in a superclass
	 * (either written in normal Java source or introduced by a superclass that uses the {@link RooEntity}
	 * annotation. 
	 * 
	 * <p>
	 * If you annotate a field with JPA's @Id annotation, it is required that you provide a public accessor
	 * for that field.
	 * 
	 * @return the name of the identifier field to use (defaults to "id"; must be provided)
	 */
	String identifierField() default "id";
	
	/**
	 * Specifies the column name that should be used for the identifier field. By default this is generally
	 * made identical to the {@link #identifierField()}, although it will be made unique as required for
	 * the particular entity fields present.
	 * 
	 * @return the name of the identifier column to use (default to ""; in this case it is automatic)
	 */
	String identifierColumn() default "";
	
	/**
	 * Creates an optimistic locking version field, unless there is already a JPA @Version field annotation
	 * in a superclass (either written in normal Java source or introduced by a superclass that uses the
	 * {@link RooEntity} annotation. The produced field will be of the type specified by {@link #versionType()}.
	 * 
	 * <p>
	 * If you annotate a field with JPA's @Version annotation, it is required that you provide a public accessor
	 * for that field.
	 * 
	 * @return the name of the version field to use (defaults to "version"; must be provided)
	 */
	String versionField() default "version";
	
	/**
	 * Specifies the column name that should be used for the version field. By default this is generally
	 * made identical to the {@link #versionField()}, although it will be made unique as required for
	 * the particular entity fields present.
	 * 
	 * @return the name of the version column to use (default to "version"; in this case it is automatic)
	 */
	String versionColumn() default "version";

	/**
	 * @return the class of version that should be used (defaults to {@link Integer}; must be provided)
	 */
	Class<? extends Serializable> versionType() default Integer.class;

	/**
	 * @return the name of the "persist" method to generate (defaults to "persist"; mandatory)
	 */
	String persistMethod() default "persist";

	/**
	 * @return the name of the "flush" method to generate (defaults to "flush"; mandatory)
	 */
	String flushMethod() default "flush";

	/**
	 * @return the name of the "clear" method to generate (defaults to "clear"; mandatory)
	 */
	String clearMethod() default "clear";

	/**
	 * @return the name of the "merge" method to generate (defaults to "merge"; mandatory)
	 */
	String mergeMethod() default "merge";

	/**
	 * @return the name of the "remove" method to generate (defaults to "remove"; mandatory)
	 */
	String removeMethod() default "remove";

	/**
	 * @return the prefix of the "count" method to generate (defaults to "count", with the plural of the entity appended
	 * after the specified method name; mandatory)
	 */
	String countMethod() default "count";

	/**
	 * @return the prefix of the "findAll" method to generate (defaults to "findAll", with the plural of the entity appended
	 * after the specified method name; if empty, does not create a "find all" method)
	 */
	String findAllMethod() default "findAll";

	/**
	 * @return the prefix of the "find" (by identifier) method to generate (defaults to "find", with the simple name of the entity appended
	 * after the specified method name; mandatory)
	 */
	String findMethod() default "find";
	
	/**
	 * @return the prefix of the "find[Name]Entries" method to generate (defaults to "find", with the simple name of the entity appended
	 * after the specified method name, followed by "Entries"; mandatory)
	 */
	String findEntriesMethod() default "find";
	
	/**
	 * @return an array of strings, with each string being the full name of a method that should be created as a "dynamic finder" by
	 * an additional add-on that can provide implementations of such methods (optional)
	 */
	String[] finders() default "";
	
	/**
	 * @return the name of the persistence unit defined in the persistence.xml file (optional)
	 */
	String persistenceUnit() default "";
	
	/**
	 * @return whether to generated a @MappedSuperclass type annotation instead of @Entity (defaults to false).
	 */
	boolean mappedSuperclass() default false;

	/**
	 * Specifies the table name that should be used for the entity. 
	 * 
	 * @return the name of the table to use (default to "")
	 */
	String table() default "";
	
	/**
	 * Specifies the database schema name that should be used for the entity. 
	 * 
	 * @return the name of the schema to use (default to "")
	 */
	String schema() default "";
	
	/**
	 * Specifies the database catalog name that should be used for the entity. 
	 * 
	 * @return the name of the catalog to use (default to "")
	 */
	String catalog() default "";
	
	/**
	 * Specifies the JPA inheritance type that should be used for the entity. 
	 * 
	 * @return the inheritance type to use (default to "")
	 */
	String inheritanceType() default "";
}
