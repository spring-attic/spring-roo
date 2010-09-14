package org.springframework.roo.addon.solr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds a Pojo to a Solr managed search index
 *  
 * @author Stefan Schmidt
 * @since 1.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooSolrSearchable {
	
	/**
	 * Specify name of the "search" method to generate. Use a value of "" to avoid the generation 
	 * of a search method.
	 * 
	 * @return the name of the "search" method to generate (defaults to "search"; mandatory)
	 */
	String searchMethod() default "search";
	
	/**
	 * Specify name of the "postPersistOrUpdate" method to generate. Use a value of "" to avoid the generation 
	 * of a postPersistOrUpdate method.
	 * 
	 * @return the name of the "postPersistOrUpdate" method to generate (defaults to "postPersistOrUpdate"; mandatory)
	 */
	String postPersistOrUpdateMethod() default "postPersistOrUpdate";
	
	/**
	 * Specify name of the "search" method to generate. Use a value of "" to avoid the generation 
	 * of a search method.
	 * 
	 * @return the name of the "search" method to generate (defaults to "search"; mandatory)
	 */
	String simpleSearchMethod() default "search";
	
	/**
	 * Specify name of the "preRemove" method to generate. Use a value of "" to avoid the generation 
	 * of a preRemove method.
	 * 
	 * @return the name of the "preRemove" method to generate (defaults to "preRemove"; mandatory)
	 */
	String preRemoveMethod() default "preRemove";
	
	/**
	 * Specify name of the "index" methods to generate. Use a value of "" to avoid the generation 
	 * of both index methods. The method name will be concatenated by the simple name of the entity type 
	 * (ie: indexOwner)
	 * 
	 * @return the name of the "index" method to generate (defaults to "index"; mandatory)
	 */
	String indexMethod() default "index";
	
	/**
	 * Specify name of the "deleteIndex" methods to generate. Use a value of "" to avoid the generation 
	 * of the deleteIndex method. 
	 * 
	 * @return the name of the "deleteIndex" method to generate (defaults to "deleteIndex"; mandatory)
	 */
	String deleteIndexMethod() default "deleteIndex";
}
