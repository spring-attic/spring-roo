package org.springframework.roo.addon.entity;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.roo.addon.beaninfo.RooBeanInfo;

/**
 * Provides services related to JPA.
 * 
 * <p>
 * Using this annotation also triggers {@link RooBeanInfo}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooEntity {
	
	/**
	 * @return the class of identifier that should be used (defaults to {@link Long}; must be provided)
	 */
	Class<? extends Serializable> identifierType() default Long.class;
	
	/**
	 * Creates an identifier, unless there is already a JPA @Id field annotation in a superclass
	 * (either written in normal Java source or introduced by a superclass that uses the {@link RooEntity}
	 * annotation. If you annotate a field with @Id, it is required that you provide a public accessor
	 * in the same type as you declared that field.
	 * 
	 * @return the name of the identifier field to use (defaults to "id"; must be provided)
	 */
	String identifierField() default "id";
	
	/**
	 * Indicates the name of the identifier column, if it will be created.
	 * 
	 * @return the name of the identifier column (defaults to "id"; must be provided)
	 */
	String identifierColumn() default "id";
	
	/**
	 * Creates an optimistic locking version field, unless there is already a JPA @Version field annotation
	 * in a superclass (either written in normal Java source or introduced by a superclass that uses the
	 * {@link RooEntity} annotation. The produced field will be a primitive integer and have a colum name
	 * of "version".
	 * 
	 * <p>
	 * If you provide a @Version field, it is required that you provide a public accessor in the same
	 * type as you declared the field.
	 * 
	 * @return true if an integer-based version should be added automatically for optimistic locking (defaults to true)
	 */
	boolean version() default true;
	
	/**
	 * @return the name of the "persist" method to generate (defaults to "persist"; mandatory)
	 */
	String persistMethod() default "persist";

	/**
	 * @return the name of the "flush" method to generate (defaults to "flush"; mandatory)
	 */
	String flushMethod() default "flush";

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
}
