package org.springframework.roo.addon.layers.dao;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public @interface RooDaoJpa {
	
	public static final String REMOVE_METHOD = "remove";
	public static final String DOMAIN_TYPES = "domainTypes";
	
	Class<?>[] domainTypes();
	
	String removeMethod() default REMOVE_METHOD;

}
