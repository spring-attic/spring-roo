package org.springframework.roo.addon.dod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates to produce a "data on demand" class, which is required for automated integration testing.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooDataOnDemand {

	/**
	 * @return the type of class that will have data on demand created (required; must offer entity services)
	 */
	Class<?> entity();
	
	/**
	 * @return the number of entities to create (required; defaults to 10)
	 */
	int quantity() default 10;

}
