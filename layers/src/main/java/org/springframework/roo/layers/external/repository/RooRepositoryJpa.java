package org.springframework.roo.layers.external.repository;

public @interface RooRepositoryJpa {
	
	public static final String REMOVE_METHOD = "remove";
	public static final String DOMAIN_TYPE = "domainType";
	
	Class<?> domainType();
	
	String removeMethod() default REMOVE_METHOD;

}
