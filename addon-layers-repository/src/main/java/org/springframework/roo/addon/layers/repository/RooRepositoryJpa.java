package org.springframework.roo.addon.layers.repository;

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
public @interface RooRepositoryJpa {
	
	public static final String FIND_ALL = "findAll";
	public static final String DOMAIN_TYPE = "domainType";
	
	Class<?> domainType();
	
	String getFindAllMethod() default FIND_ALL;

}
