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
	 * The default prefix of the "count all" method
	 */
	public static final String COUNT_ALL_METHOD = "countAll";
	
	/**
	 * The default name of the "delete" method
	 */
	public static final String DELETE_METHOD = "delete";
	
	/**
	 * The default prefix of the "find all" method
	 */
	public static final String FIND_ALL_METHOD = "findAll";
	
	/**
	 * The default prefix of the "find entries" method
	 */
	public static final String FIND_ENTRIES_METHOD = "find";
	
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
	 * Returns the prefix of the "count all" method
	 * 
	 * @return a blank string if the annotated type doesn't support this method
	 */
	String countAllMethod() default COUNT_ALL_METHOD;
	
	/**
	 * Returns the name of the "delete" method
	 * 
	 * @return a blank string if the annotated type doesn't support this method
	 */
	String deleteMethod() default DELETE_METHOD;
	
	/**
	 * Returns the name of the "find all" method
	 * 
	 * @return a blank string if the annotated type doesn't support this method
	 */
	String findAllMethod() default FIND_ALL_METHOD;

	/**
	 * Returns the prefix of the "findFooEntries" method
	 * 
	 * @return a blank string if the annotated type doesn't support this method
	 */
	String findEntriesMethod() default FIND_ENTRIES_METHOD;
	
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
