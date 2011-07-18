package org.springframework.roo.addon.layers.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indicating a service interface in a user project
 * 
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooService {
	
	/**
	 * The name of this annotation's "domain types" attribute
	 */
	public static final String DOMAIN_TYPES_ATTRIBUTE = "domainTypes";

	/**
	 * The default name of the "find all" method
	 */
	public static final String FIND_ALL_METHOD = "findAll";
	
	/**
	 * The default name of the "save" method
	 */
	public static final String SAVE_METHOD = "save";
	
	/**
	 * The default name of the "update" method
	 */
	public static final String UPDATE_METHOD = "update";
	
	/**
	 * Returns the domain type(s) managed by this service
	 * 
	 * @return a non-<code>null</code> array
	 */
	Class<?>[] domainTypes();

	/**
	 * Returns the name of the "find all" method
	 * 
	 * @return a blank string if the annotated type doesn't support this method
	 */
	String findAllMethod() default FIND_ALL_METHOD;
	
	/**
	 * Returns the name of the "save" method
	 * 
	 * @return a blank string if the annotated type doesn't support this method
	 */
	String saveMethod() default SAVE_METHOD;
	
	/**
	 * Returns the name of the "update" method
	 * 
	 * @return a blank string if the annotated type doesn't support this method
	 */
	String updateMethod() default UPDATE_METHOD;
	
	/**
	 * Indicates whether the annotated service should be transactional
	 * 
	 * @return see above
	 */
	boolean transactional() default true;
}
