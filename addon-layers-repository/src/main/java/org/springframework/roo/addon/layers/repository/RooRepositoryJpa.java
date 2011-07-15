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
	
	public static final String FIND_ALL_METHOD = "findAll";
	public static final String SAVE_METHOD = "save";
	public static final String UPDATE_METHOD = SAVE_METHOD;
	public static final String DOMAIN_TYPE_ATTRIBUTE = "domainType";
	
	Class<?> domainType();
	
	String findAllMethod() default FIND_ALL_METHOD;
	
	String saveMethod() default SAVE_METHOD;
	
	String updateMethod() default UPDATE_METHOD;

}
