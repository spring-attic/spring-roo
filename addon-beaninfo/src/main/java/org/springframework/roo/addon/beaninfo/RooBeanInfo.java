
package org.springframework.roo.addon.beaninfo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides basic information about JavaBeans.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooBeanInfo {

	/**
	 * @return the plural name to use when working with this object (defaults to an empty string, which means to compute
	 * dynamically)
	 */
	String plural() default "";
	
}
