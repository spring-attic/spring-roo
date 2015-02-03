package org.springframework.roo.addon.solr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds a Pojo to expose controller method 'search' and 'autoComplete'
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooSolrWebSearchable {

    /**
     * Specify name of the "autoComplete" method to generate. Use a value of ""
     * to avoid the generation of a autoComplete method.
     * 
     * @return the name of the "search" method to generate (defaults to
     *         "search"; mandatory)
     */
    String autoCompleteMethod() default "autoComplete";

    /**
     * Specify name of the "search" method to generate. Use a value of "" to
     * avoid the generation of a search method.
     * 
     * @return the name of the "search" method to generate (defaults to
     *         "search"; mandatory)
     */
    String searchMethod() default "search";
}
