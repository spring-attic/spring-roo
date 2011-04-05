package org.springframework.roo.addon.jsf;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  Indicates a type that requires ROO JSF managed-bean support.
 *  
 *  @author Alan Stewart
 * 	@since 1.2.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooJsfManagedBean {
	
	Class<?> entity();
}

