package org.springframework.roo.addon.layers.repository;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public @interface RooRepositoryJpa {
	
	public static final String REMOVE_METHOD = "remove";
	public static final String DOMAIN_TYPE = "domainType";
	
	Class<?> domainType();
	
	String removeMethod() default REMOVE_METHOD;

}
