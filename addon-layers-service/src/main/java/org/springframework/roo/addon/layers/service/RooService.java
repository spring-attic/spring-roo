package org.springframework.roo.addon.layers.service;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public @interface RooService {
	public static final String DOMAIN_TYPES = "domainTypes";
	
	Class<?>[] domainTypes();

	String findAllMethod() default "findAll";
}
