package org.springframework.roo.addon.layers.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooService {
	
	public static final String FIND_ALL_METHOD = "findAll";
	public static final String SAVE_METHOD = "save";
	public static final String DOMAIN_TYPES = "domainTypes";
	
	Class<?>[] domainTypes();

	String findAllMethod() default FIND_ALL_METHOD;
	
	String saveMethod() default SAVE_METHOD;
	
	boolean transactional() default true;
}
