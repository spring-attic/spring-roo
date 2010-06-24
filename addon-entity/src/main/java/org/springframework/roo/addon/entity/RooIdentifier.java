package org.springframework.roo.addon.entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.roo.addon.serializable.RooSerializable;

/**
 * Provides services related to JPA.
 * 
 * <p>
 * Using this annotation also triggers {@link RooSerializable}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooIdentifier {
	
	/**
	 * @return whether to generate getters for each non-transient field declared in this class (defaults to true)
	 */
	boolean gettersByDefault() default true;

	/**
	 * @return whether to generate setters for each non-transient field declared in this class (defaults to true)
	 */
	boolean settersByDefault() default true;
	
	/**
	 * @return an array of strings, with each string being the name of a composite primary key field
	 */
	String[] idFields() default "";
	
	/**
	 * @return an array of classes, with each member being the class of a composite primary key field
	 */
	String[] idTypes() default "";
	
	/**
	 * @return an array of strings, with each string being the column name representing the composite primary key field
	 */
	String[] idColumns() default "";
}
