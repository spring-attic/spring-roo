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
 * Using this annotation also triggers {@link RooBeanInfo} and {@link RooSerializable}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooIdentifier {
	
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

}
