
package org.springframework.roo.addon.plural;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides the plural of a particular class.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooPlural {

	/**
	 * @return the plural name to use when working with this object (defaults to an empty string, which means to compute
	 * dynamically)
	 */
	String value() default "";
	
}
