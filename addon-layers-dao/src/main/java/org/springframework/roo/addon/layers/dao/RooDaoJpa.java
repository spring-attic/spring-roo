package org.springframework.roo.addon.layers.dao;

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
public @interface RooDaoJpa {
	
	public static final String REMOVE_METHOD = "remove";
	public static final String DOMAIN_TYPES = "domainTypes";
	
	Class<?>[] domainTypes();
	
	String removeMethod() default REMOVE_METHOD;

}
